package com.example.classfit.course.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThatCode;

/** 실제 로컬 DB에서 읽기 전용으로 실행한다. 스키마와 데이터를 변경하지 않는다. */
@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false"})
@EnabledIfEnvironmentVariable(named = "CLASSFIT_POSTGRES_TEST", matches = "true")
@Transactional(readOnly = true)
class PostgresCourseSearchTest {
    @Autowired CourseService service;

    @Test
    void optionalFiltersWorkOnPostgresWithNullAndTextKeywords() {
        assertThatCode(() -> {
            service.searchCourses("51130", "12", null, 0, 4);
            service.searchCourses(null, null, null, 0, 4);
            service.searchCourses(null, null, "수영", 0, 4);
            service.searchFilteredCourses("51130", java.util.List.of("12"), true, "중급", 0, 4);
            service.searchFilteredCourses("51130", java.util.List.of("12"), false, "", 0, 4);
            service.searchCoursesByDays("51130", java.util.List.of("12"), "1000000", "중급", 0, 4);
        }).doesNotThrowAnyException();
    }
}
