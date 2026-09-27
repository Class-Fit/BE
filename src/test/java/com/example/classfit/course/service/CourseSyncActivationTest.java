package com.example.classfit.course.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CourseSyncActivationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CourseSyncScheduler.class, TestConfig.class);

    @Test
    void schedulerIsNotRegisteredWhenSyncIsNotExplicitlyEnabled() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(CourseSyncScheduler.class));
    }

    @Test
    void applicationDefaultsDisableScheduledSync() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yaml"))
                .forEach(environment.getPropertySources()::addLast);

        assertThat(environment.getProperty("public-data.sync.enabled"))
                .isEqualTo("false");
    }

    @Configuration
    static class TestConfig {
        @Bean
        CourseSyncService courseSyncService() {
            return mock(CourseSyncService.class);
        }
    }
}
