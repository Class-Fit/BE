package com.example.classfit.home.service;

import com.example.classfit.course.domain.Course;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.course.repository.CourseRepository;
import com.example.classfit.home.dto.HomeResponse;
import com.example.classfit.inbody.dto.res.InBodyCreateRes;
import com.example.classfit.inbody.service.InBodyService;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.OAuthProvider;
import com.example.classfit.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HomeServiceTest {

    CourseRepository courseRepository;
    MemberService memberService;
    InBodyService inBodyService;
    HomeService homeService;

    @BeforeEach
    void setUp() {
        courseRepository = mock(CourseRepository.class);
        memberService = mock(MemberService.class);
        inBodyService = mock(InBodyService.class);
        homeService = new HomeService(courseRepository, memberService, inBodyService);
    }

    @Test
    void returnsPublicCourseSectionsWithoutMemberQueriesForAnonymousUser() {
        Course popular = course("인기 수영");
        Course latest = course("최신 배드민턴");
        when(courseRepository.findPopularCourses(any(Pageable.class))).thenReturn(List.of(popular));
        when(courseRepository.findLatestCourses(any(Pageable.class))).thenReturn(List.of(latest));

        HomeResponse response = homeService.getHome(null);

        assertThat(response.member().loggedIn()).isFalse();
        assertThat(response.popularCourses()).extracting("courseName").containsExactly("인기 수영");
        assertThat(response.latestCourses()).extracting("courseName").containsExactly("최신 배드민턴");
        assertThat(response.latestInBody()).isNull();
        verifyNoInteractions(memberService, inBodyService);
        verify(courseRepository).findPopularCourses(argThat(page -> page.getPageSize() == 6));
        verify(courseRepository).findLatestCourses(argThat(page -> page.getPageSize() == 6));
    }

    @Test
    void includesMemberAndLatestInBodyForAuthenticatedUser() {
        Long memberId = 7L;
        Member member = Member.createOAuthMember(
                OAuthProvider.KAKAO, "home-member", "홍길동", "home@test.com", null
        );
        InBodyCreateRes latestInBody = new InBodyCreateRes(
                11L, new BigDecimal("175.0"), new BigDecimal("70.0"),
                new BigDecimal("18.0"), new BigDecimal("32.0"),
                new BigDecimal("12.6"), new BigDecimal("22.9"), LocalDateTime.now()
        );
        when(courseRepository.findPopularCourses(any(Pageable.class))).thenReturn(List.of());
        when(courseRepository.findLatestCourses(any(Pageable.class))).thenReturn(List.of());
        when(memberService.findById(memberId)).thenReturn(member);
        when(inBodyService.findLatestInBody(memberId)).thenReturn(Optional.of(latestInBody));

        HomeResponse response = homeService.getHome(memberId);

        assertThat(response.member().loggedIn()).isTrue();
        assertThat(response.member().id()).isEqualTo(memberId);
        assertThat(response.member().name()).isEqualTo("홍길동");
        assertThat(response.latestInBody()).isEqualTo(latestInBody);
    }

    private Course course(String name) {
        Facility facility = Facility.from(new PublicFacilityItem(
                "1234567890", "100", "체육시설", "51", "강원", "51110", "춘천시",
                "강원특별자치도 춘천시", null, "24000", "12", "수영"
        ));
        return Course.from(facility, new PublicCourseItem(
                "1234567890", "100", name, name, "12", "수영", "강사",
                "10:00", "11:00", "1000000", 30000, null
        ));
    }
}
