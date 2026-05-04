package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.domain.repository.SeatRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.domain.repository.TripRepository
import com.example.busbooking.domain.models.BookingResult
import com.example.busbooking.presentation.ui.state.*
import kotlinx.coroutines.launch

/**
 * SeatSelectionViewModel - Seat selection for booking
 *
 * Flow:
 * 1. Load bus and available seats for trip
 * 2. User selects a seat
 * 3. Enable/disable "Book" button
 *
 * Validation:
 * - A seat must be selected before proceeding
 */
class SeatSelectionViewModel(
    private val seatRepository: SeatRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<SeatSelectionUIState>(SeatSelectionUIState.Loading)
    val uiState: LiveData<SeatSelectionUIState> = _uiState

    private val _navigationEvents = MutableLiveData<SeatSelectionEvent>()
    val navigationEvents: LiveData<SeatSelectionEvent> = _navigationEvents

    private var selectedSeatId: Long? = null
    private var selectedSeatNumber: String? = null

    fun loadSeatsForTrip(tripId: Long) {
        _uiState.value = SeatSelectionUIState.Loading

        viewModelScope.launch {
            val result = seatRepository.getFreeSeatsForTrip(tripId)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = SeatSelectionUIState.Loaded(result.data)
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = SeatSelectionUIState.Error(result.message)
                }
            }
        }
    }

    fun selectSeat(seatId: Long, seatNumber: String) {
        selectedSeatId = seatId
        selectedSeatNumber = seatNumber

        val currentState = _uiState.value
        if (currentState is SeatSelectionUIState.Loaded) {
            _uiState.value = currentState.copy(
                selectedSeatId = seatId,
                selectedSeatNumber = seatNumber
            )
        }
    }

    fun proceedToConfirmation(tripId: Long) {
        if (selectedSeatId == null) {
            // Show error - no seat selected
            return
        }
        _navigationEvents.value = SeatSelectionEvent.NavigateToConfirmation(tripId, selectedSeatId!!)
    }

    fun goBack() {
        _navigationEvents.value = SeatSelectionEvent.NavigateBack
    }
}

/**
 * BookingConfirmationViewModel - Booking confirmation and execution
 *
 * This is where the ATOMIC BOOKING happens
 *
 * Flow:
 * 1. Display trip, seat, and price information
 * 2. User clicks "Confirm Booking"
 * 3. Execute atomic booking transaction
 * 4. Handle BookingResult:
 *    - Success -> Navigate to ticket detail
 *    - AlreadyBooked -> Show error, go back to seat selection
 *    - InvalidSeat -> Show error
 *    - Failure -> Show generic error
 */
class BookingConfirmationViewModel(
    private val ticketRepository: TicketRepository,
    private val tripRepository: TripRepository,
    private val sessionManager: com.example.busbooking.utils.SessionManager
) : ViewModel() {

    private val _uiState = MutableLiveData<BookingConfirmationUIState>(BookingConfirmationUIState.Idle)
    val uiState: LiveData<BookingConfirmationUIState> = _uiState

    private val _navigationEvents = MutableLiveData<BookingConfirmationEvent>()
    val navigationEvents: LiveData<BookingConfirmationEvent> = _navigationEvents

    fun loadBookingDetails(tripId: Long, seatNumber: String) {
        _uiState.value = BookingConfirmationUIState.Loading

        viewModelScope.launch {
            val result = tripRepository.getTripById(tripId)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = BookingConfirmationUIState.Loaded(
                        trip = result.data,
                        seatNumber = seatNumber,
                        totalPrice = result.data.trip.price
                    )
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = BookingConfirmationUIState.Error(result.message)
                }
            }
        }
    }

    /**
     * ⚠️ CRITICAL: Perform atomic booking
     *
     * This calls TicketRepository.bookTicket() which is protected by:
     * - Room @Transaction
     * - OnConflictStrategy.ABORT
     * - Ensures only 1 booking per (trip, seat)
     */
    fun confirmBooking(tripId: Long, seatId: Long) {
        _uiState.value = BookingConfirmationUIState.Loading

        val userId = sessionManager.getUserId() ?: return

        viewModelScope.launch {
            val result = ticketRepository.bookTicket(userId, tripId, seatId)

            when (result) {
                is BookingResult.Success -> {
                    _uiState.value = BookingConfirmationUIState.BookingSuccess
                    _navigationEvents.value = BookingConfirmationEvent.NavigateToTicketDetail(result.ticketId)
                }
                is BookingResult.AlreadyBooked -> {
                    _uiState.value = BookingConfirmationUIState.SeatAlreadyBooked
                    _navigationEvents.value = BookingConfirmationEvent.NavigateBackToSearch
                }
                is BookingResult.InvalidSeat -> {
                    _uiState.value = BookingConfirmationUIState.BookingFailed("Invalid seat selection")
                }
                is BookingResult.InvalidTrip -> {
                    _uiState.value = BookingConfirmationUIState.BookingFailed("Invalid trip")
                }
                is BookingResult.Failure -> {
                    _uiState.value = BookingConfirmationUIState.BookingFailed(result.exception.message ?: "Booking failed")
                }
            }
        }
    }
}

