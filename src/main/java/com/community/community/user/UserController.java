package com.community.community.user;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.community.community.user.UserDto.*;

@Tag(name = "🔐 유저 인증 API", description = "회원가입, 로그인, 로그아웃 및 JWT 토큰 재발급 기능을 제공합니다.")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "아이디, 비밀번호, 이메일을 입력받아 새로운 사용자를 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody UserSignupRequest request) {
        Long userId = userService.signUp(request);
        return ResponseEntity.ok("가입 완료");
    }

    @Operation(summary = "로그인", description = "아이디와 비밀번호를 검증한 후, 성공 시 Access Token과 Refresh Token을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserLoginRequest request) {

        // 서비스에 아이디와 비밀번호 넘겨 검증.
        TokenResponse tokenResponse = userService.login(request);

        // 에러 발생 없이 여기까지 오면 로그인 성공
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "토큰 재발급"
            , description = "Refresh Token을 보내면 새로운 Access, Refresh Token을 발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody TokenReissueRequest request) {

        // 서비스에 Refresh Token 던져주고 새 토큰 세트 받아오기
        TokenResponse token = userService.reissue(request);

        return ResponseEntity.ok(token);
    }

    @Operation(summary = "로그아웃",
            description = "서버에 저장된 Refresh Token을 삭제하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        userService.logout(userId);

        return ResponseEntity.ok("성공적으로 로그아웃 되었습니다.");
    }

    @Operation(
            summary = "비밀번호 찾기",
            description = "아이디와 이메일을 입력하면, 해당 이메일로 임시 비밀번호를 발송합니다.")
    @PostMapping("/password/find")
    public ResponseEntity<String> findPassword(@RequestBody @Valid PasswordFindRequest request) {

        userService.resetPasswordAndSendEmail(request);

        return ResponseEntity.ok("입력하신 이메일로 임시 비밀번호가 성공적으로 발송되었습니다.");
    }

    @Hidden
    @PostMapping("/promote")
    public ResponseEntity<String> promoteToAdmin(
            @RequestParam String username,
            @RequestParam String secretKey) { // 주소창이나 파라미터로 시크릿 키를 받음

        userService.promoteToAdmin(username, secretKey);
        return ResponseEntity.ok("관리자로 성공적으로 승급되었습니다. 환영합니다, 마스터.");
    }
}
