package com.example.classfit.course.controller;

import com.example.classfit.course.domain.sync.SyncRunStatus;
import com.example.classfit.course.domain.sync.SyncTriggerType;
import com.example.classfit.course.dto.CourseSyncResponse;
import com.example.classfit.course.dto.SyncResultCount;
import com.example.classfit.course.service.CourseSyncCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CourseSyncControllerTest {

    CourseSyncCoordinator coordinator;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        coordinator = mock(CourseSyncCoordinator.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseSyncController(coordinator)).build();
    }

    @Test
    void returnsManualRunIdStatusAndDetailedCounts() throws Exception {
        when(coordinator.sync(SyncTriggerType.MANUAL)).thenReturn(new CourseSyncResponse(
                15L,
                SyncTriggerType.MANUAL,
                SyncRunStatus.SUCCESS,
                LocalDateTime.of(2026, 9, 29, 3, 0),
                LocalDateTime.of(2026, 9, 29, 3, 6),
                new SyncResultCount(1, 2, 3, 4, 0),
                new SyncResultCount(5, 6, 7, 8, 0)
        ));

        mockMvc.perform(post("/api/admin/courses/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId").value(15))
                .andExpect(jsonPath("$.data.triggerType").value("MANUAL"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.facilities.inserted").value(1))
                .andExpect(jsonPath("$.data.facilities.updated").value(2))
                .andExpect(jsonPath("$.data.facilities.unchanged").value(3))
                .andExpect(jsonPath("$.data.facilities.skipped").value(4))
                .andExpect(jsonPath("$.data.facilities.failed").value(0))
                .andExpect(jsonPath("$.data.courses.inserted").value(5))
                .andExpect(jsonPath("$.data.courses.updated").value(6))
                .andExpect(jsonPath("$.data.courses.unchanged").value(7))
                .andExpect(jsonPath("$.data.courses.skipped").value(8))
                .andExpect(jsonPath("$.data.courses.failed").value(0));

        verify(coordinator).sync(SyncTriggerType.MANUAL);
    }
}
