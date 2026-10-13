package com.example.classfit.member.dto;

import com.example.classfit.member.domain.enums.Gender;
import jakarta.validation.constraints.NotNull;

public record MemberGenderUpdateRequest(@NotNull Gender gender) {
}
