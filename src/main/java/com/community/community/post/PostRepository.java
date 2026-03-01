package com.community.community.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository  extends JpaRepository<PostEntity, Long> {

    // 1. 전체 목록 가져올 때 N+1 방어
    @EntityGraph(attributePaths = {"userEntity"})
    Page<PostEntity> findAll(Pageable pageable);

    // 2. 검색할 때 N+1 방어
    // 제목(Title)에 포함(Containing)되거나(Or) 내용(Content)에 포함(Containing)된 것 찾기
    @EntityGraph(attributePaths = {"userEntity"})
    Page<PostEntity> findByTitleContainingOrContentContaining(String title, String content,
                                                              Pageable pageable);
}
