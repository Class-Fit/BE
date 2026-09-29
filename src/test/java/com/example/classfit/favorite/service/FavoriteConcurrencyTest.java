package com.example.classfit.favorite.service;

import com.example.classfit.config.JpaAuditingConfig;
import com.example.classfit.course.domain.Course;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.course.repository.CourseRepository;
import com.example.classfit.course.repository.FacilityRepository;
import com.example.classfit.favorite.domain.Favorite;
import com.example.classfit.favorite.dto.FavoriteStatusResponse;
import com.example.classfit.favorite.repository.FavoriteRepository;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.OAuthProvider;
import com.example.classfit.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** 독립된 DB 트랜잭션에서 찜 중복 등록의 원자성과 감사 시각 보존을 검증한다. */
@DataJpaTest
@ActiveProfiles("test")
@Import({FavoriteService.class, JpaAuditingConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FavoriteConcurrencyTest {

    @Autowired FavoriteService favoriteService;
    @Autowired FavoriteRepository favoriteRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired FacilityRepository facilityRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired PlatformTransactionManager transactionManager;

    private Long memberId;
    private Long facilityId;
    private Long courseId;

    /** 다른 스레드의 트랜잭션에서도 조회할 수 있도록 테스트 데이터를 커밋해 저장한다. */
    @BeforeEach
    void setUp() {
        // Worker transactions must be able to see committed fixtures.
        memberId = memberRepository.saveAndFlush(Member.createOAuthMember(
                OAuthProvider.KAKAO, "concurrent-favorite-member", "회원", null, null
        )).getId();
        Facility facility = facilityRepository.saveAndFlush(Facility.from(new PublicFacilityItem(
                "1234567890", "100", "춘천국민체육센터", "51", "강원",
                "51110", "춘천시", "강원특별자치도 춘천시 스포츠로 1", null,
                "24000", "12", "수영"
        )));
        facilityId = facility.getId();
        courseId = courseRepository.saveAndFlush(Course.from(facility, new PublicCourseItem(
                "1234567890", "100", "200", "수영 초급", "12", "수영",
                "홍길동", "10:00", "11:00", "1010000", 30000, "초급 수영 수업"
        ))).getId();
    }

    /** 테스트가 저장한 찜, 강좌, 시설 및 회원을 외래 키 의존 순서에 맞춰 삭제한다. */
    @AfterEach
    void tearDown() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            if (memberId != null) {
                favoriteRepository.deleteAll(favoriteRepository.findAllByMemberIdOrderByIdDesc(memberId));
            }
            if (courseId != null) {
                courseRepository.deleteById(courseId);
            }
            if (facilityId != null) {
                facilityRepository.deleteById(facilityId);
            }
            if (memberId != null) {
                memberRepository.deleteById(memberId);
            }
        });
    }

    /** 같은 찜을 동시에 등록하는 요청 8개가 모두 성공하고 한 행만 남기는지 검증한다. */
    @Test
    void concurrentDuplicateRegistrationsAllSucceedWithOneFavorite() throws Exception {
        int requestCount = 8;
        var executor = Executors.newFixedThreadPool(requestCount);
        var ready = new CountDownLatch(requestCount);
        var start = new CountDownLatch(1);
        List<Future<FavoriteStatusResponse>> requests = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                requests.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent requests did not start in time");
                    }
                    return favoriteService.addFavorite(memberId, courseId);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (var request : requests) {
                var response = request.get(20, TimeUnit.SECONDS);
                assertThat(response.courseId()).isEqualTo(courseId);
                assertThat(response.favorited()).isTrue();
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }

        var favorites = savedFavorites();
        assertThat(favorites).hasSize(1);
        assertThat(favorites.getFirst().getCourse().getId()).isEqualTo(courseId);
        assertThat(favorites.getFirst().getCreatedAt()).isNotNull();
        assertThat(favorites.getFirst().getUpdatedAt()).isNotNull();
    }

    /** 중복 등록이 기존 찜의 식별자와 생성 및 수정 시각을 변경하지 않는지 검증한다. */
    @Test
    void duplicateRegistrationKeepsOriginalIdentityAndAuditTimestamps() {
        favoriteService.addFavorite(memberId, courseId);
        var original = savedFavorites().getFirst();

        var response = favoriteService.addFavorite(memberId, courseId);

        var favorites = savedFavorites();
        assertThat(response.favorited()).isTrue();
        assertThat(favorites).hasSize(1);
        assertThat(original.getCreatedAt()).isNotNull();
        assertThat(original.getUpdatedAt()).isNotNull();
        assertThat(favorites.getFirst().getId()).isEqualTo(original.getId());
        assertThat(favorites.getFirst().getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(favorites.getFirst().getUpdatedAt()).isEqualTo(original.getUpdatedAt());
    }

    /**
     * 시설의 LOB 필드도 트랜잭션 안에서 읽도록 테스트 회원의 찜을 조회한다.
     *
     * @return 강좌와 시설이 함께 로딩된 테스트 회원의 찜 목록
     */
    private List<Favorite> savedFavorites() {
        return new TransactionTemplate(transactionManager).execute(
                status -> favoriteRepository.findAllByMemberIdOrderByIdDesc(memberId)
        );
    }
}
