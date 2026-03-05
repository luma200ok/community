package com.community.community.comment;

import com.community.community.post.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    // 게시글 번호를 통한 조회
    @EntityGraph(attributePaths = {"userEntity"})
    List<CommentEntity> findByPostEntityId(Long postId);

    // 특정 유저가 쓴 댓글 목록 조회
    Page<CommentEntity> findByUserEntity_id(Long userId, Pageable pageable);
}
