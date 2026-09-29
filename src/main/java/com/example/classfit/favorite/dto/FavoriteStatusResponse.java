package com.example.classfit.favorite.dto;

/**
 * 찜 등록 또는 취소 처리 결과를 전달한다. 이후 동시 요청에 의한 상태 변경은 포함하지 않는다.
 *
 * @param courseId 처리 대상 강좌의 내부 식별자
 * @param favorited 등록 성공이면 true, 취소 성공이면 false
 */
public record FavoriteStatusResponse(
        Long courseId,
        boolean favorited
) {
}
