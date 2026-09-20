package com.example.classfit.security;

import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.OAuthProvider;
import com.example.classfit.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 보안 필터와 DB를 사용해 서비스 회원 조회 및 API 접근 정책을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberSecurityTest {
    @Autowired WebApplicationContext context;
    @Autowired MemberRepository members;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void anonymousMemberRequestReturnsJson401EvenFromBrowser() throws Exception {
        mvc.perform(get("/api/members/me").accept("text/html"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void returnsPersistedMemberWithoutProviderSecrets() throws Exception {
        Member member = members.saveAndFlush(Member.createOAuthMember(
                OAuthProvider.KAKAO, "7654321", "회원", "member@example.com", null));
        mvc.perform(get("/api/members/me").with(oauth2Login().oauth2User(LoginMember.from(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(member.getId()))
                .andExpect(jsonPath("$.data.name").value("회원"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.providerId").doesNotExist())
                .andExpect(jsonPath("$.data.attributes").doesNotExist());
    }

    @Test
    void missingMemberReturnsDomain404() throws Exception {
        Member member = members.saveAndFlush(Member.createOAuthMember(
                OAuthProvider.KAKAO, "7654322", null, null, null));
        LoginMember principal = LoginMember.from(member);
        members.delete(member);
        members.flush();
        mvc.perform(get("/api/members/me").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void authenticatedUserCannotAccessUnapprovedRoute() throws Exception {
        mvc.perform(get("/api/unapproved").with(oauth2Login()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void redirectsLoginStartToKakao() throws Exception {
        mvc.perform(get("/oauth2/authorization/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://kauth.kakao.com/oauth/authorize?")));
    }

    @Test
    void invalidCallbackReturnsSafeJsonFailure() throws Exception {
        mvc.perform(get("/login/oauth2/code/kakao").param("code", "secret-code").param("state", "invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("OAUTH_LOGIN_FAILED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-code"))));
    }
}
