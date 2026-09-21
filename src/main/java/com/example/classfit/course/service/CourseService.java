package com.example.classfit.course.service;

import com.example.classfit.common.PageResponse;
import com.example.classfit.common.exception.BusinessException;
import com.example.classfit.common.exception.CommonErrorCode;
import com.example.classfit.course.domain.Course;
import com.example.classfit.course.dto.CourseDetailResponse;
import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.course.repository.CourseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 강좌 동기화 데이터의 검색과 상세 조회 규칙을 담당한다. */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /** 검색 조건을 정규화하고 최신 등록 순으로 페이지 조회한다. */
    public PageResponse<CourseSearchResponse> searchCourses(
            String localCode,
            String sportCode,
            String keyword,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").descending());
        return PageResponse.from(courseRepository.search(
                        normalize(localCode),
                        normalize(sportCode),
                        normalize(keyword),
                        pageRequest
                )
                .map(CourseSearchResponse::from)
        );
    }

    public CourseDetailResponse getCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        return CourseDetailResponse.from(course);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
