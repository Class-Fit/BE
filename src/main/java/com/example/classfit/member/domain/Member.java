package com.example.classfit.member.domain;

import com.example.classfit.common.BaseEntity;
import com.example.classfit.member.domain.enums.Gender;
import com.example.classfit.member.domain.enums.MemberRole;
import com.example.classfit.member.domain.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_members_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        }
)
public class Member extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(length = 40)
    private String name;

    private String email;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberRole role;

    private Long height;

    private Long weight;

    /**
     * 처음 로그인한 소셜 계정을 일반 회원으로 생성한다.
     * 제공자가 전달하지 않을 수 있는 프로필 값은 null을 허용한다.
     */
    public static Member createOAuthMember(
            OAuthProvider provider,
            String providerId,
            String name,
            String email,
            Gender gender
    ) {
        Member member = new Member();
        member.provider = provider;
        member.providerId = providerId;
        member.name = name;
        member.email = email;
        member.gender = gender;
        member.role = MemberRole.USER;
        return member;
    }

}
