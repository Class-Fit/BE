package com.example.classfit.favorite.controller;

import com.example.classfit.common.exception.GlobalExceptionHandler;
import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.favorite.dto.FavoriteStatusResponse;
import com.example.classfit.favorite.service.FavoriteService;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.MemberRole;
import com.example.classfit.security.LoginMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    private MockMvc mockMvc;
    private LoginMember loginMember;

    @BeforeEach
    void setUp() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getRole()).thenReturn(MemberRole.USER);
        loginMember = LoginMember.from(member);

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(loginMember, null, loginMember.getAuthorities())
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new FavoriteController(favoriteService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addsFavoriteForAuthenticatedMember() throws Exception {
        when(favoriteService.addFavorite(1L, 10L))
                .thenReturn(new FavoriteStatusResponse(10L, true));

        mockMvc.perform(post("/api/courses/10/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseId").value(10))
                .andExpect(jsonPath("$.data.favorited").value(true));
    }

    @Test
    void removesFavoriteForAuthenticatedMember() throws Exception {
        when(favoriteService.removeFavorite(1L, 10L))
                .thenReturn(new FavoriteStatusResponse(10L, false));

        mockMvc.perform(delete("/api/courses/10/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(10))
                .andExpect(jsonPath("$.data.favorited").value(false));
    }

    @Test
    void returnsFavoriteCoursesForAuthenticatedMember() throws Exception {
        CourseSearchResponse course = new CourseSearchResponse(
                10L, "수영 초급", "12", "수영", "춘천국민체육센터",
                "강원특별자치도 춘천시", "10:00", "11:00", "월, 수", 30000
        );
        when(favoriteService.getFavorites(1L)).thenReturn(List.of(course));

        mockMvc.perform(get("/api/members/me/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].courseId").value(10))
                .andExpect(jsonPath("$.data[0].courseName").value("수영 초급"));

        verify(favoriteService).getFavorites(1L);
    }
}
