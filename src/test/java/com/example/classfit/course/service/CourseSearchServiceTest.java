package com.example.classfit.course.service;

import com.example.classfit.common.PageResponse;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({CourseService.class, CourseSyncPersistenceService.class})
class CourseSearchServiceTest {

    @Autowired
    CourseService courseService;

    @Autowired
    CourseSyncPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        Facility wonju = persistenceService.upsertFacility(facility(
                "1000000001", "1", "원주 국민체육센터", "51110"
        ));
        Facility chuncheon = persistenceService.upsertFacility(facility(
                "1000000002", "2", "춘천 시민체육관", "51210"
        ));

        saveCourse(wonju, "100", "Morning PILATES", "PILATES");
        saveCourse(wonju, "101", "초급 수영 100%", "SWIM");
        saveCourse(chuncheon, "200", "춘천 수영_특강", "SWIM");
        saveCourse(chuncheon, "201", "축구 교실", "SOCCER");
    }

    @Test
    void returnsAllCoursesInDescendingIdOrder() {
        PageResponse<CourseSearchResponse> response = search(null, null, null, 0, 20);

        assertThat(response.content())
                .extracting(CourseSearchResponse::courseName)
                .containsExactly("축구 교실", "춘천 수영_특강", "초급 수영 100%", "Morning PILATES");
        assertThat(response.totalCount()).isEqualTo(4);
    }

    @Test
    void filtersCoursesByLocalCode() {
        PageResponse<CourseSearchResponse> response = search("51110", null, null, 0, 20);

        assertThat(response.content())
                .extracting(CourseSearchResponse::courseName)
                .containsExactly("초급 수영 100%", "Morning PILATES");
    }

    @Test
    void filtersCoursesBySportCode() {
        PageResponse<CourseSearchResponse> response = search(null, "SWIM", null, 0, 20);

        assertThat(response.content())
                .extracting(CourseSearchResponse::courseName)
                .containsExactly("춘천 수영_특강", "초급 수영 100%");
    }

    @Test
    void filtersCoursesByKeywordIgnoringCase() {
        PageResponse<CourseSearchResponse> response = search(null, null, "pilates", 0, 20);

        assertThat(response.content())
                .extracting(CourseSearchResponse::courseName)
                .containsExactly("Morning PILATES");
    }

    @Test
    void combinesSearchConditionsWithAnd() {
        PageResponse<CourseSearchResponse> response = search("51110", "SWIM", "초급", 0, 20);

        assertThat(response.content())
                .extracting(CourseSearchResponse::courseName)
                .containsExactly("초급 수영 100%");
    }

    @Test
    void trimsValuesAndIgnoresBlankConditions() {
        PageResponse<CourseSearchResponse> response = search(" 51110 ", " SWIM ", "   ", 0, 20);

        assertThat(response.content())
                .extracting(CourseSearchResponse::courseName)
                .containsExactly("초급 수영 100%");
    }

    @Test
    void returnsEmptyPageWhenNoCourseMatches() {
        PageResponse<CourseSearchResponse> response = search(null, null, "없는 강좌", 0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalCount()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    void appliesPaginationAfterFiltering() {
        PageResponse<CourseSearchResponse> first = search(null, "SWIM", null, 0, 1);
        PageResponse<CourseSearchResponse> second = search(null, "SWIM", null, 1, 1);

        assertThat(first.content()).extracting(CourseSearchResponse::courseName)
                .containsExactly("춘천 수영_특강");
        assertThat(first.totalCount()).isEqualTo(2);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.first()).isTrue();
        assertThat(first.last()).isFalse();

        assertThat(second.content()).extracting(CourseSearchResponse::courseName)
                .containsExactly("초급 수영 100%");
        assertThat(second.first()).isFalse();
        assertThat(second.last()).isTrue();
    }

    @Test
    void treatsLikeWildcardsAsLiteralKeyword() {
        assertThat(search(null, null, "%", 0, 20).content())
                .extracting(CourseSearchResponse::courseName)
                .containsExactly("초급 수영 100%");
        assertThat(search(null, null, "_", 0, 20).content())
                .extracting(CourseSearchResponse::courseName)
                .containsExactly("춘천 수영_특강");
    }

    @Test
    void selectedDayMatchesCoursesContainingOtherDaysAndFiltersBeforePaging() {
        Facility facility = persistenceService.upsertFacility(facility("weekday-test", "days", "요일 시설", "51130"));
        String[] masks = {"1000000", "1110000", "1010100", "0101000", "0010000", null, "0000000", "x000000"};
        for (int i = 0; i < masks.length; i++) {
            persistenceService.upsertCourse("weekday-test", "days", new PublicCourseItem(
                    "weekday-test", "days", "d" + i, "중급 수영 " + i, i % 2 == 0 ? "12" : "112",
                    "수영", "강사", "10:00", "11:00", masks[i], 10000, "설명"));
        }
        var first = courseService.searchCoursesByDays("51130", java.util.List.of("12", "112"), "1000000", "중급", 0, 2);
        var second = courseService.searchCoursesByDays("51130", java.util.List.of("12", "112"), "1000000", "중급", 1, 2);
        assertThat(first.totalCount()).isEqualTo(3);
        assertThat(first.content()).extracting(CourseSearchResponse::weekdays).containsExactly("월, 수, 금", "월, 화, 수");
        assertThat(second.content()).extracting(CourseSearchResponse::weekdays).containsExactly("월");
        assertThat(second.last()).isTrue();
        assertThat(courseService.searchCoursesByDays("51130", java.util.List.of("12", "112"), "1010000", "중급", 0, 10).totalCount()).isEqualTo(4);
        assertThat(courseService.searchCoursesByDays("51130", java.util.List.of("12", "112"), "0101000", "중급", 0, 10).totalCount()).isEqualTo(2);
        assertThat(courseService.searchCoursesByDays("51130", java.util.List.of("12", "112"), "1000000", "초급", 0, 10).content()).isEmpty();
    }

    @Test
    void weekendIncludesMixedWeekdayClasses() {
        persistenceService.upsertFacility(facility("weekend-test", "days", "주말 시설", "51130"));
        for (String mask : java.util.List.of("1000010", "0000001", "1111100")) {
            persistenceService.upsertCourse("weekend-test", "days", new PublicCourseItem(
                    "weekend-test", "days", mask, "중급 수영", "12", "수영", "강사",
                    "10:00", "11:00", mask, 10000, "설명"));
        }
        assertThat(courseService.searchFilteredCourses("51130", java.util.List.of("12"), true, "중급", 0, 4).content())
                .extracting(CourseSearchResponse::weekdays).containsExactlyInAnyOrder("월, 토", "일");
    }

    private PageResponse<CourseSearchResponse> search(
            String localCode,
            String sportCode,
            String keyword,
            int page,
            int size
    ) {
        return courseService.searchCourses(localCode, sportCode, keyword, page, size);
    }

    private PublicFacilityItem facility(
            String businessNumber,
            String serialNumber,
            String name,
            String localCode
    ) {
        return new PublicFacilityItem(
                businessNumber, serialNumber, name, "51", "강원", localCode, "지역",
                "강원도 도로명", null, "12345", "SWIM", "수영"
        );
    }

    private void saveCourse(Facility facility, String courseNumber, String name, String sportCode) {
        persistenceService.upsertCourse(
                facility.getBusinessRegistrationNumber(),
                facility.getFacilitySerialNumber(),
                new PublicCourseItem(
                        facility.getBusinessRegistrationNumber(), facility.getFacilitySerialNumber(),
                        courseNumber, name, sportCode, sportCode, "강사", "10:00", "11:00",
                        "1010100", 10000, "설명"
                )
        );
    }
}
