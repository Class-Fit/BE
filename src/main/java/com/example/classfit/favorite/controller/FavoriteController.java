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

@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/api/courses/{courseId}/favorites")
    public ApiResponse<FavoriteStatusResponse> addFavorite(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginMember loginMember
    ) {
        return ApiResponse.success(favoriteService.addFavorite(loginMember.getMemberId(), courseId));
    }

    @DeleteMapping("/api/courses/{courseId}/favorites")
    public ApiResponse<FavoriteStatusResponse> removeFavorite(
            @PathVariable Long courseId,
            @AuthenticationPrincipal LoginMember loginMember
    ) {
        return ApiResponse.success(favoriteService.removeFavorite(loginMember.getMemberId(), courseId));
    }

    @GetMapping("/api/members/me/favorites")
    public ApiResponse<List<CourseSearchResponse>> getFavorites(
            @AuthenticationPrincipal LoginMember loginMember
    ) {
        return ApiResponse.success(favoriteService.getFavorites(loginMember.getMemberId()));
    }
}
