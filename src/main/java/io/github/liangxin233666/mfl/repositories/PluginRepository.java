package io.github.liangxin233666.mfl.repositories;

import io.github.liangxin233666.mfl.entities.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PluginRepository extends JpaRepository<Plugin, Long> {

    /**
     * 根据 Slug 查找插件
     * 用途：如果你的API不仅支持 ID 还要支持 slug 查询（如 /api/plugins/dark-mode）时使用
     */
    Optional<Plugin> findBySlug(String slug);

    /**
     * 检查 Slug 是否存在
     * 用途：在上架新插件(publishPlugin)前调用，防止用户提交了重名的插件 slug
     */
    boolean existsBySlug(String slug);

    /**
     * 根据类型查找
     * 用途：虽然我们上次 Entity 里没严格校验 type 字段，但通常会有 "THEME"(主题) 和 "EXTENSION"(功能扩展) 的区分
     * 前端可能需要 tabs 切换筛选
     */
    List<Plugin> findByType(String type);

    /**
     * 按下载量倒序查询
     * 用途：用于首页的“热门插件”列表
     */
    List<Plugin> findAllByOrderByDownloadsDesc();

    /**
     * 根据作者名查找
     * 用途：查看某个大神开发的所有插件
     */
    List<Plugin> findByAuthorName(String authorName);
}