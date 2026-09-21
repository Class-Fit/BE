package com.example.classfit.common;

import org.springframework.data.domain.Page;

import java.util.List;

/** Spring Data 구현을 노출하지 않고 클라이언트에 필요한 페이징 정보만 제공한다. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalCount,
        int totalPages,
        boolean first,
        boolean last
) {
    /** 0부터 시작하는 페이지 번호와 조회 결과를 API 응답 모델로 변환한다. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
