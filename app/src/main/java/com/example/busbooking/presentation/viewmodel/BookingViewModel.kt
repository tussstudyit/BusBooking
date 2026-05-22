package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.domain.models.BookingResult
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.FirebaseTicketRepository
import com.example.busbooking.domain.repository.SeatRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.presentation.ui.state.BookingState
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.launch

class BookingViewModel(
    private val seatRepository: SeatRepository,
    private val ticketRepository: TicketRepository,
    private val firebaseTicketRepository: FirebaseTicketRepository = FirebaseTicketRepository()
) : ViewModel() {

    private val _bookingState = MutableLiveData<BookingState>(BookingState.Idle)
    val bookingState: LiveData<BookingState> = _bookingState

    // Ghế đang được chọn (highlight trên UI)
    private val _selectedSeat = MutableLiveData<Seat?>(null)
    val selectedSeat: LiveData<Seat?> = _selectedSeat

    /**
     * Load danh sách ghế của 1 chuyến xe
     * Phân biệt ghế trống / đã đặt qua Seat.isBooked
     */
    fun loadSeats(tripId: Long) {
        _bookingState.value = BookingState.Loading

        viewModelScope.launch {
            when (val result = seatRepository.getSeatsByTripId(tripId)) {
                is Result.Success -> {
                    _bookingState.value = BookingState.SeatsLoaded(result.data)
                }

                is Result.Error -> {
                    _bookingState.value = BookingState.Error(result.message)
                }

                else -> {
                    _bookingState.value = BookingState.Error("Không thể tải danh sách ghế")
                }
            }
        }
    }

    /**
     * Người dùng chọn ghế trên sơ đồ
     * Không thay đổi state, chỉ cập nhật selectedSeat
     */
    fun selectSeat(seat: Seat) {
        _selectedSeat.value = seat
    }

    fun clearSelectedSeat() {
        _selectedSeat.value = null
    }

    /**
     * Đặt vé - gọi sau khi người dùng xác nhận
     * SessionManager cung cấp userId hiện tại
     */
    fun bookTicket(tripId: Long) {
        val seat = _selectedSeat.value
        if (seat == null) {
            _bookingState.value = BookingState.Error("Vui lòng chọn ghế trước khi đặt vé")
            return
        }

        val userId = SessionManager.getCurrentUser()?.id
        if (userId == null) {
            _bookingState.value =
                BookingState.Error("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại")
            return
        }

        _bookingState.value = BookingState.Loading

        viewModelScope.launch {

            when (val result = ticketRepository.bookTicket(userId, tripId, seat.id)) {

                is BookingResult.Success -> {
                    _bookingState.value =
                        BookingState.BookingSuccess(result.ticketId)
                }

                is BookingResult.AlreadyBooked -> {
                    _bookingState.value =
                        BookingState.Error("Ghế này vừa được người khác đặt, vui lòng chọn ghế khác")
                }

                is BookingResult.InvalidSeat -> {
                    _bookingState.value =
                        BookingState.Error("Ghế không hợp lệ")
                }

                is BookingResult.InvalidTrip -> {
                    _bookingState.value =
                        BookingState.Error("Chuyến đi không hợp lệ")
                }

                is BookingResult.Failure -> {
                    _bookingState.value =
                        BookingState.Error("Đặt vé thất bại, vui lòng thử lại")
                }
            }
        }
    }

        /**
         * Load chi tiết vé sau khi đặt thành công
         * Dùng trong BookingConfirmationFragment
         */
        fun loadTicket(ticketId: Long) {
            _bookingState.value = BookingState.Loading

            viewModelScope.launch {
                when (val result = firebaseTicketRepository.getTicketById(ticketId)) {
                    is Result.Success -> {
                        _bookingState.value = BookingState.TicketLoaded(result.data)
                    }

                    is Result.Error -> {
                        loadLocalTicket(ticketId, result.message)
                    }

                    else -> {
                        _bookingState.value = BookingState.Error("Không tìm thấy thông tin vé")
                    }
                }
            }
        }

        private suspend fun loadLocalTicket(ticketId: Long, fallbackError: String) {
            when (val result = ticketRepository.getTicketById(ticketId)) {
                is Result.Success -> {
                    _bookingState.value = BookingState.TicketLoaded(result.data)
                }
                is Result.Error -> {
                    _bookingState.value = BookingState.Error(fallbackError)
                }
                else -> {
                    _bookingState.value = BookingState.Error(fallbackError)
                }
            }
        }

        /**
         * Hủy vé
         * refundAmount = 0.0 mặc định (logic tính hoàn tiền tùy business rule)
         */
        fun cancelTicket(ticketId: Long, reason: String = "Người dùng hủy") {
            val userId = SessionManager.getCurrentUser()?.id
            if (userId == null) {
                _bookingState.value = BookingState.Error("Phiên đăng nhập hết hạn")
                return
            }

            _bookingState.value = BookingState.Loading

            viewModelScope.launch {
                when (val result = ticketRepository.cancelTicket(
                    ticketId = ticketId,
                    userId = userId,
                    reason = reason,
                    refundAmount = 0.0
                )) {
                    is Result.Success -> {
                        _bookingState.value = BookingState.CancelSuccess()
                    }

                    is Result.Error -> {
                        _bookingState.value = BookingState.Error(result.message)
                    }

                    else -> {
                        _bookingState.value = BookingState.Error("Hủy vé thất bại")
                    }
                }
            }
        }

        fun resetState() {
            _bookingState.value = BookingState.Idle
        }
    }
