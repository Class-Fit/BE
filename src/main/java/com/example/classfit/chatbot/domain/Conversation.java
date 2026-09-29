package com.example.classfit.chatbot.domain;

import com.example.classfit.common.BaseEntity;
import com.example.classfit.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "conversation")
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 100)
    private String title;

    private String recommendedSport;
    private String recommendationLocalCode;
    private Integer recommendationPage;
    private Boolean recommendationHasNext;
    private Boolean personalizedRecommendation;
    private Boolean recommendationWeekendOnly;
    private String recommendationLevel;
    private String recommendationAllowedDays;

    @Column(columnDefinition = "TEXT")
    private String candidateSportNames;
    private String selectedCandidateSport;
    private String candidateRegionName;

    public java.util.List<String> candidateSports() {
        return candidateSportNames == null ? java.util.List.of() : candidateSportNames.lines().toList();
    }

    public void offerSports(java.util.List<String> names, String region) {
        candidateSportNames = String.join("\n", names);
        selectedCandidateSport = null;
        candidateRegionName = region;
        recommendedSport = null;
        recommendationLocalCode = null;
        recommendationPage = null;
        recommendationHasNext = false;
        personalizedRecommendation = true;
    }

    public void chooseSport(String sport) {
        selectedCandidateSport = sport;
    }

    public void clearSportChoices() {
        candidateSportNames = null;
        selectedCandidateSport = null;
        candidateRegionName = null;
    }

    @Version
    private Long version;

    private String pendingRequestToken;
    private java.time.Instant requestLeaseExpiresAt;

    public void claimRequest(String token, java.time.Instant now) {
        if (pendingRequestToken != null && requestLeaseExpiresAt != null && requestLeaseExpiresAt.isAfter(now)) {
            throw new com.example.classfit.common.exception.BusinessException(
                    com.example.classfit.chatbot.exception.ChatbotErrorCode.CONVERSATION_BUSY);
        }
        pendingRequestToken = token;
        // 프로세스 중단 후에도 대화가 영구 잠기지 않도록 한다. 이전 작업의 결과는 토큰으로 차단한다.
        requestLeaseExpiresAt = now.plus(java.time.Duration.ofMinutes(10));
    }

    public boolean ownsRequest(String token) {
        return token != null && token.equals(pendingRequestToken);
    }

    public void releaseRequest(String token) {
        if (ownsRequest(token)) {
            pendingRequestToken = null;
            requestLeaseExpiresAt = null;
        }
    }

    public void rememberSearch(String sport, String localCode, int page, boolean hasNext, boolean personalized) {
        this.recommendedSport = sport;
        this.recommendationLocalCode = localCode;
        this.recommendationPage = page;
        this.recommendationHasNext = hasNext;
        this.personalizedRecommendation = personalized;
    }

    public void rememberFilters(boolean weekendOnly, String level) {
        this.recommendationWeekendOnly = weekendOnly;
        this.recommendationLevel = level;
    }

    public void rememberDays(String days) {
        this.recommendationAllowedDays = days;
    }

    public void updateTitle(String title) {
        this.title = title;
    }
}
