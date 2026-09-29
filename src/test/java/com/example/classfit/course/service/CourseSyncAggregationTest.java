package com.example.classfit.course.service;

import com.example.classfit.course.config.PublicDataProperties;
import com.example.classfit.course.domain.Course;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.course.dto.SyncResultCount;
import com.example.classfit.course.external.PublicDataPage;
import com.example.classfit.course.external.VoucherCourseApiClient;
import com.example.classfit.course.external.VoucherFacilityApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class CourseSyncAggregationTest {

    PublicDataProperties properties;
    VoucherFacilityApiClient facilityApiClient;
    VoucherCourseApiClient courseApiClient;
    CourseSyncPersistenceService persistenceService;
    CourseSyncService courseSyncService;

    @BeforeEach
    void setUp() {
        properties = new PublicDataProperties();
        properties.setFacilityServiceKey("facility-key");
        properties.setCourseServiceKey("course-key");
        properties.setFacilityUrl("https://facility.test");
        properties.setCourseUrl("https://course.test");
        properties.setPageSize(1000);
        facilityApiClient = mock(VoucherFacilityApiClient.class);
        courseApiClient = mock(VoucherCourseApiClient.class);
        persistenceService = mock(CourseSyncPersistenceService.class);
        courseSyncService = new CourseSyncService(
                properties, facilityApiClient, courseApiClient, persistenceService
        );
    }

    @Test
    void aggregatesFacilityAndCourseResultsAndSkippedItems() {
        PublicFacilityItem validFacility = facility("123", "1");
        when(facilityApiClient.fetchGangwonFacilities(1)).thenReturn(new PublicDataPage<>(
                List.of(validFacility, facility("", "2")), 2
        ));
        Facility savedFacility = Facility.from(validFacility);
        when(persistenceService.upsertFacility(validFacility))
                .thenReturn(new SyncItemResult<>(savedFacility, SyncItemStatus.INSERTED));

        PublicCourseItem inserted = course("100");
        PublicCourseItem updated = course("101");
        PublicCourseItem unchanged = course("102");
        when(courseApiClient.fetchCourses("123", "1", 1)).thenReturn(new PublicDataPage<>(
                List.of(inserted, updated, unchanged, course("")), 4
        ));
        when(persistenceService.upsertCourse("123", "1", inserted))
                .thenReturn(new SyncItemResult<>(mock(Course.class), SyncItemStatus.INSERTED));
        when(persistenceService.upsertCourse("123", "1", updated))
                .thenReturn(new SyncItemResult<>(mock(Course.class), SyncItemStatus.UPDATED));
        when(persistenceService.upsertCourse("123", "1", unchanged))
                .thenReturn(new SyncItemResult<>(mock(Course.class), SyncItemStatus.UNCHANGED));

        CourseSyncResult response = courseSyncService.syncGangwonCourses();

        assertThat(response.facilities()).isEqualTo(new SyncResultCount(1, 0, 0, 1, 0));
        assertThat(response.courses()).isEqualTo(new SyncResultCount(1, 1, 1, 1, 0));
        verify(persistenceService, never()).upsertCourse(anyString(), anyString(), eq(course("")));
    }

    @Test
    void countsFacilityPersistenceFailureAndContinues(CapturedOutput output) {
        PublicFacilityItem failed = facility("123", "1");
        PublicFacilityItem inserted = facility("456", "2");
        when(facilityApiClient.fetchGangwonFacilities(1))
                .thenReturn(new PublicDataPage<>(List.of(failed, inserted), 2));
        when(persistenceService.upsertFacility(failed))
                .thenThrow(new IllegalStateException("serviceKey=secret-facility-key"));
        Facility savedFacility = Facility.from(inserted);
        when(persistenceService.upsertFacility(inserted))
                .thenReturn(new SyncItemResult<>(savedFacility, SyncItemStatus.INSERTED));
        when(courseApiClient.fetchCourses("456", "2", 1))
                .thenReturn(new PublicDataPage<>(List.of(), 0));

        CourseSyncResult response = courseSyncService.syncGangwonCourses();

        assertThat(response.facilities().inserted()).isEqualTo(1);
        assertThat(response.facilities().failed()).isEqualTo(1);
        assertThat(output).contains("brno=123", "facil_sn=1", "IllegalStateException");
        assertThat(output).doesNotContain("secret-facility-key");
        verify(courseApiClient).fetchCourses("456", "2", 1);
    }

    @Test
    void countsCoursePersistenceFailureAndContinues(CapturedOutput output) {
        PublicFacilityItem item = facility("123", "1");
        Facility savedFacility = Facility.from(item);
        when(facilityApiClient.fetchGangwonFacilities(1))
                .thenReturn(new PublicDataPage<>(List.of(item), 1));
        when(persistenceService.upsertFacility(item))
                .thenReturn(new SyncItemResult<>(savedFacility, SyncItemStatus.UNCHANGED));
        PublicCourseItem failed = course("100");
        PublicCourseItem inserted = course("101");
        when(courseApiClient.fetchCourses("123", "1", 1))
                .thenReturn(new PublicDataPage<>(List.of(failed, inserted), 2));
        when(persistenceService.upsertCourse("123", "1", failed))
                .thenThrow(new IllegalStateException("serviceKey=secret-course-key"));
        when(persistenceService.upsertCourse("123", "1", inserted))
                .thenReturn(new SyncItemResult<>(mock(Course.class), SyncItemStatus.INSERTED));

        CourseSyncResult response = courseSyncService.syncGangwonCourses();

        assertThat(response.courses().inserted()).isEqualTo(1);
        assertThat(response.courses().failed()).isEqualTo(1);
        assertThat(output).contains("brno=123", "facil_sn=1", "course_no=100", "IllegalStateException");
        assertThat(output).doesNotContain("secret-course-key");
        verify(persistenceService).upsertCourse("123", "1", inserted);
    }

    private PublicFacilityItem facility(String businessNumber, String serialNumber) {
        return new PublicFacilityItem(
                businessNumber, serialNumber, "시설", "51", "강원", "51110", "춘천시",
                "강원도 춘천시", null, "12345", "12", "수영"
        );
    }

    private PublicCourseItem course(String courseNumber) {
        return new PublicCourseItem(
                "123", "1", courseNumber, "강좌", "12", "수영", "강사",
                "10:00", "11:00", "1000000", 10000, null
        );
    }
}
