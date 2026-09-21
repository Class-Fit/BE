package com.example.classfit.inbody.dto.req;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InBodyCreateReq(

        @NotNull
        @DecimalMax(value = "300.0")
        BigDecimal heightCm,

        @NotNull
        @DecimalMax(value = "300.0")
        BigDecimal weightKg,

        @NotNull
        @DecimalMax(value = "100.0")
        BigDecimal bodyFatPercentage,

        @NotNull
        @DecimalMax(value = "100.0")
        BigDecimal skeletalMuscleMassKg,

        @NotNull
        @DecimalMax(value = "300.0")
        BigDecimal bodyFatMassKg,

        @NotNull
        @DecimalMax(value = "100.0", message = "BMI는 100 이하여야 합니다.")
        BigDecimal bmi

) {
}