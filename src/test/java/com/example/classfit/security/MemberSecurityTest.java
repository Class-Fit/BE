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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 보안 필터와 DB를 사용해 서비스 회원 조회 및 API 접근 정책을 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberSecurityTest {
    @Autowired WebApplicationContext context;
    @Autowired MemberRepository members;
    MockMvc mvc;

    /** 실제 애플리케이션 보안 필터를 적용한 MVC 테스트 환경을 구성한다. */
    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** HTML을 요청하는 비로그인 회원 조회도 로그인 리다이렉트 대신 JSON 401을 반환하는지 검증한다. */
    @Test
    void anonymousMemberRequestReturnsJson401EvenFromBrowser() throws Exception {
        mvc.perform(get("/api/members/me").accept("text/html"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void chatRequiresLoginAndUsesPrincipalInsteadOfSuppliedMemberId() throws Exception {
        mvc.perform(get("/api/chat/conversations").param("memberId", "1"))
                .andExpect(status().isUnauthorized());
        Member member = members.saveAndFlush(Member.createOAuthMember(
                OAuthProvider.KAKAO, "chat-owner", "회원", null, null));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/chat/conversations")
                        .param("memberId", "999999")
                        .with(oauth2Login().oauth2User(LoginMember.from(member))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/chat/conversations").param("memberId", "999999")
                        .with(oauth2Login().oauth2User(LoginMember.from(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    /** 비로그인 사용자의 찜 목록 요청이 JSON 401로 거부되는지 검증한다. */
    @Test
    void anonymousFavoriteRequestReturnsJson401() throws Exception {
        mvc.perform(get("/api/members/me/favorites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    /** 메인 화면 공개 데이터는 로그인하지 않은 사용자도 조회할 수 있는지 검증한다. */
    @Test
    void anonymousUserCanReadHome() throws Exception {
        mvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.member.loggedIn").value(false))
                .andExpect(jsonPath("$.data.popularCourses").isArray())
                .andExpect(jsonPath("$.data.latestCourses").isArray());
    }

    /** 로그인한 회원이 자신의 빈 찜 목록을 정상적으로 조회할 수 있는지 검증한다. */
    @Test
    void authenticatedMemberCanReadFavorites() throws Exception {
        LoginMember principal = savedLoginMember("favorite-reader");

        mvc.perform(get("/api/members/me/favorites")
                        .with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /** 로그인한 회원의 찜 등록 요청이 서비스에 도달해 없는 강좌의 404를 반환하는지 검증한다. */
    @Test
    void authenticatedMemberCanReachFavoriteRegistration() throws Exception {
        LoginMember principal = savedLoginMember("favorite-writer");

        mvc.perform(post("/api/courses/999999/favorites")
                        .with(oauth2Login().oauth2User(principal))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    /** 로그인한 회원이 없는 강좌의 찜을 취소하면 JSON 404를 반환하는지 검증한다. */
    @Test
    void favoriteCancellationReturns404ForMissingCourse() throws Exception {
        LoginMember principal = savedLoginMember("favorite-remover");

        mvc.perform(delete("/api/courses/999999/favorites")
                        .with(oauth2Login().oauth2User(principal))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    /** 로그인 회원 조회가 저장된 회원 정보를 반환하고 OAuth 내부 정보를 노출하지 않는지 검증한다. */
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

    /** 인증 정보에 남아 있는 회원이 DB에서 삭제되면 회원 없음 오류를 반환하는지 검증한다. */
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

    /** 로그인한 사용자도 허용되지 않은 API 경로에는 접근할 수 없는지 검증한다. */
    @Test
    void authenticatedUserCannotAccessUnapprovedRoute() throws Exception {
        mvc.perform(get("/api/unapproved").with(oauth2Login()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    /** OAuth 로그인 시작 요청이 카카오 인증 페이지로 이동하는지 검증한다. */
    @Test
    void redirectsLoginStartToKakao() throws Exception {
        mvc.perform(get("/oauth2/authorization/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://kauth.kakao.com/oauth/authorize?")));
    }

    /** 잘못된 OAuth 콜백이 인증 코드를 노출하지 않는 JSON 오류로 처리되는지 검증한다. */
    @Test
    void invalidCallbackReturnsSafeJsonFailure() throws Exception {
        mvc.perform(get("/login/oauth2/code/kakao").param("code", "secret-code").param("state", "invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("OAUTH_LOGIN_FAILED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-code"))));
    }

    /**
     * DB에 OAuth 회원을 저장하고 보안 테스트에서 사용할 인증 주체를 생성한다.
     *
     * @param providerId 테스트 회원을 구분하는 OAuth 제공자 식별자
     * @return 저장된 회원을 가리키는 인증 주체
     */
    private LoginMember savedLoginMember(String providerId) {
        Member member = members.saveAndFlush(Member.createOAuthMember(
                OAuthProvider.KAKAO, providerId, "회원", null, null
        ));
        return LoginMember.from(member);
    }
}
