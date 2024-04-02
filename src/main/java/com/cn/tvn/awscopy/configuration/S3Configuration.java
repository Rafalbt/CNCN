package com.cn.tvn.awscopy.configuration;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.crt.S3CrtRetryConfiguration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.time.Duration;
import java.util.function.Consumer;

import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

@Configuration
public class S3Configuration {

    @Value("${aws.s3.region}")
    Region s3Region;

    @Value("${aws.s3.profile}")
    String s3Profile;


    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.builder()
                .profileName(s3Profile)
                .build();
    }

    @Bean
    AmazonS3 createAmazonS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(s3Region.id())
                .withCredentials(new ProfileCredentialsProvider(s3Profile))
                .build();
    }

    @Bean
    S3CrtRetryConfiguration createS3CrtRetryConfiguration() {
        return S3CrtRetryConfiguration.builder()
                .numRetries(1)
                .build();
    }


    @Bean
    S3AsyncClient getS3AsyncClient(AwsCredentialsProvider credentialsProvider, S3CrtRetryConfiguration retryConfiguration) {
        return S3AsyncClient.crtBuilder()
                .credentialsProvider(credentialsProvider)
                .region(s3Region)
                .checksumValidationEnabled(true)
                .retryConfiguration(retryConfiguration)
//                .maxConcurrency(10)
                .targetThroughputInGbps(20.0)
                .minimumPartSizeInBytes(8 * MB)
                .httpConfiguration(
                        httpBuilder -> httpBuilder
                                .connectionTimeout(Duration.ofDays(1))
                                .build()
                )
                .build();
    }

    @Bean
    S3TransferManager createS3TransferManager(S3AsyncClient s3AsyncClient) {
        return S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();
    }
}
