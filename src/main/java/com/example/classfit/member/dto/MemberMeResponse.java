package com.example.classfit.member.dto;

import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.Gender;
import com.example.classfit.member.domain.enums.MemberRole;

/** 로그인한 회원 본인에게 반환하는 정보이며 소셜 식별자와 토큰은 포함하지 않는다. */
public record MemberMeResponse(Long id, String name, String email, Gender gender,
                               MemberRole role, Long height, Long weight) {

    /** 영속 엔티티를 API 전용 응답으로 변환한다. */
    public static MemberMeResponse from(Member member) {
        return new MemberMeResponse(member.getId(), member.getName(), member.getEmail(),
                member.getGender(), member.getRole(), member.getHeight(), member.getWeight());
    }
}
