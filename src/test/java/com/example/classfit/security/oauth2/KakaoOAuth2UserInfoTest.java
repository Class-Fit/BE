package com.example.classfit.security.oauth2;

import com.example.classfit.member.domain.enums.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoOAuth2UserInfoTest {

    @Test
    void rejectsInvalidIdentifierTypesAndValues() {
        for (Object id : java.util.List.of(0L, -1L, 1.5, true, " ", "null")) {
            assertThatThrownBy(() -> new KakaoOAuth2UserInfo(Map.of("id", id)).getProviderId())
                    .isInstanceOf(OAuth2AuthenticationException.class);
        }
    }

    @Test
    void readsKakaoIdAndOptionalProfileValues() {
        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(Map.of(
                "id", 123456789L,
                "kakao_account", Map.of(
                        "email", "member@example.com",
                        "gender", "female",
                        "profile", Map.of("nickname", "카카오 사용자")
                )
        ));

        assertThat(userInfo.getProviderId()).isEqualTo("123456789");
        assertThat(userInfo.getName()).isEqualTo("카카오 사용자");
        assertThat(userInfo.getEmail()).isEqualTo("member@example.com");
        assertThat(userInfo.getGender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    void returnsNullWhenOptionalProfileValuesAreMissing() {
        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(Map.of("id", "provider-id"));

        assertThat(userInfo.getProviderId()).isEqualTo("provider-id");
        assertThat(userInfo.getName()).isNull();
        assertThat(userInfo.getEmail()).isNull();
        assertThat(userInfo.getGender()).isNull();
    }

    @Test
    void rejectsMissingKakaoId() {
        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(Map.of());

        assertThatThrownBy(userInfo::getProviderId)
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("invalid_user_info");
    }

    @Test
    void ignoresMalformedOptionalNestedValues() {
        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(Map.of(
                "id", 1L,
                "kakao_account", "not-an-object"
        ));

        assertThat(userInfo.getName()).isNull();
        assertThat(userInfo.getEmail()).isNull();
        assertThat(userInfo.getGender()).isNull();
    }
}
