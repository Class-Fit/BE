package com.example.classfit.course.controller;

import com.example.classfit.common.ApiResponse;
import com.example.classfit.course.domain.sync.SyncTriggerType;
import com.example.classfit.course.dto.CourseSyncResponse;
import com.example.classfit.course.service.CourseSyncCoordinator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
public class CourseSyncController {

    private final CourseSyncCoordinator courseSyncCoordinator;

    public CourseSyncController(CourseSyncCoordinator courseSyncCoordinator) {
        this.courseSyncCoordinator = courseSyncCoordinator;
    }

    @PostMapping("/sync")
    public ApiResponse<CourseSyncResponse> syncGangwonCourses() {
        return ApiResponse.success(courseSyncCoordinator.sync(SyncTriggerType.MANUAL));
    }
}
