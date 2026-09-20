package com.example.classfit.chatbot.dto.res;


import java.time.LocalDateTime;

public record ConversationListRes(
        Long conversationId,
        String title,
        LocalDateTime createdAt
) {
}