package com.example.classfit.course.service;

public record SyncItemResult<T>(
        T entity,
        SyncItemStatus status
) {
}
