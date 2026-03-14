package com.community.community.comment;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.community.community.comment.QCommentEntity.commentEntity;
import static com.community.community.user.QUserEntity.userEntity;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommentEntity> findCommentsByPostIdWithUser(Long postId) {
        return queryFactory
                .selectFrom(commentEntity)
                .leftJoin(commentEntity.userEntity, userEntity).fetchJoin() // 🔥 댓글 작성자 정보 N+1 방어
                .where(commentEntity.postEntity.id.eq(postId))
                .orderBy(
                        commentEntity.parent.id.asc().nullsFirst(), // 부모 댓글 먼저
                        commentEntity.createdAt.asc()              // 그 다음 작성순
                )
                .fetch();
    }
}
