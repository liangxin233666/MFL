package io.github.liangxin233666.mfl.controllers;

import io.github.liangxin233666.mfl.dtos.CommentResponse;
import io.github.liangxin233666.mfl.dtos.MultipleCommentsResponse;
import io.github.liangxin233666.mfl.dtos.NewCommentRequest;
import io.github.liangxin233666.mfl.services.CommentService;
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
            @RequestBody NewCommentRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        CommentResponse commentResponse = commentService.addComment(slug, request, currentUser);
        return ResponseEntity.ok(commentResponse);
    }

    @GetMapping
    public ResponseEntity<MultipleCommentsResponse> getComments(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails currentUser) {
        MultipleCommentsResponse comments = commentService.getCommentsBySlug(slug, currentUser);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String slug,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        commentService.deleteComment(slug, id, currentUser);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{commentId}") // id 是 parentCommentId
    public ResponseEntity<CommentResponse> addReplyToComment(
            @PathVariable String slug,
            @PathVariable Long commentId, // 被回复的评论ID
            @RequestBody NewCommentRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        CommentResponse replyResponse = commentService.addReply(slug, commentId, request, currentUser);
        return ResponseEntity.ok(replyResponse);
    }
}