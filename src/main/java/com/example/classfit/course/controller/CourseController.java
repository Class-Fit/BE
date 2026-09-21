package com.example.classfit.course.controller;

import com.example.classfit.common.ApiResponse;
import com.example.classfit.common.PageResponse;
import com.example.classfit.common.exception.BusinessException;
import com.example.classfit.common.exception.CommonErrorCode;
import com.example.classfit.course.dto.CourseDetailResponse;
import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.course.service.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공개 강좌 검색과 상세 조회 요청을 처리한다. */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** 검색 조건과 페이지 요청을 받아 강좌 목록을 공통 응답 형식으로 반환한다. */
    @GetMapping
    public ApiResponse<PageResponse<CourseSearchResponse>> searchCourses(
            @RequestParam(required = false) String localCode,
            @RequestParam(required = false) String sportCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size <= 0) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        int safeSize = Math.min(size, 100);

        return ApiResponse.success(courseService.searchCourses(
                localCode,
                sportCode,
                keyword,
                page,
                safeSize
        ));
    }

    /** 강좌 식별자로 공개 상세 정보를 조회한다. */
    @GetMapping("/{courseId}")
    public ApiResponse<CourseDetailResponse> getCourse(@PathVariable Long courseId) {
        return ApiResponse.success(courseService.getCourse(courseId));
    }
}
