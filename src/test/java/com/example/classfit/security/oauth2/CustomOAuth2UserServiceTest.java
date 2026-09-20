package com.example.classfit.security.oauth2;

import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.MemberRole;
import com.example.classfit.member.service.MemberService;
import com.example.classfit.security.LoginMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Test
    void rejectsUnsupportedProviderBeforeCallingExternalService() {
        assertThatThrownBy(() -> new CustomOAuth2UserService(memberService, delegate)
                .loadUser(oauth2UserRequest("naver")))
                .isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class);
    }

    @Test
    void convertsPersistenceFailureToAuthenticationFailure() {
        OAuth2UserRequest request = oauth2UserRequest("kakao");
        when(delegate.loadUser(request)).thenReturn(new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("id", 123L), "id"));
        when(memberService.findOrCreate(
                com.example.classfit.member.domain.enums.OAuthProvider.KAKAO, "123", null, null, null))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("private database details"));

        assertThatThrownBy(() -> new CustomOAuth2UserService(memberService, delegate).loadUser(request))
                .isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class)
                .hasMessage("회원 연결에 실패했습니다.");
    }

    @Mock
    private MemberService memberService;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @Mock
    private Member member;

    @Test
    void linksKakaoUserToMemberAndUsesStoredRole() {
        Map<String, Object> attributes = Map.of(
                "id", 987654321L,
                "kakao_account", Map.of("profile", Map.of("nickname", "카카오 사용자"))
        );
        OAuth2UserRequest request = oauth2UserRequest("kakao");
        OAuth2User providerUser = new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
        when(delegate.loadUser(request)).thenReturn(providerUser);
        when(memberService.findOrCreate(
                com.example.classfit.member.domain.enums.OAuthProvider.KAKAO,
                "987654321",
                "카카오 사용자",
                null,
                null
        )).thenReturn(member);
        when(member.getId()).thenReturn(42L);
        when(member.getRole()).thenReturn(MemberRole.ADMIN);

        LoginMember loginMember = (LoginMember) new CustomOAuth2UserService(memberService, delegate)
                .loadUser(request);

        assertThat(loginMember.getMemberId()).isEqualTo(42L);
        assertThat(loginMember.getName()).isEqualTo("42");
        assertThat(loginMember.getAttributes()).containsOnlyKeys("memberId");
        assertThat(loginMember.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    private OAuth2UserRequest oauth2UserRequest(String registrationId) {
        Builder registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id");
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );
        return new OAuth2UserRequest(registration.build(), token);
    }
}
