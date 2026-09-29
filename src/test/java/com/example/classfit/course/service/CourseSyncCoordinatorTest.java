package com.example.classfit.course.service;

import com.example.classfit.course.domain.sync.CourseSyncRun;
import com.example.classfit.course.domain.sync.SyncRunStatus;
import com.example.classfit.course.domain.sync.SyncTriggerType;
import com.example.classfit.course.dto.CourseSyncResponse;
import com.example.classfit.course.dto.SyncResultCount;
import com.example.classfit.course.repository.CourseSyncRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseSyncCoordinatorTest {

    CourseSyncService syncService;
    CourseSyncRunRepository runRepository;
    CourseSyncCoordinator coordinator;
    List<SyncRunStatus> savedStatuses;

    @BeforeEach
    void setUp() {
        syncService = mock(CourseSyncService.class);
        runRepository = mock(CourseSyncRunRepository.class);
        savedStatuses = new ArrayList<>();
        when(runRepository.save(any(CourseSyncRun.class))).thenAnswer(invocation -> {
            CourseSyncRun run = invocation.getArgument(0);
            savedStatuses.add(run.getStatus());
            return run;
        });
        coordinator = new CourseSyncCoordinator(syncService, runRepository);
    }

    @Test
    void storesSuccessfulManualRunAndReturnsDetailedCounts() {
        SyncResultCount facilities = new SyncResultCount(1, 2, 3, 4, 0);
        SyncResultCount courses = new SyncResultCount(5, 6, 7, 8, 0);
        when(syncService.syncGangwonCourses()).thenReturn(new CourseSyncResult(facilities, courses));

        CourseSyncResponse response = coordinator.sync(SyncTriggerType.MANUAL);

        assertThat(savedStatuses).containsExactly(SyncRunStatus.RUNNING, SyncRunStatus.SUCCESS);
        assertThat(response.triggerType()).isEqualTo(SyncTriggerType.MANUAL);
        assertThat(response.status()).isEqualTo(SyncRunStatus.SUCCESS);
        assertThat(response.facilities()).isEqualTo(facilities);
        assertThat(response.courses()).isEqualTo(courses);
        assertThat(response.startedAt()).isNotNull();
        assertThat(response.finishedAt()).isNotNull();
    }

    @Test
    void storesFailedStatusWhenItemFailuresWereCounted() {
        SyncResultCount facilities = new SyncResultCount(1, 0, 0, 0, 1);
        SyncResultCount courses = SyncResultCount.empty();
        when(syncService.syncGangwonCourses()).thenReturn(new CourseSyncResult(facilities, courses));

        CourseSyncResponse response = coordinator.sync(SyncTriggerType.SCHEDULED);

        assertThat(savedStatuses).containsExactly(SyncRunStatus.RUNNING, SyncRunStatus.FAILED);
        assertThat(response.status()).isEqualTo(SyncRunStatus.FAILED);
        assertThat(response.triggerType()).isEqualTo(SyncTriggerType.SCHEDULED);
    }

    @Test
    void storesSanitizedFailureAndRethrowsUnexpectedException() {
        when(syncService.syncGangwonCourses()).thenThrow(new IllegalStateException(
                "request failed: https://api.test?serviceKey=secret-key"
        ));

        assertThatThrownBy(() -> coordinator.sync(SyncTriggerType.MANUAL))
                .isInstanceOf(IllegalStateException.class);

        assertThat(savedStatuses).containsExactly(SyncRunStatus.RUNNING, SyncRunStatus.FAILED);
        verify(runRepository, atLeastOnce()).save(argThat(run ->
                run.getStatus() == SyncRunStatus.FAILED
                        && "SYNC_FAILED".equals(run.getErrorCode())
                        && "공공데이터 동기화 중 오류가 발생했습니다.".equals(run.getErrorMessage())
                        && !run.getErrorMessage().contains("secret-key")
        ));
    }
}
