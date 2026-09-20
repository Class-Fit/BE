package com.example.classfit.member.service;

import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.Gender;
import com.example.classfit.member.domain.enums.MemberRole;
import com.example.classfit.member.domain.enums.OAuthProvider;
import com.example.classfit.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(MemberService.class)
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void createsMemberForNewOAuthIdentity() {
        Member member = memberService.findOrCreate(
                OAuthProvider.KAKAO,
                "new-member-id",
                null,
                null,
                null
        );

        assertThat(member.getId()).isNotNull();
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.getName()).isNull();
        assertThat(member.getHeight()).isNull();
        assertThat(member.getWeight()).isNull();
    }

    @Test
    void returnsExistingMemberWithoutOverwritingProfile() {
        Member existing = memberService.findOrCreate(
                OAuthProvider.KAKAO,
                "existing-member-id",
                "기존 이름",
                "old@example.com",
                Gender.FEMALE
        );

        Member loggedInAgain = memberService.findOrCreate(
                OAuthProvider.KAKAO,
                "existing-member-id",
                "변경된 카카오 이름",
                "new@example.com",
                Gender.MALE
        );

        assertThat(loggedInAgain.getId()).isEqualTo(existing.getId());
        assertThat(loggedInAgain.getName()).isEqualTo("기존 이름");
        assertThat(loggedInAgain.getEmail()).isEqualTo("old@example.com");
        assertThat(loggedInAgain.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(memberRepository.count()).isOne();
    }
}
