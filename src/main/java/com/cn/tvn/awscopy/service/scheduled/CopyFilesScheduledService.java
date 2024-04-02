package com.cn.tvn.awscopy.service.scheduled;

import com.cn.tvn.awscopy.model.FileToCopy;
import com.cn.tvn.awscopy.model.ListToCopy;
import com.cn.tvn.awscopy.model.ObjectToCopy;
import com.cn.tvn.awscopy.model.status.ListStatus;
import com.cn.tvn.awscopy.model.status.ObjectStatus;
import com.cn.tvn.awscopy.model.status.FileStatus;
import com.cn.tvn.awscopy.service.FileToCopyService;
import com.cn.tvn.awscopy.service.ListToCopyService;
import com.cn.tvn.awscopy.service.ObjectToCopyService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Log
@Service
public class CopyFilesScheduledService {
    @Autowired
    private ListToCopyService listToCopyService;

    @Autowired
    private ObjectToCopyService objectToCopyService;

    @Autowired
    private FileToCopyService fileToCopyService;

    @Scheduled(fixedRate = 500, initialDelay = 250)
    public void copy() {
        var list = getNextListToCopy(true);
        if (list == null) {
//            log.finest("No lists to copy.");
            return;
        }

        saveListStatus(list, ListStatus.COPYING);

        var object = getNextObjectToCopy(list, true);

        if (isListCancelled(list)) return;

        if (object == null) {
            log.info("No objects to copy in list " + list.getId());
            saveListCopyFinish(list);
            return;
        }

        log.fine("Processing object to copy: " + object.getId() + " from list " + list.getId());

        saveObjectCopyStart(object);

        var file = getNextFileToCopy(object);

        if (isListCancelled(list)) return;

        if (file != null) {
            try {
                log.info("Copying file " + file);

                saveFileCopyStart(file);

                fileToCopyService.copy(file);

                saveFileCopyFinish(file, FileStatus.COPIED_OK);

                log.info("File copied: " + file);
            } catch (Exception e) {
                log.severe("Error copying file " + file);
                log.severe("Exception: " + e);
                saveFileCopyFinish(file, FileStatus.COPIED_FAILED);
            }
        } else {
            log.info("No files to copy in object " + object.getId() + " from list " + list.getId());
        }

        if (isListCancelled(list)) return;

        if (null == getNextFileToCopy(object)) {
            log.info("No more files to copy in object" + object.getId() + " from list " + list.getId());
            saveObjectCopyFinish(object);

            if (isListCancelled(list)) return;

            if (null == getNextObjectToCopy(list, false)) {
                log.info("No more objects to copy in this list " + list.getId());
                saveListCopyFinish(list);
            }
        }
    }

    private boolean isListCancelled(ListToCopy list) {
        if (listToCopyService.isCancelled(list.getId())) {
            log.info("List " + list.getId() + " is cancelled");
            return true;
        }
        return false;
    }

    private void saveFileCopyStart(FileToCopy file) {
        file.setStatus(FileStatus.COPYING);
        file.setCopyStartTimestamp(Instant.now());
        fileToCopyService.save(file);
    }

    private void saveFileCopyFinish(FileToCopy file, FileStatus status) {
        file.setCopyFinishTimestamp(Instant.now());
        file.setStatus(status);
        fileToCopyService.save(file);
    }

    private void saveObjectCopyStart(ObjectToCopy object) {
        if (object.getStatus() != ObjectStatus.COPYING) {
            object.setStatus(ObjectStatus.COPYING);
            object.setCopyStartTimestamp(Instant.now());
            objectToCopyService.save(object);
        }
    }

    private void saveListStatus(ListToCopy list, ListStatus status) {
        if (list.getStatus() != status) {
            list.setStatus(status);
            listToCopyService.save(list);
        }
    }

    private void saveObjectCopyFinish(ObjectToCopy object) {
        int notOK = fileToCopyService.countAllWithParentObject(object)
                    - fileToCopyService.countAllWithParentObjectAndStatus(object, FileStatus.COPIED_OK);
        object.setCopyFinishTimestamp(Instant.now());
        object.setStatus(notOK > 0 ? ObjectStatus.COPIED_FAILED : ObjectStatus.COPIED_OK);
        objectToCopyService.save(object);
    }

    private void saveListCopyFinish(ListToCopy list) {
        int notOK = objectToCopyService.countAllWithParentList(list)
                    - objectToCopyService.countAllWithParentListAndStatus(list, ObjectStatus.COPIED_OK);
        list.setStatus(notOK > 0 ? ListStatus.COPIED_FAILED : ListStatus.COPIED_OK);
        listToCopyService.save(list);
    }

    private FileToCopy getNextFileToCopy(ObjectToCopy object) {
        return fileToCopyService.findFirstWithParentObjectAndStatus(object, FileStatus.PENDING);
    }

    private ObjectToCopy getNextObjectToCopy(ListToCopy list, boolean includeCurrent) {
        var object = includeCurrent
                ? objectToCopyService.findFirstWithParentListAndStatus(list, ObjectStatus.COPYING)
                : null;
        return object == null
                ? objectToCopyService.findFirstWithParentListAndStatus(list, ObjectStatus.LISTED_OK)
                : object;
    }

    private ListToCopy getNextListToCopy(boolean includeCurrent) {
        var list = includeCurrent
                ? listToCopyService.findFirstWithStatus(ListStatus.COPYING)
                : null;
        return list == null
                ? listToCopyService.findFirstWithStatus(ListStatus.LISTED_OK)
                : list;
    }
}
