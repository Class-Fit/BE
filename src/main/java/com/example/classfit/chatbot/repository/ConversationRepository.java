package com.example.classfit.chatbot.repository;

import com.example.classfit.chatbot.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}