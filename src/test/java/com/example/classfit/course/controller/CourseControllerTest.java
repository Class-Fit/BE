package com.example.classfit.course.controller;

import com.example.classfit.common.PageResponse;
import com.example.classfit.common.exception.GlobalExceptionHandler;
import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.course.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    CourseService courseService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseController(courseService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsPageResponseJsonContract() throws Exception {
        CourseSearchResponse course = new CourseSearchResponse(
                1L, "수영 초급", "12", "수영", "국민체육센터", "강원도 원주시",
                "10:00", "11:00", "월, 수", 10000
        );
        when(courseService.searchCourses(null, null, null, 0, 20))
                .thenReturn(new PageResponse<>(List.of(course), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].courseName").value("수영 초급"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/courses").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        verifyNoInteractions(courseService);
    }

    @Test
    void rejectsNonPositiveSize() throws Exception {
        mockMvc.perform(get("/api/courses").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        verifyNoInteractions(courseService);
    }

    @Test
    void rejectsPageAboveApiLimit() throws Exception {
        mockMvc.perform(get("/api/courses").param("page", "10001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        verifyNoInteractions(courseService);
    }

    @Test
    void capsRequestedSizeAtOneHundred() throws Exception {
        when(courseService.searchCourses(null, null, null, 0, 100))
                .thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0, true, true));

        mockMvc.perform(get("/api/courses").param("size", "101"))
                .andExpect(status().isOk());

        verify(courseService).searchCourses(null, null, null, 0, 100);
    }

    @Test
    void returnsEmptyMetadataWhenRequestedPageHasNoContent() throws Exception {
        when(courseService.searchCourses(null, null, null, 5, 20))
                .thenReturn(new PageResponse<>(List.of(), 5, 20, 42, 3, false, true));

        mockMvc.perform(get("/api/courses").param("page", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page").value(5))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalCount").value(42))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(true));
    }
}
