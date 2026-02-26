package com.community.community.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.community.community.exception.ErrorDto.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * IllegalArgumentException 이 터지면 이 메서드가 실행됩니다.
     * 우리가 서비스 로직에서 orElseThrow(...)로 던졌던 바로 그 에러입니다!
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {

        // 1. 상태 코드는 400 (잘못된 요청)으로 설정
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // 2. DTO에 상태 코드 숫자와, 메시지 담기
        ErrorResponse errorResponse = new ErrorResponse(status.value(), e.getMessage());

        // 3. 반환
        return ResponseEntity.status(status).body(errorResponse);
    }
}
