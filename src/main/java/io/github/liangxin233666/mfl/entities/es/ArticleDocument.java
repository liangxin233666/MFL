// src/main/java/io/github/liangxin233666/mfl/entities/es/ArticleDocument.java
package io.github.liangxin233666.mfl.entities.es;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "articles")
public class ArticleDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String slug;


    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private List<String> aiKeywords; // AI 生成的标签

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private List<String> originalTags; // 用户手动填的 tags

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String authorName; // 支持搜作者名

    @Field(type = FieldType.Date)
    private OffsetDateTime createdAt;


    @Field(
            type = FieldType.Dense_Vector,
            dims = 768,
            index = true,
            similarity = "cosine"
    )
    private float[] embeddingVector;



    // 将 favoritesCount 同步到 ES，可用于 Function Score 查询 (让热度高的排前面)
    @Field(type = FieldType.Long)
    private Long favoritesCount;

}