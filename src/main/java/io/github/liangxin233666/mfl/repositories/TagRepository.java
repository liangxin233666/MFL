package io.github.liangxin233666.mfl.repositories;

import io.github.liangxin233666.mfl.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    // 根据标签名查找标签
    Optional<Tag> findByName(String name);
    Set<Tag> findByNameIn(Set<String> tagNames);
}