package com.community.community.user;

public class UserDto {

    public record UserSignupRequest(
            String username,
            String password,
            String email
    ) {
        // 핵심 : DTO를 Entity로 변환해주는 편의 메서드
        public UserEntity toEntity() {
            return UserEntity.builder()
                    .username(this.username())
                    .password(this.password())
                    .email(this.email())
                    .build();
        }
    }

    public record UserLoginRequest(
            String username,
            String password
    ) {
    }
}