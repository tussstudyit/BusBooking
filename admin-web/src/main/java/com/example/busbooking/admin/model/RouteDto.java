package com.example.busbooking.admin.model;

public record RouteDto(
        String documentId,
        Long id,
        String originId,
        String destinationId,
        String origin,
        String destination,
        Integer distance,
        Long suggestedPrice,
        Long durationMs,
        Boolean isActive,
        Long createdAt
) {
}
