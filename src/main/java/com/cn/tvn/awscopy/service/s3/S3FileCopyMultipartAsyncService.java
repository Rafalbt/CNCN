package com.cn.tvn.awscopy.service.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.cn.tvn.awscopy.model.S3FileCopyRequest;
import jakarta.annotation.PreDestroy;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log
@Service("s3FileCopyMultipartAsyncService")
public class S3FileCopyMultipartAsyncService implements IS3CopyService {

    @Autowired
    private AmazonS3 s3;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @PreDestroy
    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
            AtomicInteger partNum = new AtomicInteger(1);
            List<CompletableFuture<CopyPartResult>> futures = new ArrayList<>();

            while (bytePosition < objectSize) {
                final long finalFirstBytePosition = bytePosition;
                final long finalLastBytePosition = Math.min(bytePosition + partSize - 1, objectSize - 1);
                CompletableFuture<CopyPartResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        CopyPartRequest copyRequest = new CopyPartRequest()
                                .withSourceBucketName(sourceBucket)
                                .withSourceKey(sourceKey)
                                .withDestinationBucketName(destBucket)
                                .withDestinationKey(destKey)
                                .withUploadId(initResult.getUploadId())
                                .withFirstByte(finalFirstBytePosition)
                                .withLastByte(finalLastBytePosition)
                                .withPartNumber(partNum.getAndIncrement());
                        return s3.copyPart(copyRequest);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executorService);
                futures.add(future);
                bytePosition += partSize;
            }

            CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]))
                    .get(); // Wait for all futures to complete

            try {
                List<CopyPartResult> copyResponses = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

                s3.completeMultipartUpload(new CompleteMultipartUploadRequest(
                        destBucket,
                        destKey,
                        initResult != null ? initResult.getUploadId() : "",
                        getETags(copyResponses)));

                log.info("[multipart-async] Copy complete: " + sourceBucket + "/" + sourceKey + " -> " + destBucket + "/" + destKey);

                return true;
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof AmazonS3Exception) {
                    // Handle AmazonS3Exception
                } else {
                    // Handle unexpected exceptions
                }
                throw e;
            }
        } catch (Exception e) {
            log.severe("[multipart-async] Copy failed: " + sourceBucket + "/" + sourceKey + " -> " + destBucket + "/" + destKey);
            log.severe(e.getMessage());

            s3.abortMultipartUpload(new AbortMultipartUploadRequest(
                    destBucket,
                    destKey,
                    initResult != null ? initResult.getUploadId() : ""));

            log.severe("[multipart-async] Aborted: " + sourceBucket + "/" + sourceKey + " -> " + destBucket + "/" + destKey);

            return false;
        }
    }

    protected long getObjectSize(String bucket, String key) throws Exception {
        return s3.getObjectMetadata(bucket, key).getContentLength();
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
        log.info("[multipart-async] Copying: " + request.toString());

        multipartCopy(
                request.getSourceBucket(),
                request.getSourceKey(),
                request.getDestinationBucket(),
                request.getDestinationKey());

        return CompletableFuture.completedFuture(null);
    }
}
