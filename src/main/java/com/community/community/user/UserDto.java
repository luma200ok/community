package com.community.community.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDto {

    public record UserSignupRequest(
            @NotBlank(message = " 아이디는 필수입니다.")
            String username,
            @NotBlank(message = "비밀번호는 필수입니다.")
            String password,
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,
            @NotBlank(message = "비밀번호 찾기 힌트 정답을 입력해주세요.")
            String hintAnswer
    ) {
    }

    public record UserLoginRequest(
            String username,
            String password
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TokenResponse(
            String accessToken,
            String refreshToken
    ) {
    }

    public record TokenReissueRequest(
            String refreshToken
    ) {
    }

    public record PromoteRequest(
            @NotBlank String username,
            @NotBlank String secretKey
    ) {}

    // 임시 비밀번호 발급용
    public record PasswordFindRequest(
            @NotBlank(message = "아이디를 입력해주세요.")
            String username,
            @NotBlank(message = "가입 시 사용한 이메일을 입력해주세요.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email,
            @NotBlank(message = "가입 시 설정한 힌트 정답을 입력해주세요.")
            String hintAnswer
    ) {
    }
}