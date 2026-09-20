package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.dto.req.ChatMessageReq;
import com.example.classfit.chatbot.dto.res.ChatMessageRes;
import com.example.classfit.chatbot.dto.res.ConversationCreateRes;

public interface ChatService {

    ConversationCreateRes createConversation(Long memberId);

    ChatMessageRes sendMessage(Long memberId, Long conversationId, ChatMessageReq request);

}
