package com.example.classfit.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void BusinessException을_정의된_에러_응답으로_변환한다() throws Exception {
        mockMvc.perform(get("/test/business-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("요청값이 올바르지 않습니다."));
    }

    @Test
    void 처리하지_못한_예외는_서버_내부_오류로_변환한다() throws Exception {
        mockMvc.perform(get("/test/unexpected-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errorCode")
                        .value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    void 존재하지_않는_경로는_NotFound_응답으로_변환한다() throws Exception {
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("요청한 리소스를 찾을 수 없습니다."));
    }

    @Test
    void 읽을_수_없는_JSON은_InvalidRequest_응답으로_변환한다() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("요청값이 올바르지 않습니다."));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business-exception")
        void throwBusinessException() {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        @GetMapping("/test/unexpected-exception")
        void throwUnexpectedException() {
            throw new RuntimeException("테스트용 예상하지 못한 예외");
        }

        @PostMapping("/test/body")
        void readBody(@RequestBody TestRequest request) {
        }
    }

    record TestRequest(String value) {
    }
}
