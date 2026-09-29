package com.example.classfit.favorite.domain;

import com.example.classfit.common.BaseEntity;
import com.example.classfit.course.domain.Course;
import com.example.classfit.member.domain.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회원과 강좌의 찜 관계를 저장하며, 같은 회원과 강좌의 중복 행을 금지한다. */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "favorites",
        indexes = @Index(name = "idx_favorites_course_id", columnList = "course_id"),
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

    /**
     * 지정한 회원과 강좌를 연결한 찜 객체를 구성한다.
     *
     * @param member 찜을 소유하는 회원
     * @param course 찜 대상 강좌
     */
    private Favorite(Member member, Course course) {
        this.member = member;
        this.course = course;
    }

    /**
     * 회원과 강좌의 찜 객체를 생성한다. DB 저장은 수행하지 않는다.
     *
     * @param member 찜을 소유하는 회원
     * @param course 찜 대상 강좌
     * @return 아직 영속화되지 않은 찜 객체
     */
    public static Favorite create(Member member, Course course) {
        return new Favorite(member, course);
    }
}
