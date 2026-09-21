package com.example.classfit.inbody.dto.req;

import java.math.BigDecimal;

public record InBodyCreateReq(
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal bodyFatPercentage,
        BigDecimal skeletalMuscleMassKg,
        BigDecimal bodyFatMassKg,
        BigDecimal bmi
) {
}
