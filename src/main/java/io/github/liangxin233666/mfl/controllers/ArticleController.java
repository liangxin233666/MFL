package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.ArticleResponse;
import io.github.liangxin233666.mfl.dtos.MultipleArticlesResponse;
import io.github.liangxin233666.mfl.dtos.NewArticleRequest;
import io.github.liangxin233666.mfl.dtos.UpdateArticleRequest;
import io.github.liangxin233666.mfl.services.ArticleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(
            @Valid @RequestBody NewArticleRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        ArticleResponse articleResponse = articleService.createArticle(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(articleResponse);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ArticleResponse> getArticle(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails currentUser) {
        ArticleResponse articleResponse = articleService.getArticleBySlug(slug, currentUser);
        return ResponseEntity.ok(articleResponse);
    }

    @PutMapping("/{slug}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable String slug,
            @Valid @RequestBody UpdateArticleRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        ArticleResponse articleResponse = articleService.updateArticle(slug, request, currentUser);
        return ResponseEntity.ok(articleResponse);
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteArticle(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails currentUser) {
        articleService.deleteArticle(slug, currentUser);
        return ResponseEntity.noContent().build(); // 删除成功，返回 204 No Content
    }

    @PostMapping("/{slug}/favorite")
    public ResponseEntity<ArticleResponse> favoriteArticle(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails currentUser) {
        ArticleResponse articleResponse = articleService.favoriteArticle(slug, currentUser);
        return ResponseEntity.ok(articleResponse);
    }

    @DeleteMapping("/{slug}/favorite")
    public ResponseEntity<ArticleResponse> unfavoriteArticle(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails currentUser) {
        ArticleResponse articleResponse = articleService.unfavoriteArticle(slug, currentUser);
        return ResponseEntity.ok(articleResponse);
    }

    @GetMapping
    public ResponseEntity<MultipleArticlesResponse> getArticles(
            @RequestParam(name = "tag", required = false) String tag,

            @RequestParam(name = "author", required = false) String author,

            @RequestParam(name = "favoritedBy", required = false) String favoritedBy,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails currentUser) {

        MultipleArticlesResponse response = articleService.getArticles(tag, author, favoritedBy, pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/feed")
    public ResponseEntity<MultipleArticlesResponse> getFeedArticles(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails currentUser) {
        MultipleArticlesResponse response = articleService.getFeedArticles(pageable, currentUser);
        return ResponseEntity.ok(response);
    }
}