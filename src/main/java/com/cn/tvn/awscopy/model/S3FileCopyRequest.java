package com.cn.tvn.awscopy.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class S3FileCopyRequest {
    private String sourceBucket;
    private String sourceKey;
    private String destinationBucket;
    private String destinationKey;
}
