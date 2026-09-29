package com.example.classfit.course.repository;

import com.example.classfit.course.domain.Course;
import com.example.classfit.course.domain.Facility;
import com.example.classfit.course.dto.PublicCourseItem;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.example.classfit.favorite.domain.Favorite;
import com.example.classfit.favorite.repository.FavoriteRepository;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.OAuthProvider;
import com.example.classfit.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class HomeCourseRepositoryTest {

    @Autowired CourseRepository courseRepository;
    @Autowired FacilityRepository facilityRepository;
    @Autowired FavoriteRepository favoriteRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    List<Course> courses;

    @BeforeEach
    void setUp() {
        Facility facility = facilityRepository.save(Facility.from(new PublicFacilityItem(
                "1234567890", "100", "테스트 체육시설", "51", "강원",
                "51110", "춘천시", "강원특별자치도 춘천시", null,
                "24000", "12", "수영"
        )));

        courses = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            courses.add(courseRepository.save(Course.from(facility, new PublicCourseItem(
                    "1234567890", "100", String.valueOf(200 + index), "강좌 " + index,
                    "12", "수영", "강사", "10:00", "11:00",
                    "1000000", 30000, null
            ))));
        }

        List<Member> members = List.of(
                member("home-member-1"), member("home-member-2"), member("home-member-3")
        );
        favoriteRepository.saveAll(List.of(
                Favorite.create(members.get(0), courses.get(0)),
                Favorite.create(members.get(0), courses.get(1)),
                Favorite.create(members.get(1), courses.get(1)),
                Favorite.create(members.get(2), courses.get(1)),
                Favorite.create(members.get(0), courses.get(2)),
                Favorite.create(members.get(1), courses.get(2))
        ));
    }

    @Test
    void findsSixCoursesByFavoriteCountThenNewestId() {
        List<Course> result = courseRepository.findPopularCourses(PageRequest.of(0, 6));

        assertThat(result).extracting(Course::getName)
                .containsExactly("강좌 1", "강좌 2", "강좌 0", "강좌 6", "강좌 5", "강좌 4");
    }

    @Test
    void findsSixNewestCoursesById() {
        List<Course> result = courseRepository.findLatestCourses(PageRequest.of(0, 6));

        assertThat(result).extracting(Course::getName)
                .containsExactly("강좌 6", "강좌 5", "강좌 4", "강좌 3", "강좌 2", "강좌 1");
    }

    @Test
    void createsCourseIdIndexForFavoriteCountLookup() {
        Integer indexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = current_schema()
                  AND tablename = 'favorites'
                  AND indexname = 'idx_favorites_course_id'
                """, Integer.class);

        assertThat(indexCount).isEqualTo(1);
    }

    private Member member(String providerId) {
        return memberRepository.save(Member.createOAuthMember(
                OAuthProvider.KAKAO, providerId, "회원", null, null
        ));
    }
}
