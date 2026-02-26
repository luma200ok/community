package com.community.community.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.hibernate.annotations.Comment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // 추후 UUID 업데이트
    private final String SECRET_KEY_STRING =
            "my-super-secret-key-for-community-project-must-be-long-enough";
    private final SecretKey secretKey =
            Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));

    // 2. 토큰 만료 시간 - 1H (1000ms * 60s *60m)
    private final long EXPIRATION_TIME = 1000 * 60 * 60;

    /**
     * 토큰 발급 메서드
     * 유저 번호(id)와 아이디(username)를 받아 암호화된 토큰을 만듬.
     */

    public String createToken(Long userId, String username) {
        return Jwts.builder()
                // 1.토큰에 담을 내용 (Payload)
                .claim("userId", userId)     // 토큰의 주인은 몇번 유저인가?
                .claim("username", username) // 아이디는 무엇인가?
                // 2. 발급 시간과 만료 시간 설정
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                // 3. 서버의 허가
                .signWith(secretKey)
                // 4. 문자열로 압축
                .compact();
    }
}
