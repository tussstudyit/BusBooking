package com.example.busbooking.admin.model;

public record RouteCatalogEntry(
        String originId,
        String originName,
        String destinationId,
        String destinationName,
        int distanceKm,
        long durationMs,
        long price
) {
}
