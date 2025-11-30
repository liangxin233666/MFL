package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.dtos.PluginDTOs.*;
import io.github.liangxin233666.mfl.entities.Plugin;
import io.github.liangxin233666.mfl.repositories.PluginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PluginService {

    private final PluginRepository pluginRepository;
    private final FileStorageService fileStorageService; // 复用你的文件服务

    /**
     * 上架新插件
     */
    @Transactional
    public PluginResponse publishPlugin(CreatePluginRequest request) {
        if (pluginRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Plugin with slug '" + request.slug() + "' already exists.");
        }

        // 1. 准备要转正的文件列表 (插件包本体 + 图标)
        List<String> filesToPromote = new ArrayList<>();
        if (request.tempFileUrl() != null) filesToPromote.add(request.tempFileUrl());
        if (request.tempIconUrl() != null) filesToPromote.add(request.tempIconUrl());

        // 2. 调用你的转正逻辑 (Temp -> Final)
        Map<String, String> urlMapping = fileStorageService.promoteFiles(filesToPromote);

        // 3. 构建实体并保存数据库
        Plugin plugin = new Plugin();
        plugin.setName(request.name());
        plugin.setSlug(request.slug()); // 实际要做重名校验
        plugin.setType(request.type());
        plugin.setDescription(request.description());
        plugin.setVersion(request.version());
        plugin.setAuthorName(request.authorName());

        // 获取转正后的 URL，如果没有变化（本来就是正的）则用原值
        plugin.setFileUrl(urlMapping.getOrDefault(request.tempFileUrl(), request.tempFileUrl()));
        plugin.setIconUrl(urlMapping.getOrDefault(request.tempIconUrl(), request.tempIconUrl()));

        Plugin saved = pluginRepository.save(plugin);
        return mapToResponse(saved);
    }

    /**
     * 只有增加下载量的时候调用一下
     */
    @Transactional
    public void incrementDownloadCount(Long id) {
        pluginRepository.findById(id).ifPresent(p -> {
            p.setDownloads(p.getDownloads() + 1);
            pluginRepository.save(p);
        });
    }

    /**
     * 列出所有插件
     */
    public List<PluginResponse> getAllPlugins() {
        return pluginRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PluginResponse mapToResponse(Plugin p) {
        return new PluginResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getVersion(),
                p.getFileUrl(), // <-- 核心：这个 URL 直通 MinIO/S3
                p.getIconUrl(),
                p.getAuthorName(),
                p.getDownloads(),
                p.getUpdatedAt().toString(),
                p.getType()
        );
    }
}