package com.community.community.like;

public interface LikeRepositoryCustom {
    boolean existsLike(Long userId, Long postId);
}
