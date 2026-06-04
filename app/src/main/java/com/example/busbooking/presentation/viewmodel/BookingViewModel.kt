package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.domain.models.BookingResult
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.ApiTicketRepository
import com.example.busbooking.domain.repository.SeatRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.presentation.ui.state.BookingState
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.launch

class BookingViewModel(
    private val seatRepository: SeatRepository,
    private val ticketRepository: TicketRepository,
    private val ApiTicketRepository: ApiTicketRepository = ApiTicketRepository()
) : ViewModel() {

    private val _bookingState = MutableLiveData<BookingState>(BookingState.Idle)
    val bookingState: LiveData<BookingState> = _bookingState

    // Gháº¿ Ä‘ang Ä‘Æ°á»£c chá»n (highlight trÃªn UI)
    private val _selectedSeat = MutableLiveData<Seat?>(null)
    val selectedSeat: LiveData<Seat?> = _selectedSeat

    /**
     * Load danh sÃ¡ch gháº¿ cá»§a 1 chuyáº¿n xe
     * PhÃ¢n biá»‡t gháº¿ trá»‘ng / Ä‘Ã£ Ä‘áº·t qua Seat.isBooked
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
                    _bookingState.value = BookingState.Error("KhÃ´ng thá»ƒ táº£i danh sÃ¡ch gháº¿")
                }
            }
        }
    }

    /**
     * NgÆ°á»i dÃ¹ng chá»n gháº¿ trÃªn sÆ¡ Ä‘á»“
     * KhÃ´ng thay Ä‘á»•i state, chá»‰ cáº­p nháº­t selectedSeat
     */
    fun selectSeat(seat: Seat) {
        _selectedSeat.value = seat
    }

    fun clearSelectedSeat() {
        _selectedSeat.value = null
    }

    /**
     * Äáº·t vÃ© - gá»i sau khi ngÆ°á»i dÃ¹ng xÃ¡c nháº­n
     * SessionManager cung cáº¥p userId hiá»‡n táº¡i
     */
    fun bookTicket(tripId: Long) {
        val seat = _selectedSeat.value
        if (seat == null) {
            _bookingState.value = BookingState.Error("Vui lÃ²ng chá»n gháº¿ trÆ°á»›c khi Ä‘áº·t vÃ©")
            return
        }

        val userId = SessionManager.getCurrentUser()?.id
        if (userId == null) {
            _bookingState.value =
                BookingState.Error("PhiÃªn Ä‘Äƒng nháº­p háº¿t háº¡n, vui lÃ²ng Ä‘Äƒng nháº­p láº¡i")
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
                        BookingState.Error("Gháº¿ nÃ y vá»«a Ä‘Æ°á»£c ngÆ°á»i khÃ¡c Ä‘áº·t, vui lÃ²ng chá»n gháº¿ khÃ¡c")
                }

                is BookingResult.InvalidSeat -> {
                    _bookingState.value =
                        BookingState.Error("Gháº¿ khÃ´ng há»£p lá»‡")
                }

                is BookingResult.InvalidTrip -> {
                    _bookingState.value =
                        BookingState.Error("Chuyáº¿n Ä‘i khÃ´ng há»£p lá»‡")
                }

                is BookingResult.Failure -> {
                    _bookingState.value =
                        BookingState.Error("Äáº·t vÃ© tháº¥t báº¡i, vui lÃ²ng thá»­ láº¡i")
                }
            }
        }
    }

        /**
         * Load chi tiáº¿t vÃ© sau khi Ä‘áº·t thÃ nh cÃ´ng
         * DÃ¹ng trong BookingConfirmationFragment
         */
        fun loadTicket(ticketId: Long) {
            _bookingState.value = BookingState.Loading

            viewModelScope.launch {
                when (val result = ApiTicketRepository.getTicketById(ticketId)) {
                    is Result.Success -> {
                        _bookingState.value = BookingState.TicketLoaded(result.data)
                    }

                    is Result.Error -> {
                        loadLocalTicket(ticketId, result.message)
                    }

                    else -> {
                        _bookingState.value = BookingState.Error("KhÃ´ng tÃ¬m tháº¥y thÃ´ng tin vÃ©")
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
         * Há»§y vÃ©
         * refundAmount = 0.0 máº·c Ä‘á»‹nh (logic tÃ­nh hoÃ n tiá»n tÃ¹y business rule)
         */
        fun cancelTicket(ticketId: Long, reason: String = "NgÆ°á»i dÃ¹ng há»§y") {
            val userId = SessionManager.getCurrentUser()?.id
            if (userId == null) {
                _bookingState.value = BookingState.Error("PhiÃªn Ä‘Äƒng nháº­p háº¿t háº¡n")
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
                        _bookingState.value = BookingState.Error("Há»§y vÃ© tháº¥t báº¡i")
                    }
                }
            }
        }

        fun resetState() {
            _bookingState.value = BookingState.Idle
        }
    }

