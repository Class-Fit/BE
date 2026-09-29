package com.example.classfit.favorite.service;

import com.example.classfit.common.exception.BusinessException;
import com.example.classfit.common.exception.CommonErrorCode;
import com.example.classfit.course.dto.CourseSearchResponse;
import com.example.classfit.course.repository.CourseRepository;
import com.example.classfit.favorite.domain.Favorite;
import com.example.classfit.favorite.dto.FavoriteStatusResponse;
import com.example.classfit.favorite.repository.FavoriteRepository;
import com.example.classfit.member.exception.MemberErrorCode;
import com.example.classfit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 회원별 찜 등록, 취소 및 조회에 필요한 존재 여부와 중복 처리 규칙을 적용한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;

    /**
     * 회원과 강좌의 존재 여부를 확인하고 찜을 등록한다.
     * 같은 회원과 강좌의 순차 및 동시 중복 등록은 한 행만 저장하고 성공으로 처리한다.
     *
     * @param memberId 인증된 회원의 내부 식별자
     * @param courseId 찜 대상 강좌의 내부 식별자
     * @return 등록된 찜 상태
     * @throws BusinessException 회원 또는 강좌가 존재하지 않을 경우
     */
    @Transactional
    public FavoriteStatusResponse addFavorite(Long memberId, Long courseId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        // Native inserts bypass JPA auditing callbacks, so supply both audit timestamps.
        favoriteRepository.insertIfAbsent(memberId, courseId, LocalDateTime.now());
        return new FavoriteStatusResponse(courseId, true);
    }

    /**
     * 회원의 강좌 찜을 취소한다. 강좌가 존재하면 찜이 없어도 취소 성공으로 처리한다.
     *
     * @param memberId 인증된 회원의 내부 식별자
     * @param courseId 찜을 취소할 강좌의 내부 식별자
     * @return 취소된 찜 상태
     * @throws BusinessException 강좌가 존재하지 않을 경우
     */
    @Transactional
    public FavoriteStatusResponse removeFavorite(Long memberId, Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        favoriteRepository.deleteByMemberIdAndCourseId(memberId, courseId);
        return new FavoriteStatusResponse(courseId, false);
    }

    /**
     * 회원의 찜을 식별자 내림차순으로 조회하고 강좌 및 시설 정보를 응답으로 변환한다.
     *
     * @param memberId 인증된 회원의 내부 식별자
     * @return 찜 강좌 목록이며, 찜이 없으면 빈 목록
     * @throws BusinessException 회원이 존재하지 않을 경우
     */
    public List<CourseSearchResponse> getFavorites(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
        }

        return favoriteRepository.findAllByMemberIdOrderByIdDesc(memberId).stream()
                .map(Favorite::getCourse)
                .map(CourseSearchResponse::from)
                .toList();
    }
}
