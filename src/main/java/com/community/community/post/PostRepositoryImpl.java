package com.community.community.post;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.community.community.post.QPostEntity.postEntity;
import static com.community.community.user.QUserEntity.userEntity;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PostEntity> findPostsWithNoticeOnTop(String keyword, Pageable pageable) {
// 1. 데이터(Content) 가져오는 쿼리
        List<PostEntity> content = queryFactory
                .selectFrom(postEntity)
                .leftJoin(postEntity.userEntity, userEntity).fetchJoin() // 🔥 User 정보를 한 방에 가져와서 N+1 완벽 방어
                .where(containsKeyword(keyword)) // 🔥 조건이 없으면 where절 생략 (동적 쿼리)
                .orderBy(
                        // 카테고리가 '공지'면 우선순위 1, 아니면 2
                        new CaseBuilder()
                                .when(postEntity.category.eq("공지")).then(1)
                                .otherwise(2).asc(),
                        postEntity.createdAt.desc() // 그 다음은 최신순 정렬
                )
                .offset(pageable.getOffset()) // 페이징 시작점
                .limit(pageable.getPageSize()) // 페이지 사이즈
                .fetch();

        // 2. 전체 데이터 개수(Count) 가져오는 쿼리 (페이징 처리를 위해 필수)
        Long total = queryFactory
                .select(postEntity.count())
                .from(postEntity)
                .where(containsKeyword(keyword))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // 💡 동적 쿼리를 위한 도우미 메서드
    private BooleanExpression containsKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null; // 검색어가 없으면 조건을 아예 빼버림 (전체 조회)
        }
        return postEntity.title.contains(keyword).or(postEntity.content.contains(keyword));
    }
}
