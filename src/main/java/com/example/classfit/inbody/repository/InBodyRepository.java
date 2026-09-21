package com.example.classfit.inbody.repository;

import com.example.classfit.inbody.domain.InBody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InBodyRepository extends JpaRepository<InBody, Long> {

    Optional<InBody> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);

    Page<InBody> findAllByMemberId(Long memberId, Pageable pageable);
}