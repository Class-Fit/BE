package com.example.classfit.home.service;

import com.example.classfit.course.domain.Course;
import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.course.repository.CourseRepository;
import com.example.classfit.home.dto.HomeMemberResponse;
import com.example.classfit.home.dto.HomeResponse;
import com.example.classfit.inbody.dto.res.InBodyCreateRes;
import com.example.classfit.inbody.service.InBodyService;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.service.MemberService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 공개 강좌와 로그인 회원 정보를 조합해 메인 화면 응답을 만든다. */
@Service
@Transactional(readOnly = true)
public class HomeService {

    private static final int COURSE_LIMIT = 6;

    private final CourseRepository courseRepository;
    private final MemberService memberService;
    private final InBodyService inBodyService;

    public HomeService(
            CourseRepository courseRepository,
            MemberService memberService,
            InBodyService inBodyService
    ) {
        this.courseRepository = courseRepository;
        this.memberService = memberService;
        this.inBodyService = inBodyService;
    }

    /** 익명 요청에는 공개 강좌만, 로그인 요청에는 회원과 최근 인바디까지 반환한다. */
    public HomeResponse getHome(Long memberId) {
        List<CourseSearchResponse> popularCourses = courseRepository
                .findPopularCourses(PageRequest.of(0, COURSE_LIMIT)).stream()
                .map(CourseSearchResponse::from)
                .toList();
        List<CourseSearchResponse> latestCourses = courseRepository
                .findLatestCourses(PageRequest.of(0, COURSE_LIMIT)).stream()
                .map(CourseSearchResponse::from)
                .toList();

        if (memberId == null) {
            return new HomeResponse(
                    HomeMemberResponse.anonymous(), popularCourses, latestCourses, null
            );
        }

        Member member = memberService.findById(memberId);
        InBodyCreateRes latestInBody = inBodyService.findLatestInBody(memberId).orElse(null);
        return new HomeResponse(
                HomeMemberResponse.authenticated(memberId, member.getName()),
                popularCourses,
                latestCourses,
                latestInBody
        );
    }
}
