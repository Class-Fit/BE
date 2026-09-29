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

/** 인증된 회원 식별자의 전달과 찜 API의 JSON 응답 형식을 검증한다. */
@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    private MockMvc mockMvc;
    private LoginMember loginMember;

    /** 인증된 테스트 회원과 독립적인 MVC 컨트롤러 테스트 환경을 구성한다. */
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

    /** 다음 테스트에 인증 정보가 남지 않도록 보안 컨텍스트를 비운다. */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 로그인한 회원의 찜 등록 요청이 등록 상태를 JSON으로 반환하는지 검증한다. */
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

    /** 로그인한 회원의 찜 취소 요청이 취소 상태를 JSON으로 반환하는지 검증한다. */
    @Test
    void removesFavoriteForAuthenticatedMember() throws Exception {
        when(favoriteService.removeFavorite(1L, 10L))
                .thenReturn(new FavoriteStatusResponse(10L, false));

        mockMvc.perform(delete("/api/courses/10/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(10))
                .andExpect(jsonPath("$.data.favorited").value(false));
    }

    /** 찜 목록 요청이 인증된 회원 식별자를 전달하고 강좌 정보를 반환하는지 검증한다. */
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
