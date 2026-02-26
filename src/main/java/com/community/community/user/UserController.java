package com.community.community.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.community.community.user.UserDto.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserSignupRequest request) {
        Long userId = userService.singUp(request);
        return ResponseEntity.ok("가입 완료");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserLoginRequest request) {

        // 서비스에 아이디와 비밀번호 넘겨 검증.
        String token = userService.login(request);

        // 에러 발생 없이 여기까지 오면 로그인 성공
        return ResponseEntity.ok("로그인 성공 , JWT 토큰:" + token);
    }
}
