package com.example.classfit.security;

import com.example.classfit.member.domain.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 세션에 저장되는 인증 사용자이다.
 * 내부 회원 ID와 권한만 보관하며 원본 카카오 개인정보는 복사하지 않는다.
 */
public class LoginMember implements OAuth2User, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long memberId;
    private final List<GrantedAuthority> authorities;
    private final Map<String, Object> attributes;

    /** 인증에 필요한 값을 복사해 생성 후 변경되지 않도록 한다. */
    private LoginMember(
            Long memberId,
            List<GrantedAuthority> authorities,
            Map<String, Object> attributes
    ) {
        this.memberId = memberId;
        this.authorities = List.copyOf(authorities);
        this.attributes = Map.copyOf(attributes);
    }

    /**
     * 저장된 회원의 역할을 Spring Security 권한으로 변환해 인증 사용자를 만든다.
     */
    public static LoginMember from(Member member) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                "ROLE_" + member.getRole().name()
        );
        return new LoginMember(member.getId(), List.of(authority), Map.of("memberId", member.getId()));
    }

    /**
     * 서비스 회원의 기본키를 반환한다.
     */
    public Long getMemberId() {
        return memberId;
    }

    /** OAuth2User 계약에 맞춰 내부 회원 ID만 속성으로 제공한다. */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** 로그인 시 DB에서 읽은 역할을 접근 제어에 제공한다. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Security principal의 안정적인 이름으로 내부 회원 ID를 사용한다.
     */
    @Override
    public String getName() {
        return memberId.toString();
    }
}
