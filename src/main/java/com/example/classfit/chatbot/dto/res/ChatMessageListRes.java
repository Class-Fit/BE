package com.example.classfit.chatbot.dto.res;


import com.example.classfit.chatbot.domain.enums.ChatRole;

import java.time.LocalDateTime;

public record ChatMessageListRes(
        Long messageId,
        ChatRole role,
        String content,
        LocalDateTime createdAt
) {
}