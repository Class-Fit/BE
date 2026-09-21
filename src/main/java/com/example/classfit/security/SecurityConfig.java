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
                        .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/chat/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/chat/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/inbodies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/inbodies/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/inbodies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/courses/sync").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/members/me").authenticated()
                        .anyRequest().denyAll())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/chat/**")
                        .ignoringRequestMatchers("/api/inbodies/**")
                )
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                // POST와 유효한 CSRF 토큰으로만 서비스 세션을 종료한다.
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(info -> info.userService(userService))
                        .defaultSuccessUrl("/api/members/me", true)
                        .failureHandler((request, response, exception) -> writer.write(response, 401,
                                "OAUTH_LOGIN_FAILED", "카카오 로그인에 실패했습니다. 다시 시도해주세요.")));
        return http.build();
    }
}
