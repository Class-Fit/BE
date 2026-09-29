package com.example.classfit.course.repository;

import com.example.classfit.course.domain.sync.CourseSyncRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSyncRunRepository extends JpaRepository<CourseSyncRun, Long> {
}
