package com.example.classfit.course.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 추천에 사용할 종목·지역 기준표를 시작 시 한 번 읽는다. 외부 API 코드 호환성은 별도 검증한다. */
@Service
public class RecommendationCatalogService {
    private final List<Sport> sports;
    private final List<Region> regions;

    public RecommendationCatalogService(
            @Value("classpath:data/sports.csv") Resource sportsResource,
            @Value("classpath:data/regions.csv") Resource regionsResource
    ) {
        sports = readRows(sportsResource, "주종목코드,종목명,비고", false).stream()
                .map(row -> new Sport(row[0], row[1], row[2])).toList();
        regions = readRows(regionsResource, "시군구코드,시군구명", true).stream()
                .map(row -> new Region(row[0], row[1], row[0].endsWith("000"))).toList();
    }

    public List<Sport> getSports() {
        return sports;
    }

    public List<Region> getRegions() {
        return regions;
    }

    /** 같은 이름의 코드를 전부 반환한다. 조회 시 하나만 임의로 선택하지 않는다. */
    public List<String> findSportCodes(String sportName) {
        String name = normalize(sportName);
        if (name.isEmpty()) {
            return List.of();
        }
        return sports.stream().filter(sport -> sport.name().equals(name))
                .map(Sport::code).toList();
    }

    /** 전체 이름 또는 행정구역 경계에 맞는 이름으로 조회하며, 동명 지역은 모두 반환한다. */
    public List<Region> findRegions(String regionName) {
        String name = normalize(regionName);
        if (name.isEmpty()) {
            return List.of();
        }
        return regions.stream()
                .filter(region -> region.name().equals(name) || region.name().endsWith(" " + name))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    /** 현재 기준표는 인용 필드가 없는 고정 열 CSV이다. 형식이 바뀌면 조용히 오독하지 않고 중단한다. */
    private static List<String[]> readRows(Resource resource, String expectedHeader, boolean region) {
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), decoder))) {
            String header = reader.readLine();
            if (header != null && header.startsWith("\uFEFF")) {
                header = header.substring(1);
            }
            if (!expectedHeader.equals(header)) {
                throw invalid(resource, 1, "열 이름을 확인하세요.");
            }
            int columnCount = expectedHeader.split(",", -1).length;
            List<String[]> rows = new ArrayList<>();
            Set<String> codes = new HashSet<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                String[] row = line.split(",", -1);
                if (line.contains("\"") || row.length != columnCount) {
                    throw invalid(resource, lineNumber, "인용 필드 없는 CSV의 열 수를 확인하세요.");
                }
                for (int index = 0; index < row.length; index++) {
                    row[index] = row[index].strip();
                }
                if (!row[0].matches(region ? "[0-9]{5}" : "[0-9]+") || row[1].isEmpty()) {
                    throw invalid(resource, lineNumber, "코드와 이름을 확인하세요.");
                }
                if (!codes.add(row[0])) {
                    throw invalid(resource, lineNumber, "동일 코드가 중복되었습니다.");
                }
                rows.add(row);
            }
            if (rows.isEmpty()) {
                throw invalid(resource, 1, "기준표가 비어 있습니다.");
            }
            return rows;
        } catch (IOException exception) {
            throw new IllegalStateException("UTF-8 CSV를 읽지 못했습니다: " + resource.getDescription(), exception);
        }
    }

    private static IllegalStateException invalid(Resource resource, int line, String reason) {
        return new IllegalStateException(resource.getDescription() + " (" + line + "행): " + reason);
    }

    public record Sport(String code, String name, String note) {}

    /** provinceLevel=true인 행은 시군구 localCode 필터에 바로 전달하지 않는다. */
    public record Region(String code, String name, boolean provinceLevel) {}
}
