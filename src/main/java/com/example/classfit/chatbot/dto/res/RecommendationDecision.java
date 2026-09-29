package com.example.classfit.chatbot.dto.res;

/** AI 판단이며, 검색 조건과 페이지는 서버가 다시 검증한다. 미확정 문자열은 빈 문자열이다. */
public record RecommendationDecision(
        String content, String action, String mode, String recommendedSport, String regionName,
        String schedule, String level, java.util.List<SportOption> sportOptions
) {
    public RecommendationDecision(String content, String action, String mode, String sport, String region) {
        this(content, action, mode, sport, region, "", "", java.util.List.of());
    }

    public RecommendationDecision(String content, String action, String mode, String sport, String region, String schedule, String level) {
        this(content, action, mode, sport, region, schedule, level, java.util.List.of());
    }
}
