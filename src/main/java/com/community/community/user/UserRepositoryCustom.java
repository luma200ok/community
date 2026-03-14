package com.community.community.user;

import java.util.Optional;

public interface UserRepositoryCustom {
    Optional<UserEntity> findActiveUserByEmail(String email);
}
