package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.*;
import com.example.classfit.chatbot.dto.req.ChatMessageReq;
import com.example.classfit.chatbot.dto.res.*;
import com.example.classfit.chatbot.repository.*;
import com.example.classfit.common.PageResponse;
import com.example.classfit.common.exception.BusinessException;
import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.course.service.*;
import com.example.classfit.inbody.service.InBodyService;
import com.example.classfit.inbody.exception.InBodyErrorCode;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.domain.enums.*;
import com.example.classfit.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatRecommendationFlowTest {
    @Test
    void returnsActualCardsAndRestoresThemFromSavedHistoryWithoutInBody() {
        var conversations = mock(ConversationRepository.class);
        var messages = mock(ChatMessageRepository.class);
        var ai = mock(AiService.class);
        var inBody = mock(InBodyService.class);
        var courses = mock(CourseService.class);
        var catalog = new RecommendationCatalogService(new ClassPathResource("data/sports.csv"),
                new ClassPathResource("data/regions.csv"));
        var member = Member.createOAuthMember(OAuthProvider.KAKAO, "test", "test", null, Gender.FEMALE);
        ReflectionTestUtils.setField(member, "id", 1L);
        var conversation = Conversation.builder().member(member).build();
        when(conversations.findById(10L)).thenReturn(Optional.of(conversation));
        List<ChatMessage> saved = new ArrayList<>();
        when(messages.save(any())).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            saved.add(message);
            return message;
        });
        when(messages.findAllByConversationIdOrderByCreatedAtAsc(10L)).thenAnswer(invocation -> List.copyOf(saved));
        when(inBody.findLatestInBody(1L)).thenReturn(Optional.empty());
        when(ai.recommend(anyList(), anyString())).thenReturn(
                new RecommendationDecision("수영을 찾아볼게요.", "SEARCH", "EXPLICIT", "수영", "원주시"));
        var card = new CourseSearchResponse(42L, "실제 수영반", "12", "수영", "시설", "주소", "10:00", "11:00", "월", 30000);
        when(courses.searchCoursesBySportCodes("51130", List.of("12"), 0, 4))
                .thenReturn(new PageResponse<>(List.of(card), 0, 4, 1, 1, true, true));
        var transactions = mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenAnswer(invocation ->
                new org.springframework.transaction.support.SimpleTransactionStatus());
        var chat = new ChatServiceImpl(conversations, mock(MemberRepository.class), messages, ai, inBody,
                catalog, new RecommendationService(courses, catalog), new ObjectMapper(), transactions);

        var response = chat.sendMessage(1L, 10L, new ChatMessageReq("원주 수영 찾아줘"));
        assertThat(response.recommendation().courses()).containsExactly(card);
        assertThat(response.recommendation().courses().getFirst().courseId()).isEqualTo(42L);
        assertThat(chat.getMessages(1L, 10L).getLast().recommendation()).isEqualTo(response.recommendation());
        assertThatThrownBy(() -> chat.sendMessage(2L, 10L, new ChatMessageReq("내 정보")))
                .isInstanceOf(BusinessException.class);
    }
}
