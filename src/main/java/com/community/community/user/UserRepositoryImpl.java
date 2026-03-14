package com.community.community.user;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static com.community.community.user.QUserEntity.userEntity;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<UserEntity> findActiveUserByEmail(String email) {
        return Optional.ofNullable(queryFactory
                .selectFrom(userEntity)
                .where(userEntity.email.eq(email))
                .fetchOne());
    }
}