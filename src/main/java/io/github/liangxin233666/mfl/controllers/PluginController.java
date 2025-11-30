package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.PluginDTOs.*;
import io.github.liangxin233666.mfl.services.FileStorageService;
import io.github.liangxin233666.mfl.services.PluginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plugins")
@RequiredArgsConstructor
public class PluginController {

    private final PluginService pluginService;
    private final FileStorageService fileStorageService;

    // 1. 获取上传 URL (为了上传 .js / .zip 插件文件)
    // 前端调用: POST /api/plugins/upload-url?filename=my-plugin.js&contentType=application/javascript
    @GetMapping("/upload-url")
    public ResponseEntity<String> getUploadUrl(
            @RequestParam String filename,
            @RequestParam String contentType) {
        // 这里 contentType 很重要：
        // 如果是 JS 插件，前端传 application/javascript
        // 如果是 ZIP 包，前端传 application/zip
        return ResponseEntity.ok(fileStorageService.generatePresignedUploadUrl(filename, contentType));
    }

    // 2. 发布插件 (管理员或开发者)
    @PostMapping
    public ResponseEntity<PluginResponse> publishPlugin(@RequestBody CreatePluginRequest request) {
        return ResponseEntity.ok(pluginService.publishPlugin(request));
    }

    // 3. 插件列表 (插件市场首页)
    @GetMapping
    public ResponseEntity<List<PluginResponse>> listPlugins() {
        return ResponseEntity.ok(pluginService.getAllPlugins());
    }

    // 4. 下载/安装埋点
    // 前端在下载或启用插件时，可以发个请求过来统计数据
    @PostMapping("/{id}/install")
    public ResponseEntity<Void> recordInstall(@PathVariable Long id) {
        pluginService.incrementDownloadCount(id);
        return ResponseEntity.ok().build();
    }
}