package io.github.liangxin233666.mfl.controllers;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import io.github.liangxin233666.mfl.services.gemini.GeminiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/debug/gemini")
public class GeminiTestController {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final GeminiService geminiService;

    public GeminiTestController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * 测试 1：【裸连测试】
     * 不依赖任何 Service，直接在这里 new Client 测试。
     * 用来排除 Spring Bean 生命周期或事务的影响。
     * 访问地址: http://localhost:8080/debug/gemini/raw?prompt=你好
     */
    @GetMapping("/raw")
    public String testRawConnectivity(@RequestParam(defaultValue = "Hello from Java!") String prompt) {
        log.info(">>> 开始裸连测试, prompt: {}", prompt);
        long start = System.currentTimeMillis();

        try {
            // 1. 强制设置网络环境 (模拟最严格的配置)
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", "20085");
            System.setProperty("https.proxyHost", "127.0.0.1");
            System.setProperty("https.proxyPort", "20085");

            // 关键：关闭长连接，防止第二次卡死
            System.setProperty("http.keepAlive", "false");
            System.setProperty("https.keepAlive", "false");

            // 2. 临时创建一个 Client
            Client client = Client.builder()
                    .apiKey(apiKey)
                    .build();

            // 3. 发送请求
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash-lite", // 确保模型名字对
                    prompt,
                    null
            );

            long duration = System.currentTimeMillis() - start;
            String result = response.text();
            log.info("<<< 裸连测试成功，耗时: {}ms, 结果: {}", duration, result);

            return "✅ 成功 (耗时 " + duration + "ms): " + result;

        } catch (Exception e) {
            log.error("❌ 裸连测试失败", e);
            return "❌ 失败: " + e.getMessage();
        }
    }

    /**
     * 测试 2：【Service 集成测试】
     * 测试经过改造后的 GeminiService 是否正常。
     * 访问地址: http://localhost:8080/debug/gemini/service
     */
    @GetMapping("/service")
    public String testServiceIntegration() {
        log.info(">>> 开始 Service 集成测试");
        long start = System.currentTimeMillis();

        try {
            // 模拟一篇文章内容
            String title = "测试标题";
            String body = "这是一篇关于Java编程的测试文章，内容非常积极向上。";

            // 调用你的 Service
            GeminiService.AnalysisResult result = geminiService.auditArticle(title, body, null);

            long duration = System.currentTimeMillis() - start;
            log.info("<<< Service 测试成功，耗时: {}ms", duration);

            return "✅ Service 正常 (耗时 " + duration + "ms) - 结果: " + result;

        } catch (Exception e) {
            log.error("❌ Service 测试失败", e);
            return "❌ Service 失败: " + e.getMessage();
        }
    }

    /**
     * 测试 3：【连续轰炸测试】
     * 专门重现“第一次成功，第二次卡死”的问题。
     * 访问地址: http://localhost:8080/debug/gemini/loop
     */
    @GetMapping("/loop")
    public String testLoop() {
        StringBuilder report = new StringBuilder();

        for (int i = 1; i <= 5; i++) {
            long start = System.currentTimeMillis();
            try {
                log.info(">>> 第 {} 次请求开始", i);

                // 这里使用 Raw 逻辑快速验证
                Client client = Client.builder().apiKey(apiKey).build();
                GenerateContentResponse response=client.models.generateContent("gemini-2.5-flash-lite", "说出三道菜名字", null);
                String jsonText = response.text();
                long time = System.currentTimeMillis() - start;
                report.append(String.format("第 %d 次: 成功 (%d ms)<br/>", i, time));
                log.info("AI响应内容: {}", jsonText);

            } catch (Exception e) {
                report.append(String.format("第 %d 次: 失败 (%s)<br/>", i, e.getMessage()));
                log.error("<<< 第 {} 次请求失败", i, e);
            }
        }
        return report.toString();
    }
}