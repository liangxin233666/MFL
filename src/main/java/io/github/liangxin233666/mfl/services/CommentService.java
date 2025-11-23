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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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

    /**
     * 创建一个新的顶层评论
     */
    @Transactional
    public CommentResponse addComment(String slug, NewCommentRequest request, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User author = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        Comment comment = new Comment();
        comment.setBody(request.comment().body());
        comment.setArticle(article);
        comment.setAuthor(author);

        Comment savedComment = commentRepository.save(comment);
        savedComment.setRoot(savedComment); // 设置root为自己，成为新树的根
        savedComment = commentRepository.save(savedComment);

        // 返回新创建评论的DTO，不包含任何回复
        return new CommentResponse(buildSingleCommentDto(savedComment));
    }

    /**
     * 回复一条已有的评论
     */
    @Transactional
    public CommentResponse addReply(String slug, Long parentCommentId, NewCommentRequest request, UserDetails currentUserDetails) {
        Article article = findArticleBySlug(slug);
        User author = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));

        Comment reply = new Comment();
        reply.setBody(request.comment().body());
        reply.setArticle(article);
        reply.setAuthor(author);
        reply.setParent(parentComment);
        reply.setRoot(parentComment.getRoot()); // 将回复的root设置为父评论的root

        Comment savedReply = commentRepository.save(reply);

        // 返回新创建回复的DTO
        return new CommentResponse(buildSingleCommentDto(savedReply));
    }

    @Transactional(readOnly = true)
    public MultipleCommentsResponse getCommentsBySlug(String slug, Pageable topLevelPageable) {
        Article article = findArticleBySlug(slug);

        // 第1步: 分页获取顶层评论
        Page<Comment> topLevelCommentPage = commentRepository.findByArticleAndParentIsNull(article, topLevelPageable);
        List<Comment> topLevelComments = topLevelCommentPage.getContent();
        if (topLevelComments.isEmpty()) {
            return new MultipleCommentsResponse(List.of());
        }

        // 第2步: 一次性获取所有相关的子孙评论
        List<Long> topLevelCommentIds = topLevelComments.stream().map(Comment::getId).collect(Collectors.toList());
        List<Comment> descendants = commentRepository.findByRootInAndIdNotIn(topLevelComments, topLevelCommentIds);

        // 第3步: 在内存中将子孙评论按其根评论ID分组
        Map<Long, List<Comment>> descendantsByRootId = descendants.stream()
                .collect(Collectors.groupingBy(comment -> comment.getRoot().getId()));

        // 第4步: 组装最终的DTO树
        List<CommentResponse.CommentDto> commentDtos = topLevelComments.stream()
                .map(topLevelComment -> buildCommentTreeDto(topLevelComment, descendantsByRootId))
                .collect(Collectors.toList());

        return new MultipleCommentsResponse(commentDtos);
    }

    /**
     * 删除一条评论 (权限校验)
     */
    @Transactional
    public void deleteComment(String slug, Long commentId, UserDetails currentUserDetails) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        User currentUser = findUserById(Long.valueOf(currentUserDetails.getUsername()));

        if (!comment.getAuthor().equals(currentUser)) {
            throw new AccessDeniedException("You are not the author of this comment.");
        }
        commentRepository.delete(comment);
    }



    private CommentResponse.CommentDto buildCommentTreeDto(Comment topLevelComment, Map<Long, List<Comment>> descendantsMap) {
        List<Comment> replies = descendantsMap.getOrDefault(topLevelComment.getId(), List.of());

        List<CommentResponse.CommentDto> replyDtos = replies.stream()
                .map(this::buildSingleCommentDto)
                .collect(Collectors.toList());

        User author = topLevelComment.getAuthor();
        ProfileResponse.ProfileDto authorProfile = new ProfileResponse.ProfileDto(
                author.getUsername(),
                null,
                author.getImage(),
                null
        );

        return new CommentResponse.CommentDto(
                topLevelComment.getId(),
                topLevelComment.getCreatedAt(),
                topLevelComment.getUpdatedAt(),
                topLevelComment.getBody(),
                authorProfile,
                replyDtos
        );
    }

    private CommentResponse.CommentDto buildSingleCommentDto(Comment comment) {
        User author = comment.getAuthor();
        ProfileResponse.ProfileDto authorProfile = new ProfileResponse.ProfileDto(
                author.getUsername(),
                null,
                author.getImage(),
                null
        );
        return new CommentResponse.CommentDto(
                comment.getId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getBody(),
                authorProfile,
                List.of() // 对于单个评论或回复，其replies数组为空
        );
    }



    private Article findArticleBySlug(String slug) {
        return articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with slug: " + slug));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for id: " + id));
    }
}