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
import java.util.List;
import java.util.ArrayList;

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

    public PageResponse<CourseSearchResponse> searchCoursesBySportCodes(
            String localCode, List<String> sportCodes, int page, int size) {
        return searchRecommendation(localCode, sportCodes, "", "", page, size);
    }

    /** 이전 주말 조건도 동일한 '선택 요일 포함' 규칙으로 적용한다. */
    public PageResponse<CourseSearchResponse> searchFilteredCourses(
            String localCode, List<String> sportCodes, boolean weekendOnly, String level, int page, int size) {
        return searchRecommendation(localCode, sportCodes, weekendOnly ? "0000011" : "", level, page, size);
    }

    /** 선택한 요일 중 하나 이상이 포함되면 다른 요일 수업이 있어도 검색한다. */
    public PageResponse<CourseSearchResponse> searchCoursesByDays(
            String localCode, List<String> sportCodes, String selectedDays, String level, int page, int size) {
        if (selectedDays == null || !selectedDays.matches("[01]{7}") || !selectedDays.contains("1")) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        return searchRecommendation(localCode, sportCodes, selectedDays, level, page, size);
    }

    private PageResponse<CourseSearchResponse> searchRecommendation(
            String localCode, List<String> sportCodes, String selectedDays, String level, int page, int size) {
        if (normalize(localCode) == null || sportCodes == null || sportCodes.isEmpty()
                || sportCodes.stream().anyMatch(code -> normalize(code) == null)
                || page < 0 || page > 10_000 || size < 1 || size > 100) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        String keyword = normalize(level);
        if (keyword != null && !List.of("초급", "중급", "고급").contains(keyword)) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        List<String> codes = sportCodes.stream().map(String::trim).distinct().toList();
        Specification<Course> specification = buildSearchSpecification(localCode.trim(), null, keyword)
                .and((root, query, cb) -> root.get("sportCode").in(codes));
        if (!selectedDays.isEmpty()) {
            int selected = Integer.parseInt(selectedDays, 2);
            List<String> masks = new ArrayList<>();
            for (int mask = 1; mask < 128; mask++) {
                if ((mask & selected) != 0) {
                    masks.add(String.format("%7s", Integer.toBinaryString(mask)).replace(' ', '0'));
                }
            }
            specification = specification.and((root, query, cb) -> root.get("weekdayMask").in(masks));
        }
        return PageResponse.from(courseRepository.findAll(specification,
                PageRequest.of(page, size, Sort.by("id").descending())).map(CourseSearchResponse::from));
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
