package com.example.classfit.course.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "public-data.sync.enabled", havingValue = "true")
public class CourseSyncScheduler {

    private final CourseSyncService courseSyncService;

    public CourseSyncScheduler(CourseSyncService courseSyncService) {
        this.courseSyncService = courseSyncService;
    }

    @Scheduled(cron = "${public-data.sync.cron}", zone = "${public-data.sync.zone}")
    public void syncGangwonCourses() {
        courseSyncService.syncGangwonCourses();
    }
}
