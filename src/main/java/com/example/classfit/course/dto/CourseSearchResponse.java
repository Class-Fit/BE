package com.example.classfit.course.dto;

import com.example.classfit.course.domain.Course;

public record CourseSearchResponse(
        Long courseId,
        String courseName,
        String sportCode,
        String sportName,
        String facilityName,
        String address,
        String startTime,
        String endTime,
        String weekdays,
        Integer fee
) {
    public static CourseSearchResponse from(Course course) {
        return new CourseSearchResponse(
                course.getId(),
                course.getName(),
                course.getSportCode(),
                course.getSportName(),
                course.getFacility().getName(),
                course.getFacility().fullAddress(),
                course.getStartTime(),
                course.getEndTime(),
                formatWeekdays(course.getWeekdayMask()),
                course.getFee()
        );
    }

    private static String formatWeekdays(String weekdayMask) {
        if (weekdayMask == null || weekdayMask.length() != 7) {
            return weekdayMask;
        }

        String[] labels = {"월", "화", "수", "목", "금", "토", "일"};
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < weekdayMask.length(); index++) {
            if (weekdayMask.charAt(index) == '1') {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(labels[index]);
            }
        }
        return result.toString();
    }
}
