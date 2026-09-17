package com.example.classfit.common.exception;

import lombok.Getter;

import java.util.Objects;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "ErrorCode는 null이 들어올 수 없습니다.").getMessage());
        this.errorCode = errorCode;
    }
}
