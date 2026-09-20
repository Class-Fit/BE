package com.example.classfit.chatbot.exception;

import com.example.classfit.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatbotErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHATBOT404_1", "존재하지 않는 회원입니다."),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND,"CHATBOT404_2","존재하지 않는 채팅방입니다."),
    CONVERSATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHATBOT403_1", "해당 채팅방에 접근할 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
