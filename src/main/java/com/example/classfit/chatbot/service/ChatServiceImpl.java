package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.ChatMessage;
import com.example.classfit.chatbot.domain.Conversation;
import com.example.classfit.chatbot.domain.enums.ChatRole;
import com.example.classfit.chatbot.dto.req.ChatMessageReq;
import com.example.classfit.chatbot.dto.res.ChatMessageListRes;
import com.example.classfit.chatbot.dto.res.ChatMessageRes;
import com.example.classfit.chatbot.dto.res.ConversationCreateRes;
import com.example.classfit.chatbot.dto.res.ConversationListRes;
import com.example.classfit.chatbot.exception.ChatbotErrorCode;
import com.example.classfit.chatbot.repository.ChatMessageRepository;
import com.example.classfit.chatbot.repository.ConversationRepository;
import com.example.classfit.common.exception.BusinessException;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.repository.MemberRepository;
import com.example.classfit.chatbot.dto.res.RecommendationResult;
import com.example.classfit.course.service.RecommendationCatalogService;
import com.example.classfit.inbody.service.InBodyService;
import com.example.classfit.inbody.dto.res.InBodyCreateRes;
import com.example.classfit.inbody.exception.InBodyErrorCode;
import com.example.classfit.common.exception.CommonErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService;
    private final InBodyService inBodyService;
    private final RecommendationCatalogService catalog;
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

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

        // 채팅방 조회
        if (request == null || request.content() == null || request.content().isBlank()
                || request.content().length() > 4000) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new BusinessException(
                                ChatbotErrorCode.CONVERSATION_NOT_FOUND
                        )
                );

        // 본인의 채팅방인지 확인
        if (!conversation.getMember().getId().equals(memberId)) {
            throw new BusinessException(
                    ChatbotErrorCode.CONVERSATION_ACCESS_DENIED
            );
        }

        // USER 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .conversation(conversation)
                .role(ChatRole.USER)
                .content(request.content())
                .build();

        chatMessageRepository.save(userMessage);

        // 지금까지의 대화 조회
        List<ChatMessage> messages =
                chatMessageRepository
                        .findAllByConversationIdOrderByCreatedAtAsc(
                                conversationId
                        );

        // GPT 호출
        InBodyCreateRes inBody = latestInBody(memberId);
        var context = new LinkedHashMap<String, Object>();
        context.put("gender", conversation.getMember().getGender());
        context.put("latestInBody", inBody == null ? null : java.util.Map.of(
                "heightCm", inBody.heightCm(), "weightKg", inBody.weightKg(),
                "bodyFatPercentage", inBody.bodyFatPercentage(),
                "skeletalMuscleMassKg", inBody.skeletalMuscleMassKg(),
                "bodyFatMassKg", inBody.bodyFatMassKg(), "bmi", inBody.bmi()));
        context.put("sports", catalog.getSports());
        context.put("regions", catalog.getRegions());
        var previousSearch = new LinkedHashMap<String, Object>();
        previousSearch.put("sport", conversation.getRecommendedSport());
        previousSearch.put("localCode", conversation.getRecommendationLocalCode());
        previousSearch.put("page", conversation.getRecommendationPage());
        previousSearch.put("hasNext", conversation.getRecommendationHasNext());
        previousSearch.put("personalized", conversation.getPersonalizedRecommendation());
        previousSearch.put("weekendOnly", conversation.getRecommendationWeekendOnly());
        previousSearch.put("level", conversation.getRecommendationLevel());
        previousSearch.put("allowedDays", conversation.getRecommendationAllowedDays());
        context.put("previousSearch", previousSearch);
        context.put("candidateSports", conversation.candidateSports());
        context.put("selectedCandidateSport", conversation.getSelectedCandidateSport());
        context.put("candidateRegionName", conversation.getCandidateRegionName());
        RecommendationResult recommendation = recommendationService.resolve(conversation,
                aiService.recommend(messages, toJson(context)), inBody != null,
                conversation.getMember().getGender() != null);
        String aiResponse = recommendation.content();

        // GPT 응답 저장
        ChatMessage assistantMessage = ChatMessage.builder()
                .conversation(conversation)
                .role(ChatRole.ASSISTANT)
                .content(aiResponse)
                .recommendationJson(toJson(recommendation))
                .build();

        ChatMessage savedAssistant =
                chatMessageRepository.save(assistantMessage);

        // GPT 응답 반환
        return new ChatMessageRes(
                savedAssistant.getId(),
                savedAssistant.getRole(),
                savedAssistant.getContent(), recommendation
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationListRes> getConversations(Long memberId) {

        return conversationRepository
                .findAllByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(conversation -> new ConversationListRes(
                        conversation.getId(),
                        conversation.getTitle(),
                        conversation.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageListRes> getMessages(
            Long memberId,
            Long conversationId
    ) {

        Conversation conversation = conversationRepository
                .findById(conversationId)
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

        return chatMessageRepository
                .findAllByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(message -> new ChatMessageListRes(
                        message.getId(),
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt(), readRecommendation(message.getRecommendationJson())
                ))
                .toList();
    }
    private InBodyCreateRes latestInBody(Long memberId) {
        try {
            return inBodyService.getLatestInBody(memberId);
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == InBodyErrorCode.INBODY_NOT_FOUND) return null;
            throw exception;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("추천 정보를 변환하지 못했습니다.", exception);
        }
    }

    private RecommendationResult readRecommendation(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, RecommendationResult.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 추천 정보를 읽지 못했습니다.", exception);
        }
    }
}
