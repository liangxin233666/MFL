package io.github.liangxin233666.mfl.repositories;

import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByArticleAndParentIsNull(Article article, Pageable pageable);
}