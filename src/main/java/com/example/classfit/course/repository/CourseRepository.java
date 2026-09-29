package com.example.classfit.course.repository;

import com.example.classfit.course.domain.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
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

    @EntityGraph(attributePaths = "facility")
    @Query("""
            SELECT course
            FROM Course course
            ORDER BY (
                SELECT COUNT(favorite.id)
                FROM Favorite favorite
                WHERE favorite.course = course
            ) DESC, course.id DESC
            """)
    List<Course> findPopularCourses(Pageable pageable);

    @EntityGraph(attributePaths = "facility")
    @Query("SELECT course FROM Course course ORDER BY course.id DESC")
    List<Course> findLatestCourses(Pageable pageable);
}
