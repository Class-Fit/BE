package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.domain.ChatMessage;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final OpenAIClient openAIClient;

    @Override
    public String generateResponse(List<ChatMessage> messages) {

        String conversationHistory = messages.stream()
                .map(message ->
                        message.getRole().name()
                                + ": "
                                + message.getContent()
                )
                .reduce("", (a, b) -> a + b + "\n");

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(ChatModel.GPT_5_2)
                .input(conversationHistory)
                .build();

        Response response =
                openAIClient.responses().create(params);

        return response.output().stream()
                .flatMap(output -> output.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(text -> text.text())
                .findFirst()
                .orElse("응답을 생성하지 못했습니다.");
    }
}