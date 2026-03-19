package com.yachaerang.backend.global.exception;

import com.yachaerang.backend.global.response.ApiResponse;
import com.yachaerang.backend.global.response.ErrorCode;
import com.yachaerang.backend.global.response.ErrorResponse;
import com.yachaerang.backend.global.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 일반적인 예외 사항
    @ExceptionHandler(GeneralException.class)
    protected ResponseEntity<ErrorResponse> generalException(GeneralException e, HttpServletRequest request) {
        LogUtil.error(e, request);
        return ErrorResponse.of(e.getErrorCode());
    }

    // Batch 서버 호출 예외
    @ExceptionHandler(BatchException.class)
    protected ResponseEntity<ErrorResponse> handleBatchException(BatchException e, HttpServletRequest request) {
        LogUtil.error(e, request);
        return ErrorResponse.of(e.getErrorCode());
    }

    // 성공 응답(200)이되, 에러 메세지를 담아서 반환
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<?>> handlerCustomException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.ok(
                ApiResponse.failure(errorCode));
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        logError(ex, request);
        return ErrorResponse.of(ErrorCode.UNAUTHORIZED_ACCESS);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        logError(ex, request);
        return ErrorResponse.of(ErrorCode.UNMATCHED_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        logError(ex, request);
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    /*
     * Error 기록을 위한 logError
     */
    private void logError(Exception e, HttpServletRequest request) {
        log.error("Request URI : [{}] {}", request.getMethod(), request.getRequestURI());
        log.error("Exception : ", e);

        log.error("Exception Type : {}", e.getClass().getName());
        log.error("Exception Message : {}", e.getMessage());
    }
}
