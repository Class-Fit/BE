package com.example.classfit.member.service;

import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.Gender;
import com.example.classfit.member.domain.enums.OAuthProvider;
import com.example.classfit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인 과정에서 내부 회원을 찾거나 새로 생성한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 소셜 식별자가 이미 등록되어 있으면 기존 회원을 반환하고,
     * 처음 보는 식별자이면 일반 회원을 생성한다.
     * 재로그인 시 카카오 프로필로 기존 서비스 프로필을 덮어쓰지 않는다.
     */
    @Transactional
    public Member findOrCreate(
            OAuthProvider provider,
            String providerId,
            String name,
            String email,
            Gender gender
    ) {
        return memberRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> memberRepository.save(Member.createOAuthMember(
                        provider,
                        providerId,
                        name,
                        email,
                        gender
                )));
    }
}
