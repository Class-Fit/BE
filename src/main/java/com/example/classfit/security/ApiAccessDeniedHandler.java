package com.example.classfit.security;

import com.example.classfit.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/** 권한 부족과 잘못된 CSRF 토큰에 대해 403 JSON을 반환한다. */
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityErrorResponseWriter writer;

    /** 접근 거부 사유의 내부 정보를 노출하지 않고 공통 오류를 작성한다. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        var error = CommonErrorCode.FORBIDDEN;
        writer.write(response, error.getStatus().value(), error.getCode(), error.getMessage());
    }
}
