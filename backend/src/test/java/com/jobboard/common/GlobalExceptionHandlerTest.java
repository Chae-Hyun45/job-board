package com.jobboard.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void ApiException을_상태코드와_메시지로_변환한다() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ApiException exception = new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");

        ResponseEntity<Map<String, String>> response = handler.handleApiException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("message", "이미 가입된 이메일입니다.");
    }

    @Test
    void 업로드_크기_초과를_413과_한글_메시지로_변환한다() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> response =
                handler.handleMaxUploadSize(new MaxUploadSizeExceededException(10 * 1024 * 1024));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).containsEntry("message", "업로드 가능한 파일 크기(10MB)를 초과했습니다.");
    }

    @Test
    void 예상하지_못한_예외를_500과_한글_메시지로_변환한다() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, String>> response =
                handler.handleUnexpected(new java.io.UncheckedIOException(new java.io.IOException("boom")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "서버 오류가 발생했습니다.");
    }
}
