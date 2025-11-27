package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.config.RabbitMqAuditConfig;
import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.Tag;
import io.github.liangxin233666.mfl.entities.es.ArticleDocument;
import io.github.liangxin233666.mfl.events.NotificationEvent;
import io.github.liangxin233666.mfl.repositories.ArticleRepository;
import io.github.liangxin233666.mfl.repositories.es.EsArticleRepository;
import io.github.liangxin233666.mfl.services.gemini.GeminiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuditConsumer {

    private final ArticleRepository articleRepository;
    private final EsArticleRepository esArticleRepository;
    private final GeminiService geminiService;
    private final NotificationProducer notificationProducer;

    private static final Long SYSTEM_ACTOR_ID = 1L;

    public AuditConsumer(ArticleRepository articleRepository, EsArticleRepository esArticleRepository, GeminiService geminiService, NotificationProducer notificationProducer) {
        this.articleRepository = articleRepository;
        this.esArticleRepository = esArticleRepository;
        this.geminiService = geminiService;
        this.notificationProducer = notificationProducer;
    }

    @RabbitListener(queues = RabbitMqAuditConfig.AUDIT_QUEUE)
    @Transactional // 开启事务
    public void processArticleAudit(Long articleId) {
        log.info("开始审核文章 ID: {}", articleId);

        // 1. 从 DB 捞出文章
        Article article = articleRepository.findById(articleId).orElse(null);
        if (article == null) return; // 已经被删了？忽略

        try {
            // 2. 召唤 AI
            GeminiService.AnalysisResult result = geminiService.auditArticle(article.getTitle(), article.getBody(), null);

            log.info("AI 审核结果: {}", result);

            if (result.approved()) {
                // --- 分支 A: 通过 ---

                // 3. DB 更新状态
                article.setStatus(Article.ArticleStatus.PUBLISHED);
                articleRepository.save(article);

                // 4. 构建 ES 文档
                ArticleDocument doc = ArticleDocument.builder()
                        .id(article.getId())
                        .slug(article.getSlug())
                        .title(article.getTitle())
                        .description(article.getDescription())
                        .aiKeywords(result.keywords()) // AI 生成的

                        // --- 新增 ---
                        .originalTags(article.getTags().stream().map(Tag::getName).toList()) // 用户填的
                        .authorName(article.getAuthor().getUsername()) // 作者名
                        .createdAt(article.getCreatedAt())
                        .build();
                esArticleRepository.save(doc);
                log.info("文章发布并索引成功: {}", article.getSlug());
                sendAuditNotification(article, NotificationEvent.EventType.ARTICLE_APPROVED, null);

            } else {
                // --- 分支 B: 拒绝 ---
                article.setStatus(Article.ArticleStatus.REJECTED);
                // 在真实项目中，可能还会保存 result.reason() 到 DB 的 reject_reason 字段
                articleRepository.save(article);
                log.warn("文章被 AI 驳回: {}, 原因: {}", article.getSlug(), result.reason());
                sendAuditNotification(article, NotificationEvent.EventType.ARTICLE_REJECTED, result.reason());
            }

        } catch (Exception e) {
            log.error("审核过程出错，消息将重试", e);
            throw e; // 抛出异常，让 RabbitMQ 稍后重试（可配置死信队列避免死循环）
        }
    }

    // 辅助方法：打包并发送通知
    private void sendAuditNotification(Article article, NotificationEvent.EventType type, String payload) {
        NotificationEvent event = new NotificationEvent(
                SYSTEM_ACTOR_ID,          // 发送者: System
                article.getAuthor().getId(), // 接收者: 文章作者
                type,                     // 类型
                article.getId(),          // 资源ID
                article.getSlug(),        // Slug
                payload                   // 拒绝理由在此
        );
        notificationProducer.sendNotification(event);
    }
}