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
