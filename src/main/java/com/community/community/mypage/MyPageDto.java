package com.community.community.mypage;

public class MyPageDto {

    public record PasswordUpdateRequest(
            String currentPassword,
            String newPassword
    ) {
    }
}
