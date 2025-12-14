// src/main/java/io/github/liangxin233666/mfl/services/GlobalTrendManager.java
package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.es.ArticleDocument;
import io.github.liangxin233666.mfl.repositories.ArticleRepository;
import io.github.liangxin233666.mfl.repositories.es.EsArticleRepository;
import io.github.liangxin233666.mfl.repositories.projections.ArticleSimpleView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class GlobalTrendManager {

    private final ArticleRepository articleRepository;
    private final EsArticleRepository esArticleRepository;

    // 常量：Gemini Embedding 的维度
    private static final int DIMENSIONS = 768;

    // 原子引用：保证高并发读取时的线程安全，默认值为零向量
    private final AtomicReference<float[]> globalHotVector = new AtomicReference<>(new float[DIMENSIONS]);

    public GlobalTrendManager(ArticleRepository articleRepository, EsArticleRepository esArticleRepository) {
        this.articleRepository = articleRepository;
        this.esArticleRepository = esArticleRepository;
    }

    /**
     * 对外暴露接口：获取当前的全网热门向量
     */
    public float[] getGlobalHotVector() {
        return globalHotVector.get();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initOnStartup() {
        log.info("【全网趋势】检测到系统启动，正在进行初始化计算...");
        refreshGlobalHotVector();
    }


    /**
     * 定时任务：每1小时（3600000毫秒）重新计算一次全网趋势
     * 这里的逻辑是：找出"已发布"且"点赞最多"的文章，算出它们向量的平均值（重心）。
     */
    @Scheduled(fixedRate = 3600000)
    public void refreshGlobalHotVector() {
        log.info("【全网趋势】开始重新计算全网热门文章向量重心...");

        // 1. 获取热门文章 ID
        // 条件：状态必须是 PUBLISHED
        // 排序：favoritesCount 倒序
        // 数量：Top 20

        List<ArticleSimpleView> topArticles = articleRepository.findTopHotArticles(Article.ArticleStatus.PUBLISHED,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "favoritesCount")));


        if (topArticles.isEmpty()) {
            log.info("【全网趋势】暂无已发布文章，跳过计算。");
            return;
        }

        // 提取 ID 列表
        List<Long> ids = topArticles.stream()
                // 只要流里的对象有 getId() 方法就能编译通过
                .map(ArticleSimpleView::getId)
                .toList();

        // 2. 去 Elasticsearch 批量捞取向量数据
        // 因为 sql 里只存了文本 url，核心向量在 ES 里
        Iterable<ArticleDocument> docsIterable = esArticleRepository.findAllById(ids);

        // 3. 收集有效向量
        List<float[]> validVectors = new ArrayList<>();
        docsIterable.forEach(doc -> {
            float[] vec = doc.getEmbeddingVector();
            // 严谨校验：非空且维度正确（防止某些旧数据或失败数据的维度不对）
            if (vec != null && vec.length == DIMENSIONS) {
                validVectors.add(vec);
            }
        });

        // 4. 计算重心 (Average)
        if (!validVectors.isEmpty()) {
            float[] newCentroid = calculateCentroid(validVectors);

            // 更新内存中的全局变量
            globalHotVector.set(newCentroid);

            log.info("【全网趋势】计算完成。采样文章数(DB): {}, 有效向量数(ES): {}。全局向量已更新。",
                    topArticles.size(), validVectors.size());
        } else {
            log.warn("【全网趋势】无法找到对应ID的有效向量（可能是刚同步或Embedding未生成）。");
        }
    }

    /**
     * 内部数学工具：计算一组向量的平均值
     */
    private float[] calculateCentroid(List<float[]> vectors) {
        float[] result = new float[DIMENSIONS];
        int count = vectors.size();

        // 累加每一维
        for (float[] vector : vectors) {
            for (int i = 0; i < DIMENSIONS; i++) {
                result[i] += vector[i];
            }
        }

        // 每一维除以总数
        for (int i = 0; i < DIMENSIONS; i++) {
            result[i] = result[i] / count;
        }

        return result;
    }
}