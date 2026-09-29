package com.example.classfit.home.dto;

/** 메인 화면에서 사용할 최소 로그인 회원 정보를 나타낸다. */
public record HomeMemberResponse(
        boolean loggedIn,
        Long id,
        String name
) {
    public static HomeMemberResponse anonymous() {
        return new HomeMemberResponse(false, null, null);
    }

    public static HomeMemberResponse authenticated(Long id, String name) {
        return new HomeMemberResponse(true, id, name);
    }
}
