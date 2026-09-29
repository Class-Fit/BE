package com.example.classfit.course.service;

import com.example.classfit.course.dto.SyncResultCount;

public record CourseSyncResult(
        SyncResultCount facilities,
        SyncResultCount courses
) {
    public boolean hasFailures() {
        return facilities.failed() > 0 || courses.failed() > 0;
    }
}
