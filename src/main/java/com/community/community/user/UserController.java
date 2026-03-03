package com.community.community.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.community.community.user.UserDto.*;

@Tag(name = "유저 회원가입,로그인", description = "유저의 회원가입과 로그인을 담당하는 API 입니다.")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserSignupRequest request) {
        Long userId = userService.signUp(request);
        return ResponseEntity.ok("가입 완료");
    }

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
}
