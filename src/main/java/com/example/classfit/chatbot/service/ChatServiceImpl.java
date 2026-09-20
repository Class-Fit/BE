package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.Conversation;
import com.example.classfit.chatbot.dto.res.ConversationCreateRes;
import com.example.classfit.chatbot.exception.ChatbotErrorCode;
import com.example.classfit.chatbot.repository.ConversationRepository;
import com.example.classfit.common.exception.BusinessException;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public ConversationCreateRes createConversation(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new BusinessException(ChatbotErrorCode.MEMBER_NOT_FOUND));

        Conversation conversation = Conversation.builder()
                .member(member)
                .title("새로운 대화")
                .build();

        Conversation savedConversation =
                conversationRepository.save(conversation);

        return new ConversationCreateRes(
                savedConversation.getId(),
                savedConversation.getTitle()
        );
    }
}
