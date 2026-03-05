package com.community.community.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;

import static com.community.community.exception.ErrorDto.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 잘못된 요청 (IllegalArgumentException 잡기) - 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. 권한 없음 (수정/삭제 시 작성자가 아닐 때) - 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorResponse response = new ErrorResponse(HttpStatus.FORBIDDEN.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 3. 파일 용량 초과 (application.yaml에서 설정한 10MB 넘었을 때) - 413 Payload Too Large
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSizeException(MaxUploadSizeExceededException e) {
        ErrorResponse response = new ErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE.value(), "업로드 할 수 있는 파일 용량을 초과했습니다.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    // 4. 그 외 예상치 못한 모든 서버 에러 - 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        e.printStackTrace(); // 서버 로그에는 진짜 에러의 원인을 찍어주고
        ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부에서 에러가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 프론트에게는 정제된 메시지만 전달
    }

    // 5. CustomException 전담 핸들러
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        // 에러 상자에서 ErrorCode를 꺼냅니다.
        ErrorCode errorCode = e.getErrorCode();

        // ErrorCode에 적힌 상태 코드와 메시지를 그대로 씁니다.
        ErrorResponse response = new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }
}
