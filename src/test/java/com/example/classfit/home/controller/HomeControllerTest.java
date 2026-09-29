package com.example.classfit.home.controller;

import com.example.classfit.home.dto.HomeMemberResponse;
import com.example.classfit.home.dto.HomeResponse;
import com.example.classfit.home.service.HomeService;
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
        when(homeService.getHome(7L)).thenReturn(new HomeResponse(
                HomeMemberResponse.authenticated(7L, "홍길동"), List.of(), List.of(), null
        ));

        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.loggedIn").value(true))
                .andExpect(jsonPath("$.data.member.id").value(7))
                .andExpect(jsonPath("$.data.member.name").value("홍길동"));

        verify(homeService).getHome(7L);
    }
}
