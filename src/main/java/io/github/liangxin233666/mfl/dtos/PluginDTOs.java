package io.github.liangxin233666.mfl.dtos;

public class PluginDTOs {

    // 发布/更新插件的请求
    public record CreatePluginRequest(
            String name,
            String slug,
            String description,
            String version,
            String tempFileUrl, // 前端传上来的临时文件地址
            String tempIconUrl, // 前端传上来的临时图标地址
            String authorName,
            String type
    ) {}

    // 返回给前端的展示数据
    public record PluginResponse(
            Long id,
            String name,
            String description,
            String version,
            String fileUrl,     // 这里的地址前端直接拿去 fetch 或 load
            String iconUrl,
            String authorName,
            Integer downloads,
            String updatedAt,
            String type
    ) {}
}