package com.example.classfit.course.dto;

public record CourseSyncResponse(
        SyncResultCount facilities,
        SyncResultCount courses
) {
}
