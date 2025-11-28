package io.github.liangxin233666.mfl.services.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*; // 包含 EmbedContentConfig, EmbedContentResponse, ContentEmbedding 等
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class GeminiService {

    // 用 final 保存 Client，因为它是线程安全的，可以复用
    private final Client client;
    private final ObjectMapper objectMapper;
    private static final String MODEL_NAME = "gemini-2.5-flash"; // 追求速度就用 flash
    private static final String EMBEDDING_MODEL = "gemini-embedding-001";


    public GeminiService(@Value("${gemini.api.key}") String apiKey, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 初始化官方 SDK Client
        this.client = Client.builder().apiKey(apiKey).build();
    }

    /**
     * 定义一个内部 Record 来承载审核结果（从 AI 返回的 JSON 解析而来）
     */
    public record AnalysisResult(
            boolean approved,
            List<String> keywords,
            String reason
    ) {}

    /**
     * 核心审核方法
     */
    public AnalysisResult auditArticle(String title, String body, String coverUrl) {

        // 1. 构造 Prompt。和之前一样，我们需要 AI 返回严谨的 JSON。
        // 精心设计的中文提示词
        String prompt = """
        你是一名基于《中华人民共和国网络安全法》进行执业的内容合规审核官，同时也是一位SEO专家。
        
        【任务一：合规性审查】
        请审核以下用户提交的内容。你的判断标准必须精准：
        1. **拒绝红线（严重违规）**：
           - **R18重度色情**：露骨的性行为描述、生殖器特写、乱伦、强奸等。
           - **严重政治红线**：煽动颠覆国家政权、分裂国家、宣扬恐怖主义。
           - **犯罪行为**：涉毒、涉赌、涉黑、教唆自杀或犯罪。
        2. **通过绿线（允许范围）**：
           - 允许**一般的时政评论**（包括对政策的批评或个人观点）。
           - 允许**非R18的情感/成人话题**（如艺术裸露、恋爱心理、正常的两性探讨）。
           - 允许文学创作中的必要冲突描写。
        
        【任务二：SEO关键词提取】
        如果内容**通过**审核，请提取 6-9 个核心中文关键词/标签（Tags），要求精准概括文章主题，利于搜索。
        如果内容**拒绝**，关键词数组可以为空。
        
        【任务三：搜索增强】
        这才是重点！即使原文没有提到，你也需要根据内容的语境、潜在主题和知识图谱，**脑补**用户可能会用来搜索这篇文章的关键词。这不仅是提取，更是预测。
        【搜索词生成要求】
        *   **广度覆盖**：包括该话题的上下级分类（如写“Spring”你要联想到“Java”、“后端”、“编程”）。
        *   **场景化与口语**：包括小白用户搜索时可能用的大白话或场景词（如写“事务失效”联想“Bug调试”、“面试题”）。
        *   **相关性延伸**：包括同义词、相关技术栈或热门话题（如写“Vue3”联想“前端”、“React”）。但是不能出现文章里没有的东西。
        *   **目标**：列出 **8-12个** 能够最大化覆盖目标受众的搜索关键词。
        
        【待审数据】
        标题：%s
        正文片段：%s
        
        【输出要求】
        请仅返回一个纯 JSON 对象，不要包含 Markdown 格式（如 ```json ... ```）。格式如下：
        {
          "approved": true,
          "keywords": ["Java教程", "后端架构", "Redis"],
          "reason": "内容合规，技术分享文章。"
        }
        或者
        {
          "approved": false,
          "keywords": [],
          "reason": "违反相关法律，包含露骨R18色情描写。"
        }
        """.formatted(title, body.length() > 2000 ? body.substring(0, 2000) : body);

        try {
            // 2. 调用 SDK！就是这么简单！
            GenerateContentResponse response = client.models.generateContent(
                    MODEL_NAME,
                    prompt,
                    null // config 可选
            );

            // 3. 获取 AI 的纯文本回复
            String jsonText = response.text();

            // 清洗一下 Markdown 代码块（Gemini 有时候不听话还是会给 ```json ... ```）
            if (jsonText != null && jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            }
            if (jsonText != null && jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }
            if (jsonText != null) {
                jsonText = jsonText.trim();
            }

            // 4. 解析结果
            return objectMapper.readValue(jsonText, AnalysisResult.class);

        } catch (Exception e) {
            // 这里既包含了网络错误，也包含了 JSON 解析错误
            // 在生产环境可以分细一点记录
            throw new RuntimeException("Gemini analysis failed", e);
        }
    }

    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[768]; // 或者返回 null，视业务需要
        }

        try {
            // 1. 配置：指定输出维度 768
            EmbedContentConfig config = EmbedContentConfig.builder()
                    .outputDimensionality(768)
                    .build();

            // 2. 调用 SDK：注意这里直接传 String text，不需要构造 Content 对象
            // 对应你反编译看到的：embedContent(String model, String text, EmbedContentConfig config)
            EmbedContentResponse response = client.models.embedContent(
                    EMBEDDING_MODEL,
                    text,
                    config
            );

            // 3. 解析结果：注意处理 Optional 和 List
            // 源码显示：public Optional<List<ContentEmbedding>> embeddings()
            Optional<List<ContentEmbedding>> embeddingsOpt = response.embeddings();

            if (embeddingsOpt.isEmpty() || embeddingsOpt.get().isEmpty()) {
                throw new RuntimeException("Gemini API returned empty embeddings");
            }


            // 获取具体的向量值 (List<Float>)
            List<Float> values = response.embeddings()
                    .flatMap(list -> list.stream().findFirst()) // 取列表第一个 ContentEmbedding
                    .flatMap(ContentEmbedding::values)          // 取里面的 values (也是 Optional)
                    .orElseThrow(() -> new RuntimeException("Failed to generate embedding"));

            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i);
            }
            return vector;

        } catch (Exception e) {

            log.error("Gemini API generation failed", e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }
}