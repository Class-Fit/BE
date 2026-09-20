package com.example.classfit.security.oauth2;

import com.example.classfit.member.domain.enums.Gender;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

/**
 * 카카오 사용자 정보 응답의 중첩 구조를 서비스 회원 정보로 변환한다.
 * 선택 동의 정보가 없거나 예상과 다른 타입이면 해당 값만 null로 처리한다.
 */
public class KakaoOAuth2UserInfo {

    private static final String INVALID_USER_INFO = "invalid_user_info";

    private final Map<String, Object> attributes;

    /** 사용자 정보 조회 결과를 받아 필드별 변환을 준비한다. */
    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * 카카오 응답의 루트 id를 손실 없는 문자열 식별자로 반환한다.
     */
    public String getProviderId() {
        Object id = attributes.get("id");
        if (!(id instanceof Long) && !(id instanceof Integer)
                && !(id instanceof java.math.BigInteger) && !(id instanceof String)) {
            throw invalidUserInfo();
        }

        String providerId = id.toString().trim();
        if (providerId.isEmpty() || "null".equals(providerId)
                || (id instanceof Number && new java.math.BigInteger(providerId).signum() <= 0)) {
            throw invalidUserInfo();
        }
        return providerId;
    }

    /**
     * 카카오 프로필 닉네임을 반환하며 제공되지 않으면 null을 반환한다.
     */
    public String getName() {
        Object profile = kakaoAccount().get("profile");
        if (!(profile instanceof Map<?, ?> profileMap)) {
            return null;
        }
        return stringValue(profileMap.get("nickname"));
    }

    /**
     * 동의 후 제공된 카카오 계정 이메일을 반환한다.
     */
    public String getEmail() {
        return stringValue(kakaoAccount().get("email"));
    }

    /**
     * 카카오의 male/female 값을 서비스 성별 enum으로 변환한다.
     */
    public Gender getGender() {
        String gender = stringValue(kakaoAccount().get("gender"));
        if ("male".equalsIgnoreCase(gender)) {
            return Gender.MALE;
        }
        if ("female".equalsIgnoreCase(gender)) {
            return Gender.FEMALE;
        }
        return null;
    }

    /** 선택적인 kakao_account 객체가 없으면 빈 객체처럼 취급한다. */
    private Map<?, ?> kakaoAccount() {
        Object account = attributes.get("kakao_account");
        if (account instanceof Map<?, ?> accountMap) {
            return accountMap;
        }
        return Map.of();
    }

    /** 비어 있지 않은 문자열만 선택 프로필 값으로 사용한다. */
    private String stringValue(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            return null;
        }
        return text;
    }

    /** 식별자 오류를 OAuth 인증 실패 처리기가 받을 수 있는 예외로 만든다. */
    private OAuth2AuthenticationException invalidUserInfo() {
        OAuth2Error error = new OAuth2Error(INVALID_USER_INFO);
        return new OAuth2AuthenticationException(error, INVALID_USER_INFO);
    }
}
