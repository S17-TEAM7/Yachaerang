package com.yachaerang.backend.global.exception;

import com.yachaerang.backend.global.response.ErrorCode;

public class BatchException extends GeneralException {

    public BatchException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static BatchException of(ErrorCode errorCode) {
        return new BatchException(errorCode);
    }
}
