package com.cn.tvn.awscopy.service.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.cn.tvn.awscopy.model.S3FileCopyRequest;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Log
@Service("s3FileCopyMultipartService")
public class S3FileCopyMultipartService implements IS3CopyService {
    private final AmazonS3 s3;

    @Autowired
    public S3FileCopyMultipartService(AmazonS3 s3) {
        this.s3 = s3;
    }

    protected long getObjectSize(String bucket, String key) throws Exception {
        return s3.getObjectMetadata(bucket, key).getContentLength();
    }

    protected boolean multipartCopy(String sourceBucket,
                                    String sourceKey,
                                    String destBucket,
                                    String destKey) {
        var initRequest = new InitiateMultipartUploadRequest(destBucket, destKey);
        var initResult = s3.initiateMultipartUpload(initRequest);

        try {
            long objectSize = getObjectSize(sourceBucket, sourceKey);
            long partSize = 5 * 1024 * 1024; // 5 MB is the minimum for S3!!!
            long bytePosition = 0;
            int partNum = 1;
            List<CopyPartResult> copyResponses = new ArrayList<>();
            while (bytePosition < objectSize) {
                long lastByte = Math.min(bytePosition + partSize - 1, objectSize - 1);
                CopyPartRequest copyRequest = new CopyPartRequest()
                        .withSourceBucketName(sourceBucket)
                        .withSourceKey(sourceKey)
                        .withDestinationBucketName(destBucket)
                        .withDestinationKey(destKey)
                        .withUploadId(initResult.getUploadId())
                        .withFirstByte(bytePosition)
                        .withLastByte(lastByte)
                        .withPartNumber(partNum++);
                copyResponses.add(s3.copyPart(copyRequest));
                bytePosition += partSize;
            }

            s3.completeMultipartUpload(new CompleteMultipartUploadRequest(
                    destBucket,
                    destKey,
                    initResult != null ? initResult.getUploadId() : "",
                    getETags(copyResponses)));

            log.info("[multipart] Copy complete: " + sourceBucket + "/" + sourceKey + " -> " + destBucket + "/" + destKey);

            return true;
        } catch (Exception e) {
            log.severe("[multipart] Copy failed: " + sourceBucket + "/" + sourceKey + " -> " + destBucket + "/" + destKey);
            log.severe(e.getMessage());

            s3.abortMultipartUpload(new AbortMultipartUploadRequest(
                    destBucket,
                    destKey,
                    initResult != null ? initResult.getUploadId() : ""));

            log.severe("[multipart] Aborted: " + sourceBucket + "/" + sourceKey + " -> " + destBucket + "/" + destKey);

            return false;
        }
    }

    private static List<PartETag> getETags(List<CopyPartResult> responses) {
        return responses.stream()
                 .map(response -> new PartETag(response.getPartNumber(), response.getETag()))
                 .collect(Collectors.toList());
    }

    @Override
    public String copyObject(S3FileCopyRequest request) throws Exception {
        copyFiles(List.of(request)).join();
        return "OK";
    }

    @Async
    protected CompletableFuture<Void> copyFiles(List<S3FileCopyRequest> requests) {
        var futures = requests.stream()
                .map(this::copyFile)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    private CompletableFuture<Void> copyFile(S3FileCopyRequest request) {
        log.info("[multipart] Copying: " + request.toString());

        multipartCopy(
                request.getSourceBucket(),
                request.getSourceKey(),
                request.getDestinationBucket(),
                request.getDestinationKey());

        return CompletableFuture.completedFuture(null);
    }
}