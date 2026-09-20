package com.example.classfit.course.service;

import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.course.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(CourseSyncPersistenceService.class)
class CourseSyncPersistenceServiceTest {

    @Autowired
    private CourseSyncPersistenceService persistenceService;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void savesCoursesSeparatelyWhenFacilitiesShareBusinessNumberAndCourseNumber() {
        Facility firstFacility = persistenceService.upsertFacility(facility("1"));
        Facility secondFacility = persistenceService.upsertFacility(facility("2"));

        persistenceService.upsertCourse("1234567890", firstFacility.getFacilitySerialNumber(), course("1"));
        persistenceService.upsertCourse("1234567890", secondFacility.getFacilitySerialNumber(), course("2"));

        assertThat(courseRepository.count()).isEqualTo(2);
        assertThat(courseRepository.findByFacilityIdAndCourseNumber(firstFacility.getId(), "100"))
                .isPresent();
        assertThat(courseRepository.findByFacilityIdAndCourseNumber(secondFacility.getId(), "100"))
                .isPresent();
    }

    private PublicFacilityItem facility(String facilitySerialNumber) {
        return new PublicFacilityItem(
                "1234567890", facilitySerialNumber, "테스트 시설", "51", "강원",
                "51110", "춘천시", "강원도 춘천시", "상세 주소", "12345", "12", "수영"
        );
    }

    private PublicCourseItem course(String facilitySerialNumber) {
        return new PublicCourseItem(
                "1234567890", facilitySerialNumber, "100", "테스트 강좌", "12", "수영",
                "강사", "10:00", "11:00", "1000000", 10000, "설명"
        );
    }
}
