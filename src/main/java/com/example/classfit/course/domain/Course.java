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

    public void update(Facility facility, PublicCourseItem item) {
        this.facility = facility;
        this.businessRegistrationNumber = item.businessRegistrationNumber();
        this.courseNumber = item.courseNumber();
        this.name = item.courseName();
        this.sportCode = item.sportCode();
        this.sportName = item.sportName();
        this.instructorName = item.instructorName();
        this.startTime = item.startTime();
        this.endTime = item.endTime();
        this.weekdayMask = item.weekdayMask();
        this.fee = item.fee();
        this.description = item.description();
    }
}
