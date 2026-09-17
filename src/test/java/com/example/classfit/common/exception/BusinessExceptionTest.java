package com.example.classfit.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void throwsNullPointerExceptionWhenErrorCodeIsNull() {
        assertThatThrownBy(() -> new BusinessException(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ErrorCode는 null이 들어올 수 없습니다.");
    }

}
