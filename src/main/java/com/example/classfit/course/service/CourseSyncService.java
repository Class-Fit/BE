package com.example.classfit.course.service;

import com.example.classfit.course.config.PublicDataProperties;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.CourseSyncResponse;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
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
        List<Facility> facilities = syncFacilities();

        int savedCourses = 0;
        for (Facility facility : facilities) {
            savedCourses += syncCoursesForFacility(facility);
        }
        return new CourseSyncResponse(facilities.size(), savedCourses);
    }

    private List<Facility> syncFacilities() {
        List<Facility> facilities = new ArrayList<>();
        int pageNumber = 1;

        while (true) {
            PublicDataPage<PublicFacilityItem> page = facilityApiClient.fetchGangwonFacilities(pageNumber);
            for (PublicFacilityItem item : page.items()) {
                if (isBlank(item.businessRegistrationNumber()) || isBlank(item.facilitySerialNumber())) {
                    continue;
                }

                facilities.add(persistenceService.upsertFacility(item));
            }

            if (page.items().isEmpty() || pageNumber * properties.getPageSize() >= page.totalCount()) {
                return facilities;
            }
            pageNumber++;
        }
    }

    private int syncCoursesForFacility(Facility facility) {
        int savedCourses = 0;
        int pageNumber = 1;

        while (true) {
            PublicDataPage<PublicCourseItem> page = courseApiClient.fetchCourses(
                    facility.getBusinessRegistrationNumber(),
                    facility.getFacilitySerialNumber(),
                    pageNumber
            );

            for (PublicCourseItem item : page.items()) {
                if (isBlank(item.courseNumber())) {
                    continue;
                }

                persistenceService.upsertCourse(
                        facility.getBusinessRegistrationNumber(),
                        facility.getFacilitySerialNumber(),
                        item
                );
                savedCourses++;
            }

            if (page.items().isEmpty() || pageNumber * properties.getPageSize() >= page.totalCount()) {
                return savedCourses;
            }
            pageNumber++;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
