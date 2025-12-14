package io.github.liangxin233666.mfl.repositories;

import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.repositories.projections.ArticleSimpleView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> , JpaSpecificationExecutor<Article> {

    @EntityGraph(attributePaths = {"author", "tags"})
    Optional<Article> findBySlug(String slug);

    @EntityGraph(attributePaths = {"author", "tags"})
    Page<Article> findByAuthorInOrderByCreatedAtDesc(List<User> authors, Pageable pageable);

    List<Article> findByStatus(Article.ArticleStatus status, Pageable pageable);

    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.tags LEFT JOIN FETCH a.author WHERE a.id = :id")
    Optional<Article> findByIdWithTagsAndAuthor(@Param("id") Long id);

    @Query("SELECT a.id as id, a.favoritesCount as favoritesCount FROM Article a WHERE a.status = :status")
    List<ArticleSimpleView> findTopHotArticles(Article.ArticleStatus status, Pageable pageable);

    // -----------------------------------------------------------
    // 1. [搜索优化] 只有 id 在 list 里，且返回的是 Projection
    // -----------------------------------------------------------
    List<ArticleSimpleView> findProjectedByIdIn(List<Long> ids);


    // -----------------------------------------------------------
    // 2. [Feed流优化] 查找关注的人的文章，返回 Projection
    // -----------------------------------------------------------
    // 只要返回类型写成 ArticleSimpleView (或 Page<ArticleSimpleView>)，Hibernate 就知道只查这几个字段
    Page<ArticleSimpleView> findByAuthorInAndStatusOrderByCreatedAtDesc(
            Collection<User> authors,
            Article.ArticleStatus status,
            Pageable pageable
    );


    // -----------------------------------------------------------
    // 3. [状态检查优化] 批量查询用户是否点赞了这批文章 (解决 N+1 问题)
    // -----------------------------------------------------------
    // 不要去加载 Article.favoritedBy 集合！那是灾难。
    // 直接查关联表。
    @Query(value = "SELECT article_id FROM article_favorites WHERE user_id = :userId AND article_id IN :articleIds", nativeQuery = true)
    Set<Long> findLikedArticleIds(@Param("userId") Long userId, @Param("articleIds") List<Long> articleIds);
}
