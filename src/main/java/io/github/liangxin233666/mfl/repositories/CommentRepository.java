package io.github.liangxin233666.mfl.repositories;

import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByArticleAndParentIsNull(Article article, Pageable pageable);

    // ===== 新增这个强大的方法 =====
    // 根据一组root评论，找到它们所有的子孙后代（除了它们自己）
    List<Comment> findByRootInAndIdNot(List<Comment> roots, Long id); // 这是一个小小的技巧

    // 更正和优化
    List<Comment> findByRootInAndIdNotIn(List<Comment> roots, List<Long> rootIds);
}