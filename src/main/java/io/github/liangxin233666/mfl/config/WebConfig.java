package io.github.liangxin233666.mfl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebConfig {
    
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableResolverCustomizer() {
        return (resolver) -> {
            // 将 "offset" 参数名设置为偏移量
            resolver.setPageParameterName("offset");
            // 将 "limit" 参数名设置为页面大小
            resolver.setSizeParameterName("limit");

            // 设置一个合理的最大页面大小，防止恶意请求
            resolver.setMaxPageSize(100);

            // 让解析器即使只有一个页面也返回Pageable，而不是unpaged
            resolver.setFallbackPageable(PageRequest.of(0, 20));
        };
    }
}