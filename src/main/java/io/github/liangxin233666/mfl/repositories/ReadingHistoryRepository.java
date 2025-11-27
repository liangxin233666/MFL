package io.github.liangxin233666.mfl.repositories;

import io.github.liangxin233666.mfl.entities.Article;
import io.github.liangxin233666.mfl.entities.ReadingHistory;
import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.repositories.projections.HistorySimpleView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {

    // 【极速查询】
    // 直接返回 HistorySimpleView 投影，Spring 会生成优化后的 SQL，只查需要的列
    Page<HistorySimpleView> findByUserOrderByViewedAtDesc(User user, Pageable pageable);

    Optional<ReadingHistory> findByUserAndArticle(User user, Article article);

    // 【极速修剪】
    // 一条 SQL 删除第 N 条之后的旧记录
    @Modifying
    @Query(value = """
        DELETE FROM reading_history 
        WHERE id IN (
            SELECT id FROM (
                SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY viewed_at DESC) as rn
                FROM reading_history 
                WHERE user_id = :userId
            ) t WHERE t.rn > :limitCount
        )
    """, nativeQuery = true)
    void keepMostRecentRecords(@Param("userId") Long userId, @Param("limitCount") int limitCount);
}