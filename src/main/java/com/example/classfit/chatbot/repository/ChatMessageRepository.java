package com.example.classfit.chatbot.repository;

import com.example.classfit.chatbot.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}