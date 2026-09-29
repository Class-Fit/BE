package com.example.classfit.course.service;

import com.example.classfit.course.domain.sync.CourseSyncRun;
import com.example.classfit.course.domain.sync.SyncTriggerType;
import com.example.classfit.course.dto.CourseSyncResponse;
import com.example.classfit.course.repository.CourseSyncRunRepository;
import org.springframework.stereotype.Service;

@Service
public class CourseSyncCoordinator {

    private final CourseSyncService syncService;
    private final CourseSyncRunRepository runRepository;

    public CourseSyncCoordinator(
            CourseSyncService syncService,
            CourseSyncRunRepository runRepository
    ) {
        this.syncService = syncService;
        this.runRepository = runRepository;
    }

    public CourseSyncResponse sync(SyncTriggerType triggerType) {
        CourseSyncRun run = runRepository.save(CourseSyncRun.start(triggerType));
        try {
            run.complete(syncService.syncGangwonCourses());
            return CourseSyncResponse.from(runRepository.save(run));
        } catch (RuntimeException exception) {
            run.failUnexpectedly();
            runRepository.save(run);
            throw exception;
        }
    }
}
