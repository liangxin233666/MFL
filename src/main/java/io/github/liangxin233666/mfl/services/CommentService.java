package io.github.liangxin233666.mfl.services;

import io.github.liangxin233666.mfl.dtos.CommentResponse;
import io.github.liangxin233666.mfl.dtos.MultipleCommentsResponse;
import io.github.liangxin233666.mfl.dtos.NewCommentRequest;
import io.github.liangxin233666.mfl.dtos.ProfileResponse;
import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.Comment;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.exceptions.ResourceNotFoundException;
import io.github.liangxin233666.mfl.repositories.ArticleRepository;
import io.github.liangxin233666.mfl.repositories.CommentRepository;
import io.github.liangxin233666.mfl.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, ArticleRepository articleRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentResponse addComment(String slug, @Valid NewCommentRequest request, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User author = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        Comment comment = new Comment();
        comment.setBody(request.comment().body());
        comment.setArticle(article);
        comment.setAuthor(author);

        Comment savedComment = commentRepository.save(comment);
        return buildCommentResponse(savedComment, author);
    }

    @Transactional(readOnly = true)
    public MultipleCommentsResponse getCommentsBySlug(String slug, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User currentUser = (currentUserDetails != null)
                ? findUserById(Long.valueOf(currentUserDetails.getUsername()))
                : null;

        // 核心改动：只查找顶层评论 (parent_id IS NULL)
        List<CommentResponse.CommentDto> topLevelCommentDtos = article.getComments().stream()
                .filter(comment -> comment.getParent() == null) // 筛选出顶层评论
                .map(comment -> buildCommentDto(comment, currentUser)) // 递归构建 DTO
                .collect(Collectors.toList());

        return new MultipleCommentsResponse(topLevelCommentDtos);
    }

    @Transactional
    public void deleteComment(String slug, Long commentId, UserDetails currentUserDetails) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        // ** 权限校验 **
        if (!comment.getAuthor().equals(currentUser)) {
            throw new AccessDeniedException("You are not the author of this comment.");
        }

        commentRepository.delete(comment);
    }


    private CommentResponse.CommentDto buildCommentDto(Comment comment, User currentUser) {
        User author = comment.getAuthor();
        boolean isFollowing = (currentUser != null) && currentUser.getFollowing().contains(author);

        ProfileResponse.ProfileDto authorProfile = new ProfileResponse.ProfileDto(
                author.getUsername(),
                author.getBio(),
                author.getImage(),
                isFollowing
        );

        // 递归构建回复列表
        List<CommentResponse.CommentDto> replyDtos = comment.getReplies().stream()
                .map(reply -> buildCommentDto(reply, currentUser))
                .collect(Collectors.toList());

        return new CommentResponse.CommentDto(
                comment.getId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getBody(),
                authorProfile,
                replyDtos // 将构建好的回复列表放入 DTO
        );
    }


    private CommentResponse buildCommentResponse(Comment comment, User currentUser) {
        return new CommentResponse(buildCommentDto(comment, currentUser));
    }

    private Article findArticleBySlug(String slug) {
        return articleRepository.findBySlug(slug)
                .orElseThrow(() -> new  ResourceNotFoundException("Article not found with slug: " + slug));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new  ResourceNotFoundException("User not found for id: " + id));
    }

    @Transactional
    public CommentResponse addReply(String slug, Long parentCommentId, @Valid NewCommentRequest request, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User currentUser = (currentUserDetails != null)
                ? findUserById(Long.valueOf(currentUserDetails.getUsername()))
                : null;

        User author = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        // 找到被回复的父评论
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new  ResourceNotFoundException("Parent comment not found"));

        Comment reply = new Comment();
        reply.setBody(request.comment().body());
        reply.setArticle(article);
        reply.setAuthor(author);
        reply.setParent(parentComment); //设置父子关系 **

        Comment savedReply = commentRepository.save(reply);

        // 返回新创建的回复
        return new CommentResponse(buildCommentDto(savedReply, currentUser));


    }
}