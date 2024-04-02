package com.cn.tvn.awscopy.service;

import com.cn.tvn.awscopy.configuration.S3SyncProperties;
import com.cn.tvn.awscopy.model.FileToCopy;
import com.cn.tvn.awscopy.model.ListToCopy;
import com.cn.tvn.awscopy.model.ObjectToCopy;
import com.cn.tvn.awscopy.model.PrefixedObject;
import com.cn.tvn.awscopy.model.status.FileStatus;
import com.cn.tvn.awscopy.model.status.ObjectStatus;
import com.cn.tvn.awscopy.repository.ObjectToCopyRepository;
import com.cn.tvn.awscopy.service.s3.FindS3FilesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ObjectToCopyService {

    @Autowired
    FileToCopyService fileToCopyService;

    @Autowired
    private ObjectToCopyRepository repository;

    @Autowired
    private FindS3FilesService findFilesService;

    @Autowired
    private S3SyncProperties s3SyncProperties;


    @Value("${s3sync.destBucketForStl}")
    private String destBucketForStl;

    @Value("${s3sync.destBucketForVideo}")
    private String destBucketForVideo;

    @Value("${s3sync.sourceStlPrefix}")
    private String sourceStlPrefix;

    @Value("${s3sync.includedPrefixes}")
    private List<String> includedPrefixes;

    @Value("${s3sync.stlExtensions}")
    private List<String> stlExtensions;

    @Value("${s3sync.orderOfExtensions}")
    private List<String> orderOfExtensions;

    @Value("${s3sync.matroxVideoExtensions}")
    private List<String> matroxVideoExtensions;

    @Value("${s3sync.internetVideoExtensions}")
    private List<String> internetVideoExtensions;

    @Value("${s3sync.uhdVideoExtensions}")
    private List<String> uhdVideoExtensions;

    @Value("${s3sync.audioExtensions}")
    private List<String> audioExtensions;


    public void save(ObjectToCopy objectToCopy) {
        repository.save(objectToCopy);
    }

    public List<ObjectToCopy> saveAll(List<ObjectToCopy> objects) {
        return repository.saveAll(objects);
    }

    public void populateFilesToCopy(ObjectToCopy objectToCopy) {
        List<String> keys = findFilesService.listFilesStartingWith(
                objectToCopy.getSourceBucket(),
                List.of(objectToCopy.getSourcePrefixedObject(),
                        // also find STLs for this object in the sourceStlPrefix
                        new PrefixedObject(
                                sourceStlPrefix,
                                objectToCopy.getSourcePrefixedObject().getObject())));

        keys.removeIf(key -> isFileToIgnore(
                key,
                objectToCopy.getSourcePrefixedObject().getObject()));

        keys.sort((fn1, fn2) -> compareFileNamesUsingExt(fn1, fn2));

        objectToCopy.clearFilesToCopy();
        objectToCopy.addAllFilesToCopy(keys.stream()
                .map(key -> FileToCopy.builder()
                        .sourceBucket(objectToCopy.getSourceBucket())
                        .sourceFile(key)
                        .destBucket(getDestBucket(key))
                        .destFile(getDestFile(
                                key,
                                objectToCopy.getSourcePrefixedObject().getPrefix(),
                                sourceStlPrefix))
                        .status(FileStatus.PENDING)
                        .build())
                .toList());
    }

    private String getDestFile(String destKey, String sourcePrefix, String stlPrefix) {
        final boolean isStl = destKey.toLowerCase().startsWith(stlPrefix);

        String noPrefix = isStl
                ? destKey.substring(stlPrefix.length())
                : destKey.substring(sourcePrefix.length());
        noPrefix = noPrefix.startsWith("/")
                ? noPrefix.substring(1)
                : noPrefix;

        var destPrefix = s3SyncProperties.getDestPrefix(isStl
                ? stlPrefix
                : sourcePrefix.toLowerCase());

        destPrefix = destPrefix != null && !destPrefix.isBlank()
                ? destPrefix + "/" + noPrefix
                : noPrefix;

        return destPrefix.startsWith("/")
                ? destPrefix.substring(1)
                : destPrefix;
    }

    private boolean isFileToIgnore(String fileName, String object) {
        return !isFileToInclude(fileName, object);
    }

    private boolean isFileToInclude(String fileName, String object) {
        var prefix = GetPrefix(fileName);

        if (isPrefixToIgnore(prefix)) return false;

        var videoExts = getValidVideoExtensions(prefix);
        if (isFileWithExt(fileName, videoExts)) return true;

        var audioExts = getValidAudioExtensions(prefix);
        if (isValidAudioFile(fileName, object, audioExts)) return true;

        var stlExts = getValidStlExtensions(prefix);
        return isFileWithExt(fileName, stlExts);
    }

    private boolean isPrefixToIgnore(String prefix) {
        return includedPrefixes.stream().noneMatch(prefix::equalsIgnoreCase);
    }

    private static boolean isFileWithExt(String fileName, List<String> extensions) {
        return extensions.stream().anyMatch(fileName::endsWith);
    }

    private boolean isValidAudioFile(String fileName, String object, List<String> audioExt) {
        return isMainAudio(fileName, object, audioExt);
    }

    private boolean isMainAudio(String fileName, String object, List<String> audioExt) {
        return audioExt.stream().anyMatch(ext -> fileName.endsWith(object + ext));
    }

    private static String GetPrefix(String fileName) {
        return fileName.substring(0, fileName.indexOf("/"));
    }

    private List<String> getValidVideoExtensions(String prefix) {
        switch (prefix) {
            case "won_matrox":
            case "won_matrox_hd":
                return matroxVideoExtensions;
            case "won_internet":
                return internetVideoExtensions;
            case "won_uhd":
                return uhdVideoExtensions;
            case "won_stl":
            default:
                return List.of();
        }
    }

    private List<String> getValidAudioExtensions(String prefix) {
        switch (prefix) {
            case "won_matrox":
            case "won_matrox_hd":
            case "won_internet":
                return audioExtensions;
            case "won_uhd":
            case "won_stl":
            default:
                return List.of();
        }
    }

    private List<String> getValidStlExtensions(String prefix) {
        switch (prefix) {
            case "won_stl":
                return stlExtensions;
            default:
                return List.of();
        }
    }

    private String getDestBucket(String fileName) {
        return isFileWithExt(fileName, stlExtensions)
                ? destBucketForStl
                : destBucketForVideo;
    }

    private int compareFileNamesUsingExt(String fn1, String fn2) {
        fn1 = fn1.toLowerCase();
        fn2 = fn2.toLowerCase();
        String ext1 = fn1.contains(".") ? fn1.substring(fn1.lastIndexOf(".") + 1) : "";
        String ext2 = fn2.contains(".") ? fn2.substring(fn2.lastIndexOf(".") + 1) : "";
        int i1 = orderOfExtensions.indexOf(ext1);
        int i2 = orderOfExtensions.indexOf(ext2);
        if (i1 == -1) i1 = orderOfExtensions.size() + 1;
        if (i2 == -1) i2 = orderOfExtensions.size() + 1;
        return i1 - i2;
    }


    public ObjectToCopy findById(Long objectId) {
        return repository.findById(objectId).orElse(null);
    }

    public List<ObjectToCopy> findAllByParentList(ListToCopy parentList) {
        return repository.findAllByParentListOrderByCreatedAtDesc(parentList);
    }

    public ObjectToCopy findFirstWithParentListAndStatus(ListToCopy list, ObjectStatus objectStatus) {
        return repository.findFirstWithParentListAndStatus(list, objectStatus);
    }

    public int countAllWithParentList(ListToCopy list) {
        return repository.countAllWithParentList(list);
    }

    public void deleteAllByParentList(ListToCopy parentList) {
        fileToCopyService.deleteAllByParentList(parentList);
        repository.deleteAllByParentList(parentList);
    }

    public int countAllWithParentListAndStatus(ListToCopy list, ObjectStatus objectStatus) {
        return repository.countAllWithParentListAndStatus(list, objectStatus);
    }

    public List<ObjectToCopy> findByStatus(ObjectStatus status) {
        return repository.findAllByStatus(status);
    }

    public List<ObjectToCopy> findByParentListAndStatus(ListToCopy parentList, ObjectStatus status) {
        return repository.findAllByParentListAndStatus(parentList, status);
    }
}