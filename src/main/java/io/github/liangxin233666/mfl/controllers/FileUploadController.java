package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.services.FileStorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 第一步：客户端请求一个预签名的URL，用于后续的文件上传。
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<UploadResponse> requestPresignedUrl(@Valid @RequestBody UploadRequest request) {
        // 调用核心服务来生成URL
        String uploadUrl = fileStorageService.generatePresignedUploadUrl(request.fileName(), request.contentType());

        // 将URL返回给客户端
        return ResponseEntity.ok(new UploadResponse(uploadUrl));
    }

    // --- DTOs (Data Transfer Objects) for this controller ---

    // 客户端发来的请求体
    public record UploadRequest(
            @NotBlank(message = "File name cannot be blank")
            String fileName,

            // ContentType很重要，它会告诉S3/OSS这个文件是什么类型
            @NotBlank(message = "Content type cannot be blank")
            String contentType
    ) {}

    // 返回给客户端的响应体
    public record UploadResponse(String uploadUrl) {}
}