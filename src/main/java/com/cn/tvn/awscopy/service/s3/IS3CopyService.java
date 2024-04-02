package com.cn.tvn.awscopy.service.s3;

import com.cn.tvn.awscopy.model.S3FileCopyRequest;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

import java.util.List;

public interface IS3CopyService {
    String copyObject(S3FileCopyRequest request) throws Exception;

    default void cancel() {
        throw new UnsupportedOperationException("Not implemented");
    }

    default CopyObjectRequest createCopyObjectRequest(S3FileCopyRequest request) {
        return CopyObjectRequest.builder()
                .sourceBucket(request.getSourceBucket())
                .sourceKey(request.getSourceKey())
                .destinationBucket(request.getDestinationBucket())
                .destinationKey(request.getDestinationKey())
                .checksumAlgorithm(ChecksumAlgorithm.SHA256)
                .build();
    }
}
