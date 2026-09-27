package com.example.classfit.course.service;

import com.example.classfit.course.config.CourseSyncSchedulingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void applicationDefaultsScheduleNextRunAtThreeAmInSeoul() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yaml"))
                .forEach(environment.getPropertySources()::addLast);

        String cron = environment.getRequiredProperty("public-data.sync.cron");
        ZoneId zone = ZoneId.of(environment.getRequiredProperty("public-data.sync.zone"));
        Instant after = Instant.parse("2026-09-27T10:00:00Z");

        var next = CronExpression.parse(cron).next(after.atZone(zone));
        assertThat(next).isNotNull();
        assertThat(next.toInstant()).isEqualTo(Instant.parse("2026-09-27T18:00:00Z"));
    }

    @Configuration
    static class TestConfig {
        @Bean
        CourseSyncService courseSyncService() {
            return mock(CourseSyncService.class);
        }
    }
}
