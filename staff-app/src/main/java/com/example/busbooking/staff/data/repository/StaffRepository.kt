package com.example.busbooking.staff.data.repository

import com.example.busbooking.staff.data.api.StaffApiClient
import com.example.busbooking.staff.data.model.LoginRequest
import com.example.busbooking.staff.data.model.LoginResponse
import com.example.busbooking.staff.data.model.StaffHomeSummary
import com.example.busbooking.staff.data.model.StaffTripDetail
import com.example.busbooking.staff.data.model.StaffTripSummary
import com.example.busbooking.staff.data.model.TicketCheckInRequest
import com.example.busbooking.staff.data.model.TicketVerificationResult
import com.example.busbooking.staff.data.model.TicketVerifyRequest

class StaffRepository {
    suspend fun login(login: String, password: String): LoginResponse {
        return StaffApiClient.post("/api/staff/auth/login", LoginRequest(login, password))
    }

    suspend fun loadHome(staffId: Long): StaffHomeSummary {
        return StaffApiClient.get("/api/staff/home?staffId=$staffId")
    }

    suspend fun loadTrips(staffId: Long): List<StaffTripSummary> {
        return StaffApiClient.get("/api/staff/trips?staffId=$staffId")
    }

    suspend fun loadTripDetail(tripId: Long, staffId: Long): StaffTripDetail {
        return StaffApiClient.get("/api/staff/trips/$tripId?staffId=$staffId")
    }

    suspend fun verifyTicket(qrContent: String, staffId: Long): TicketVerificationResult {
        return StaffApiClient.post("/api/staff/tickets/verify", TicketVerifyRequest(qrContent, staffId))
    }

    suspend fun checkInTicket(ticketId: Long, staffId: Long): TicketVerificationResult {
        return StaffApiClient.post("/api/staff/tickets/$ticketId/check-in", TicketCheckInRequest(staffId))
    }
}
