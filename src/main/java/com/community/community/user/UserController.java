package com.community.community.user;

import com.community.community.exception.CustomException;
import com.community.community.exception.ErrorCode;
import com.community.community.security.RateLimitService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static com.community.community.user.UserDto.PasswordFindRequest;
import static com.community.community.user.UserDto.PromoteRequest;
import static com.community.community.user.UserDto.TokenReissueRequest;
import static com.community.community.user.UserDto.TokenResponse;
import static com.community.community.user.UserDto.UserLoginRequest;
import static com.community.community.user.UserDto.UserSignupRequest;

@Tag(name = "🔐 유저 인증 API", description = "회원가입, 로그인, 로그아웃 및 JWT 토큰 재발급 기능을 제공합니다.")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RateLimitService rateLimitService;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    @Operation(summary = "회원가입", description = "아이디, 비밀번호, 이메일을 입력받아 새로운 사용자를 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody UserSignupRequest request,
                                         HttpServletRequest httpRequest) {
        rateLimitService.checkSignupRateLimit(httpRequest.getRemoteAddr());
        userService.signUp(request);
        return ResponseEntity.ok("가입 완료");
    }

    @Operation(summary = "로그인", description = "아이디와 비밀번호를 검증한 후, Access Token 반환 및 Refresh Token을 httpOnly 쿠키로 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserLoginRequest request,
                                               HttpServletResponse response,
                                               HttpServletRequest httpRequest) {
        rateLimitService.checkLoginRateLimit(httpRequest.getRemoteAddr());
        TokenResponse tokenResponse = userService.login(request);

        setRefreshTokenCookie(response, tokenResponse.refreshToken());

        // refreshToken은 쿠키로만 전달하므로 응답 바디에는 포함하지 않음
        return ResponseEntity.ok(new TokenResponse(tokenResponse.accessToken(), null));
    }

    @Operation(summary = "토큰 재발급",
            description = "httpOnly 쿠키의 Refresh Token을 검증하고 새로운 Access, Refresh Token을 발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        TokenResponse token = userService.reissue(new TokenReissueRequest(refreshToken));

        setRefreshTokenCookie(response, token.refreshToken());

        return ResponseEntity.ok(new TokenResponse(token.accessToken(), null));
    }

    @Operation(summary = "로그아웃",
            description = "서버에 저장된 Refresh Token을 삭제하고 쿠키를 만료시켜 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            HttpServletResponse response) {

        userService.logout(userId);
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok("성공적으로 로그아웃 되었습니다.");
    }

    // ─── 쿠키 헬퍼 ────────────────────────────────────────────
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/users")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/users")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Operation(
            summary = "비밀번호 찾기",
            description = "아이디와 이메일을 입력하면, 해당 이메일로 임시 비밀번호를 발송합니다.")
    @PostMapping("/password/find")
    public ResponseEntity<String> findPassword(@RequestBody @Valid PasswordFindRequest request,
                                               HttpServletRequest httpRequest) {
        rateLimitService.checkPasswordFindRateLimit(httpRequest.getRemoteAddr());
        userService.resetPasswordAndSendEmail(request);

        return ResponseEntity.ok("입력하신 이메일로 임시 비밀번호가 성공적으로 발송되었습니다.");
    }

    @Hidden
    @PostMapping("/promote")
    public ResponseEntity<String> promoteToAdmin(@RequestBody @Valid PromoteRequest request) {
        userService.promoteToAdmin(request.username(), request.secretKey());
        return ResponseEntity.ok("관리자로 성공적으로 승급되었습니다. 환영합니다, 마스터.");
    }
}
