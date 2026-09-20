package com.example.classfit.member.repository;

import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 영속화와 소셜 계정 식별자 조회를 담당한다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 소셜 제공자와 제공자 회원 ID가 모두 일치하는 회원을 조회한다.
     */
    Optional<Member> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}
