package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.MultipleArticlesResponse;
import io.github.liangxin233666.mfl.services.ArticleService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ArticleService articleService;

    public SearchController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /**
     * 智能搜索文章接口
     *
     * @param query 搜索关键词（支持模糊匹配标题、正文、AI隐式标签）
     * @param pageable 分页参数，默认20条，Spring 自动处理 ?page=0&size=20
     * @param currentUser 当前登录用户（可选），用于展示“是否点赞”等个性化状态
     */
    @GetMapping
    public ResponseEntity<MultipleArticlesResponse> search(
            @RequestParam("query") String query,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails currentUser) {

        // 防御性检查：如果是空搜索，直接返回空（或者你可以选择调用 getAllArticles 返回所有）
        if (query == null || query.isBlank()) {
            // 策略 A: 返回空
            // return ResponseEntity.ok(new MultipleArticlesResponse(List.of(), 0));

            // 策略 B (推荐): 如果没输关键词，可能是在用某种特殊的发现模式，但也暂时返回空吧，等明确业务再改
            return ResponseEntity.ok(new MultipleArticlesResponse(java.util.Collections.emptyList(), 0));
        }

        MultipleArticlesResponse response = articleService.searchArticles(query, pageable, currentUser);
        return ResponseEntity.ok(response);
    }
}