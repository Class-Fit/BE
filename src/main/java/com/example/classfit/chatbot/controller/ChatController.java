package com.example.classfit.chatbot.controller;

import com.example.classfit.chatbot.domain.ChatMessage;
import com.example.classfit.chatbot.domain.enums.ChatRole;
import com.example.classfit.chatbot.dto.req.ChatMessageReq;
import com.example.classfit.chatbot.dto.res.ChatMessageListRes;
import com.example.classfit.chatbot.dto.res.ChatMessageRes;
import com.example.classfit.chatbot.dto.res.ConversationCreateRes;
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

    // OpenAI 단독 테스트용
    @GetMapping("/test")
    public String test(@RequestParam String message) {

        ChatMessage chatMessage = ChatMessage.builder()
                .role(ChatRole.USER)
                .content(message)
                .build();

        return aiService.generateResponse(
                List.of(chatMessage)
        );
    }

    // 채팅방 생성
    @PostMapping("/conversations")
    public ConversationCreateRes createConversation(
            @RequestParam Long memberId
    ) {
        return chatService.createConversation(memberId);
    }

    // 내 채팅방 목록 조회
    @GetMapping("/conversations")
    public List<ConversationListRes> getConversations(
            @RequestParam Long memberId
    ) {
        return chatService.getConversations(memberId);
    }

    // 메시지 전송
    @PostMapping("/conversations/{conversationId}/messages")
    public ChatMessageRes sendMessage(
            @RequestParam Long memberId,
            @PathVariable Long conversationId,
            @RequestBody ChatMessageReq request
    ) {
        return chatService.sendMessage(
                memberId,
                conversationId,
                request
        );
    }

    // 특정 채팅방 메시지 목록 조회
    @GetMapping("/conversations/{conversationId}/messages")
    public List<ChatMessageListRes> getMessages(
            @RequestParam Long memberId,
            @PathVariable Long conversationId
    ) {
        return chatService.getMessages(
                memberId,
                conversationId
        );
    }
}