package com.example.classfit.favorite.repository;

import com.example.classfit.favorite.domain.Favorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // Only the member/course duplicate is ignored; unrelated constraint errors still propagate.
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO favorites (member_id, course_id, created_at, updated_at)
            VALUES (:memberId, :courseId, :now, :now)
            ON CONFLICT (member_id, course_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("memberId") Long memberId,
                       @Param("courseId") Long courseId,
                       @Param("now") LocalDateTime now);

    long deleteByMemberIdAndCourseId(Long memberId, Long courseId);

    @EntityGraph(attributePaths = {"course", "course.facility"})
    List<Favorite> findAllByMemberIdOrderByIdDesc(Long memberId);
}
