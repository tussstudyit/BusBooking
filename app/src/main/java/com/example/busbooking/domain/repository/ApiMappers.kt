package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.data.entity.Ticket
import com.example.busbooking.data.entity.Trip
import com.example.busbooking.data.entity.User
import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.data.relations.TripWithRouteAndBus

internal fun UserApi.toEntity() = User(id, name, email, "", phone, role, isBlocked, createdAt)
internal fun RouteApi.toEntity() = Route(id, origin, destination, distance, isActive, createdAt)
internal fun BusApi.toEntity() = Bus(id, busName, totalSeats, licensePlate, seatLayoutJson, isActive, createdAt)
internal fun TripApi.toRelation() = TripWithRouteAndBus(
    trip = Trip(id, routeId, busId, departureTime, arrivalTime, price, tripDate, status, createdAt),
    route = route.toEntity(),
    bus = bus.toEntity()
)
internal fun SeatApi.toEntity() = Seat(id, busId, seatNumber, floor, rowIndex, columnIndex, isWindow, isAisle, seatType, createdAt)
internal fun TicketApi.toEntity() = Ticket(id, userId, tripId, seatId, bookingTime, status, cancellationReason, refundAmount, refundStatus, paymentId, qrContent, qrImageBase64, qrMimeType)
internal fun TicketDetailsApi.toRelation() = TicketDetails(ticket.toEntity(), user.toEntity(), tripWithRouteAndBus.toRelation(), seat.toEntity())

