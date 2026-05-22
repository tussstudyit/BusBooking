package com.example.busbooking.admin.model;

public record TripDto(
        String documentId,
        Long id,
        Long routeId,
        Long busId,
        String routeLabel,
        String busLicensePlate,
        Long departureTime,
        Long arrivalTime,
        Double price,
        Long tripDate,
        String status,
        String bookingStatus,
        Integer availableSeats,
        Integer totalSeats,
        Long createdAt
) {
}
