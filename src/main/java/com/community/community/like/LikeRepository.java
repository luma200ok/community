package com.community.community.like;

import com.community.community.post.PostEntity;
import com.community.community.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikeEntity, Long> {

    // 유저가 게시글에 좋아요를 눌렀는가? 확인용
    Optional<LikeEntity> findByUserEntityAndPostEntity(UserEntity user, PostEntity post);
}
