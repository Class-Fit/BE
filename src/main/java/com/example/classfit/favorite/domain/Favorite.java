package com.example.classfit.favorite.domain;

import com.example.classfit.common.BaseEntity;
import com.example.classfit.course.domain.Course;
import com.example.classfit.member.domain.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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
        name = "favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorites_member_course",
                columnNames = {"member_id", "course_id"}
        )
)
public class Favorite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private Favorite(Member member, Course course) {
        this.member = member;
        this.course = course;
    }

    public static Favorite create(Member member, Course course) {
        return new Favorite(member, course);
    }
}
