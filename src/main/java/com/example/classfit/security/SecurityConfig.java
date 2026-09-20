package com.example.classfit.security;

import com.example.classfit.security.oauth2.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

/** 로컬 백엔드의 세션 기반 카카오 로그인과 API 접근 정책을 구성한다. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** OAuth 인증, 고정 성공 경로, 보안 오류 JSON과 허용 경로를 연결한다. */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CustomOAuth2UserService userService,
                                            ApiAuthenticationEntryPoint entryPoint,
                                            ApiAccessDeniedHandler deniedHandler,
                                            SecurityErrorResponseWriter writer) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/members/me").authenticated()
                        .anyRequest().denyAll())
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(info -> info.userService(userService))
                        .defaultSuccessUrl("/api/members/me", true)
                        .failureHandler((request, response, exception) -> writer.write(response, 401,
                                "OAUTH_LOGIN_FAILED", "카카오 로그인에 실패했습니다. 다시 시도해주세요.")));
        return http.build();
    }
}
