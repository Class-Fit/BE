package com.example.classfit.security.oauth2;

import com.example.classfit.member.domain.enums.OAuthProvider;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;

import java.util.Map;

/** OAuth 등록 이름에 맞는 제공자와 사용자 정보 변환기를 선택한다. */
@Component
public class OAuthProviderUserInfoFactory {

    /** 미지원 제공자는 외부 사용자 정보 요청 전에 인증 실패로 처리한다. */
    public OAuthProvider resolveProvider(String registrationId) {
        if ("kakao".equals(registrationId)) {
            return OAuthProvider.KAKAO;
        }
        throw unsupportedProvider();
    }

    /** 등록 이름과 원본 응답으로 해당 제공자의 사용자 정보 변환기를 만든다. */
    public OAuthProviderUserInfo create(String registrationId, Map<String, Object> attributes) {
        OAuthProvider provider = resolveProvider(registrationId);
        return switch (provider) {
            case KAKAO -> new KakaoOAuth2UserInfo(attributes);
            default -> throw unsupportedProvider();
        };
    }

    private OAuth2AuthenticationException unsupportedProvider() {
        return new OAuth2AuthenticationException(
                new OAuth2Error("unsupported_provider"),
                "지원하지 않는 OAuth 제공자입니다."
        );
    }
}
