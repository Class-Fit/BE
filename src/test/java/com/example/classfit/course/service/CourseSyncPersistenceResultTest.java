package com.example.classfit.course.service;

import com.example.classfit.course.domain.Course;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.course.repository.CourseRepository;
import com.example.classfit.course.repository.FacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseSyncPersistenceResultTest {

    FacilityRepository facilityRepository;
    CourseRepository courseRepository;
    CourseSyncPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        facilityRepository = mock(FacilityRepository.class);
        courseRepository = mock(CourseRepository.class);
        persistenceService = new CourseSyncPersistenceService(facilityRepository, courseRepository);
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void classifiesNewFacilityAsInserted() {
        when(facilityRepository.findByBusinessRegistrationNumberAndFacilitySerialNumber("123", "1"))
                .thenReturn(Optional.empty());

        SyncItemResult<Facility> result = persistenceService.upsertFacility(facility());

        assertThat(result.status()).isEqualTo(SyncItemStatus.INSERTED);
        verify(facilityRepository).save(any(Facility.class));
    }

    @Test
    void classifiesChangedFacilityAsUpdated() {
        Facility existing = Facility.from(facility());
        when(facilityRepository.findByBusinessRegistrationNumberAndFacilitySerialNumber("123", "1"))
                .thenReturn(Optional.of(existing));

        SyncItemResult<Facility> result = persistenceService.upsertFacility(
                facility("변경된 시설", "상세 주소")
        );

        assertThat(result.status()).isEqualTo(SyncItemStatus.UPDATED);
        assertThat(existing.getName()).isEqualTo("변경된 시설");
        verify(facilityRepository).save(existing);
    }

    @Test
    void classifiesNormalizedEquivalentFacilityAsUnchanged() {
        Facility existing = Facility.from(facility("테스트 시설", null));
        when(facilityRepository.findByBusinessRegistrationNumberAndFacilitySerialNumber("123", "1"))
                .thenReturn(Optional.of(existing));

        SyncItemResult<Facility> result = persistenceService.upsertFacility(
                facility("  테스트 시설  ", "   ")
        );

        assertThat(result.status()).isEqualTo(SyncItemStatus.UNCHANGED);
        verify(facilityRepository, never()).save(any());
    }

    @Test
    void classifiesNewCourseAsInserted() {
        Facility facility = Facility.from(facility());
        when(facilityRepository.findByBusinessRegistrationNumberAndFacilitySerialNumber("123", "1"))
                .thenReturn(Optional.of(facility));
        when(courseRepository.findByFacilityIdAndCourseNumber(null, "100"))
                .thenReturn(Optional.empty());

        SyncItemResult<Course> result = persistenceService.upsertCourse("123", "1", course());

        assertThat(result.status()).isEqualTo(SyncItemStatus.INSERTED);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void classifiesChangedCourseAsUpdated() {
        Facility facility = Facility.from(facility());
        Course existing = Course.from(facility, course());
        when(facilityRepository.findByBusinessRegistrationNumberAndFacilitySerialNumber("123", "1"))
                .thenReturn(Optional.of(facility));
        when(courseRepository.findByFacilityIdAndCourseNumber(null, "100"))
                .thenReturn(Optional.of(existing));

        SyncItemResult<Course> result = persistenceService.upsertCourse(
                "123", "1", course("변경된 강좌", "새 설명")
        );

        assertThat(result.status()).isEqualTo(SyncItemStatus.UPDATED);
        assertThat(existing.getName()).isEqualTo("변경된 강좌");
        verify(courseRepository).save(existing);
    }

    @Test
    void classifiesNormalizedEquivalentCourseAsUnchanged() {
        Facility facility = Facility.from(facility());
        Course existing = Course.from(facility, course("테스트 강좌", null));
        when(facilityRepository.findByBusinessRegistrationNumberAndFacilitySerialNumber("123", "1"))
                .thenReturn(Optional.of(facility));
        when(courseRepository.findByFacilityIdAndCourseNumber(null, "100"))
                .thenReturn(Optional.of(existing));

        SyncItemResult<Course> result = persistenceService.upsertCourse(
                "123", "1", course(" 테스트 강좌 ", "")
        );

        assertThat(result.status()).isEqualTo(SyncItemStatus.UNCHANGED);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void usesNormalizedFacilityAndCourseIdentifiersForLookup() {
        Facility facility = Facility.from(facility());
        Course course = Course.from(facility, course());
        PublicFacilityItem paddedFacility = new PublicFacilityItem(
                " 123 ", " 1 ", "테스트 시설", "51", "강원", "51110", "춘천시",
                "강원도 춘천시", "상세 주소", "12345", "12", "수영"
        );
        PublicCourseItem paddedCourse = new PublicCourseItem(
                " 123 ", " 1 ", " 100 ", "테스트 강좌", "12", "수영", "강사",
                "10:00", "11:00", "1000000", 10000, "설명"
        );
        when(facilityRepository.findByBusinessRegistrationNumberAndFacilitySerialNumber("123", "1"))
                .thenReturn(Optional.of(facility));
        when(courseRepository.findByFacilityIdAndCourseNumber(null, "100"))
                .thenReturn(Optional.of(course));

        SyncItemResult<Facility> facilityResult = persistenceService.upsertFacility(paddedFacility);
        SyncItemResult<Course> courseResult = persistenceService.upsertCourse(" 123 ", " 1 ", paddedCourse);

        assertThat(facilityResult.status()).isEqualTo(SyncItemStatus.UNCHANGED);
        assertThat(courseResult.status()).isEqualTo(SyncItemStatus.UNCHANGED);
        verify(facilityRepository, never()).save(any());
        verify(courseRepository, never()).save(any());
    }

    private PublicFacilityItem facility() {
        return facility("테스트 시설", "상세 주소");
    }

    private PublicFacilityItem facility(String name, String detailAddress) {
        return new PublicFacilityItem(
                "123", "1", name, "51", "강원", "51110", "춘천시",
                "강원도 춘천시", detailAddress, "12345", "12", "수영"
        );
    }

    private PublicCourseItem course() {
        return course("테스트 강좌", "설명");
    }

    private PublicCourseItem course(String name, String description) {
        return new PublicCourseItem(
                "123", "1", "100", name, "12", "수영", "강사",
                "10:00", "11:00", "1000000", 10000, description
        );
    }
}
