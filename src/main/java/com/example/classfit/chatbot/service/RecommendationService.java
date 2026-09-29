package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.Conversation;
import com.example.classfit.chatbot.dto.res.RecommendationDecision;
import com.example.classfit.chatbot.dto.res.RecommendationResult;
import com.example.classfit.chatbot.dto.res.SportOption;
import com.example.classfit.course.service.CourseService;
import com.example.classfit.course.service.RecommendationCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final CourseService courseService;
    private final RecommendationCatalogService catalog;

    public RecommendationResult resolve(Conversation conversation, RecommendationDecision decision,
                                        boolean hasInBody, boolean hasGender) {
        if (decision == null) return message("응답을 만들지 못했습니다. 다시 말씀해 주세요.");
        boolean next = "NEXT_PAGE".equals(decision.action());
        boolean refine = "REFINE".equals(decision.action());
        boolean select = "SELECT_SPORT".equals(decision.action());
        boolean personalized = next || refine ? Boolean.TRUE.equals(conversation.getPersonalizedRecommendation())
                : "PERSONALIZED".equals(decision.mode()) || select || "OFFER_SPORTS".equals(decision.action());
        boolean offer = "OFFER_SPORTS".equals(decision.action())
                || (personalized && "SEARCH".equals(decision.action()) && conversation.getSelectedCandidateSport() == null);
        if (personalized && !hasInBody) {
            return message("맞춤 운동 추천을 받으려면 인바디 데이터를 등록한 후 다시 물어봐 주세요. 원하는 종목을 직접 말씀하시면 강좌를 찾아드릴 수 있어요.");
        }
        if (personalized && !hasGender) {
            return message("맞춤 운동 추천에 사용할 성별 정보가 없습니다. 회원 성별 정보를 등록한 후 다시 물어봐 주세요.");
        }
        if ("ASK".equals(decision.action())) {
            return message(decision.content() == null || decision.content().isBlank()
                    ? "원하는 종목과 시군구를 말씀해 주세요. 맞춤 종목 추천도 가능합니다." : decision.content());
        }
        if (!next && !refine && !offer && !select && (!"SEARCH".equals(decision.action())
                || !("EXPLICIT".equals(decision.mode()) || personalized))) {
            return message("원하는 종목 또는 맞춤 추천 여부와 지역을 다시 말씀해 주세요.");
        }
        String sport;
        String localCode;
        int page;
        boolean weekendOnly = Boolean.TRUE.equals(conversation.getRecommendationWeekendOnly());
        String allowedDays = conversation.getRecommendationAllowedDays();
        if (allowedDays == null) allowedDays = weekendOnly ? "0000011" : "";
        String level = conversation.getRecommendationLevel() == null ? "" : conversation.getRecommendationLevel();
        if (!next) {
            String schedule = decision.schedule() == null ? "" : decision.schedule();
            if (!List.of("", "ANY", "WEEKEND_ONLY").contains(schedule)
                    && !(schedule.matches("[01]{7}") && schedule.contains("1"))) {
                return message("수업 가능한 요일을 월요일부터 일요일 중에서 알려주세요.");
            }
            if (!schedule.isEmpty()) {
                allowedDays = "ANY".equals(schedule) ? "" : "WEEKEND_ONLY".equals(schedule) ? "0000011" : schedule;
            }
            weekendOnly = "0000011".equals(allowedDays);
            String requestedLevel = decision.level() == null ? "" : decision.level();
            switch (requestedLevel) {
                case "ANY" -> level = "";
                case "BEGINNER" -> level = "초급";
                case "INTERMEDIATE" -> level = "중급";
                case "ADVANCED" -> level = "고급";
                case "" -> { }
                default -> { return message("수준은 초급·중급·고급 또는 수준 무관 중에서 알려주세요."); }
            }
        }
        if (next || refine) {
            if (conversation.getRecommendationPage() == null) {
                return message("먼저 원하는 종목과 지역으로 강좌를 검색해 주세요.");
            }
            if (next && (!Boolean.TRUE.equals(conversation.getRecommendationHasNext())
                    || conversation.getRecommendationPage() >= 10_000)) {
                return message("마지막 강좌까지 보여드렸습니다. 다른 종목이나 지역으로 찾아볼까요?");
            }
            sport = conversation.getRecommendedSport();
            localCode = conversation.getRecommendationLocalCode();
            page = next ? conversation.getRecommendationPage() + 1 : 0;
        } else {
            if (offer) {
                List<SportOption> options = decision.sportOptions();
                if (options == null || options.size() < 4 || options.size() > 5
                        || options.stream().anyMatch(option -> option == null || option.sportName() == null
                        || catalog.findSportCodes(option.sportName()).isEmpty()
                        || List.of("기타종목", "종합체육시설").contains(option.sportName().strip())
                        || option.reason() == null || option.reason().isBlank())
                        || options.stream().map(option -> option.sportName().strip()).distinct().count() != options.size()) {
                    return message("유효한 맞춤 종목 후보 4~5개를 구성하지 못했습니다. 맞춤 운동 추천을 다시 요청해 주세요.");
                }
                List<SportOption> validated = options.stream()
                        .map(option -> new SportOption(option.sportName().strip(), option.reason().strip())).toList();
                String candidateRegion = decision.regionName();
                if (candidateRegion == null || candidateRegion.isBlank()) {
                    candidateRegion = catalog.getRegions().stream()
                            .filter(region -> region.code().equals(conversation.getRecommendationLocalCode()))
                            .map(RecommendationCatalogService.Region::name).findFirst().orElse("");
                }
                conversation.offerSports(validated.stream().map(SportOption::sportName).toList(), candidateRegion);
                conversation.rememberFilters(weekendOnly, level);
                conversation.rememberDays(allowedDays);
                StringBuilder content = new StringBuilder("등록된 인바디·성별과 말씀하신 목표·선호를 참고한 운동 후보입니다. 관심 있는 종목의 이름이나 번호를 골라주세요.");
                for (int i = 0; i < validated.size(); i++) {
                    content.append("\n").append(i + 1).append(". ").append(validated.get(i).sportName())
                            .append(": ").append(validated.get(i).reason());
                }
                return new RecommendationResult(content.toString(), null, List.of(), null, null, false,
                        List.of(), weekendOnly, level, allowedDays, validated);
            }
            sport = decision.recommendedSport() == null ? "" : decision.recommendedSport().strip();
            if (select) {
                if (!conversation.candidateSports().contains(sport)) {
                    return message("앞서 추천한 종목의 이름이나 번호를 선택해 주세요.");
                }
                conversation.chooseSport(sport);
            } else if (personalized && conversation.getSelectedCandidateSport() != null) {
                sport = conversation.getSelectedCandidateSport();
            } else if (!personalized) {
                conversation.clearSportChoices();
            }
            if (catalog.findSportCodes(sport).isEmpty()) {
                return message("검색할 수 있는 종목을 확인하지 못했습니다. 원하는 종목을 다시 말씀해 주세요.");
            }
            String regionName = decision.regionName();
            if (personalized && (regionName == null || regionName.isBlank())) regionName = conversation.getCandidateRegionName();
            var regions = catalog.findRegions(regionName);
            if (regions.size() > 1) {
                return message("어느 지역인가요? " + regions.stream().map(RecommendationCatalogService.Region::name)
                        .collect(Collectors.joining(", ")) + " 중에서 알려주세요.");
            }
            if (regions.isEmpty() || regions.getFirst().provinceLevel()) {
                return message("강좌를 찾을 시·도와 시군구를 알려주세요. 예: 강원특별자치도 원주시");
            }
            localCode = regions.getFirst().code();
            page = 0;
        }
        List<String> codes = catalog.findSportCodes(sport);
        if (codes.isEmpty()) return message("종목 정보를 다시 확인해 주세요.");
        var courses = !allowedDays.isEmpty() && !weekendOnly
                ? courseService.searchCoursesByDays(localCode, codes, allowedDays, level, page, 4)
                : weekendOnly || !level.isEmpty()
                ? courseService.searchFilteredCourses(localCode, codes, weekendOnly, level, page, 4)
                : courseService.searchCoursesBySportCodes(localCode, codes, page, 4);
        conversation.rememberSearch(sport, localCode, page, !courses.last(), personalized);
        conversation.rememberFilters(weekendOnly, level);
        conversation.rememberDays(allowedDays);
        String content = courses.content().isEmpty()
                ? "해당 조건으로 더 보여드릴 강좌가 없습니다. 다른 지역이나 종목으로 찾아볼까요?"
                : next ? "같은 조건의 다음 강좌를 보여드릴게요."
                : (decision.content() == null || decision.content().isBlank() ? sport + " 강좌를 찾아봤어요." : decision.content());
        if (!allowedDays.isEmpty() || !level.isEmpty()) {
            content += "\n적용 조건: " + (allowedDays.isEmpty() ? "요일 무관" : dayNames(allowedDays) + " 중 하나 이상 포함된 강좌(다른 요일 수업도 포함 가능)")
                    + (level.isEmpty() ? "" : ", 강좌명에 '" + level + "' 표기");
        }
        return new RecommendationResult(content, sport, codes, localCode, page, !courses.last(), courses.content(), weekendOnly, level, allowedDays, List.of());
    }

    private RecommendationResult message(String content) {
        return RecommendationResult.message(content);
    }

    private String dayNames(String mask) {
        String[] names = {"월", "화", "수", "목", "금", "토", "일"};
        var selected = new java.util.ArrayList<String>();
        for (int index = 0; index < names.length; index++) {
            if (mask.charAt(index) == '1') selected.add(names[index]);
        }
        return String.join("·", selected);
    }
}
