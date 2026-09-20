package com.example.classfit.course.service;

import com.example.classfit.course.config.PublicDataProperties;
import com.example.classfit.course.domain.Course;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.CourseSyncResponse;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.course.external.PublicDataPage;
import com.example.classfit.course.external.VoucherCourseApiClient;
import com.example.classfit.course.external.VoucherFacilityApiClient;
import com.example.classfit.course.repository.CourseRepository;
import com.example.classfit.course.repository.FacilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseSyncService {

    private final PublicDataProperties properties;
    private final VoucherFacilityApiClient facilityApiClient;
    private final VoucherCourseApiClient courseApiClient;
    private final FacilityRepository facilityRepository;
    private final CourseRepository courseRepository;

    public CourseSyncService(
            PublicDataProperties properties,
            VoucherFacilityApiClient facilityApiClient,
            VoucherCourseApiClient courseApiClient,
            FacilityRepository facilityRepository,
            CourseRepository courseRepository
    ) {
        this.properties = properties;
        this.facilityApiClient = facilityApiClient;
        this.courseApiClient = courseApiClient;
        this.facilityRepository = facilityRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public CourseSyncResponse syncGangwonCourses() {
        properties.validateForSync();
        List<Facility> facilities = syncFacilities();

        int savedCourses = 0;
        for (Facility facility : facilities) {
            savedCourses += syncCoursesForFacility(facility);
        }
        return new CourseSyncResponse(facilities.size(), savedCourses);
    }

    @Transactional
    protected List<Facility> syncFacilities() {
        List<Facility> facilities = new ArrayList<>();
        int pageNumber = 1;

        while (true) {
            PublicDataPage<PublicFacilityItem> page = facilityApiClient.fetchGangwonFacilities(pageNumber);
            for (PublicFacilityItem item : page.items()) {
                if (isBlank(item.businessRegistrationNumber()) || isBlank(item.facilitySerialNumber())) {
                    continue;
                }

                Facility facility = facilityRepository
                        .findByBusinessRegistrationNumberAndFacilitySerialNumber(
                                item.businessRegistrationNumber(),
                                item.facilitySerialNumber()
                        )
                        .map(existing -> {
                            existing.update(item);
                            return existing;
                        })
                        .orElseGet(() -> Facility.from(item));

                facilities.add(facilityRepository.save(facility));
            }

            if (page.items().isEmpty() || pageNumber * properties.getPageSize() >= page.totalCount()) {
                return facilities;
            }
            pageNumber++;
        }
    }

    @Transactional
    protected int syncCoursesForFacility(Facility facility) {
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

                Course course = courseRepository
                        .findByBusinessRegistrationNumberAndCourseNumber(
                                item.businessRegistrationNumber(),
                                item.courseNumber()
                        )
                        .map(existing -> {
                            existing.update(facility, item);
                            return existing;
                        })
                        .orElseGet(() -> Course.from(facility, item));

                courseRepository.save(course);
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
