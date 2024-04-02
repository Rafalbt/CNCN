package com.cn.tvn.awscopy;

import com.amazonaws.services.s3.AmazonS3;
import com.cn.tvn.awscopy.model.S3FileCopyRequest;
import com.cn.tvn.awscopy.service.s3.IS3CopyService;
import com.cn.tvn.awscopy.service.s3.S3FileCopyMultipartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

class S3FileCopyMultipartServiceTest {

    @Mock
    private AmazonS3 s3;

    private IS3CopyService s3FileCopyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        s3FileCopyService = new S3FileCopyMultipartService(s3);
    }

    @Test
    void copyObject_ShouldFailCopySingleObject() throws Exception {
        // Arrange
        S3FileCopyRequest request = new S3FileCopyRequest("source1", "destination1", "source2", "destination2");
        // Act
        s3FileCopyService.copyObject(request);

        // Assert
        verify(s3, times(1)).initiateMultipartUpload(any());
        verify(s3, times(1)).abortMultipartUpload(any());
    }
}