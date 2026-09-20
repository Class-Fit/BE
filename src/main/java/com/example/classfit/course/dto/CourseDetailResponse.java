package com.example.classfit.course.dto;

import com.example.classfit.course.domain.Course;

public record CourseDetailResponse(
        Long courseId,
        String courseName,
        String sportCode,
        String sportName,
        String instructorName,
        String startTime,
        String endTime,
        String weekdays,
        Integer fee,
        String description,
        String facilityName,
        String address,
        String cityName,
        String localName
) {
    public static CourseDetailResponse from(Course course) {
        CourseSearchResponse summary = CourseSearchResponse.from(course);
        return new CourseDetailResponse(
                summary.courseId(),
                summary.courseName(),
                summary.sportCode(),
                summary.sportName(),
                course.getInstructorName(),
                summary.startTime(),
                summary.endTime(),
                summary.weekdays(),
                summary.fee(),
                course.getDescription(),
                summary.facilityName(),
                summary.address(),
                course.getFacility().getCityName(),
                course.getFacility().getLocalName()
        );
    }
}
