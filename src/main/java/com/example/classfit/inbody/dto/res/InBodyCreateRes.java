package com.example.classfit.inbody.dto.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InBodyCreateRes(
        Long inBodyId,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal bodyFatPercentage,
        BigDecimal skeletalMuscleMassKg,
        BigDecimal bodyFatMassKg,
        BigDecimal bmi,
        LocalDateTime createdAt
) {
}