package com.community.community.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. 유저가 보낸 HTTP 요청 헤더에서 "Authorization"라는 이름의 티켓 찾음.
        String bearerToken = request.getHeader("Authorization");

        // 2. 티켓이 존재하고, "Bearer"로 시작하는지 확인.
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // 3. "bearer" 글자를 떼어내고 진짜 토큰 알맹이만(aaa.bbb.ccc) 추출
            String token = bearerToken.substring(7);

            // 4. 토큰 검증
            if (jwtUtil.validateToken(token)) {
                // 5. 유효한 토큰이라면, 유저 번호(userId)를 추출
                Long userId = jwtUtil.getClaims(token).get("userId", Long.class);

                // 6. 스프링 시큐리티에게 "이 토큰은 확인 끝낫다" 라고 확인시켜줌.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 7. 검사가 끝났으면 다음 필터로 요청
        filterChain.doFilter(request, response);
    }
}
