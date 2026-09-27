package com.example.classfit.course.service;

import com.example.classfit.course.config.CourseSyncSchedulingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        CourseSyncSchedulingConfig.class,
        CourseSyncScheduler.class,
        CourseSyncSchedulerTest.TestConfig.class
})
@TestPropertySource(properties = {
        "public-data.sync.cron=*/1 * * * * *",
        "public-data.sync.zone=Asia/Seoul"
})
class CourseSyncSchedulerTest {

    @Autowired
    private CourseSyncService courseSyncService;

    @Test
    void scheduledTriggerStartsTheExistingSyncService() {
        verify(courseSyncService, timeout(2500).atLeastOnce()).syncGangwonCourses();
    }

    @Configuration
    static class TestConfig {
        @Bean
        CourseSyncService courseSyncService() {
            return mock(CourseSyncService.class);
        }
    }
}
