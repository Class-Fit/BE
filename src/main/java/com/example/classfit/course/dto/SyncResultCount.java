package com.example.classfit.course.dto;

import com.example.classfit.course.service.SyncItemStatus;

public record SyncResultCount(
        int inserted,
        int updated,
        int unchanged,
        int skipped,
        int failed
) {
    public static SyncResultCount empty() {
        return new SyncResultCount(0, 0, 0, 0, 0);
    }

    public SyncResultCount add(SyncItemStatus status) {
        return switch (status) {
            case INSERTED -> new SyncResultCount(inserted + 1, updated, unchanged, skipped, failed);
            case UPDATED -> new SyncResultCount(inserted, updated + 1, unchanged, skipped, failed);
            case UNCHANGED -> new SyncResultCount(inserted, updated, unchanged + 1, skipped, failed);
        };
    }

    public SyncResultCount skip() {
        return new SyncResultCount(inserted, updated, unchanged, skipped + 1, failed);
    }

    public SyncResultCount fail() {
        return new SyncResultCount(inserted, updated, unchanged, skipped, failed + 1);
    }
}
