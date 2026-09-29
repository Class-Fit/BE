package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.enums.ChatRole;
import com.example.classfit.chatbot.dto.req.ChatMessageReq;
import com.example.classfit.chatbot.dto.res.RecommendationDecision;
import com.example.classfit.chatbot.exception.ChatbotErrorCode;
import com.example.classfit.chatbot.repository.*;
import com.example.classfit.common.exception.BusinessException;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.*;
import com.example.classfit.member.repository.MemberRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.connection-timeout=2000", "public-data.sync.cron=-"})
@ActiveProfiles("test")
class ChatTransactionTest {
    @Autowired ChatService chat;
    @Autowired MemberRepository members;
    @Autowired ConversationRepository conversations;
    @Autowired ChatMessageRepository messages;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean AiService ai;
    Long memberId;
    Long conversationId;
    TransactionTemplate transaction;

    @BeforeEach
    void setup() {
        transaction = new TransactionTemplate(manager);
        memberId = members.save(Member.createOAuthMember(OAuthProvider.KAKAO,
                UUID.randomUUID().toString(), "transaction-test", null, Gender.FEMALE)).getId();
        conversationId = chat.createConversation(memberId).conversationId();
    }

    @AfterEach
    void cleanup() {
        transaction.executeWithoutResult(status -> {
            messages.deleteAll(messages.findAllByConversationIdOrderByCreatedAtAsc(conversationId));
            conversations.deleteById(conversationId);
            members.deleteById(memberId);
        });
    }

    private RecommendationDecision answer() {
        return new RecommendationDecision("어떤 운동을 원하시나요?", "ASK", "EXPLICIT", null, null);
    }

    @Test
    void commitsUserAndReturnsOnlyConnectionBeforeCallingAiWithoutInBody() {
        when(ai.recommend(anyList(), anyString())).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
            // 풀 크기가 1이므로 이전 커넥션을 점유하면 이 조회가 타임아웃된다.
            var committed = transaction.execute(status -> messages.findAllByConversationIdOrderByCreatedAtAsc(conversationId));
            assertThat(committed)
                    .extracting(message -> message.getRole()).containsExactly(ChatRole.USER);
            return answer();
        });
        chat.sendMessage(memberId, conversationId, new ChatMessageReq("안녕"));
        assertThat(messages.findAllByConversationIdOrderByCreatedAtAsc(conversationId))
                .extracting(message -> message.getRole()).containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
        assertThat(conversations.findById(conversationId).orElseThrow().getPendingRequestToken()).isNull();
    }

    @Test
    void overlappingRequestIsRejectedBeforeSecondAiCall() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(ai.recommend(anyList(), anyString())).thenAnswer(call -> {
            entered.countDown();
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            return answer();
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var first = executor.submit(() -> chat.sendMessage(memberId, conversationId, new ChatMessageReq("첫 요청")));
            try {
                assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> chat.sendMessage(memberId, conversationId, new ChatMessageReq("겹친 요청")))
                        .isInstanceOfSatisfying(BusinessException.class,
                                error -> assertThat(error.getErrorCode()).isEqualTo(ChatbotErrorCode.CONVERSATION_BUSY));
            } finally {
                release.countDown();
            }
            first.get(10, TimeUnit.SECONDS);
        }
        verify(ai, times(1)).recommend(anyList(), anyString());
        assertThat(messages.findAllByConversationIdOrderByCreatedAtAsc(conversationId)).hasSize(2);
    }

    @Test
    void aiFailureKeepsUserAndReleasesClaimForNextRequest() {
        when(ai.recommend(anyList(), anyString())).thenThrow(new IllegalStateException("AI unavailable"));
        assertThatThrownBy(() -> chat.sendMessage(memberId, conversationId, new ChatMessageReq("첫 요청")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(messages.findAllByConversationIdOrderByCreatedAtAsc(conversationId))
                .extracting(message -> message.getRole()).containsExactly(ChatRole.USER);
        assertThat(conversations.findById(conversationId).orElseThrow().getPendingRequestToken()).isNull();
        doReturn(answer()).when(ai).recommend(anyList(), anyString());
        chat.sendMessage(memberId, conversationId, new ChatMessageReq("다시 요청"));
        assertThat(messages.findAllByConversationIdOrderByCreatedAtAsc(conversationId)).hasSize(3);
    }

    @Test
    void interveningVersionChangeRejectsStaleResultAndKeepsUser() {
        when(ai.recommend(anyList(), anyString())).thenAnswer(call -> {
            transaction.executeWithoutResult(status -> conversations.findById(conversationId)
                    .orElseThrow().updateTitle("다른 작업이 변경함"));
            return answer();
        });
        assertThatThrownBy(() -> chat.sendMessage(memberId, conversationId, new ChatMessageReq("추천해줘")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ChatbotErrorCode.CONVERSATION_CONFLICT));
        assertThat(messages.findAllByConversationIdOrderByCreatedAtAsc(conversationId))
                .extracting(message -> message.getRole()).containsExactly(ChatRole.USER);
        assertThat(conversations.findById(conversationId).orElseThrow().getPendingRequestToken()).isNull();
    }
}
