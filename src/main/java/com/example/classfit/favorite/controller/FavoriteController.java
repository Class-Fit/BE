package com.example.classfit.favorite.controller;

import com.example.classfit.common.ApiResponse;
import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.favorite.dto.FavoriteStatusResponse;
import com.example.classfit.favorite.service.FavoriteService;
import com.example.classfit.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 로그인한 회원의 강좌 찜 등록, 취소 및 목록 조회 API를 제공한다. */
@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 인증된 회원의 강좌 찜을 등록하고 중복 요청에도 등록 상태를 반환한다.
     *
     * @param courseId 찜할 강좌의 내부 식별자
     * @param loginMember 인증된 요청 회원
     * @return 강좌 식별자와 등록된 찜 상태를 담은 공통 응답
     */
    @PostMapping("/api/courses/{courseId}/favorites")
    public ApiResponse<FavoriteStatusResponse> addFavorite(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginMember loginMember
    ) {
        return ApiResponse.success(favoriteService.addFavorite(loginMember.getMemberId(), courseId));
    }

    /**
     * 인증된 회원의 찜을 취소한다. 존재하지 않는 강좌는 서비스에서 오류로 처리한다.
     *
     * @param courseId 찜을 취소할 강좌의 내부 식별자
     * @param loginMember 인증된 요청 회원
     * @return 강좌 식별자와 취소된 찜 상태를 담은 공통 응답
     */
    @DeleteMapping("/api/courses/{courseId}/favorites")
    public ApiResponse<FavoriteStatusResponse> removeFavorite(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginMember loginMember
    ) {
        return ApiResponse.success(favoriteService.removeFavorite(loginMember.getMemberId(), courseId));
    }

    /**
     * 인증된 회원의 찜 강좌를 찜 식별자 내림차순으로 조회한다.
     *
     * @param loginMember 인증된 요청 회원
     * @return 강좌와 시설 정보를 포함한 찜 목록이며, 찜이 없으면 빈 목록
     */
    @GetMapping("/api/members/me/favorites")
    public ApiResponse<List<CourseSearchResponse>> getFavorites(
            @AuthenticationPrincipal LoginMember loginMember
    ) {
        return ApiResponse.success(favoriteService.getFavorites(loginMember.getMemberId()));
    }
}
