package com.example.classfit.common;

import com.example.classfit.common.exception.CommonErrorCode;
import com.example.classfit.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiResponseTest {

    @Test
    void createsSuccessResponse(){

        String data = "응답 값";

        // when
        ApiResponse<String> res = ApiResponse.success(data);

        // then
        assertThat(res.success()).isTrue();
        assertThat(res.data()).isEqualTo(data);
        assertThat(res.errorCode()).isNull();
        assertThat(res.message()).isNull();

    }

    @Test
    void createsErrorResponse(){

        ErrorCode errorCode = CommonErrorCode.INVALID_REQUEST;

        // when
        ApiResponse<Void> response =
                ApiResponse.failure(
                        errorCode.getCode(),
                        errorCode.getMessage()
                );

        // then
        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.errorCode()).isEqualTo(errorCode.getCode());
        assertThat(response.message()).isEqualTo(errorCode.getMessage());

    }
}
