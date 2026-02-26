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
}