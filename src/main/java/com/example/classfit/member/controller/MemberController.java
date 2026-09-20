package com.example.classfit.member.controller;

import com.example.classfit.common.ApiResponse;
import com.example.classfit.member.dto.MemberMeResponse;
import com.example.classfit.member.service.MemberService;
import com.example.classfit.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인증된 회원 본인의 정보 조회를 제공한다. */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    /** 요청 파라미터 대신 인증 principal의 ID로 본인 정보를 조회한다. */
    @GetMapping("/me")
    public ApiResponse<MemberMeResponse> me(@AuthenticationPrincipal LoginMember loginMember) {
        return ApiResponse.success(MemberMeResponse.from(memberService.findById(loginMember.getMemberId())));
    }
}
