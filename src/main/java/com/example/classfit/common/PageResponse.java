package com.example.classfit.common;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long totalCount,
        long totalPage,
        boolean first,
        boolean last
){

}
