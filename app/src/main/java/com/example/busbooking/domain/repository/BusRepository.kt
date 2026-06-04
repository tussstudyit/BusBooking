package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.relations.BusWithSeats
import com.example.busbooking.domain.models.Result

class BusRepository {
    suspend fun createBus(busName: String, totalSeats: Int, licensePlate: String): Result<Long> = Result.Error(UnsupportedOperationException(), "Quáº£n trá»‹ xe thá»±c hiá»‡n trÃªn web admin")
    suspend fun getBusById(id: Long): Result<Bus> = Result.Error(UnsupportedOperationException(), "KhÃ´ng há»— trá»£ táº£i xe riÃªng láº»")
    suspend fun getBusWithSeats(busId: Long): Result<BusWithSeats> = Result.Error(UnsupportedOperationException(), "KhÃ´ng há»— trá»£ táº£i xe riÃªng láº»")
    suspend fun getAllBuses(): Result<List<Bus>> = Result.Error(UnsupportedOperationException(), "Quáº£n trá»‹ xe thá»±c hiá»‡n trÃªn web admin")
    suspend fun searchBuses(query: String): Result<List<Bus>> = Result.Error(UnsupportedOperationException(), "Quáº£n trá»‹ xe thá»±c hiá»‡n trÃªn web admin")
    suspend fun deactivateBus(busId: Long): Result<Unit> = Result.Error(UnsupportedOperationException(), "Quáº£n trá»‹ xe thá»±c hiá»‡n trÃªn web admin")
    suspend fun updateBus(busId: Long, busName: String, totalSeats: Int, licensePlate: String): Result<Unit> = Result.Error(UnsupportedOperationException(), "Quáº£n trá»‹ xe thá»±c hiá»‡n trÃªn web admin")
}

