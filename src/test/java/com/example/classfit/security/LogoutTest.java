package com.example.classfit.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 로그아웃 시 실제 세션 무효화와 CSRF 접근 제한을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
class LogoutTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void invalidatesSessionAndExpiresCookie() throws Exception {
        MockHttpSession session = authenticatedSession();
        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/members/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingCsrfTokenDoesNotLogOut() throws Exception {
        MockHttpSession session = authenticatedSession();
        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    void invalidCsrfTokenDoesNotLogOut() throws Exception {
        MockHttpSession session = authenticatedSession();
        mvc.perform(post("/api/auth/logout").session(session).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    void getRequestDoesNotLogOut() throws Exception {
        MockHttpSession session = authenticatedSession();
        mvc.perform(get("/api/auth/logout").session(session)).andExpect(status().isForbidden());
        assertThat(session.isInvalid()).isFalse();
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "member", null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }
}
