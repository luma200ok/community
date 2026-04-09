package com.community.community.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 💡 400 BAD_REQUEST (잘못된 요청)
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 확장자입니다. (jpg, jpeg, png, gif, webp만 허용)"),

    // 💡 403 FORBIDDEN (권한 없음)
    EDIT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "수정/삭제할 권한이 없습니다."),

    // 💡 404 NOT_FOUND (찾을 수 없음)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),

    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다."),
    INVALID_HINT_ANSWER(HttpStatus.BAD_REQUEST, "힌트 정답이 일치하지 않습니다."),
    EMAIL_SEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "메일 발송은 5분에 한 번만 가능합니다. 잠시 후 다시 시도해주세요."),

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    COMMENT_MISMATCH(HttpStatus.BAD_REQUEST, "해당 게시글의 댓글이 아닙니다."),

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 파일입니다."),
    FILE_EXTENSION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 확장자입니다."),

    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");

    // 필요한 에러가 생길 때마다 여기에 한 줄씩 추가만 하면 됩니다!

    private final HttpStatus httpStatus;
    private final String message;
}
