package com.example.classfit.course.dto;

import com.example.classfit.course.domain.sync.CourseSyncRun;
import com.example.classfit.course.domain.sync.SyncRunStatus;
import com.example.classfit.course.domain.sync.SyncTriggerType;

import java.time.LocalDateTime;

public record CourseSyncResponse(
        Long runId,
        SyncTriggerType triggerType,
        SyncRunStatus status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        SyncResultCount facilities,
        SyncResultCount courses
) {
    public static CourseSyncResponse from(CourseSyncRun run) {
        return new CourseSyncResponse(
                run.getId(),
                run.getTriggerType(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.facilityCounts(),
                run.courseCounts()
        );
    }
}
