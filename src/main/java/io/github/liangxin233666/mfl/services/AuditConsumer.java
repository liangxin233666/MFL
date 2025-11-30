package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.config.RabbitMqAuditConfig;
import io.github.liangxin233666.mfl.dtos.AuditPassedEvent;
import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.Tag;
import io.github.liangxin233666.mfl.entities.es.ArticleDocument;
import io.github.liangxin233666.mfl.events.NotificationEvent;
import io.github.liangxin233666.mfl.repositories.ArticleRepository;
import io.github.liangxin233666.mfl.repositories.es.EsArticleRepository;
import io.github.liangxin233666.mfl.services.gemini.GeminiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuditConsumer {

    private final ArticleRepository articleRepository;
    private final EsArticleRepository esArticleRepository;
    private final GeminiService geminiService;
    private final NotificationProducer notificationProducer;
    private final TransactionTemplate transactionTemplate;
    private final RabbitTemplate rabbitTemplate;
    private static final Long SYSTEM_ACTOR_ID = 1L;

    public AuditConsumer(ArticleRepository articleRepository, EsArticleRepository esArticleRepository, GeminiService geminiService, NotificationProducer notificationProducer,TransactionTemplate transactionTemplate, RabbitTemplate rabbitTemplate) {
        this.articleRepository = articleRepository;
        this.esArticleRepository = esArticleRepository;
        this.geminiService = geminiService;
        this.notificationProducer = notificationProducer;
        this.transactionTemplate = transactionTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMqAuditConfig.AUDIT_QUEUE)
    public void processArticleAudit(Long articleId) {
        log.info("开始审核文章 ID: {}", articleId);

        // 1. 从 DB 捞出文章
        Article article = articleRepository.findById(articleId).orElse(null);
        if (article == null) {
            try {
                Thread.sleep(500); // 等500毫秒让主事务提交
                article = articleRepository.findById(articleId).orElse(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (article == null) {
            log.warn("文章 ID {} 未找到，可能已被删除或事务未提交", articleId);
            return;
        }

        try {
            // 2. 召唤 AI
            GeminiService.AnalysisResult result = geminiService.auditArticle(article.getTitle(), article.getBody(), null);

            log.info("AI 审核结果: {}", result);

            if (result.approved()) {
                AuditPassedEvent nextStepEvent = new AuditPassedEvent(articleId, result);

                rabbitTemplate.convertAndSend(RabbitMqAuditConfig.VECTOR_SAVE_QUEUE, nextStepEvent);
                log.info("阶段1-审核: 通过，已转发至向量队列");

            } else {
                transactionTemplate.execute(status -> {
                    saveRejectedResult(articleId, result.reason());
                    return null;
                });
            }


        } catch (Exception e) {
            log.error("阶段1-审核: 出错，RabbitMQ将重试此阶段", e);
            throw e; // 抛出异常，触发审核重试
        }
    }

    @RabbitListener(queues = RabbitMqAuditConfig.VECTOR_SAVE_QUEUE)
    public void processVectorAndSave(AuditPassedEvent event) {
        log.info("阶段2-向量: 开始处理文章 ID: {}", event.articleId());

        // 1. 重新查询文章 (此时必须要 Tags 了，为了生成向量)
        Article article = articleRepository.findById(event.articleId()).orElse(null);

        // 防御性编程：可能审核期间文章被删了
        if (article == null) {
            log.warn("阶段2-向量: 文章已消失");
            return;
        }

        try {
            // 2. 使用第一阶段传过来的结果 (result)，去生成向量
            // 如果这里报错（例如超时），只会重试 generateVector，不重试 auditArticle
            float[] vector = generateVector(article, event.analysisResult());

            // 3. 最终入库 (和原来逻辑一样，TransactionTemplate)
            transactionTemplate.execute(status -> {
                saveApprovedResult(article.getId(), event.analysisResult(), vector);
                return null;
            });

            log.info("阶段2-向量: 流程全部完成");

        } catch (Exception e) {
            log.error("阶段2-向量: 生成向量或保存失败，RabbitMQ将仅重试此阶段", e);
            throw e;
        }
    }



    // 辅助方法：生成向量（避免主逻辑太乱）
    private float[] generateVector(Article article, GeminiService.AnalysisResult result) {
        StringBuilder embeddingSource = new StringBuilder();

        // A. 标题
        embeddingSource.append(article.getTitle()).append("\n");

        // B. 简介
        if (article.getDescription() != null) {
            embeddingSource.append(article.getDescription()).append("\n");
        }

        // C. 用户填写的原生 Tags (List<Tag> 转 String)
        // 判空防止空指针，虽然 JPA 通常返回空 List
        if (article.getTags() != null && !article.getTags().isEmpty()) {
            List<String> userTagList = article.getTags().stream()
                    .map(Tag::getName)
                    .toList();

            if (!userTagList.isEmpty()) {
                embeddingSource.append("Tags: ").append(String.join(", ", userTagList)).append("\n");
            }
        }

        // D. AI 分析出的关键词 (List<String>)
        // 这里使用的是新的独立 DTO: io.github.liangxin233666.mfl.dtos.AnalysisResult
        if (result.keywords() != null && !result.keywords().isEmpty()) {
            embeddingSource.append("Keywords: ").append(String.join(", ", result.keywords()));
        }

        // E. 调用 Gemini 生成向量
        return geminiService.generateEmbedding(embeddingSource.toString());
    }

    // 5. 【关键】独立的事务方法：通过

    public void saveApprovedResult(Long articleId, GeminiService.AnalysisResult result, float[] vector) {
        // 重新 fetch 一次，确保数据最新（虽然大部分情况可以直接用）
        Article article = articleRepository.findById(articleId).orElseThrow();
        article.setStatus(Article.ArticleStatus.PUBLISHED);
        articleRepository.save(article);

        // 构建 ES 文档逻辑保持不变
        ArticleDocument doc = ArticleDocument.builder()
                .id(article.getId())
                .slug(article.getSlug())
                .title(article.getTitle())
                .description(article.getDescription())
                .aiKeywords(result.keywords())
                .embeddingVector(vector)
                .originalTags(article.getTags().stream().map(Tag::getName).toList())
                .authorName(article.getAuthor().getUsername())
                .createdAt(article.getCreatedAt())
                .build();

        esArticleRepository.save(doc);
        sendAuditNotification(article, NotificationEvent.EventType.ARTICLE_APPROVED, null);
    }

    // 6. 【关键】独立的事务方法：拒绝

    public void saveRejectedResult(Long articleId, String reason) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        article.setStatus(Article.ArticleStatus.REJECTED);
        articleRepository.save(article);
        sendAuditNotification(article, NotificationEvent.EventType.ARTICLE_REJECTED, reason);
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