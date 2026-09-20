package com.example.classfit.chatbot.domain;

import com.example.classfit.common.BaseEntity;
import com.example.classfit.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "conversation")
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(length = 100)
    private String title;

    public void updateTitle(String title) {
        this.title = title;
    }
}
