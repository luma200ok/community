package com.community.community.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository  extends JpaRepository<PostEntity, Long> {

    // 제목(Title)에 포함(Containing)되거나(Or) 내용(Content)에 포함(Containing)된 것 찾기
    Page<PostEntity> findByTitleContainingOrContentContaining(String title, String content,
                                                              Pageable pageable);
}
