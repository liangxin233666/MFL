package io.github.liangxin233666.mfl.dtos; // 放到适合的包

import io.github.liangxin233666.mfl.services.gemini.GeminiService;
import java.io.Serializable;

// 必须实现 Serializable，或者使用 Jackson 序列化配置
public record AuditPassedEvent(
        Long articleId,
        GeminiService.AnalysisResult analysisResult // 携带已经生成的审核结果
) implements Serializable {}