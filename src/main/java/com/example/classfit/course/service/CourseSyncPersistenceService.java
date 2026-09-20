package com.example.classfit.course.service;

import com.example.classfit.course.domain.Course;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.course.repository.CourseRepository;
import com.example.classfit.course.repository.FacilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 외부 API 호출과 분리해 시설·강좌 한 건만 짧은 트랜잭션으로 저장한다. */
@Service
public class CourseSyncPersistenceService {

    private final FacilityRepository facilityRepository;
    private final CourseRepository courseRepository;

    public CourseSyncPersistenceService(
            FacilityRepository facilityRepository,
            CourseRepository courseRepository
    ) {
        this.facilityRepository = facilityRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public Facility upsertFacility(PublicFacilityItem item) {
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

        return facilityRepository.save(facility);
    }

    @Transactional
    public void upsertCourse(
            String businessRegistrationNumber,
            String facilitySerialNumber,
            PublicCourseItem item
    ) {
        Facility facility = facilityRepository
                .findByBusinessRegistrationNumberAndFacilitySerialNumber(
                        businessRegistrationNumber,
                        facilitySerialNumber
                )
                .orElseThrow(() -> new IllegalStateException("등록시설을 찾을 수 없습니다."));

        Course course = courseRepository
                .findByFacilityIdAndCourseNumber(facility.getId(), item.courseNumber())
                .map(existing -> {
                    existing.update(facility, item);
                    return existing;
                })
                .orElseGet(() -> Course.from(facility, item));

        courseRepository.save(course);
    }
}
