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

/**
 * ViewModel cho SeatSelectionFragment.
 * Quản lý danh sách ghế, ghế đang chọn, và kết quả đặt vé.
 */
class SeatSelectionViewModel(
    private val seatRepository: SeatRepository,
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _seats = MutableLiveData<List<Seat>>(emptyList())
    val seats: LiveData<List<Seat>> = _seats

    private val _selectedSeat = MutableLiveData<Seat?>(null)
    val selectedSeat: LiveData<Seat?> = _selectedSeat

    // null = chưa đặt, Long = ticketId sau khi đặt thành công
    private val _bookingResult = MutableLiveData<Long?>(null)
    val bookingResult: LiveData<Long?> = _bookingResult

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadSeats(tripId: Long) {
        viewModelScope.launch {
            when (val result = seatRepository.getSeatsByTripId(tripId)) {
                is Result.Success -> _seats.value = result.data
                is Result.Error   -> _error.value = result.message
                else              -> _error.value = "Không thể tải danh sách ghế"
            }
        }
    }

    fun selectSeat(seat: Seat) {
        _selectedSeat.value = seat
    }

    fun bookSeat(tripId: Long) {
        val seat = _selectedSeat.value ?: run {
            _error.value = "Vui lòng chọn ghế trước khi đặt vé"
            return
        }

        val userId = SessionManager.getCurrentUser()?.id ?: run {
            _error.value = "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại"
            return
        }

        viewModelScope.launch {
            when (val result = ticketRepository.bookTicket(userId, tripId, seat.id)) {
                is BookingResult.Success      -> _bookingResult.value = result.ticketId
                is BookingResult.AlreadyBooked -> _error.value = "Ghế vừa được người khác đặt, vui lòng chọn ghế khác"
                is BookingResult.InvalidSeat  -> _error.value = "Ghế không hợp lệ"
                is BookingResult.Failure      -> _error.value = "Đặt vé thất bại, vui lòng thử lại"
            }
        }
    }
}