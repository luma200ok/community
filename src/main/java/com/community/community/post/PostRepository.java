package com.community.community.post;

import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository  extends JpaRepository<PostEntity, Long>,PostRepositoryCustom {

    // 특정 유저가 쓴 글 목록 조회
    Page<PostEntity> findByUserEntity_id(Long userId, Pageable pageable);

    // ==========================================
    // 💡 [추가] S3 좀비 파일 + 오래된 삭제 게시글 일괄 청소용 Native 쿼리
    // ==========================================
    // 1. S3에서 지워야 할 이미지 URL 목록 싹 뽑아오기
    @Query(value = "SELECT pi.image_url FROM post_images pi JOIN post p ON pi.post_id = p.id WHERE p.is_deleted = true AND p.updated_at < :threshold", nativeQuery = true)
    List<String> findImageUrlsByOldDeletedPosts(@Param("threshold") LocalDateTime threshold);

    // 2. DB에서 부모가 지워지기 전에 자식(post_image) 데이터 먼저 싹 지우기 (외래키 제약조건 방어)
    @Modifying
    @Query(value = "DELETE FROM post_images WHERE post_id IN (SELECT id FROM post WHERE is_deleted = true AND updated_at < :threshold)", nativeQuery = true)
    void deleteImagesByOldDeletedPosts(@Param("threshold") LocalDateTime threshold);
    
    // 2-1. DB에서 부모가 지워지기 전에 자식(comment) 데이터 먼저 싹 지우기 (외래키 제약조건 방어)
    @Modifying
    @Query(value = "DELETE FROM comment WHERE post_id IN (SELECT id FROM post WHERE is_deleted = true AND updated_at < :threshold)", nativeQuery = true)
    void deleteCommentsByOldDeletedPosts(@Param("threshold") LocalDateTime threshold);

    // 3. DB에서 30일 지난 부모(게시글) 싹 지우기
    @Modifying
    @Query(value = "DELETE FROM post WHERE is_deleted = true AND updated_at < :threshold", nativeQuery = true)
    int deleteOldDeletedPosts(@Param("threshold") LocalDateTime threshold);

    // H-1: 좋아요 카운트 원자적 증가/감소 (Race Condition 방지)
    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PostEntity p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);
}
