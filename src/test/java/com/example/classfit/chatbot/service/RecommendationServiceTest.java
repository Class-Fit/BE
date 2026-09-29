package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.Conversation;
import com.example.classfit.chatbot.dto.res.RecommendationDecision;
import com.example.classfit.common.PageResponse;
import com.example.classfit.course.service.CourseService;
import com.example.classfit.course.service.RecommendationCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {
    private final CourseService courses = mock(CourseService.class);
    private final RecommendationService service = new RecommendationService(courses,
            new RecommendationCatalogService(new ClassPathResource("data/sports.csv"),
                    new ClassPathResource("data/regions.csv")));
    private final Conversation conversation = Conversation.builder().build();

    @Test
    void personalizedRecommendationWithoutInBodyRequiresRegistration() {
        var result = service.resolve(conversation, decision("SEARCH", "PERSONALIZED", "수영", "원주시"), false, true);
        assertThat(result.content()).contains("인바디", "등록");
        assertThat(result.courses()).isEmpty();
        verifyNoInteractions(courses);
    }

    @Test
    void explicitSportCanSearchWithoutInBodyAndKeepsMultipleCodes() {
        when(courses.searchCoursesBySportCodes("51130", List.of("79", "106"), 0, 4))
                .thenReturn(new PageResponse<>(List.of(), 0, 4, 0, 0, true, true));
        var result = service.resolve(conversation, decision("SEARCH", "EXPLICIT", "필라테스", "원주시"), false, false);
        assertThat(result.sportCodes()).containsExactly("79", "106");
        assertThat(result.localCode()).isEqualTo("51130");
        assertThat(result.content()).contains("없");
    }

    @Test
    void ambiguousRegionAsksForProvinceWithoutSearching() {
        var result = service.resolve(conversation, decision("SEARCH", "EXPLICIT", "수영", "고성군"), false, true);
        assertThat(result.content()).contains("강원특별자치도 고성군", "경상남도 고성군");
        verifyNoInteractions(courses);
    }

    @Test
    void nextPageUsesSavedConditionsAndNeverAiInventedConditions() {
        conversation.rememberSearch("수영", "51130", 0, true, false);
        when(courses.searchCoursesBySportCodes("51130", List.of("12"), 1, 4))
                .thenReturn(new PageResponse<>(List.of(), 1, 4, 4, 1, false, true));
        var result = service.resolve(conversation, decision("NEXT_PAGE", "EXPLICIT", "골프", "서울특별시"), false, true);
        assertThat(result.recommendedSport()).isEqualTo("수영");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(conversation.getRecommendationPage()).isEqualTo(1);
        assertThat(service.resolve(conversation, decision("NEXT_PAGE", "EXPLICIT", "", ""), false, true).content())
                .contains("마지막");
    }

    @Test
    void missingPreviousSearchOrInvalidSportNeverBroadensSearch() {
        assertThat(service.resolve(conversation, decision("NEXT_PAGE", "EXPLICIT", "", ""), false, true).courses()).isEmpty();
        assertThat(service.resolve(conversation, decision("SEARCH", "EXPLICIT", "없는종목", "원주시"), false, true).courses()).isEmpty();
        verifyNoInteractions(courses);
    }

    @Test
    void personalizedRecommendationRequiresGenderAndNewSearchResetsPage() {
        assertThat(service.resolve(conversation, decision("SEARCH", "PERSONALIZED", "수영", "원주시"), true, false).content())
                .contains("성별");
        verifyNoInteractions(courses);
        conversation.rememberSearch("골프", "51130", 3, true, false);
        when(courses.searchCoursesBySportCodes("51130", List.of("12"), 0, 4))
                .thenReturn(new PageResponse<>(List.of(), 0, 4, 0, 0, true, true));
        var result = service.resolve(conversation, decision("SEARCH", "EXPLICIT", "수영", "원주시"), true, true);
        assertThat(result.page()).isZero();
        assertThat(conversation.getPersonalizedRecommendation()).isFalse();
    }

    @Test
    void provinceAloneAndUnknownActionNeverSearch() {
        assertThat(service.resolve(conversation, decision("SEARCH", "EXPLICIT", "수영", "강원특별자치도"), false, true).content())
                .contains("시군구");
        service.resolve(conversation, decision("INVALID", "EXPLICIT", "수영", "원주시"), true, true);
        verifyNoInteractions(courses);
    }

    private RecommendationDecision decision(String action, String mode, String sport, String region) {
        return new RecommendationDecision("추천합니다.", action, mode, sport, region);
    }

    @Test
    void refinementRestartsPaginationAndNextPageKeepsWeekendAndIntermediateFilters() {
        conversation.rememberSearch("수영", "51130", 2, true, false);
        var card = new com.example.classfit.course.dto.CourseSearchResponse(
                100L, "수영 중급", "12", "수영", "시설", "주소", "10:00", "11:00", "토", 10000);
        when(courses.searchFilteredCourses("51130", List.of("12"), true, "중급", 0, 4))
                .thenReturn(new PageResponse<>(List.of(card), 0, 4, 5, 2, true, false));
        when(courses.searchFilteredCourses("51130", List.of("12"), true, "중급", 1, 4))
                .thenReturn(new PageResponse<>(List.of(card), 1, 4, 5, 2, false, true));
        var filtered = service.resolve(conversation, new RecommendationDecision("찾아볼게요", "REFINE", "EXPLICIT",
                "", "", "WEEKEND_ONLY", "INTERMEDIATE"), false, true);
        assertThat(filtered.page()).isZero();
        assertThat(filtered.weekendOnly()).isTrue();
        assertThat(filtered.level()).isEqualTo("중급");
        var next = service.resolve(conversation, decision("NEXT_PAGE", "EXPLICIT", "", ""), false, true);
        assertThat(next.page()).isEqualTo(1);
        assertThat(next.weekendOnly()).isTrue();
        assertThat(next.level()).isEqualTo("중급");
    }

    @Test
    void refinementCanRemoveFiltersEvenAfterLastPage() {
        conversation.rememberSearch("수영", "51130", 1, false, false);
        conversation.rememberFilters(true, "중급");
        when(courses.searchCoursesBySportCodes("51130", List.of("12"), 0, 4))
                .thenReturn(new PageResponse<>(List.of(), 0, 4, 0, 0, true, true));
        var result = service.resolve(conversation, new RecommendationDecision("다시 찾아볼게요", "REFINE", "EXPLICIT",
                "", "", "ANY", "ANY"), false, true);
        assertThat(result.page()).isZero();
        assertThat(result.weekendOnly()).isFalse();
        assertThat(result.level()).isEmpty();
        assertThat(conversation.getRecommendationWeekendOnly()).isFalse();
    }

    @Test
    void mondayWednesdayFilterReplacesWeekendAndSurvivesNextPage() {
        conversation.rememberSearch("수영", "51130", 2, true, false);
        conversation.rememberFilters(true, "중급");
        when(courses.searchCoursesByDays("51130", List.of("12"), "1010000", "중급", 0, 4))
                .thenReturn(new PageResponse<>(List.of(), 0, 4, 5, 2, true, false));
        when(courses.searchCoursesByDays("51130", List.of("12"), "1010000", "중급", 1, 4))
                .thenReturn(new PageResponse<>(List.of(), 1, 4, 5, 2, false, true));
        var result = service.resolve(conversation, new RecommendationDecision("찾아볼게요", "REFINE", "EXPLICIT",
                "", "", "1010000", ""), false, true);
        assertThat(result.allowedDays()).isEqualTo("1010000");
        assertThat(result.weekendOnly()).isFalse();
        assertThat(result.page()).isZero();
        assertThat(result.content()).contains("월·수");
        var next = service.resolve(conversation, decision("NEXT_PAGE", "EXPLICIT", "", ""), false, true);
        assertThat(next.allowedDays()).isEqualTo("1010000");
        assertThat(next.level()).isEqualTo("중급");
        assertThat(next.page()).isEqualTo(1);
        when(courses.searchCoursesBySportCodes("51130", List.of("12"), 0, 4))
                .thenReturn(new PageResponse<>(List.of(), 0, 4, 0, 0, true, true));
        var cleared = service.resolve(conversation, new RecommendationDecision("", "REFINE", "EXPLICIT",
                "", "", "ANY", "ANY"), false, true);
        assertThat(cleared.allowedDays()).isEmpty();
    }
}
