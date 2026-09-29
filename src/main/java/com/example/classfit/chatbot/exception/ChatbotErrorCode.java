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
    CONVERSATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHATBOT403_1", "해당 채팅방에 접근할 권한이 없습니다."),
    CONVERSATION_BUSY(HttpStatus.CONFLICT, "CHATBOT409_BUSY", "이 대화의 이전 요청을 처리 중입니다. 응답을 받은 뒤 다시 시도해 주세요."),
    CONVERSATION_CONFLICT(HttpStatus.CONFLICT, "CHATBOT409_CONFLICT", "대화 상태가 변경되어 응답을 반영하지 못했습니다. 대화를 새로 조회해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
