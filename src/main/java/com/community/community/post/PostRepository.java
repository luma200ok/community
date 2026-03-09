package com.community.community.post;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository  extends JpaRepository<PostEntity, Long> {

    // 1. 전체 목록 가져올 때 N+1 방어
    @EntityGraph(attributePaths = {"userEntity"})
    Page<PostEntity> findAll(Pageable pageable);

    // 2. 검색할 때 N+1 방어
    // 제목(Title)에 포함(Containing)되거나(Or) 내용(Content)에 포함(Containing)된 것 찾기
    @EntityGraph(attributePaths = {"userEntity"})
    Page<PostEntity> findByTitleContainingOrContentContaining(
            String title, String content,Pageable pageable);

    // 특정 유저가 쓴 글 목록 조회
    Page<PostEntity> findByUserEntity_id(Long userId, Pageable pageable);

    // 카테고리가 '공지'면 무조건 위로(1), 아니면 아래로(2). 그 안에서는 최신순 정렬!
    // 검색어가 없으면 전체 조회, 있으면 제목/내용 검색까지 한 번에 처리합니다.
    @Query("SELECT p FROM PostEntity p " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
            "ORDER BY CASE WHEN p.category = '공지' THEN 1 ELSE 2 END, p.createdAt DESC")
    Page<PostEntity> findPostsWithNoticeOnTop(@Param("keyword") String keyword, Pageable pageable);
}
