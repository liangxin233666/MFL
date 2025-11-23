package io.github.liangxin233666.mfl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration; // 引入这个类
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(StorageProperties properties) {
        // 配置强制使用路径模式 (Path Style)
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true) // <--- 关键修改：开启路径模式
                .build();

        return S3Client.builder()
                .region(Region.of(properties.region()))
                .endpointOverride(URI.create(properties.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .serviceConfiguration(serviceConfiguration) // <--- 应用配置
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties properties) {
        //同样配置强制使用路径模式
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true) // <--- 关键修改
                .build();

        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .endpointOverride(URI.create(properties.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .serviceConfiguration(serviceConfiguration) // <--- 应用配置
                .build();
    }
}