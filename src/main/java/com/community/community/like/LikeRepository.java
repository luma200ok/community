package com.community.community.like;

import com.community.community.post.PostEntity;
import com.community.community.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {

    // 유저가 게시글에 좋아요를 눌렀는가? 확인용
    Optional<LikeEntity> findByUserEntityAndPostEntity(UserEntity user, PostEntity post);

    // 특정 유저가 좋아요 누른 내역 조회 (이걸 통해 게시글에 접근)
    Page<LikeEntity> findByUserEntity_Id(Long userId, Pageable pageable);

}
