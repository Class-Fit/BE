package com.example.classfit.course.service;

import com.example.classfit.course.config.PublicDataProperties;
import com.example.classfit.course.domain.Course;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.course.external.PublicDataPage;
import com.example.classfit.course.external.VoucherCourseApiClient;
import com.example.classfit.course.external.VoucherFacilityApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

        assertThat(count(response, "facilities", "inserted")).isEqualTo(1);
        assertThat(count(response, "facilities", "updated")).isZero();
        assertThat(count(response, "facilities", "unchanged")).isZero();
        assertThat(count(response, "facilities", "skipped")).isEqualTo(1);
        assertThat(count(response, "facilities", "failed")).isZero();

        assertThat(count(response, "courses", "inserted")).isEqualTo(1);
        assertThat(count(response, "courses", "updated")).isEqualTo(1);
        assertThat(count(response, "courses", "unchanged")).isEqualTo(1);
        assertThat(count(response, "courses", "skipped")).isEqualTo(1);
        assertThat(count(response, "courses", "failed")).isZero();
        verify(persistenceService, never()).upsertCourse(anyString(), anyString(), eq(course("")));
    }

    @Test
    void countsFacilityPersistenceFailureAndContinues() {
        PublicFacilityItem failed = facility("123", "1");
        PublicFacilityItem inserted = facility("456", "2");
        when(facilityApiClient.fetchGangwonFacilities(1))
                .thenReturn(new PublicDataPage<>(List.of(failed, inserted), 2));
        when(persistenceService.upsertFacility(failed))
                .thenThrow(new IllegalStateException("시설 저장 실패"));
        Facility savedFacility = Facility.from(inserted);
        when(persistenceService.upsertFacility(inserted))
                .thenReturn(new SyncItemResult<>(savedFacility, SyncItemStatus.INSERTED));
        when(courseApiClient.fetchCourses("456", "2", 1))
                .thenReturn(new PublicDataPage<>(List.of(), 0));

        CourseSyncResult response = courseSyncService.syncGangwonCourses();

        assertThat(count(response, "facilities", "inserted")).isEqualTo(1);
        assertThat(count(response, "facilities", "failed")).isEqualTo(1);
        verify(courseApiClient).fetchCourses("456", "2", 1);
    }

    @Test
    void countsCoursePersistenceFailureAndContinues() {
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
                .thenThrow(new IllegalStateException("강좌 저장 실패"));
        when(persistenceService.upsertCourse("123", "1", inserted))
                .thenReturn(new SyncItemResult<>(mock(Course.class), SyncItemStatus.INSERTED));

        CourseSyncResult response = courseSyncService.syncGangwonCourses();

        assertThat(count(response, "courses", "inserted")).isEqualTo(1);
        assertThat(count(response, "courses", "failed")).isEqualTo(1);
        verify(persistenceService).upsertCourse("123", "1", inserted);
    }

    private int count(Object response, String groupMethod, String countMethod) {
        try {
            Method group = response.getClass().getMethod(groupMethod);
            Object counts = group.invoke(response);
            return (int) counts.getClass().getMethod(countMethod).invoke(counts);
        } catch (ReflectiveOperationException exception) {
            return -1;
        }
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
