package com.community.community.like;

import com.community.community.post.PostEntity;
import com.community.community.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long>, LikeRepositoryCustom {

    Optional<LikeEntity> findByUserEntityAndPostEntity(UserEntity user, PostEntity post);

    Page<LikeEntity> findByUserEntity_Id(Long userId, Pageable pageable);

    boolean existsByUserEntity_IdAndPostEntity_Id(Long userId, Long postId);

    // H-1: INSERT IGNORE — 중복 시 예외 없이 0 반환 (Race Condition 완전 제거)
    @Modifying
    @Query(value = "INSERT IGNORE INTO post_like (user_id, post_id, created_at, updated_at) VALUES (:userId, :postId, NOW(), NOW())", nativeQuery = true)
    int insertIgnore(@Param("userId") Long userId, @Param("postId") Long postId);

    @Modifying
    @Query(value = "DELETE FROM post_like WHERE user_id = :userId AND post_id = :postId", nativeQuery = true)
    int deleteByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);
}
