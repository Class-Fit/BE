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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

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

        Specification<Course> specification = buildSearchSpecification(
                normalize(localCode), normalize(sportCode), normalize(keyword)
        );

        return PageResponse.from(courseRepository
                .findAll(specification, pageRequest).map(CourseSearchResponse::from)
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

    private Specification<Course> buildSearchSpecification(
            String localCode,
            String sportCode,
            String keyword
    ) {
        Specification<Course> specification = Specification.allOf();

        if (localCode != null) {
            specification = specification.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("facility").get("localCode"),
                                    localCode
                            )
            );
        }
        if (sportCode != null) {
            specification = specification.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("sportCode"), sportCode
                            )
            );
        }
        if (keyword != null) {
            String pattern = "%" + escapeLikePattern(keyword.toLowerCase(Locale.ROOT)) + "%";
            specification = specification.and(
                    (root, query, cb) ->
                            cb.like(cb.lower(root.get("name")), pattern, '\\')
            );
        }

        return specification;
    }

    /** LIKE 검색에서 사용자 입력의 %, _, 역슬래시를 일반 문자로 취급한다. */
    private String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
