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
    public SyncItemResult<Facility> upsertFacility(PublicFacilityItem item) {
        return facilityRepository
                .findByBusinessRegistrationNumberAndFacilitySerialNumber(
                        item.businessRegistrationNumber(),
                        item.facilitySerialNumber()
                )
                .map(existing -> updateFacility(existing, item))
                .orElseGet(() -> new SyncItemResult<>(
                        facilityRepository.save(Facility.from(item)),
                        SyncItemStatus.INSERTED
                ));
    }

    @Transactional
    public SyncItemResult<Course> upsertCourse(
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

        return courseRepository
                .findByFacilityIdAndCourseNumber(facility.getId(), item.courseNumber())
                .map(existing -> updateCourse(existing, facility, item))
                .orElseGet(() -> new SyncItemResult<>(
                        courseRepository.save(Course.from(facility, item)),
                        SyncItemStatus.INSERTED
                ));
    }

    private SyncItemResult<Facility> updateFacility(Facility facility, PublicFacilityItem item) {
        if (!facility.update(item)) {
            return new SyncItemResult<>(facility, SyncItemStatus.UNCHANGED);
        }
        return new SyncItemResult<>(facilityRepository.save(facility), SyncItemStatus.UPDATED);
    }

    private SyncItemResult<Course> updateCourse(
            Course course,
            Facility facility,
            PublicCourseItem item
    ) {
        if (!course.update(facility, item)) {
            return new SyncItemResult<>(course, SyncItemStatus.UNCHANGED);
        }
        return new SyncItemResult<>(courseRepository.save(course), SyncItemStatus.UPDATED);
    }
}
