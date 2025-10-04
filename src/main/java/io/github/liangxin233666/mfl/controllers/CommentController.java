package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.CommentResponse;
import io.github.liangxin233666.mfl.dtos.MultipleCommentsResponse;
import io.github.liangxin233666.mfl.dtos.NewCommentRequest;
import io.github.liangxin233666.mfl.services.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles/{slug}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable String slug,
            @Valid @RequestBody NewCommentRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        CommentResponse commentResponse = commentService.addComment(slug, request, currentUser);
        return ResponseEntity.ok(commentResponse);
    }

    @GetMapping
    public ResponseEntity<MultipleCommentsResponse> getComments(
            @PathVariable String slug,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal UserDetails currentUser) {
        MultipleCommentsResponse comments = commentService.getCommentsBySlug(slug,pageable, currentUser);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String slug,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails currentUser) {
        commentService.deleteComment(slug, commentId, currentUser);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{commentId}") // id 是 parentCommentId
    public ResponseEntity<CommentResponse> addReplyToComment(
            @PathVariable String slug,
            @PathVariable Long commentId, // 被回复的评论ID
            @Valid @RequestBody NewCommentRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        CommentResponse replyResponse = commentService.addReply(slug, commentId, request, currentUser);
        return ResponseEntity.ok(replyResponse);
    }
}