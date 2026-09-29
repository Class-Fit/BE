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
import com.example.classfit.common.exception.CommonErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;
import java.time.Instant;
import com.example.classfit.chatbot.dto.res.RecommendationDecision;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.OptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
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
    private final PlatformTransactionManager transactionManager;

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
    @Transactional(propagation = Propagation.NEVER)
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
        String token = UUID.randomUUID().toString();
        boolean prepared = false;
        boolean completed = false;
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            PreparedRequest input = transaction.execute(status -> prepare(memberId, conversationId, request, token));
            prepared = true;
            // 이 시점에는 저장 트랜잭션과 DB 커넥션이 모두 반환되어 있다.
            RecommendationDecision decision = aiService.recommend(input.messages(), input.context());
            ChatMessageRes response = transaction.execute(status -> complete(memberId, conversationId, token, input, decision));
            completed = true;
            return response;
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            throw new BusinessException(ChatbotErrorCode.CONVERSATION_CONFLICT);
        } finally {
            if (prepared && !completed) {
                try {
                    transaction.executeWithoutResult(status -> conversationRepository.findById(conversationId)
                            .ifPresent(conversation -> conversation.releaseRequest(token)));
                } catch (RuntimeException cleanupFailure) {
                    log.warn("대화 요청 정리 실패: conversationId={}", conversationId, cleanupFailure);
                }
            }
        }
    }

    private Conversation ownedConversation(Long memberId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ChatbotErrorCode.CONVERSATION_NOT_FOUND));
        if (!conversation.getMember().getId().equals(memberId)) {
            throw new BusinessException(ChatbotErrorCode.CONVERSATION_ACCESS_DENIED);
        }
        return conversation;
    }

    private PreparedRequest prepare(Long memberId, Long conversationId, ChatMessageReq request, String token) {
        Conversation conversation = ownedConversation(memberId, conversationId);
        conversation.claimRequest(token, Instant.now());
        // 경쟁 요청은 AI 호출 전에 버전 검증으로 차단한다.
        conversationRepository.flush();

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

        // AI에 전달할 데이터는 트랜잭션 안에서 모두 읽는다.
        InBodyCreateRes inBody = inBodyService.findLatestInBody(memberId).orElse(null);
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
        return new PreparedRequest(List.copyOf(messages), toJson(context), conversation.getVersion(),
                inBody != null, conversation.getMember().getGender() != null);
    }

    private ChatMessageRes complete(Long memberId, Long conversationId, String token,
                                    PreparedRequest input, RecommendationDecision decision) {
        Conversation conversation = ownedConversation(memberId, conversationId);
        if (!conversation.ownsRequest(token) || !Objects.equals(conversation.getVersion(), input.version())) {
            throw new BusinessException(ChatbotErrorCode.CONVERSATION_CONFLICT);
        }
        RecommendationResult recommendation = recommendationService.resolve(conversation, decision,
                input.hasInBody(), input.hasGender());
        conversation.releaseRequest(token);
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
    private record PreparedRequest(List<ChatMessage> messages, String context, Long version,
                                   boolean hasInBody, boolean hasGender) {}

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
