package com.community.community.user;

public class UserDto {

    public record UserSignupRequest(
            String username,
            String password,
            String email
    ) {
    }

    public record UserLoginRequest(
            String username,
            String password
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken
    ) {
    }

    public record TokenReissueRequest(
            String refreshToken
    ) {
    }
}