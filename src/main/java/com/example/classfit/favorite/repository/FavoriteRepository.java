package com.example.classfit.favorite.repository;

import com.example.classfit.favorite.domain.Favorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);

    long deleteByMemberIdAndCourseId(Long memberId, Long courseId);

    @EntityGraph(attributePaths = {"course", "course.facility"})
    List<Favorite> findAllByMemberIdOrderByIdDesc(Long memberId);
}
