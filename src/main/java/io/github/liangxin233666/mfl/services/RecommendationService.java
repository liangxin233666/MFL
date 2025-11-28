package io.github.liangxin233666.mfl.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import io.github.liangxin233666.mfl.entities.es.ArticleDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    // 重点：直接注入官方原生 Client，绕过 Spring Data 的限制，发挥 ES 8.11 的全部实力
    private final ElasticsearchClient esClient;

    public RecommendationService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    /**
     * 场景一: 猜你喜欢 (Feed流) - 纯向量检索
     * 最佳实践: 使用 Top-level knn 参数 + Pre-filtering
     */
    public List<Long> recommendForUser(float[] userVector, int limit) {
        if (userVector == null || userVector.length == 0) {
            return Collections.emptyList();
        }

        List<Float> queryVector = toFloatList(userVector);

        try {
            SearchResponse<ArticleDocument> response = esClient.search(s -> s
                            .index("articles") // 指定索引名
                            .knn(k -> k
                                    .field("embeddingVector")
                                    .queryVector(queryVector)
                                    .k(limit)
                                    .numCandidates(Math.max(50, limit * 10)) // 提高召回精度
                                    // 【关键优化】Pre-filtering: 在遍历向量图之前过滤，性能极高
                                    .filter(f -> f.term(t -> t.field("status").value("PUBLISHED")))
                            )
                            .source(src -> src.filter(f -> f.includes("id"))) // 只取ID，减少网络传输
                            .size(limit),
                    ArticleDocument.class
            );

            return extractIds(response);

        } catch (IOException e) {
            // 建议接入你的日志系统
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 场景二: 看了又看 (混合检索 Hybrid Search) - RRF 融合
     * 最佳实践: ES 8.11 RRF (knn + query + rank)
     * 这是一个 "Game Changer"，它不需要调整 boost 权重，自动平衡文本和语义的相关性。
     */
    public List<Long> getRelatedArticles(ArticleDocument sourceArticle, int limit) {
        if (sourceArticle == null) return Collections.emptyList();

        float[] vector = sourceArticle.getEmbeddingVector();
        List<Float> queryVector = (vector != null) ? toFloatList(vector) : null;
        String docId = sourceArticle.getId().toString();

        try {
            SearchResponse<ArticleDocument> response = esClient.search(s -> s
                            .index("articles")

                            // 1. 向量部分 (语义相关)
                            .knn(k -> {
                                if (queryVector != null) {
                                    return k
                                            .field("embeddingVector")
                                            .queryVector(queryVector)
                                            .k(limit)
                                            .numCandidates(50)
                                            // 预过滤：必须排除当前文章本身，且必须是已发布
                                            .filter(f -> f.bool(b -> b
                                                    .mustNot(mn -> mn.ids(i -> i.values(docId)))
                                                    .must(m -> m.term(t -> t.field("status").value("PUBLISHED")))
                                            ));
                                }
                                return null; // 如果没有向量，此部分会被忽略
                            })

                            // 2. 文本部分 (内容/标签相关)
                            .query(q -> q
                                    .bool(b -> b
                                            .must(m -> m.moreLikeThis(mlt -> mlt
                                                    .fields("title", "description", "originalTags")
                                                    .like(l -> l.document(d -> d.index("articles").id(docId)))
                                                    .minTermFreq(1)
                                                    .minDocFreq(1)
                                            ))
                                            // 文本查询也要排除自己 (虽然 KNN 排除了，但 Query 部分也需要逻辑一致)
                                            .mustNot(mn -> mn.ids(i -> i.values(docId)))
                                            .filter(f -> f.term(t -> t.field("status").value("PUBLISHED")))
                                    )
                            )

                            // 3. RRF 排名融合 (ES 8.x 杀手级特性)
                            // 只有当同时存在 knn 和 query 时，rank 才会生效融合两者
                            // 如果只有 query 或只有 knn，ES 会自动降级为标准排序，非常智能
                            .rank(r -> r
                                    .rrf(rrf -> rrf) // 什么都不设，直接返回 builder，使用 ES 默认配置
                            )

                            .source(src -> src.filter(f -> f.includes("id")))
                            .size(limit),

                    ArticleDocument.class
            );

            return extractIds(response);

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // --- 辅助方法 ---

    // ES Client 新版 API 接受 List<Float>，直接转 Float 即可，省内存
    private List<Float> toFloatList(float[] floats) {
        if (floats == null) return Collections.emptyList();
        List<Float> list = new ArrayList<>(floats.length);
        for (float f : floats) {
            list.add(f);
        }
        return list;
    }

    private List<Long> extractIds(SearchResponse<ArticleDocument> response) {
        return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(ArticleDocument::getId)
                .collect(Collectors.toList());
    }
}