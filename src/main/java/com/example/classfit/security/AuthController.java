package com.example.classfit.security;

import com.example.classfit.common.ApiResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 로그인 세션에서 로그아웃 요청에 필요한 CSRF 토큰을 제공한다. */
@RestController
public class AuthController {

    @GetMapping("/api/auth/csrf")
    public ApiResponse<Map<String, String>> csrf(@RequestAttribute("_csrf") CsrfToken token) {
        return ApiResponse.success(Map.of("token", token.getToken()));
    }
}
