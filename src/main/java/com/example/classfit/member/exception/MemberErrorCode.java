package com.example.classfit.member.exception;

import com.example.classfit.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 회원 조회 실패를 HTTP 상태와 공통 응답 코드로 표현한다. */
@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    /** enum 이름을 클라이언트가 구분할 수 있는 오류 코드로 사용한다. */
    @Override
    public String getCode() {
        return name();
    }
}
