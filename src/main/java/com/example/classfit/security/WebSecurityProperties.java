package com.example.classfit.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 브라우저에서 백엔드를 호출할 수 있는 프론트엔드 출처를 정의한다. */
@ConfigurationProperties(prefix = "classfit.web")
public record WebSecurityProperties(String frontendOrigin) {

    public WebSecurityProperties {
        if (frontendOrigin == null || frontendOrigin.isBlank()) {
            throw new IllegalArgumentException("classfit.web.frontend-origin must not be blank");
        }
    }
}
