package com.community.community.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
    private final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 60;
    private final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7;

    /**
     * AccessToken 발급 (기존 createToken)
     * 유저 번호(id)와 아이디(username)를 받아 암호화된 토큰을 만듬.
     */
    public String createAccessToken(Long userId, String username) {
        return Jwts.builder()
                // 1.토큰에 담을 내용 (Payload)
                .claim("userId", userId)     // 토큰의 주인은 몇번 유저인가?
                .claim("username", username) // 아이디는 무엇인가?
                // 2. 발급 시간과 만료 시간 설정
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                // 3. 서버의 허가
                .signWith(secretKey)
                // 4. 문자열로 압축
                .compact();
    }

    /**\
     * Refresh Token 발급
     * Refresh Token은 검증 및 교환용이므로 username없이 userId만 담아도 충분
     */
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                // 1.토큰에 담을 내용 (Payload)
                .claim("userId", userId)     // 토큰의 주인은 몇번 유저인가?
                // 2. 발급 시간과 만료 시간 설정
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                // 3. 서버의 허가
                .signWith(secretKey)
                // 4. 문자열로 압축
                .compact();
    }

    /**
     * 토큰 해독기: 토큰을 열어 내부 알맹이 확인
     * 이 과정에서 토큰이 조작되거나 만료되었다면 에러 발생
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token).getPayload();
    }

    /**
     * 토큰 검증기 : 이 토큰이 유효한지 확인.
     */

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
