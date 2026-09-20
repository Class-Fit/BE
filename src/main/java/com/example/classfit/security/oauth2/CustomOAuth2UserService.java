package com.example.classfit.security.oauth2;

import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.OAuthProvider;
import com.example.classfit.member.service.MemberService;
import com.example.classfit.security.LoginMember;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 카카오 사용자 정보를 내부 회원과 연결하고 세션에 저장할 인증 사용자를 만든다.
 */
@Service
public class CustomOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String KAKAO = "kakao";

    private final MemberService memberService;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    /** 운영에서는 Spring의 기본 사용자 정보 HTTP 클라이언트를 사용한다. */
    @Autowired
    public CustomOAuth2UserService(MemberService memberService) {
        this(memberService, new DefaultOAuth2UserService());
    }

    /** 사용자 정보 조회 구현을 주입받아 외부 요청과 회원 연결 책임을 분리한다. */
    CustomOAuth2UserService(
            MemberService memberService,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate
    ) {
        this.memberService = memberService;
        this.delegate = delegate;
    }

    /**
     * 제공자 사용자 정보를 조회한 뒤 카카오 ID로 회원을 찾거나 생성한다.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!KAKAO.equals(registrationId)) {
            OAuth2Error error = new OAuth2Error("unsupported_provider");
            throw new OAuth2AuthenticationException(error, "지원하지 않는 OAuth 제공자입니다.");
        }

        try {
            OAuth2User providerUser = delegate.loadUser(userRequest);
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(providerUser.getAttributes());
            Member member = memberService.findOrCreate(
                    OAuthProvider.KAKAO,
                    userInfo.getProviderId(),
                    userInfo.getName(),
                    userInfo.getEmail(),
                    userInfo.getGender()
            );
            return LoginMember.from(member);
        } catch (IllegalArgumentException exception) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"),
                    "카카오 사용자 정보를 확인할 수 없습니다.", exception);
        } catch (org.springframework.dao.DataAccessException exception) {
            throw new OAuth2AuthenticationException(new OAuth2Error("member_link_failed"),
                    "회원 연결에 실패했습니다.", exception);
        }
    }
}
