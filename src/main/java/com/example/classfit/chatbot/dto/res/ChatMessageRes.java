package com.example.classfit.chatbot.dto.res;

import com.example.classfit.chatbot.domain.enums.ChatRole;

public record ChatMessageRes(
        Long messageId,
        ChatRole role,
        String content
) {
}
