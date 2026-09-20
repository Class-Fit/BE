package com.example.classfit.course.repository;

import com.example.classfit.course.domain.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByBusinessRegistrationNumberAndCourseNumber(
            String businessRegistrationNumber,
            String courseNumber
    );

    @EntityGraph(attributePaths = "facility")
    @Query("""
            select c from Course c
            where (:localCode is null or c.facility.localCode = :localCode)
              and (:sportCode is null or c.sportCode = :sportCode)
              and (:keyword is null or lower(c.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<Course> search(
            @Param("localCode") String localCode,
            @Param("sportCode") String sportCode,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = "facility")
    Optional<Course> findById(Long id);
}
