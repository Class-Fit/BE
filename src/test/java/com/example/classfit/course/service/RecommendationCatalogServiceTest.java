package com.example.classfit.course.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecommendationCatalogServiceTest {
    private final RecommendationCatalogService catalog = new RecommendationCatalogService(
            new ClassPathResource("data/sports.csv"), new ClassPathResource("data/regions.csv"));

    @Test
    void preservesLeadingZeroAndAllCodesForSameSport() {
        assertThat(catalog.findSportCodes(" 검도 ")).containsExactly("01");
        assertThat(catalog.findSportCodes("수영")).containsExactly("12");
        assertThat(catalog.findSportCodes("필라테스")).containsExactly("79", "106");
        assertThat(catalog.findSportCodes("태권도")).containsExactly("22", "96");
    }

    @Test
    void resolvesFullOrShortRegionNameWithoutChoosingAnAmbiguousRegion() {
        assertThat(catalog.findRegions(" 원주시 "))
                .extracting(RecommendationCatalogService.Region::code).containsExactly("51130");
        assertThat(catalog.findRegions("강원특별자치도 원주시"))
                .extracting(RecommendationCatalogService.Region::code).containsExactly("51130");
        assertThat(catalog.findRegions("고성군"))
                .extracting(RecommendationCatalogService.Region::code)
                .containsExactlyInAnyOrder("48820", "51820");
    }

    @Test
    void distinguishesProvinceEntriesAndTrimsCsvValues() {
        assertThat(catalog.findRegions("강원특별자치도")).singleElement()
                .satisfies(region -> assertThat(region.provinceLevel()).isTrue());
        assertThat(catalog.findRegions("원주시")).singleElement()
                .satisfies(region -> assertThat(region.provinceLevel()).isFalse());
        assertThat(catalog.findRegions("경기도 부천시 원미구"))
                .extracting(RecommendationCatalogService.Region::code).containsExactly("41192");
    }

    @Test
    void missingInputNeverReturnsAllCandidates() {
        assertThat(catalog.findSportCodes(null)).isEmpty();
        assertThat(catalog.findSportCodes("없는종목")).isEmpty();
        assertThat(catalog.findRegions(" ")).isEmpty();
        assertThat(catalog.findRegions(null)).isEmpty();
        assertThat(catalog.findRegions("없는지역")).isEmpty();
    }

    @Test
    void exposesReadOnlyCatalogsForRecommendationContext() {
        assertThat(catalog.getSports()).isNotEmpty();
        assertThat(catalog.getRegions()).isNotEmpty();
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> catalog.getSports().clear());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> catalog.getRegions().clear());
    }

    @Test
    void rejectsMalformedOrDuplicateCodesInsteadOfLoadingPartialCatalog() {
        for (String csv : new String[]{
                "wrong,header\n12,수영",
                "주종목코드,종목명,비고\n12,수영,\n12,골프,",
                "주종목코드,종목명,비고\n12,,",
                "주종목코드,종목명,비고\n12,수영,비고,추가열",
                "주종목코드,종목명,비고\n"
        }) {
            assertThatThrownBy(() -> new RecommendationCatalogService(
                    new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)),
                    new ClassPathResource("data/regions.csv")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void rejectsNonUtf8Data() {
        assertThatThrownBy(() -> new RecommendationCatalogService(
                new ByteArrayResource(new byte[]{(byte) 0xff}),
                new ClassPathResource("data/regions.csv")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("UTF-8");
    }
}
