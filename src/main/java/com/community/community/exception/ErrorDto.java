package com.community.community.exception;

public class ErrorDto {

    public record ErrorResponse(
            int status,
            String message
    ) {
    }
}