/**
 * MyTicketsViewModel - View active bookings
 */
class MyTicketsViewModel(
    private val ticketRepository: TicketRepository,
    private val sessionManager: com.example.busbooking.utils.SessionManager
) : ViewModel() {

    private val _uiState = MutableLiveData<MyTicketsUIState>(MyTicketsUIState.Loading)
    val uiState: LiveData<MyTicketsUIState> = _uiState

    private val _navigationEvents = MutableLiveData<MyTicketsEvent>()
    val navigationEvents: LiveData<MyTicketsEvent> = _navigationEvents

    fun loadActiveTickets() {
        _uiState.value = MyTicketsUIState.Loading

        val userId = sessionManager.getUserId() ?: return

        viewModelScope.launch {
            val result = ticketRepository.getUserActiveTickets(userId)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    val tickets = result.data
                    _uiState.value = if (tickets.isEmpty()) {
                        MyTicketsUIState.Empty
                    } else {
                        MyTicketsUIState.Loaded(tickets)
                    }
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = MyTicketsUIState.Error(result.message)
                }
            }
        }
    }

    fun navigateToTicketDetail(ticketId: Long) {
        _navigationEvents.value = MyTicketsEvent.NavigateToDetail(ticketId)
    }
}

/**
 * TicketDetailViewModel - View single ticket details
 */
class TicketDetailViewModel(
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<TicketDetailUIState>(TicketDetailUIState.Loading)
    val uiState: LiveData<TicketDetailUIState> = _uiState

    private val _navigationEvents = MutableLiveData<TicketDetailEvent>()
    val navigationEvents: LiveData<TicketDetailEvent> = _navigationEvents

    fun loadTicket(ticketId: Long) {
        _uiState.value = TicketDetailUIState.Loading

        viewModelScope.launch {
            val result = ticketRepository.getTicketById(ticketId)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = TicketDetailUIState.Loaded(result.data)
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = TicketDetailUIState.Error(result.message)
                }
            }
        }
    }

    fun navigateToCancel(ticketId: Long) {
        _navigationEvents.value = TicketDetailEvent.NavigateToCancel(ticketId)
    }

    fun goBack() {
        _navigationEvents.value = TicketDetailEvent.NavigateBack
    }
}

/**
 * CancelTicketViewModel - Ticket cancellation with refund
 *
 * Validation Rules:
 * - Cancellation reason: non-empty, at least 10 characters
 * - Can only cancel CONFIRMED or PENDING tickets
 */
class CancelTicketViewModel(
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<CancelTicketUIState>(CancelTicketUIState.Idle)
    val uiState: LiveData<CancelTicketUIState> = _uiState

    private val _navigationEvents = MutableLiveData<CancelTicketEvent>()
    val navigationEvents: LiveData<CancelTicketEvent> = _navigationEvents

    fun loadTicketForCancellation(ticketId: Long) {
        _uiState.value = CancelTicketUIState.Loading

        viewModelScope.launch {
            val result = ticketRepository.getTicketById(ticketId)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    val ticket = result.data
                    // Calculate refund (for example: 80% of original price)
                    val refundAmount = ticket.trip.trip.price * 0.8
                    _uiState.value = CancelTicketUIState.Loaded(ticket, refundAmount)
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = CancelTicketUIState.Error(result.message)
                }
            }
        }
    }

    /**
     * Cancel ticket with reason
     *
     * Validation:
     * - Reason: not empty, reasonable length
     */
    fun cancelTicket(ticketId: Long, userId: Long, reason: String) {
        if (validateReason(reason) == null) {
            _uiState.value = CancelTicketUIState.Loading

            viewModelScope.launch {
                val refundAmount = (uiState.value as? CancelTicketUIState.Loaded)?.refundAmount ?: 0.0
                val result = ticketRepository.cancelTicket(ticketId, userId, reason, refundAmount)

                when (result) {
                    is com.example.busbooking.domain.models.Result.Success -> {
                        _uiState.value = CancelTicketUIState.CancellationSuccess
                        _navigationEvents.value = CancelTicketEvent.NavigateToHistory()
                    }
                    is com.example.busbooking.domain.models.Result.Error -> {
                        _uiState.value = CancelTicketUIState.Error(result.message)
                    }
                }
            }
        } else {
            _uiState.value = CancelTicketUIState.ValidationError(validateReason(reason)!!)
        }
    }

    private fun validateReason(reason: String): String? {
        return when {
            reason.isBlank() -> "Reason cannot be empty"
            reason.length < 10 -> "Reason must be at least 10 characters"
            else -> null
        }
    }
}

