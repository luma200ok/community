package com.community.community.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // 응답 형식을 JSON으로, 인코딩을 UTF-8로 설정
        response.setContentType("application/json;charset=UTF-8");

        // 상태 코드 401 (Unauthorized)
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // JSON 문자열로 에러 메시지 작성
        response.getWriter().write(
                "{\"status\": 401, \"message\": \"로그인이 필요하거나 토큰이 유효하지 않습니다.\"}");
    }
}
