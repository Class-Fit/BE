package com.example.classfit.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class OpenAiConfig {

    @Bean
    public OpenAIClient openAIClient(@Value("${OPENAI_API_KEY}") String apiKey) {
        return OpenAIOkHttpClient.builder().apiKey(apiKey).build();
    }
}
