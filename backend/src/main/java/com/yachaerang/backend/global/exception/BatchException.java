package com.yachaerang.backend.global.exception;

import com.yachaerang.backend.global.response.ErrorCode;

public class BatchException extends RuntimeException {

    private final ErrorCode errorCode;

    public BatchException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static BatchException of(ErrorCode errorCode) {
        return new BatchException(errorCode);
    }
}