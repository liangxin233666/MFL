package io.github.liangxin233666.mfl.repositories;

import io.github.liangxin233666.mfl.entities.User;
import io.github.liangxin233666.mfl.repositories.projections.UserSimpleView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // 1. 查询我关注的人 (My Following)
    // 逻辑：查询 User u，条件是 u 的 followers 集合中包含 :userId
    @Query("SELECT u.id as id, u.username as username, u.image as image " +
            "FROM User u JOIN u.followers f WHERE f.id = :userId")
    Page<UserSimpleView> findFollowingByUserId(@Param("userId") Long userId, Pageable pageable);

    // 2. 查询关注我的人 (My Followers/Fans)
    // 逻辑：查询 User u，条件是 u 的 following 集合中包含 :userId
    @Query("SELECT u.id as id, u.username as username, u.image as image " +
            "FROM User u JOIN u.following f WHERE f.id = :userId")
    Page<UserSimpleView> findFollowersByUserId(@Param("userId") Long userId, Pageable pageable);

    // 3. 批量检查我是否关注了这些用户 (用于处理粉丝列表中的"回关"状态)
    // 直接查关联表 user_follows，避免加载 Entity
    @Query(value = "SELECT followed_id FROM user_follows WHERE follower_id = :currentUserId AND followed_id IN :targetIds", nativeQuery = true)
    Set<Long> checkFollowingStatus(@Param("currentUserId") Long currentUserId, @Param("targetIds") List<Long> targetIds);
}