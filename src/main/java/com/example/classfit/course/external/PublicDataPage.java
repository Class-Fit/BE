package com.example.classfit.course.external;

import java.util.List;

public record PublicDataPage<T>(
        List<T> items,
        int totalCount
) {
}
