package io.github.liangxin233666.mfl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 这个Record会把 application.properties 中所有以 app.storage 开头的配置项映射进来
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String region,
        String bucketName,
        String publicBaseUrl,
        long presignUrlDurationMinutes
) {}