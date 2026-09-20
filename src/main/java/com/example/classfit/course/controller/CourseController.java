package com.example.classfit.course.controller;

import com.example.classfit.common.ApiResponse;
import com.example.classfit.course.dto.CourseDetailResponse;
import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.course.service.CourseService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ApiResponse<Page<CourseSearchResponse>> searchCourses(
            @RequestParam(required = false) String localCode,
            @RequestParam(required = false) String sportCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return ApiResponse.success(courseService.searchCourses(
                localCode,
                sportCode,
                keyword,
                safePage,
                safeSize
        ));
    }

    @GetMapping("/{courseId}")
    public ApiResponse<CourseDetailResponse> getCourse(@PathVariable Long courseId) {
        return ApiResponse.success(courseService.getCourse(courseId));
    }
}
