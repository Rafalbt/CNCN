package com.cn.tvn.awscopy.service.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.cn.tvn.awscopy.model.PrefixedObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class FindS3FilesService {

    @Autowired
    private AmazonS3 s3;

    public List<String> listFilesStartingWith(String bucketName, List<PrefixedObject> prefixedObjectsStarts) {
         return prefixedObjectsStarts.stream()
                 .map(prefix -> listFiles(bucketName, prefix.toString()))
                 .flatMap(List::stream)
                 .collect(Collectors.toList());
    }

    private List<String> listFiles(String bucketName, String prefixedObjectStart) {
        ListObjectsV2Request req = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefixedObjectStart);
        ListObjectsV2Result result;
        List<String> keys = new ArrayList<>();

        do {
            result = s3.listObjectsV2(req);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                keys.add(objectSummary.getKey());   // full key
            }

            String token = result.getNextContinuationToken();
            req.setContinuationToken(token);
        } while (result.isTruncated());

        return keys;
    }
}
