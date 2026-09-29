package com.example.classfit.course.repository;

import com.example.classfit.course.domain.sync.CourseSyncRun;
import com.example.classfit.course.domain.sync.SyncRunStatus;
import com.example.classfit.course.domain.sync.SyncTriggerType;
import com.example.classfit.course.dto.SyncResultCount;
import com.example.classfit.course.service.CourseSyncResult;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CourseSyncRunRepositoryTest {

    @Autowired
    CourseSyncRunRepository runRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void storesExecutionStatusTimesAndCounts() {
        CourseSyncRun run = runRepository.saveAndFlush(CourseSyncRun.start(SyncTriggerType.MANUAL));
        run.complete(new CourseSyncResult(
                new SyncResultCount(1, 2, 3, 4, 0),
                new SyncResultCount(5, 6, 7, 8, 0)
        ));
        Long runId = runRepository.saveAndFlush(run).getId();
        entityManager.clear();

        CourseSyncRun saved = runRepository.findById(runId).orElseThrow();

        assertThat(saved.getTriggerType()).isEqualTo(SyncTriggerType.MANUAL);
        assertThat(saved.getStatus()).isEqualTo(SyncRunStatus.SUCCESS);
        assertThat(saved.getStartedAt()).isNotNull();
        assertThat(saved.getFinishedAt()).isNotNull();
        assertThat(saved.facilityCounts()).isEqualTo(new SyncResultCount(1, 2, 3, 4, 0));
        assertThat(saved.courseCounts()).isEqualTo(new SyncResultCount(5, 6, 7, 8, 0));
    }
}
