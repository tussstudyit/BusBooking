package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.BusRepository
import com.example.busbooking.domain.repository.SeatRepository
import kotlinx.coroutines.launch

sealed class BusState {
    object Idle    : BusState()
    object Loading : BusState()
    data class BusesLoaded(val buses: List<Bus>)       : BusState()
    data class SeatsLoaded(val seats: List<Seat>)      : BusState()
    data class ActionSuccess(val message: String)      : BusState()
    data class Error(val message: String)              : BusState()
}

/**
 * ViewModel cho BusListAdminFragment, BusFormAdminFragment, SeatManagementFragment.
 */
class BusViewModel(
    private val busRepository: BusRepository,
    private val seatRepository: SeatRepository
) : ViewModel() {

    private val _busState = MutableLiveData<BusState>(BusState.Idle)
    val busState: LiveData<BusState> = _busState

    // ── Bus ──────────────────────────────────────────────────────────────────

    fun loadAllBuses() {
        _busState.value = BusState.Loading
        viewModelScope.launch {
            when (val result = busRepository.getAllBuses()) {
                is Result.Success -> _busState.value = BusState.BusesLoaded(result.data)
                is Result.Error   -> _busState.value = BusState.Error(result.message)
                is Result.Loading -> { }
            }
        }
    }

    fun createBus(busName: String, totalSeats: Int, licensePlate: String) {
        if (busName.isBlank() || licensePlate.isBlank() || totalSeats <= 0) {
            _busState.value = BusState.Error("Vui lòng điền đầy đủ thông tin hợp lệ")
            return
        }
        _busState.value = BusState.Loading
        viewModelScope.launch {
            when (val result = busRepository.createBus(busName, totalSeats, licensePlate)) {
                is Result.Success -> _busState.value = BusState.ActionSuccess("Đã tạo xe thành công")
                is Result.Error   -> _busState.value = BusState.Error(result.message)
                is Result.Loading -> { }
            }
        }
    }

    fun updateBus(busId: Long, busName: String, totalSeats: Int, licensePlate: String) {
        if (busName.isBlank() || licensePlate.isBlank() || totalSeats <= 0) {
            _busState.value = BusState.Error("Vui lòng điền đầy đủ thông tin hợp lệ")
            return
        }

        _busState.value = BusState.Loading

        viewModelScope.launch {
            when (val result = busRepository.updateBus(busId, busName, totalSeats, licensePlate)) {
                is Result.Success -> _busState.value =
                    BusState.ActionSuccess("Đã cập nhật xe")

                is Result.Error -> _busState.value =
                    BusState.Error(result.message)

                is Result.Loading -> { }
            }
        }
    }

    fun loadBusById(busId: Long) {
        _busState.value = BusState.Loading

        viewModelScope.launch {
            when (val result = busRepository.getBusById(busId)) {

                is Result.Success -> _busState.value =
                    BusState.BusesLoaded(listOf(result.data))

                is Result.Error -> _busState.value =
                    BusState.Error(result.message)

                is Result.Loading -> { }
            }
        }
    }

    // ── Seat ─────────────────────────────────────────────────────────────────

    fun loadSeatsByBus(busId: Long) {
        _busState.value = BusState.Loading

        viewModelScope.launch {
            when (val result = seatRepository.getSeatsByBusId(busId)) {

                is Result.Success -> _busState.value =
                    BusState.SeatsLoaded(result.data)

                is Result.Error -> _busState.value =
                    BusState.Error(result.message)

                is Result.Loading -> { }
            }
        }
    }

    /**
     * Tạo ghế tự động theo số lượng, đặt tên A1..An theo layout 4 cột.
     * Ví dụ 40 ghế → A1-A4, B1-B4, ... J1-J4.
     */
    fun generateSeats(busId: Long, totalSeats: Int) {

        if (totalSeats <= 0 || totalSeats > 60) {
            _busState.value = BusState.Error("Số ghế phải từ 1 đến 60")
            return
        }

        _busState.value = BusState.Loading

        viewModelScope.launch {
            when (val result = seatRepository.generateSeats(busId, totalSeats)) {

                is Result.Success -> {
                    _busState.value =
                        BusState.ActionSuccess("Đã tạo $totalSeats ghế")

                    loadSeatsByBus(busId)
                }

                is Result.Error -> _busState.value =
                    BusState.Error(result.message)

                is Result.Loading -> { }
            }
        }
    }

    fun resetState() {
        _busState.value = BusState.Idle
    }
}