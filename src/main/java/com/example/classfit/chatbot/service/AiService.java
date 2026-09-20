package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.ChatMessage;

import java.util.List;

public interface AiService {

    String generateResponse(List<ChatMessage> messages);
}