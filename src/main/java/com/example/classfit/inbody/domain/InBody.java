package com.example.classfit.inbody.domain;

import com.example.classfit.common.BaseEntity;
import com.example.classfit.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "inbody")
public class InBody extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "height_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "body_fat_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal bodyFatPercentage;

    @Column(name = "skeletal_muscle_mass_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal skeletalMuscleMassKg;

    @Column(name = "body_fat_mass_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal bodyFatMassKg;

    @Column(name = "bmi", nullable = false, precision = 5, scale = 2)
    private BigDecimal bmi;
}