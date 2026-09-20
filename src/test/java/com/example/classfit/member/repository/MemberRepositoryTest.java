package com.example.classfit.member.repository;

import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void findsMemberByOAuthProviderAndProviderId() {
        Member member = memberRepository.saveAndFlush(Member.createOAuthMember(
                OAuthProvider.KAKAO,
                "123456789",
                "카카오 사용자",
                null,
                null
        ));

        Member found = memberRepository
                .findByProviderAndProviderId(OAuthProvider.KAKAO, "123456789")
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(member.getId());
        assertThat(found.getProviderId()).isEqualTo("123456789");
    }

    @Test
    void rejectsDuplicateOAuthIdentity() {
        memberRepository.saveAndFlush(Member.createOAuthMember(
                OAuthProvider.KAKAO,
                "duplicated-id",
                null,
                null,
                null
        ));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(Member.createOAuthMember(
                OAuthProvider.KAKAO,
                "duplicated-id",
                null,
                null,
                null
        )))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
