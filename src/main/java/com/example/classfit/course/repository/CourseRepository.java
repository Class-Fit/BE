package com.example.classfit.course.repository;

import com.example.classfit.course.domain.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    Optional<Course> findByFacilityIdAndCourseNumber(
            Long facilityId,
            String courseNumber
    );

    @Override
    @EntityGraph(attributePaths = "facility")
    Page<Course> findAll(Specification<Course> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "facility")
    Optional<Course> findById(Long id);
}
