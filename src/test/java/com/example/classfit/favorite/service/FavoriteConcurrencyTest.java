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

    private List<Favorite> savedFavorites() {
        return new TransactionTemplate(transactionManager).execute(
                status -> favoriteRepository.findAllByMemberIdOrderByIdDesc(memberId)
        );
    }
}
