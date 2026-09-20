package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.ChatMessage;
import com.example.classfit.chatbot.domain.Conversation;
import com.example.classfit.chatbot.domain.enums.ChatRole;
import com.example.classfit.chatbot.dto.req.ChatMessageReq;
import com.example.classfit.chatbot.dto.res.ChatMessageRes;
import com.example.classfit.chatbot.dto.res.ConversationCreateRes;
import com.example.classfit.chatbot.exception.ChatbotErrorCode;
import com.example.classfit.chatbot.repository.ChatMessageRepository;
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
    private final ChatMessageRepository chatMessageRepository;

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

    @Override
    @Transactional
    public ChatMessageRes sendMessage(
            Long memberId,
            Long conversationId,
            ChatMessageReq request
    ) {

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() ->
                        new BusinessException(
                                ChatbotErrorCode.CONVERSATION_NOT_FOUND
                        )
                );

        if (!conversation.getMember().getId().equals(memberId)) {
            throw new BusinessException(
                    ChatbotErrorCode.CONVERSATION_ACCESS_DENIED
            );
        }

        ChatMessage userMessage = ChatMessage.builder()
                .conversation(conversation)
                .role(ChatRole.USER)
                .content(request.content())
                .build();

        ChatMessage savedMessage =
                chatMessageRepository.save(userMessage);

        return new ChatMessageRes(
                savedMessage.getId(),
                savedMessage.getContent()
        );
    }
}
