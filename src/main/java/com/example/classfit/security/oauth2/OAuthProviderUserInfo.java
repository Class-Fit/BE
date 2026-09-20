package com.example.classfit.security.oauth2;

import com.example.classfit.member.domain.enums.Gender;
import com.example.classfit.member.domain.enums.OAuthProvider;

/**
 * 소셜 로그인 제공자마다 다른 사용자 응답을 서비스 회원 형식으로 통일한다.
 */
public interface OAuthProviderUserInfo {

    /** 내부 회원 식별에 사용할 소셜 로그인 제공자를 반환한다. */
    OAuthProvider getProvider();

    /** 제공자가 발급한 변경되지 않는 사용자 식별자를 반환한다. */
    String getProviderId();

    /** 제공자가 전달한 표시 이름을 반환하며 없으면 null이다. */
    String getName();

    /** 사용자가 제공에 동의한 이메일을 반환하며 없으면 null이다. */
    String getEmail();

    /** 제공자 값을 서비스 성별 형식으로 반환하며 없으면 null이다. */
    Gender getGender();
}
