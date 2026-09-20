package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.ChatMessage;
import com.example.classfit.chatbot.domain.enums.ChatRole;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private static final int MAX_HISTORY_MESSAGES = 20;
    private final OpenAIClient openAIClient;

    @Override
    public String generateResponse(List<ChatMessage> messages) {

        List<ChatMessage> recentMessages = getRecentMessages(messages);

        List<ResponseInputItem> inputs = recentMessages.stream()
                .map(this::toResponseInputItem)
                .toList();

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(ChatModel.GPT_5_2)
                .inputOfResponse(inputs)
                .build();

        Response response = openAIClient.responses().create(params);

        return response.output().stream()
                .flatMap(output -> output.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(text -> text.text())
                .findFirst()
                .orElse("응답을 생성하지 못했습니다.");
    }

    private ResponseInputItem toResponseInputItem(ChatMessage chatMessage) {

        // 사용자 메시지
        if (chatMessage.getRole() == ChatRole.USER) {

            return ResponseInputItem.ofMessage(
                    ResponseInputItem.Message.builder()
                            .role(ResponseInputItem.Message.Role.USER)
                            .addInputTextContent(chatMessage.getContent())
                            .build()
            );
        }

        // 이전 AI 응답
        ResponseOutputText outputText =
                ResponseOutputText.builder()
                        .text(chatMessage.getContent())
                        .annotations(Collections.emptyList())
                        .build();

        ResponseOutputMessage assistantMessage =
                ResponseOutputMessage.builder()
                        .id("msg_" + chatMessage.getId())
                        .status(ResponseOutputMessage.Status.COMPLETED)
                        .addContent(outputText)
                        .build();

        return ResponseInputItem.ofResponseOutputMessage(
                assistantMessage
        );
    }

    private List<ChatMessage> getRecentMessages(List<ChatMessage> messages) {

        if (messages.size() <= MAX_HISTORY_MESSAGES) {
            return messages;
        }

        return messages.subList(
                messages.size() - MAX_HISTORY_MESSAGES,
                messages.size()
        );
    }
}