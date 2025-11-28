// src/main/java/io/.../es/EsArticleRepository.java
package io.github.liangxin233666.mfl.repositories.es;

import io.github.liangxin233666.mfl.entities.es.ArticleDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EsArticleRepository extends ElasticsearchRepository<ArticleDocument, Long> {

    // 只需要这一个方法！
    // fields: 指定去哪些字段搜
    // ^3: 代表如果这个字段匹配上了，分数(Score)翻3倍
    @Query("""
        {
          "multi_match": {
            "query": "?0",
            "fields": [
              "title^3",
              "aiKeywords^2",
              "originalTags^2",
              "authorName",
              "description"
            ],
            "type": "best_fields"
          }
        }
    """)
    Page<ArticleDocument> searchIdeally(String keyword, Pageable pageable);

    ArticleDocument findBySlug(String slug);
}