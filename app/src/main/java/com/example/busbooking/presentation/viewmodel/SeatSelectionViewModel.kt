package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.domain.models.BookingResult
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.SeatRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.launch

class SeatSelectionViewModel(
    private val seatRepository: SeatRepository,
    private val ticketRepository: TicketRepository
) : ViewModel() {

    // =========================
    // Danh sách ghế
    // =========================

    private val _seats = MutableLiveData<List<Seat>>(emptyList())
    val seats: LiveData<List<Seat>> = _seats

    // =========================
    // Ghế đang chọn
    // =========================

    private val _selectedSeats = MutableLiveData<List<Seat>>(emptyList())
    val selectedSeats: LiveData<List<Seat>> = _selectedSeats

    // =========================
    // Tổng tiền
    // =========================

    private val _totalPrice = MutableLiveData<Double>(0.0)
    val totalPrice: LiveData<Double> = _totalPrice

    // =========================
    // Kết quả đặt vé
    // =========================

    private val _bookingResult = MutableLiveData<List<Long>?>(null)
    val bookingResult: LiveData<List<Long>?> = _bookingResult

    // =========================
    // Error
    // =========================

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // =========================
    // Giá vé 1 ghế
    // =========================

    private var tripPrice: Double = 0.0

    // =========================
    // Load ghế
    // =========================

    fun loadSeats(tripId: Long) {
        viewModelScope.launch {

            when (val result = seatRepository.getSeatsByTripId(tripId)) {

                is Result.Success -> {
                    _seats.value = result.data
                }

                is Result.Error -> {
                    _error.value = result.message
                }

                else -> {
                    _error.value = "Không thể tải danh sách ghế"
                }
            }
        }
    }

    // =========================
    // Set giá vé
    // =========================

    fun setTripPrice(price: Double) {
        tripPrice = price
        recalcTotal()
    }

    // =========================
    // Chọn / bỏ chọn ghế
    // =========================

    fun toggleSeat(seat: Seat) {

        val currentSeats = _selectedSeats.value?.toMutableList()
            ?: mutableListOf()

        val alreadySelected = currentSeats.any { it.id == seat.id }

        if (alreadySelected) {

            // Bỏ chọn ghế
            currentSeats.removeAll { it.id == seat.id }

        } else {

            // Thêm ghế
            currentSeats.add(seat)
        }

        _selectedSeats.value = currentSeats

        recalcTotal()
    }

    // =========================
    // Check ghế đã chọn
    // =========================

    fun isSeatSelected(seat: Seat): Boolean {
        return _selectedSeats.value?.any { it.id == seat.id } == true
    }

    // =========================
    // Tính tổng tiền
    // =========================

    private fun recalcTotal() {

        val seatCount = _selectedSeats.value?.size ?: 0

        _totalPrice.value = seatCount * tripPrice
    }

    // =========================
    // Đặt vé
    // =========================

    fun bookSeats(tripId: Long) {

        val selectedSeats = _selectedSeats.value

        // Chưa chọn ghế
        if (selectedSeats.isNullOrEmpty()) {
            _error.value = "Vui lòng chọn ghế trước khi đặt vé"
            return
        }

        // Kiểm tra đăng nhập
        val userId = SessionManager.getCurrentUser()?.id ?: run {
            _error.value = "Vui lòng đăng nhập lại"
            return
        }

        viewModelScope.launch {

            val ticketIds = mutableListOf<Long>()

            for (seat in selectedSeats) {

                when (
                    val result = ticketRepository.bookTicket(
                        userId = userId,
                        tripId = tripId,
                        seatId = seat.id
                    )
                ) {

                    is BookingResult.Success -> {

                        ticketIds.add(result.ticketId)
                    }

                    is BookingResult.AlreadyBooked -> {

                        _error.value =
                            "Ghế ${seat.seatNumber} đã được đặt"

                        return@launch
                    }

                    is BookingResult.InvalidSeat -> {

                        _error.value =
                            "Ghế ${seat.seatNumber} không hợp lệ"

                        return@launch
                    }

                    is BookingResult.InvalidTrip -> {

                        _error.value =
                            "Chuyến xe không hợp lệ"

                        return@launch
                    }

                    is BookingResult.Failure -> {

                        _error.value =
                            "Đặt vé thất bại"

                        return@launch
                    }
                }
            }

            // Thành công
            _bookingResult.value = ticketIds

            // Clear ghế đã chọn
            _selectedSeats.value = emptyList()

            // Reset tổng tiền
            recalcTotal()

            // Reload danh sách ghế
            loadSeats(tripId)
        }
    }

    // =========================
    // Clear lỗi
    // =========================

    fun clearError() {
        _error.value = null
    }
}