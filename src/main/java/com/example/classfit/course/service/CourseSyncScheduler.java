package com.example.classfit.course.service;

import com.example.classfit.course.domain.sync.SyncTriggerType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "public-data.sync.enabled", havingValue = "true")
public class CourseSyncScheduler {

    private final CourseSyncCoordinator courseSyncCoordinator;

    public CourseSyncScheduler(CourseSyncCoordinator courseSyncCoordinator) {
        this.courseSyncCoordinator = courseSyncCoordinator;
    }

    @Scheduled(cron = "${public-data.sync.cron}", zone = "${public-data.sync.zone}")
    public void syncGangwonCourses() {
        courseSyncCoordinator.sync(SyncTriggerType.SCHEDULED);
    }
}
