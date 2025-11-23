package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    public FileStorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.storageProperties = storageProperties;
    }

    /**
     * 生成一个有时效的、预签名的URL，用于将文件上传到临时目录。
     * @param fileName 原始文件名
     * @param contentType 文件的MIME类型, e.g., "image/jpeg"
     * @return 一个可用于HTTP PUT请求的完整URL
     */
    public String generatePresignedUploadUrl(String fileName, String contentType) {
        // 1. 创建一个唯一的对象键 (在S3/OSS中的文件路径)
        //    格式: uploads/temp/{唯一ID}-{原始文件名}
        //    'uploads/temp/' -> 这个前缀至关重要，用于后续的自动清理
        String objectKey = "uploads/temp/" + UUID.randomUUID() + "-" + fileName;

        // 2. 构建一个S3的PutObject请求描述
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.bucketName())
                .key(objectKey)
                .contentType(contentType) // 告知S3文件类型
                .build();

        // 3. 构建预签名请求的描述
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(storageProperties.presignUrlDurationMinutes()))
                .putObjectRequest(objectRequest)
                .build();

        // 4. 使用S3Presigner生成最终的URL
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }


    public Map<String, String> promoteFiles(List<String> tempUrls) {
        if (tempUrls == null || tempUrls.isEmpty()) {
            return Collections.emptyMap();
        }

        return tempUrls.parallelStream() // 使用并行流提升性能
                .map(this::promoteSingleFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // 处理单个文件的转正逻辑
    private Optional<Map.Entry<String, String>> promoteSingleFile(String tempUrl) {
        if (tempUrl == null || !tempUrl.contains("/uploads/temp/")) {
            // 如果URL为空或不是临时URL，直接忽略或原样返回
            return Optional.ofNullable(tempUrl).map(url -> Map.entry(url, url));
        }

        try {
            String tempKey = extractKeyFromUrl(tempUrl);
            String finalKey = tempKey.replace("uploads/temp/", "uploads/final/");

            // 1. 复制对象到新位置
            CopyObjectRequest copyReq = CopyObjectRequest.builder()
                    .sourceBucket(storageProperties.bucketName())
                    .sourceKey(tempKey)
                    .destinationBucket(storageProperties.bucketName())
                    .destinationKey(finalKey)
                    .build();
            s3Client.copyObject(copyReq);

            // 2. (可选但推荐) 删除原始的临时对象
            DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                    .bucket(storageProperties.bucketName())
                    .key(tempKey)
                    .build();
            s3Client.deleteObject(deleteReq);

            // 3. 构建并返回新的永久公共URL
            String finalUrl = storageProperties.publicBaseUrl() + "/" + finalKey;
            return Optional.of(Map.entry(tempUrl, finalUrl));
        } catch (Exception e) {

            log.error("Failed to promote file: {}", tempUrl, e);
            return Optional.empty(); // 失败的文件将被忽略，后续由生命周期策略清理
        }
    }

    // 从完整的URL中提取出在S3中的对象键(路径)
    private String extractKeyFromUrl(String url) {
        String baseUrl = storageProperties.publicBaseUrl();
        // 处理一下，防止baseUrl末尾有/
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // 1. 截取掉域名部分，得到 encoded 的 key
        String encodedKey = url.substring(baseUrl.length() + 1);

        // 2. 进行 URL 解码，将 %E6... 转换回中文
        try {
            return URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decode URL key: {}", encodedKey, e);
            // 如果解码失败，回退到原始字符串，但这通常不会发生
            return encodedKey;
        }
    }

    @Async // <--- 这个注解是魔法的来源
    public void deleteFilesAsync(List<String> urlsToDelete) {
        if (urlsToDelete == null || urlsToDelete.isEmpty()) {
            return;
        }

        // 我们也并行化这个删除操作
        urlsToDelete.parallelStream().forEach(url -> {
            try {
                String key = extractKeyFromUrl(url);
                // 增加安全校验，防止误删不该删的文件
                if (key.startsWith("uploads/final/")) {
                    DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                            .bucket(storageProperties.bucketName())
                            .key(key)
                            .build();
                    s3Client.deleteObject(deleteReq);
                }
            } catch (Exception e) {
                // 在异步任务中，日志记录至关重要
                log.error("Async deletion failed for url: {}", url, e);
            }
        });
    }
}
