package com.hsu_mafia.motoo.kisdatacollector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("잘못된 인자 오류: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_ARGUMENT", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("서버 오류 발생", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }

    @Data
    @AllArgsConstructor
    static class ErrorResponse {
        private String code;
        private String message;
    }
}
