package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.dto.res.ConversationCreateRes;

public interface ChatService {

    ConversationCreateRes createConversation(Long memberId);

}
