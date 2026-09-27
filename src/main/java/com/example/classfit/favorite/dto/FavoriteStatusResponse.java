package com.example.classfit.favorite.dto;

public record FavoriteStatusResponse(
        Long courseId,
        boolean favorited
) {
}
