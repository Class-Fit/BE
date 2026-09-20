package com.example.classfit.course.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PublicCourseItem(
        @JsonProperty("brno") String businessRegistrationNumber,
        @JsonProperty("facil_sn") String facilitySerialNumber,
        @JsonProperty("course_no") String courseNumber,
        @JsonProperty("course_nm") String courseName,
        @JsonProperty("item_cd") String sportCode,
        @JsonProperty("item_nm") String sportName,
        @JsonProperty("lectr_nm") String instructorName,
        @JsonProperty("start_tm") String startTime,
        @JsonProperty("equip_tm") String endTime,
        @JsonProperty("lectr_weekday_val") String weekdayMask,
        @JsonProperty("settl_amt") Integer fee,
        @JsonProperty("course_seta_desc_cn") String description
) {
}
