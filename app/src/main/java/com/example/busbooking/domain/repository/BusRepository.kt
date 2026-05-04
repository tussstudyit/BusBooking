package com.example.busbooking.domain.repository

import com.example.busbooking.data.dao.BusDAO
import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.relations.BusWithSeats
import com.example.busbooking.domain.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BusRepository(private val busDAO: BusDAO) {

    /**
     * Create new bus
     */
    suspend fun createBus(
        busName: String,
        totalSeats: Int,
        licensePlate: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val bus = Bus(
                busName = busName,
                totalSeats = totalSeats,
                licensePlate = licensePlate
            )

            val id = busDAO.insertBus(bus)

            if (id > 0) {
                Result.Success(id)
            } else {
                Result.Error(Exception("Insert failed"), "Could not create bus")
            }

        } catch (e: Exception) {
            Result.Error(e, "Error creating bus: ${e.message}")
        }
    }

    /**
     * Get bus by ID (basic info only)
     */
    suspend fun getBusById(id: Long): Result<Bus> = withContext(Dispatchers.IO) {
        try {
            val bus = busDAO.getBusById(id)

            if (bus != null) {
                Result.Success(bus)
            } else {
                Result.Error(Exception("Not found"), "Bus not found")
            }

        } catch (e: Exception) {
            Result.Error(e, "Error fetching bus: ${e.message}")
        }
    }

    /**
     * Get bus with all its seats (for seat selection UI)
     */
    suspend fun getBusWithSeats(busId: Long): Result<BusWithSeats> =
        withContext(Dispatchers.IO) {
            try {
                val busWithSeats = busDAO.getBusWithSeats(busId)

                if (busWithSeats != null) {
                    Result.Success(busWithSeats)
                } else {
                    Result.Error(Exception("Not found"), "Bus not found")
                }

            } catch (e: Exception) {
                Result.Error(e, "Error fetching bus with seats: ${e.message}")
            }
        }

    /**
     * Get all active buses (Flow collection)
     */
    suspend fun getAllBuses(): Result<List<Bus>> = withContext(Dispatchers.IO) {
        try {
            var busList: List<Bus> = emptyList()
            busDAO.getAllActiveBuses().collect { buses ->
                busList = buses
            }
            Result.Success(busList)
        } catch (e: Exception) {
            Result.Error(e, "Error fetching buses: ${e.message}")
        }
    }

    /**
     * Search buses by name or license plate
     */
    suspend fun searchBuses(query: String): Result<List<Bus>> =
        withContext(Dispatchers.IO) {
            try {
                val buses = busDAO.searchBuses(query)
                Result.Success(buses)
            } catch (e: Exception) {
                Result.Error(e, "Error searching buses: ${e.message}")
            }
        }

    /**
     * Deactivate bus
     */
    suspend fun deactivateBus(busId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                busDAO.deactivateBus(busId)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, "Error deactivating bus: ${e.message}")
            }
        }
}