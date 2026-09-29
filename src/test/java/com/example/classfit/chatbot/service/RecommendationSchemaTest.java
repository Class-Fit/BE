package com.example.classfit.chatbot.service;

import com.example.classfit.chatbot.dto.res.RecommendationDecision;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;

class RecommendationSchemaTest {
    @Test
    void sdkAcceptsRecommendationRecordAsStructuredOutput() {
        assertThatCode(() -> ResponseCreateParams.builder().model(ChatModel.GPT_5_2)
                .input("schema validation only").text(RecommendationDecision.class).build())
                .doesNotThrowAnyException();
    }
}
