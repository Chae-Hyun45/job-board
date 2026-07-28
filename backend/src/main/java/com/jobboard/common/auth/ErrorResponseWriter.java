package com.jobboard.common.auth;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 인터셉터에서 GlobalExceptionHandler와 동일한 {"message": "..."} 형식으로 응답한다.
 * sendError()는 server.error.include-message 기본 설정에서 메시지가 제거되기 때문에 직접 작성한다.
 */
final class ErrorResponseWriter {

    private ErrorResponseWriter() {
    }

    static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
