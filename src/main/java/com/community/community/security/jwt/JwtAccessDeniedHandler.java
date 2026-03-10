package com.community.community.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // 응답 형식을 JSON으로, 인코딩 UTF-8 설정
        response.setContentType("application/json;charset=UTF-8");
        // 상태 코드 403 (Forbidden)
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        // JSON 문자열로 에러 메시지 작성
        response.getWriter().write(
                "{\"status\":403, \"message\": \"해당 기능에 대한 접근 권한이 없습니다.\"}"
        );
    }
}
