package io.github.liangxin233666.mfl.repositories;

import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> , JpaSpecificationExecutor<Article> {

    Optional<Article> findBySlug(String slug);
    Page<Article> findByAuthorInOrderByCreatedAtDesc(List<User> authors, Pageable pageable);
}
