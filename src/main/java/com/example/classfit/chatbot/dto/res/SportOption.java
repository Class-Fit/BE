package com.example.classfit.chatbot.dto.res;

/** 검증된 CSV 종목명과 해당 사용자의 정보에 근거한 후보 설명. */
public record SportOption(String sportName, String reason) {}
