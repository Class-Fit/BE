package com.example.classfit.favorite.service;

import com.example.classfit.common.exception.BusinessException;
import com.example.classfit.course.domain.Course;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.course.repository.CourseRepository;
import com.example.classfit.course.repository.FacilityRepository;
import com.example.classfit.favorite.repository.FavoriteRepository;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.OAuthProvider;
import com.example.classfit.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** DB를 사용해 찜 등록, 조회, 취소 및 존재하지 않는 강좌의 오류 처리를 검증한다. */
@DataJpaTest
@ActiveProfiles("test")
@Import(FavoriteService.class)
class FavoriteServiceTest {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Member member;
    private Course course;

    /** 각 테스트에서 사용할 회원, 시설 및 강좌를 저장한다. */
    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.createOAuthMember(
                OAuthProvider.KAKAO, "favorite-member", "회원", null, null
        ));

        Facility facility = facilityRepository.save(Facility.from(new PublicFacilityItem(
                "1234567890", "100", "춘천국민체육센터", "51", "강원",
                "51110", "춘천시", "강원특별자치도 춘천시 스포츠로 1", null,
                "24000", "12", "수영"
        )));

        course = courseRepository.save(Course.from(facility, new PublicCourseItem(
                "1234567890", "100", "200", "수영 초급", "12", "수영",
                "홍길동", "10:00", "11:00", "1010000", 30000, "초급 수영 수업"
        )));
    }

    /** 유효한 회원과 강좌의 찜 등록이 한 행을 저장하고 등록 상태를 반환하는지 검증한다. */
    @Test
    void savesFavoriteForMemberAndCourse() {
        var response = favoriteService.addFavorite(member.getId(), course.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.favorited()).isTrue();
        assertThat(favoriteRepository.count()).isOne();
    }

    /** 같은 회원과 강좌를 순차적으로 중복 등록해도 찜 행이 늘어나지 않는지 검증한다. */
    @Test
    void doesNotDuplicateExistingFavorite() {
        favoriteService.addFavorite(member.getId(), course.getId());
        favoriteService.addFavorite(member.getId(), course.getId());

        assertThat(favoriteRepository.count()).isOne();
    }

    /** 회원의 찜 목록에 강좌명과 연결된 시설명이 포함되는지 검증한다. */
    @Test
    void returnsFavoriteCoursesForMember() {
        favoriteService.addFavorite(member.getId(), course.getId());

        var favorites = favoriteService.getFavorites(member.getId());

        assertThat(favorites).hasSize(1);
        assertThat(favorites.getFirst().courseName()).isEqualTo("수영 초급");
        assertThat(favorites.getFirst().facilityName()).isEqualTo("춘천국민체육센터");
    }

    /** 저장된 찜을 취소하면 행이 삭제되고 취소 상태를 반환하는지 검증한다. */
    @Test
    void removesFavorite() {
        favoriteService.addFavorite(member.getId(), course.getId());

        var response = favoriteService.removeFavorite(member.getId(), course.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.favorited()).isFalse();
        assertThat(favoriteRepository.count()).isZero();
    }

    /** 존재하지 않는 강좌의 찜 취소가 리소스 없음 오류를 발생시키는지 검증한다. */
    @Test
    void rejectsCancellationOfMissingCourse() {
        assertThatThrownBy(() -> favoriteService.removeFavorite(member.getId(), 999999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    /** 찜하지 않은 기존 강좌를 반복 취소해도 동일한 성공 응답을 반환하는지 검증한다. */
    @Test
    void cancellationOfExistingUnfavoritedCourseIsIdempotent() {
        var firstResponse = favoriteService.removeFavorite(member.getId(), course.getId());
        var secondResponse = favoriteService.removeFavorite(member.getId(), course.getId());

        assertThat(firstResponse.courseId()).isEqualTo(course.getId());
        assertThat(firstResponse.favorited()).isFalse();
        assertThat(secondResponse).isEqualTo(firstResponse);
        assertThat(favoriteRepository.count()).isZero();
    }

    /** 존재하지 않는 강좌의 찜 등록이 리소스 없음 오류를 발생시키는지 검증한다. */
    @Test
    void rejectsMissingCourse() {
        assertThatThrownBy(() -> favoriteService.addFavorite(member.getId(), 999999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }
}
