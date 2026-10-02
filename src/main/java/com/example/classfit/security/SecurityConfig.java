package com.example.classfit.security;

import com.example.classfit.security.oauth2.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

import java.util.List;

/** 로컬 백엔드의 세션 기반 카카오 로그인과 API 접근 정책을 구성한다. */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(WebSecurityProperties.class)
public class SecurityConfig {

    /**
     * OAuth 로그인, API 접근 권한 및 보안 오류 응답을 구성한다.
     * 찜 쓰기 API는 인증이 필요하지만 시연용 CSRF 예외가 적용되어 있다.
     * 운영 배포 전에는 클라이언트의 CSRF 토큰 전달과 함께 해당 예외를 제거해야 한다.
     *
     * @param http 보안 필터 체인 빌더
     * @param userService OAuth 회원 정보를 연결하는 서비스
     * @param entryPoint 인증되지 않은 요청의 오류 처리기
     * @param deniedHandler 접근 권한이 없는 요청의 오류 처리기
     * @param writer OAuth 실패의 JSON 응답 작성기
     * @return 서비스의 HTTP 보안 필터 체인
     * @throws Exception 보안 필터 체인을 구성하지 못할 경우
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CustomOAuth2UserService userService,
                                            ApiAuthenticationEntryPoint entryPoint,
                                            ApiAccessDeniedHandler deniedHandler,
                                            SecurityErrorResponseWriter writer) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/home").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/chat/**").authenticated()
                        .requestMatchers(HttpMethod.POST,"/api/chat/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/inbodies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/inbodies/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/inbodies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/members/api/test/auth").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/courses/*/favorites").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/*/favorites").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/members/me/favorites").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/admin/courses/sync").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/members/me").authenticated()
                        .anyRequest().denyAll())
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/chat/**")
                        .ignoringRequestMatchers("/api/inbodies/**")
                        .ignoringRequestMatchers("/api/admin/courses/sync")
                        .ignoringRequestMatchers("/api/courses/*/favorites")
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

    @Bean
    CorsConfigurationSource corsConfigurationSource(WebSecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(properties.frontendOrigin()));
        configuration.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
