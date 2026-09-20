package com.example.classfit.course.controller;

import com.example.classfit.common.ApiResponse;
import com.example.classfit.course.dto.CourseSyncResponse;
import com.example.classfit.course.service.CourseSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
public class CourseSyncController {

    private final CourseSyncService courseSyncService;

    public CourseSyncController(CourseSyncService courseSyncService) {
        this.courseSyncService = courseSyncService;
    }

    @PostMapping("/sync")
    public ApiResponse<CourseSyncResponse> syncGangwonCourses() {
        return ApiResponse.success(courseSyncService.syncGangwonCourses());
    }
}
