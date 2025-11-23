package io.github.liangxin233666.mfl.config;

import jakarta.validation.constraints.Min; // 引入验证注解
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated; // 引入 Validated

// 这个Record会把 application.properties 中所有以 app.storage 开头的配置项映射进来
@ConfigurationProperties(prefix = "app.storage")
@Validated // 1. 开启验证功能
public record StorageProperties(
        @NotBlank String endpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String region,
        @NotBlank String bucketName,
        @NotBlank String publicBaseUrl,

        @Min(10) // 2. 确保至少为 1 分钟
        long presignUrlDurationMinutes
) {}