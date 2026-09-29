package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.Conversation;
import com.example.classfit.chatbot.dto.req.ChatMessageReq;
import com.example.classfit.chatbot.exception.ChatbotErrorCode;
import com.example.classfit.chatbot.repository.*;
import com.example.classfit.common.exception.BusinessException;
import com.example.classfit.course.service.RecommendationCatalogService;
import com.example.classfit.inbody.service.InBodyService;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.*;
import com.example.classfit.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatConflictTranslationTest {
    @Test
    void prepareCommitConflictIsTranslatedBeforeAiCall() {
        var transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doThrow(new OptimisticLockingFailureException("concurrent update")).when(transactions).commit(any());
        var repository = mock(ConversationRepository.class);
        var member = Member.createOAuthMember(OAuthProvider.KAKAO, "test", "test", null, Gender.FEMALE);
        ReflectionTestUtils.setField(member, "id", 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(Conversation.builder().member(member).build()));
        var ai = mock(AiService.class);
        var service = new ChatServiceImpl(repository, mock(MemberRepository.class), mock(ChatMessageRepository.class),
                ai, mock(InBodyService.class), mock(RecommendationCatalogService.class),
                mock(RecommendationService.class), new ObjectMapper(), transactions);
        assertThatThrownBy(() -> service.sendMessage(1L, 1L, new ChatMessageReq("추천해줘")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ChatbotErrorCode.CONVERSATION_CONFLICT));
        verifyNoInteractions(ai);
    }

    @Test
    void expiredClaimCanBeReplacedButOldRequestCannotReleaseReplacement() {
        var conversation = Conversation.builder().build();
        var now = Instant.parse("2026-09-29T00:00:00Z");
        conversation.claimRequest("old", now);
        assertThatThrownBy(() -> conversation.claimRequest("new", now.plusSeconds(1)))
                .isInstanceOf(BusinessException.class);
        conversation.claimRequest("new", now.plus(Duration.ofMinutes(10)));
        conversation.releaseRequest("old");
        assertThat(conversation.ownsRequest("new")).isTrue();
        conversation.releaseRequest("new");
        assertThat(conversation.getPendingRequestToken()).isNull();
    }
}
