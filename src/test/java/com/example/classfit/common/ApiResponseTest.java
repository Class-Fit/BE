package com.example.classfit.common;

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
}
