package com.example.classfit.chatbot.exception;

import com.example.classfit.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatbotErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "CHATBOT404_1", "존재하지 않는 회원입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
