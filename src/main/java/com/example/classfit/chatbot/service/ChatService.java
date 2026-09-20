package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.dto.req.ChatMessageReq;
import com.example.classfit.chatbot.dto.res.ChatMessageRes;
import com.example.classfit.chatbot.dto.res.ConversationCreateRes;
import com.example.classfit.chatbot.dto.res.ConversationListRes;

import java.util.List;

public interface ChatService {

    ConversationCreateRes createConversation(Long memberId);

    ChatMessageRes sendMessage(Long memberId, Long conversationId, ChatMessageReq request);

    List<ConversationListRes> getConversations(Long memberId);
}
