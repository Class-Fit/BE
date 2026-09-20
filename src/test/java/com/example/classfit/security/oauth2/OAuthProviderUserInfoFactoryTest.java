package com.example.classfit.security.oauth2;

import com.example.classfit.member.domain.enums.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthProviderUserInfoFactoryTest {

    private final OAuthProviderUserInfoFactory factory = new OAuthProviderUserInfoFactory();

    @Test
    void createsKakaoAdapterFromRegistrationId() {
        OAuthProviderUserInfo userInfo = factory.create("kakao", Map.of("id", 123L));

        assertThat(userInfo).isInstanceOf(KakaoOAuth2UserInfo.class);
        assertThat(userInfo.getProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(userInfo.getProviderId()).isEqualTo("123");
    }

    @Test
    void rejectsUnsupportedProvider() {
        assertThatThrownBy(() -> factory.create("naver", Map.of("id", "naver-id")))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessage("지원하지 않는 OAuth 제공자입니다.");
    }
}
