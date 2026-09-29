package com.example.classfit.home.controller;

import com.example.classfit.common.ApiResponse;
import com.example.classfit.home.dto.HomeResponse;
import com.example.classfit.home.service.HomeService;
import com.example.classfit.security.LoginMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 익명 사용자와 로그인 사용자가 함께 사용하는 메인 화면 조회 API를 제공한다. */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    /** 인증 주체가 있으면 회원 ID를 전달하고 없으면 공개 메인 데이터를 조회한다. */
    @GetMapping
    public ApiResponse<HomeResponse> getHome(
            @AuthenticationPrincipal LoginMember loginMember
    ) {
        Long memberId = loginMember == null ? null : loginMember.getMemberId();
        return ApiResponse.success(homeService.getHome(memberId));
    }
}
