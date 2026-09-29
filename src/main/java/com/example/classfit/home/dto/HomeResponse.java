package com.example.classfit.home.dto;

import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.inbody.dto.res.InBodyCreateRes;

import java.util.List;

/** 메인 화면 한 번의 요청에 필요한 공개 강좌와 선택적 회원 정보를 묶는다. */
public record HomeResponse(
        HomeMemberResponse member,
        List<CourseSearchResponse> popularCourses,
        List<CourseSearchResponse> latestCourses,
        InBodyCreateRes latestInBody
) {
}
