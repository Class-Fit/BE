package com.example.classfit.course.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PublicFacilityItem(
        @JsonProperty("brno") String businessRegistrationNumber,
        @JsonProperty("facil_sn") String facilitySerialNumber,
        @JsonProperty("facil_nm") String facilityName,
        @JsonProperty("city_cd") String cityCode,
        @JsonProperty("city_nm") String cityName,
        @JsonProperty("local_cd") String localCode,
        @JsonProperty("local_nm") String localName,
        @JsonProperty("road_addr") String roadAddress,
        @JsonProperty("faci_daddr") String detailAddress,
        @JsonProperty("faci_zip") String zipCode,
        @JsonProperty("main_event_cd") String mainSportCode,
        @JsonProperty("main_event_nm") String mainSportName
) {
}
