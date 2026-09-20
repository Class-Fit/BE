package com.example.classfit.security;

import com.example.classfit.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;

/** 인증되지 않은 요청을 로그인 HTML 대신 401 JSON으로 응답한다. */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityErrorResponseWriter writer;

    /** 인증이 필요한 API를 익명 사용자가 호출했을 때 실행된다. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        var error = CommonErrorCode.UNAUTHORIZED;
        writer.write(response, error.getStatus().value(), error.getCode(), error.getMessage());
    }
}
