package com.example.classfit.chatbot.controller;

import com.example.classfit.chatbot.dto.res.ChatMessageListRes;
import com.example.classfit.chatbot.dto.res.ChatMessageRes;
import com.example.classfit.chatbot.dto.res.ConversationListRes;
import com.example.classfit.chatbot.service.AiService;
import com.example.classfit.chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final AiService aiService;
    private final ChatService chatService;

    @GetMapping("/test")
    public String test(@RequestParam String message) {
        return aiService.generateResponse(message);
    }

    @GetMapping("/conversations")
    public List<ConversationListRes> getConversations(
            @RequestParam Long memberId
    ) {
        return chatService.getConversations(memberId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<ChatMessageListRes> getMessages(
            @RequestParam Long memberId,
            @PathVariable Long conversationId
    ) {
        return chatService.getMessages(memberId, conversationId);
    }
}