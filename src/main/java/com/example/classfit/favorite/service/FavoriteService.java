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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;

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

    @Transactional
    public FavoriteStatusResponse removeFavorite(Long memberId, Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        favoriteRepository.deleteByMemberIdAndCourseId(memberId, courseId);
        return new FavoriteStatusResponse(courseId, false);
    }

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
