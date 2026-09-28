package com.example.classfit.course.service;

import com.example.classfit.course.config.PublicDataProperties;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.CourseSyncResponse;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.course.dto.SyncResultCount;
import com.example.classfit.course.external.PublicDataPage;
import com.example.classfit.course.external.VoucherCourseApiClient;
import com.example.classfit.course.external.VoucherFacilityApiClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseSyncService {

    private final PublicDataProperties properties;
    private final VoucherFacilityApiClient facilityApiClient;
    private final VoucherCourseApiClient courseApiClient;
    private final CourseSyncPersistenceService persistenceService;

    public CourseSyncService(
            PublicDataProperties properties,
            VoucherFacilityApiClient facilityApiClient,
            VoucherCourseApiClient courseApiClient,
            CourseSyncPersistenceService persistenceService
    ) {
        this.properties = properties;
        this.facilityApiClient = facilityApiClient;
        this.courseApiClient = courseApiClient;
        this.persistenceService = persistenceService;
    }

    public CourseSyncResponse syncGangwonCourses() {
        properties.validateForSync();
        FacilitySyncResult facilityResult = syncFacilities();

        SyncResultCount courseResult = SyncResultCount.empty();
        for (Facility facility : facilityResult.facilities()) {
            courseResult = add(courseResult, syncCoursesForFacility(facility));
        }
        return new CourseSyncResponse(facilityResult.counts(), courseResult);
    }

    private FacilitySyncResult syncFacilities() {
        List<Facility> facilities = new ArrayList<>();
        SyncResultCount counts = SyncResultCount.empty();
        int pageNumber = 1;

        while (true) {
            PublicDataPage<PublicFacilityItem> page = facilityApiClient.fetchGangwonFacilities(pageNumber);
            for (PublicFacilityItem item : page.items()) {
                if (isBlank(item.businessRegistrationNumber()) || isBlank(item.facilitySerialNumber())) {
                    counts = counts.skip();
                    continue;
                }

                try {
                    SyncItemResult<Facility> result = persistenceService.upsertFacility(item);
                    facilities.add(result.entity());
                    counts = counts.add(result.status());
                } catch (RuntimeException exception) {
                    counts = counts.fail();
                }
            }

            if (page.items().isEmpty() || pageNumber * properties.getPageSize() >= page.totalCount()) {
                return new FacilitySyncResult(facilities, counts);
            }
            pageNumber++;
        }
    }

    private SyncResultCount syncCoursesForFacility(Facility facility) {
        SyncResultCount counts = SyncResultCount.empty();
        int pageNumber = 1;

        while (true) {
            PublicDataPage<PublicCourseItem> page = courseApiClient.fetchCourses(
                    facility.getBusinessRegistrationNumber(),
                    facility.getFacilitySerialNumber(),
                    pageNumber
            );

            for (PublicCourseItem item : page.items()) {
                if (isBlank(item.courseNumber())) {
                    counts = counts.skip();
                    continue;
                }

                try {
                    SyncItemResult<?> result = persistenceService.upsertCourse(
                            facility.getBusinessRegistrationNumber(),
                            facility.getFacilitySerialNumber(),
                            item
                    );
                    counts = counts.add(result.status());
                } catch (RuntimeException exception) {
                    counts = counts.fail();
                }
            }

            if (page.items().isEmpty() || pageNumber * properties.getPageSize() >= page.totalCount()) {
                return counts;
            }
            pageNumber++;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private SyncResultCount add(SyncResultCount left, SyncResultCount right) {
        return new SyncResultCount(
                left.inserted() + right.inserted(),
                left.updated() + right.updated(),
                left.unchanged() + right.unchanged(),
                left.skipped() + right.skipped(),
                left.failed() + right.failed()
        );
    }

    private record FacilitySyncResult(
            List<Facility> facilities,
            SyncResultCount counts
    ) {
    }
}
