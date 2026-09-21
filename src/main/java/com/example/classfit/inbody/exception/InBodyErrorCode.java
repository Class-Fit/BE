package com.example.classfit.inbody.exception;

import com.example.classfit.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InBodyErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "INBODY_MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    INBODY_NOT_FOUND(HttpStatus.NOT_FOUND, "INBODY_NOT_FOUND", "등록된 인바디 정보가 없습니다."),
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "INBODY_IMAGE_REQUIRED", "인바디 이미지를 첨부해주세요."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "INBODY_INVALID_IMAGE_TYPE", "이미지 파일만 업로드할 수 있습니다."),
    IMAGE_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INBODY_IMAGE_ANALYSIS_FAILED", "인바디 이미지 분석에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}