package com.example.classfit.course.domain;

import com.example.classfit.course.dto.PublicCourseItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "courses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_courses_facility_course_no",
                columnNames = {"facility_id", "course_no"}
        )
)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "brno", nullable = false, length = 20)
    private String businessRegistrationNumber;

    @Column(name = "course_no", nullable = false, length = 30)
    private String courseNumber;

    @Column(name = "course_nm", nullable = false)
    private String name;

    @Column(name = "item_cd", length = 20)
    private String sportCode;

    @Column(name = "item_nm")
    private String sportName;

    @Column(name = "lectr_nm")
    private String instructorName;

    @Column(name = "start_tm", length = 10)
    private String startTime;

    @Column(name = "end_tm", length = 10)
    private String endTime;

    @Column(name = "weekday_mask", length = 7)
    private String weekdayMask;

    private Integer fee;

    @Lob
    private String description;

    private Course(Facility facility, PublicCourseItem item) {
        update(facility, item);
    }

    public static Course from(Facility facility, PublicCourseItem item) {
        return new Course(facility, item);
    }

    public boolean update(Facility facility, PublicCourseItem item) {
        String businessRegistrationNumber = normalize(item.businessRegistrationNumber());
        String courseNumber = normalize(item.courseNumber());
        String name = normalize(item.courseName());
        String sportCode = normalize(item.sportCode());
        String sportName = normalize(item.sportName());
        String instructorName = normalize(item.instructorName());
        String startTime = normalize(item.startTime());
        String endTime = normalize(item.endTime());
        String weekdayMask = normalize(item.weekdayMask());
        String description = normalize(item.description());

        boolean changed = !Objects.equals(this.businessRegistrationNumber, businessRegistrationNumber)
                || !Objects.equals(this.courseNumber, courseNumber)
                || !Objects.equals(this.name, name)
                || !Objects.equals(this.sportCode, sportCode)
                || !Objects.equals(this.sportName, sportName)
                || !Objects.equals(this.instructorName, instructorName)
                || !Objects.equals(this.startTime, startTime)
                || !Objects.equals(this.endTime, endTime)
                || !Objects.equals(this.weekdayMask, weekdayMask)
                || !Objects.equals(this.fee, item.fee())
                || !Objects.equals(this.description, description);

        if (changed) {
            this.facility = facility;
            this.businessRegistrationNumber = businessRegistrationNumber;
            this.courseNumber = courseNumber;
            this.name = name;
            this.sportCode = sportCode;
            this.sportName = sportName;
            this.instructorName = instructorName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.weekdayMask = weekdayMask;
            this.fee = item.fee();
            this.description = description;
        }
        return changed;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
