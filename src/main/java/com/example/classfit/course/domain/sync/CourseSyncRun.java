package com.example.classfit.course.domain.sync;

import com.example.classfit.course.dto.SyncResultCount;
import com.example.classfit.course.service.CourseSyncResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "course_sync_runs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSyncRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private SyncTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncRunStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    private int facilityInserted;
    private int facilityUpdated;
    private int facilityUnchanged;
    private int facilitySkipped;
    private int facilityFailed;
    private int courseInserted;
    private int courseUpdated;
    private int courseUnchanged;
    private int courseSkipped;
    private int courseFailed;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    private CourseSyncRun(SyncTriggerType triggerType) {
        this.triggerType = triggerType;
        this.status = SyncRunStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public static CourseSyncRun start(SyncTriggerType triggerType) {
        return new CourseSyncRun(triggerType);
    }

    public void complete(CourseSyncResult result) {
        applyCounts(result.facilities(), result.courses());
        this.finishedAt = LocalDateTime.now();
        if (result.hasFailures()) {
            this.status = SyncRunStatus.FAILED;
            this.errorCode = "ITEM_PROCESSING_FAILED";
            this.errorMessage = "일부 시설 또는 강좌를 저장하지 못했습니다.";
            return;
        }
        this.status = SyncRunStatus.SUCCESS;
    }

    public void failUnexpectedly() {
        this.status = SyncRunStatus.FAILED;
        this.finishedAt = LocalDateTime.now();
        this.errorCode = "SYNC_FAILED";
        this.errorMessage = "공공데이터 동기화 중 오류가 발생했습니다.";
    }

    public SyncResultCount facilityCounts() {
        return new SyncResultCount(
                facilityInserted, facilityUpdated, facilityUnchanged, facilitySkipped, facilityFailed
        );
    }

    public SyncResultCount courseCounts() {
        return new SyncResultCount(
                courseInserted, courseUpdated, courseUnchanged, courseSkipped, courseFailed
        );
    }

    private void applyCounts(SyncResultCount facilities, SyncResultCount courses) {
        this.facilityInserted = facilities.inserted();
        this.facilityUpdated = facilities.updated();
        this.facilityUnchanged = facilities.unchanged();
        this.facilitySkipped = facilities.skipped();
        this.facilityFailed = facilities.failed();
        this.courseInserted = courses.inserted();
        this.courseUpdated = courses.updated();
        this.courseUnchanged = courses.unchanged();
        this.courseSkipped = courses.skipped();
        this.courseFailed = courses.failed();
    }
}
