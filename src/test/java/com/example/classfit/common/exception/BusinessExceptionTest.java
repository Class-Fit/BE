package com.example.classfit.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BusinessExceptionTest {

    @Test
    void createBusinessException() {

        ErrorCode errorCode = CommonErrorCode.INVALID_REQUEST;

        // when
        BusinessException exception = new BusinessException(errorCode);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());

    }

}
