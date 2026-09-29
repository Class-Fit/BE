package com.example.classfit.favorite.repository;

import com.example.classfit.favorite.domain.Favorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 찜 관계를 저장하고 회원별 찜 목록을 조회하는 저장소다. */
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    /**
     * PostgreSQL에서 찜을 원자적으로 삽입하고 같은 회원과 강좌의 중복만 무시한다.
     * 다른 제약 위반은 호출자에게 전달되며, 네이티브 쿼리이므로 감사 시각을 직접 저장한다.
     *
     * @param memberId 찜을 소유하는 회원 식별자
     * @param courseId 찜 대상 강좌 식별자
     * @param now 새 행의 생성 및 수정 시각
     * @return 삽입한 행 수이며, 기존 찜이 있으면 0
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO favorites (member_id, course_id, created_at, updated_at)
            VALUES (:memberId, :courseId, :now, :now)
            ON CONFLICT (member_id, course_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("memberId") Long memberId,
                       @Param("courseId") Long courseId,
                       @Param("now") LocalDateTime now);

    /**
     * 특정 회원의 특정 강좌 찜만 삭제한다.
     *
     * @param memberId 찜을 소유하는 회원 식별자
     * @param courseId 찜 대상 강좌 식별자
     * @return 삭제한 행 수이며, 해당 찜이 없으면 0
     */
    long deleteByMemberIdAndCourseId(Long memberId, Long courseId);

    /**
     * 회원의 찜을 식별자 내림차순으로 조회하면서 강좌와 시설을 함께 가져온다.
     *
     * @param memberId 조회 대상 회원 식별자
     * @return 해당 회원의 찜 목록이며, 찜이 없으면 빈 목록
     */
    @EntityGraph(attributePaths = {"course", "course.facility"})
    List<Favorite> findAllByMemberIdOrderByIdDesc(Long memberId);
}
