package com.cn.tvn.awscopy.service.s3;

import com.cn.tvn.awscopy.model.S3FileCopyRequest;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

@Log
@Service("s3TransferService")
public class S3TransferService implements IS3CopyService {

    @Autowired
    S3TransferManager transferManager;

    private CompletableFuture<CompletedCopy> cfCopy;

    protected String copy(S3FileCopyRequest request) {
        try {
            log.fine("[tm-copy] Creating copy request: " + request);

            CopyRequest copyRequest = CopyRequest.builder()
                    .copyObjectRequest(createCopyObjectRequest(request))
                    .addTransferListener(new TransferListener() {
                            @Override
                            public void transferInitiated(Context.TransferInitiated context) {
                                log.fine("Managed Transfer initiated: " + context);
                                // No other events will be fired
                                // because we copy between S3 buckets (totally remote)!
                                // https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/transfer/s3/S3TransferManager.html#copy(software.amazon.awssdk.transfer.s3.model.CopyRequest)
                            }
                            @Override
                            public void transferFailed(Context.TransferFailed context) {
                                log.severe("Transfer failed: " + context);
                                log.severe("Exception: " + context.exception());
                            }
                        }
                    )
                    .build();

            log.fine("[tm-copy] Copy request created: " + copyRequest);

            Copy copy = transferManager.copy(copyRequest);

            log.fine("[tm-copy] Copy request started: " + copy);

            cfCopy = copy.completionFuture();
            CompletedCopy completedCopy = cfCopy.join();

            log.fine("[tm-copy] Copy request completed: " + completedCopy);
            log.fine("[tm-copy] ETag: " + completedCopy.response().copyObjectResult().eTag());
            log.fine("[tm-copy] SHA256: " + completedCopy.response().copyObjectResult().checksumSHA256());

            return completedCopy.response().copyObjectResult().eTag();
        } catch (CancellationException e) {
            log.fine("[tm-copy] Copy request cancelled: " + e);
            return null;
        } catch (Exception e) {
            log.severe("[tm-copy] Error during copy: " + e);
            return null;
        }
    }

    @Override
    public void cancel() {
        if (cfCopy != null) {
            cfCopy.cancel(true);
        }
    }

    @Override
    public String copyObject(S3FileCopyRequest request) throws Exception {
        return copy(request);
    }
}
