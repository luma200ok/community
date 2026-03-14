package com.community.community.like;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static com.community.community.like.QLikeEntity.likeEntity;

@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsLike(Long userId, Long postId) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(likeEntity)
                .where(likeEntity.userEntity.id.eq(userId)
                        .and(likeEntity.postEntity.id.eq(postId)))
                .fetchFirst(); // limit 1과 동일
        return fetchOne != null;
    }
}