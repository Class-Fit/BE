package com.example.classfit.security;

import com.example.classfit.member.repository.MemberRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 외부 HTTP 응답만 대체하고 실제 인가 코드 교환·회원 저장·세션 인증을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OAuthCallbackIntegrationTest {
    static final AtomicBoolean tokenFails = new AtomicBoolean();
    static final AtomicBoolean userInfoFails = new AtomicBoolean();
    static final AtomicBoolean userInfoMissingId = new AtomicBoolean();
    static final AtomicInteger externalCalls = new AtomicInteger();
    static final HttpServer provider = startProvider();

    @Autowired WebApplicationContext context;
    @Autowired MemberRepository members;
    MockMvc mvc;

    @DynamicPropertySource
    static void overrideProvider(DynamicPropertyRegistry properties) {
        String base = "http://127.0.0.1:" + provider.getAddress().getPort();
        properties.add("spring.security.oauth2.client.provider.kakao.token-uri", () -> base + "/token");
        properties.add("spring.security.oauth2.client.provider.kakao.user-info-uri", () -> base + "/user");
        properties.add("spring.datasource.url", () -> "jdbc:h2:mem:oauth-callback;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        members.deleteAll();
        tokenFails.set(false);
        userInfoFails.set(false);
        userInfoMissingId.set(false);
        externalCalls.set(0);
    }

    @AfterAll
    static void stopProvider() {
        provider.stop(0);
    }

    @Test
    void callbackCreatesSessionAndRepeatedLoginReusesMember() throws Exception {
        MockHttpSession first = login();
        Long memberId = members.findAll().getFirst().getId();
        mvc.perform(get("/api/members/me").session(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.name").value("카카오 회원"));

        MockHttpSession second = login();
        mvc.perform(get("/api/members/me").session(second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(memberId));
        assertThat(members.count()).isOne();
        assertThat(externalCalls.get()).isEqualTo(4);
    }

    @Test
    void invalidStateDoesNotContactProviderOrCreateMember() throws Exception {
        LoginAttempt attempt = begin();
        mvc.perform(get("/login/oauth2/code/kakao").session(attempt.session())
                        .param("code", "test-code").param("state", "wrong-state"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("OAUTH_LOGIN_FAILED"));
        assertFailedLogin(attempt);
        assertThat(externalCalls.get()).isZero();
    }

    @Test
    void cancelledConsentDoesNotCreateMember() throws Exception {
        LoginAttempt attempt = begin();
        mvc.perform(get("/login/oauth2/code/kakao").session(attempt.session())
                        .param("error", "access_denied").param("state", attempt.state()))
                .andExpect(status().isUnauthorized());
        assertFailedLogin(attempt);
        assertThat(externalCalls.get()).isZero();
    }

    @Test
    void tokenExchangeFailureDoesNotAuthenticate() throws Exception {
        tokenFails.set(true);
        LoginAttempt attempt = begin();
        callback(attempt).andExpect(status().isUnauthorized());
        assertFailedLogin(attempt);
    }

    @Test
    void userInfoFailureDoesNotAuthenticate() throws Exception {
        userInfoFails.set(true);
        LoginAttempt attempt = begin();
        callback(attempt).andExpect(status().isUnauthorized());
        assertFailedLogin(attempt);
    }

    @Test
    void userInfoWithoutProviderIdDoesNotAuthenticate() throws Exception {
        userInfoMissingId.set(true);
        LoginAttempt attempt = begin();

        callback(attempt)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("OAUTH_LOGIN_FAILED"));

        assertFailedLogin(attempt);
    }

    private void assertFailedLogin(LoginAttempt attempt) throws Exception {
        assertThat(members.count()).isZero();
        mvc.perform(get("/api/members/me").session(attempt.session()))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = callback(begin()).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/api/members/me")).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private org.springframework.test.web.servlet.ResultActions callback(LoginAttempt attempt) throws Exception {
        return mvc.perform(get("/login/oauth2/code/kakao").session(attempt.session())
                .param("code", "test-code").param("state", attempt.state()));
    }

    private LoginAttempt begin() throws Exception {
        MvcResult result = mvc.perform(get("/oauth2/authorization/kakao"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String state = UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl())
                .build().getQueryParams().getFirst("state");
        return new LoginAttempt((MockHttpSession) result.getRequest().getSession(false),
                URLDecoder.decode(state, StandardCharsets.UTF_8));
    }

    private record LoginAttempt(MockHttpSession session, String state) {}

    private static HttpServer startProvider() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/token", exchange -> {
                externalCalls.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                String body = tokenFails.get() ? "{\"error\":\"invalid_grant\"}" :
                        "{\"access_token\":\"fake-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(tokenFails.get() ? 400 : 200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.createContext("/user", exchange -> {
                externalCalls.incrementAndGet();
                String body = userInfoFails.get() ? "{\"error\":\"unavailable\"}" :
                        userInfoMissingId.get() ? "{\"kakao_account\":{}}" :
                                "{\"id\":123456789,\"kakao_account\":{\"profile\":{\"nickname\":\"카카오 회원\"}}}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(userInfoFails.get() ? 503 : 200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("모의 OAuth 서버 시작 실패", exception);
        }
    }
}
