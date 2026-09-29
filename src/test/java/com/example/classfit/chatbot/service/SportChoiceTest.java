package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.Conversation;
import com.example.classfit.chatbot.dto.res.RecommendationDecision;
import com.example.classfit.chatbot.dto.res.SportOption;
import com.example.classfit.common.PageResponse;
import com.example.classfit.course.service.CourseService;
import com.example.classfit.course.service.RecommendationCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SportChoiceTest {
    private final CourseService courses = mock(CourseService.class);
    private final RecommendationService service = new RecommendationService(courses,
            new RecommendationCatalogService(new ClassPathResource("data/sports.csv"), new ClassPathResource("data/regions.csv")));
    private final Conversation conversation = Conversation.builder().build();
    private final List<SportOption> options = List.of(new SportOption("수영", "목표와 선호를 고려한 후보"),
            new SportOption("헬스", "근력 운동을 원하는 목적을 고려한 후보"),
            new SportOption("요가", "선호를 고려한 후보"), new SportOption("필라테스", "경험을 고려한 후보"));

    private RecommendationDecision offer() {
        return new RecommendationDecision("선택해 주세요", "OFFER_SPORTS", "PERSONALIZED", "", "원주시", "1010000", "INTERMEDIATE", options);
    }

    @Test
    void offersFourSportsWithoutCourseSearchThenSearchesChosenSportWithSavedConditions() {
        var menu = service.resolve(conversation, offer(), true, true);
        assertThat(menu.sportOptions()).hasSize(4);
        assertThat(menu.courses()).isEmpty();
        assertThat(menu.content()).contains("1. 수영", "4. 필라테스");
        verifyNoInteractions(courses);
        when(courses.searchCoursesByDays("51130", List.of("79", "106"), "1010000", "중급", 0, 4))
                .thenReturn(new PageResponse<>(List.of(), 0, 4, 0, 0, true, true));
        var result = service.resolve(conversation, new RecommendationDecision("", "SELECT_SPORT", "PERSONALIZED", "필라테스", ""), true, true);
        assertThat(result.recommendedSport()).isEqualTo("필라테스");
        assertThat(result.localCode()).isEqualTo("51130");
        assertThat(result.allowedDays()).isEqualTo("1010000");
    }

    @Test
    void noInBodyNoGenderOrInvalidOptionsCannotOfferOrSearch() {
        assertThat(service.resolve(conversation, offer(), false, true).content()).contains("인바디");
        assertThat(service.resolve(conversation, offer(), true, false).content()).contains("성별");
        var duplicate = new RecommendationDecision("", "OFFER_SPORTS", "PERSONALIZED", "", "", "", "",
                List.of(options.getFirst(), options.getFirst(), options.getFirst(), options.getFirst()));
        assertThat(service.resolve(conversation, duplicate, true, true).sportOptions()).isEmpty();
        verifyNoInteractions(courses);
    }

    @Test
    void cannotAutoSearchPersonalizedSportBeforeUserSelects() {
        var result = service.resolve(conversation, new RecommendationDecision("추천", "SEARCH", "PERSONALIZED", "수영", "원주시"), true, true);
        assertThat(result.courses()).isEmpty();
        verifyNoInteractions(courses);
    }

    @Test
    void unknownSelectionIsRejectedAndNewMenuResetsOldSearch() {
        conversation.rememberSearch("골프", "51130", 2, true, false);
        service.resolve(conversation, offer(), true, true);
        assertThat(conversation.getRecommendationPage()).isNull();
        assertThat(service.resolve(conversation, new RecommendationDecision("", "SELECT_SPORT", "PERSONALIZED", "골프", "원주시"), true, true).courses()).isEmpty();
        verifyNoInteractions(courses);
    }

    @Test
    void retainsSelectionWhileAskingForMissingRegion() {
        service.resolve(conversation, new RecommendationDecision("", "OFFER_SPORTS", "PERSONALIZED", "", "", "", "", options), true, true);
        var question = service.resolve(conversation, new RecommendationDecision("", "SELECT_SPORT", "PERSONALIZED", "수영", ""), true, true);
        assertThat(question.content()).contains("시군구");
        assertThat(conversation.getSelectedCandidateSport()).isEqualTo("수영");
        verifyNoInteractions(courses);
        when(courses.searchCoursesBySportCodes("51130", List.of("12"), 0, 4))
                .thenReturn(new PageResponse<>(List.of(), 0, 4, 0, 0, true, true));
        var result = service.resolve(conversation, new RecommendationDecision("", "SEARCH", "PERSONALIZED", "수영", "원주시"), true, true);
        assertThat(result.recommendedSport()).isEqualTo("수영");
        assertThat(result.sportOptions()).isEmpty();
    }

    @Test
    void menuCanBeSerializedForConversationHistory() throws Exception {
        var menu = service.resolve(conversation, offer(), true, true);
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        assertThat(mapper.readValue(mapper.writeValueAsString(menu), com.example.classfit.chatbot.dto.res.RecommendationResult.class))
                .isEqualTo(menu);
    }
}
