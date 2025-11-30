package io.github.liangxin233666.mfl.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import io.github.liangxin233666.mfl.entities.es.ArticleDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationService {


    private final ElasticsearchClient esClient;

    public RecommendationService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    /**
     * 场景一: 猜你喜欢 (Feed流) - 纯向量检索
     * 最佳实践: 使用 Top-level knn 参数 + Pre-filtering
     */
    public List<Long> recommendForUser(float[] userVector, int limit) {
        if (userVector == null || userVector.length == 0) return Collections.emptyList();
        List<Float> queryVector = toFloatList(userVector);

        try {
            log.info("【Debug】开始搜索，limit={}, 向量维度={}", limit, queryVector.size());

            SearchResponse<ArticleDocument> response = esClient.search(s -> s
                            .index("articles")
                            .knn(k -> k
                                            .field("embeddingVector")
                                            .queryVector(queryVector)
                                            .k(limit)
                                            .numCandidates(100)

                            )
                            .source(src -> src.filter(f -> f.includes("id")))
                            .size(limit),
                    ArticleDocument.class
            );


            List<Long> resultIds = new ArrayList<>();

            // 遍历命中的每一条
            for (Hit<ArticleDocument> hit : response.hits().hits()) {
                ArticleDocument doc = hit.source();
                String metaId = hit.id(); // ES 的 _id


                if (doc != null) {
                    if (doc.getId() != null) {
                        resultIds.add(doc.getId());
                    } else {

                        try {
                            resultIds.add(Long.parseLong(metaId));

                        } catch (Exception e) {}
                    }
                } else {
                    log.error("【Debug】Hit.source() 为 null！可能是 JSON 反序列化失败");
                }
            }

            log.info("【Debug】最终返回给 MySQL 的 ID 列表: {}", resultIds);
            return resultIds;

        } catch (Exception e) {
            log.error("【Debug】ES 查询报错: ", e);
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

                            // 1. 向量部分 (语义相关) - 权重 0.5
                            .knn(k -> {
                                if (queryVector != null) {
                                    return k
                                            .field("embeddingVector")
                                            .queryVector(queryVector)
                                            .k(limit)
                                            .numCandidates(50)
                                            .boost(0.5f) // 【关键修改】设置权重
                                            // 预过滤
                                            .filter(f -> f.bool(b -> b
                                                    .mustNot(mn -> mn.ids(i -> i.values(docId)))
                                                    //.must(m -> m.term(t -> t.field("status").value("PUBLISHED")))
                                            ));
                                }
                                return null;
                            })

                            // 2. 文本部分 (内容/标签相关) - 权重 0.5
                            .query(q -> q
                                    .bool(b -> b
                                            .must(m -> m.moreLikeThis(mlt -> mlt
                                                    .fields("title", "description", "originalTags")
                                                    .like(l -> l.document(d -> d.index("articles").id(docId)))
                                                    .minTermFreq(1)
                                                    .minDocFreq(1)
                                            ))
                                            .mustNot(mn -> mn.ids(i -> i.values(docId)))
                                            //.filter(f -> f.term(t -> t.field("status").value("PUBLISHED")))
                                            .boost(0.5f) // 【关键修改】设置权重，与 knn 累加
                                    )
                            )

                            // 【关键修改】彻底删除 .rank(...) 部分，避免 License 报错

                            .source(src -> src.filter(f -> f.includes("id")))
                            .size(limit),

                    ArticleDocument.class
            );

            return extractIds(response);

        } catch (IOException e) {

            log.error("有错误 ",e);
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