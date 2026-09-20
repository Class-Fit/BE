package com.example.classfit.chatbot.controller;

import com.example.classfit.chatbot.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final AiService aiService;

    @GetMapping("/test")
    public String test(@RequestParam String message) {
        return aiService.generateResponse(message);
    }
}