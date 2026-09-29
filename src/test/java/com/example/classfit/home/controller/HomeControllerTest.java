package com.example.classfit.home.controller;

import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.home.dto.HomeMemberResponse;
import com.example.classfit.home.dto.HomeResponse;
import com.example.classfit.home.service.HomeService;
import com.example.classfit.inbody.dto.res.InBodyCreateRes;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.MemberRole;
import com.example.classfit.security.LoginMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HomeControllerTest {

    HomeService homeService;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        homeService = mock(HomeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new HomeController(homeService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsPublicHomeResponseForAnonymousUser() throws Exception {
        when(homeService.getHome(null)).thenReturn(new HomeResponse(
                HomeMemberResponse.anonymous(), List.of(), List.of(), null
        ));

        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.member.loggedIn").value(false))
                .andExpect(jsonPath("$.data.popularCourses").isArray())
                .andExpect(jsonPath("$.data.latestCourses").isArray())
                .andExpect(jsonPath("$.data.latestInBody").isEmpty());

        verify(homeService).getHome(null);
    }

    @Test
    void passesAuthenticatedMemberIdToHomeService() throws Exception {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(7L);
        when(member.getRole()).thenReturn(MemberRole.USER);
        LoginMember loginMember = LoginMember.from(member);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(loginMember, null, loginMember.getAuthorities())
        );
        CourseSearchResponse popularCourse = new CourseSearchResponse(
                10L, "인기 수영", "12", "수영", "춘천 체육시설",
                "강원특별자치도 춘천시", "10:00", "11:00", "월, 수", 30000
        );
        CourseSearchResponse latestCourse = new CourseSearchResponse(
                11L, "최신 배드민턴", "13", "배드민턴", "원주 체육시설",
                "강원특별자치도 원주시", "19:00", "20:00", "화, 목", 20000
        );
        InBodyCreateRes latestInBody = new InBodyCreateRes(
                20L, new BigDecimal("175.0"), new BigDecimal("70.0"),
                new BigDecimal("18.0"), new BigDecimal("32.0"),
                new BigDecimal("12.6"), new BigDecimal("22.9"),
                LocalDateTime.of(2026, 9, 29, 10, 0)
        );
        when(homeService.getHome(7L)).thenReturn(new HomeResponse(
                HomeMemberResponse.authenticated(7L, "홍길동"),
                List.of(popularCourse),
                List.of(latestCourse),
                latestInBody
        ));

        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.loggedIn").value(true))
                .andExpect(jsonPath("$.data.member.id").value(7))
                .andExpect(jsonPath("$.data.member.name").value("홍길동"))
                .andExpect(jsonPath("$.data.popularCourses[0].courseId").value(10))
                .andExpect(jsonPath("$.data.popularCourses[0].courseName").value("인기 수영"))
                .andExpect(jsonPath("$.data.latestCourses[0].courseId").value(11))
                .andExpect(jsonPath("$.data.latestCourses[0].courseName").value("최신 배드민턴"))
                .andExpect(jsonPath("$.data.latestInBody.inBodyId").value(20))
                .andExpect(jsonPath("$.data.latestInBody.weightKg").value(70.0))
                .andExpect(jsonPath("$.data.latestInBody.bodyFatPercentage").value(18.0))
                .andExpect(jsonPath("$.data.favoriteCourses").doesNotExist());

        verify(homeService).getHome(7L);
    }
}
