package com.example.classfit.chatbot.dto.res;

import com.example.classfit.course.dto.CourseSearchResponse;
import java.util.List;

public record RecommendationResult(
        String content, String recommendedSport, List<String> sportCodes, String localCode,
        Integer page, boolean hasNext, List<CourseSearchResponse> courses,
        Boolean weekendOnly, String level, String allowedDays, List<SportOption> sportOptions
) {
    public RecommendationResult {
        sportOptions = sportOptions == null ? List.of() : List.copyOf(sportOptions);
    }

    public static RecommendationResult message(String content) {
        return new RecommendationResult(content, null, List.of(), null, null, false, List.of(), null, null, null, List.of());
    }
}
