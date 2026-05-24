package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.domain.models.SeatDisplay
import com.example.busbooking.domain.models.SeatReservationResult
import com.example.busbooking.domain.models.SeatStatus
import com.example.busbooking.domain.repository.FirebaseSeatRepository
import com.example.busbooking.domain.repository.SeatReservationSeat
import com.example.busbooking.domain.repository.SeatReservationSegment
import com.example.busbooking.domain.repository.VnpayRepository
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.launch

data class BookingCheckout(
    val ticketIds: List<Long>,
    val seatNumbers: List<String>,
    val totalPrice: Double,
    val paymentId: String,
    val paymentUrl: String?,
    val qrContent: String?,
    val qrImageBase64: String?,
    val qrMimeType: String?,
    val paymentExpiresAt: Long?,
    val outboundSeatNumbers: List<String> = emptyList(),
    val returnSeatNumbers: List<String> = emptyList(),
    val isRoundTrip: Boolean = false,
    val paymentError: String? = null
)

private data class PendingCheckout(
    val ticketIds: List<Long>,
    val seatNumbers: List<String>,
    val totalPrice: Double,
    val paymentId: String,
    val outboundSeatNumbers: List<String> = emptyList(),
    val returnSeatNumbers: List<String> = emptyList(),
    val isRoundTrip: Boolean = false
)

class SeatSelectionViewModel(
    private val seatRepository: FirebaseSeatRepository,
    private val vnpayRepository: VnpayRepository = VnpayRepository()
) : ViewModel() {
    private companion object {
        private const val TAG = "VNPAY_FLOW"
    }

    private val _seats = MutableLiveData<List<SeatDisplay>>()
    val seats: LiveData<List<SeatDisplay>> = _seats

    private val _selectedSeats = MutableLiveData<List<Seat>>(emptyList())
    val selectedSeats: LiveData<List<Seat>> = _selectedSeats

    private val _totalPrice = MutableLiveData(0.0)
    val totalPrice: LiveData<Double> = _totalPrice

    private val _bookingResult = MutableLiveData<List<Long>?>(null)
    val bookingResult: LiveData<List<Long>?> = _bookingResult

    private val _paymentUrl = MutableLiveData<String?>(null)
    val paymentUrl: LiveData<String?> = _paymentUrl

    private val _checkout = MutableLiveData<BookingCheckout?>(null)
    val checkout: LiveData<BookingCheckout?> = _checkout

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private var tripPrice: Double = 0.0
    private var pendingCheckout: PendingCheckout? = null

    fun loadSeats(tripId: Long) {
        viewModelScope.launch {
            try {
                _seats.value = seatRepository.getSeatsForTrip(tripId)
                removeUnavailableSelections()
            } catch (e: Exception) {
                _error.value = e.message ?: "Kh\u00f4ng th\u1ec3 t\u1ea3i danh s\u00e1ch gh\u1ebf"
            }
        }
    }

    fun setTripPrice(price: Double) {
        tripPrice = price
        recalcTotal()
    }

    fun toggleSeat(seat: Seat) {
        val status = _seats.value
            ?.firstOrNull { it.seat.id == seat.id }
            ?.status
            ?: SeatStatus.AVAILABLE

        if (status != SeatStatus.AVAILABLE) {
            _error.value = "Gh\u1ebf ${seat.seatNumber} kh\u00f4ng c\u00f2n tr\u1ed1ng"
            return
        }

        val currentSeats = _selectedSeats.value?.toMutableList() ?: mutableListOf()
        val alreadySelected = currentSeats.any { it.id == seat.id }
        if (alreadySelected) {
            currentSeats.removeAll { it.id == seat.id }
        } else {
            currentSeats.add(seat)
        }
        _selectedSeats.value = currentSeats
        recalcTotal()
    }

    fun bookSeats(tripId: Long) {
        val selectedSeats = _selectedSeats.value.orEmpty()
        Log.d(TAG, "bookSeats called tripId=$tripId selected=${selectedSeats.map { it.seatNumber }} price=$tripPrice")
        if (_isProcessing.value == true) {
            Log.d(TAG, "bookSeats ignored: payment creation is already running")
            return
        }

        pendingCheckout?.let { pending ->
            createPaymentPayload(pending)
            return
        }

        if (selectedSeats.isEmpty()) {
            Log.d(TAG, "bookSeats blocked: no selected seats")
            _error.value = "Vui l\u00f2ng ch\u1ecdn gh\u1ebf tr\u01b0\u1edbc khi ti\u1ebfp t\u1ee5c"
            return
        }

        val userId = SessionManager.getCurrentUser()?.id ?: 0L
        val selectedSeatNumbers = selectedSeats.map { it.seatNumber }
        val selectedTotal = selectedSeats.size * tripPrice
        viewModelScope.launch {
            _isProcessing.value = true
            when (val result = seatRepository.reserveSeats(userId, tripId, selectedSeats, tripPrice)) {
                is SeatReservationResult.Success -> {
                    Log.d(TAG, "reserveSeats success paymentId=${result.paymentId} tickets=${result.ticketIds}")
                    _bookingResult.value = result.ticketIds
                    pendingCheckout = PendingCheckout(
                        ticketIds = result.ticketIds,
                        seatNumbers = selectedSeatNumbers,
                        totalPrice = selectedTotal,
                        paymentId = result.paymentId
                    )
                    createPaymentPayload(pendingCheckout ?: return@launch)
                }
                is SeatReservationResult.AlreadyTaken -> {
                    Log.d(TAG, "reserveSeats already taken seat=${result.seatNumber}")
                    _error.value = "Gh\u1ebf ${result.seatNumber} \u0111\u00e3 b\u00e1n"
                    _isProcessing.value = false
                    loadSeats(tripId)
                }
                is SeatReservationResult.Failure -> {
                    Log.e(TAG, "reserveSeats failed: ${result.message}")
                    _error.value = result.message
                    _isProcessing.value = false
                    loadSeats(tripId)
                }
            }
        }
    }

    fun bookRoundTripSeats(
        outboundTripId: Long,
        outboundSeatIds: List<Long>,
        outboundSeatNumbers: List<String>,
        outboundPrice: Double,
        returnTripId: Long
    ) {
        val returnSeats = _selectedSeats.value.orEmpty()
        Log.d(
            TAG,
            "bookRoundTripSeats outboundTripId=$outboundTripId returnTripId=$returnTripId " +
                "outboundSeats=$outboundSeatNumbers returnSeats=${returnSeats.map { it.seatNumber }} " +
                "outboundPrice=$outboundPrice returnPrice=$tripPrice"
        )
        if (_isProcessing.value == true) return

        if (outboundSeatIds.isEmpty() || outboundSeatNumbers.isEmpty()) {
            _error.value = "Thi\u1ebfu th\u00f4ng tin gh\u1ebf chi\u1ec1u \u0111i"
            return
        }
        if (returnSeats.isEmpty()) {
            _error.value = "Vui l\u00f2ng ch\u1ecdn gh\u1ebf chi\u1ec1u v\u1ec1"
            return
        }

        val userId = SessionManager.getCurrentUser()?.id ?: 0L
        val outboundSeats = outboundSeatIds.mapIndexed { index, seatId ->
            SeatReservationSeat(
                id = seatId,
                seatNumber = outboundSeatNumbers.getOrNull(index) ?: seatId.toString()
            )
        }
        val returnSeatNumbers = returnSeats.map { it.seatNumber }
        val selectedTotal = outboundSeats.size * outboundPrice + returnSeats.size * tripPrice

        viewModelScope.launch {
            _isProcessing.value = true
            val segments = listOf(
                SeatReservationSegment(outboundTripId, outboundSeats, outboundPrice),
                SeatReservationSegment(
                    returnTripId,
                    returnSeats.map { SeatReservationSeat(it.id, it.seatNumber) },
                    tripPrice
                )
            )
            when (val result = seatRepository.reserveSeatSegments(userId, segments)) {
                is SeatReservationResult.Success -> {
                    Log.d(TAG, "reserveSeatSegments success paymentId=${result.paymentId} tickets=${result.ticketIds}")
                    _bookingResult.value = result.ticketIds
                    pendingCheckout = PendingCheckout(
                        ticketIds = result.ticketIds,
                        seatNumbers = outboundSeatNumbers + returnSeatNumbers,
                        totalPrice = selectedTotal,
                        paymentId = result.paymentId,
                        outboundSeatNumbers = outboundSeatNumbers,
                        returnSeatNumbers = returnSeatNumbers,
                        isRoundTrip = true
                    )
                    createPaymentPayload(pendingCheckout ?: return@launch)
                }
                is SeatReservationResult.AlreadyTaken -> {
                    Log.d(TAG, "reserveSeatSegments already taken seat=${result.seatNumber}")
                    _error.value = "Gh\u1ebf ${result.seatNumber} \u0111\u00e3 b\u00e1n"
                    _isProcessing.value = false
                    loadSeats(returnTripId)
                }
                is SeatReservationResult.Failure -> {
                    Log.e(TAG, "reserveSeatSegments failed: ${result.message}")
                    _error.value = result.message
                    _isProcessing.value = false
                    loadSeats(returnTripId)
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearPaymentUrl() {
        _paymentUrl.value = null
    }

    fun clearCheckout() {
        _checkout.value = null
    }

    private fun recalcTotal() {
        _totalPrice.value = _selectedSeats.value.orEmpty().size * tripPrice
    }

    private fun createPaymentPayload(pending: PendingCheckout) {
        viewModelScope.launch {
            _isProcessing.value = true
            Log.d(TAG, "createPaymentPayload start paymentId=${pending.paymentId}")
            vnpayRepository.createPaymentPayload(pending.paymentId)
                .onSuccess { payment ->
                    Log.d(TAG, "createPaymentPayload success paymentId=${payment.paymentId} hasUrl=${payment.paymentUrl.isNotBlank()} qrBytes=${payment.qrImageBase64.length}")
                    pendingCheckout = null
                    _selectedSeats.value = emptyList()
                    recalcTotal()
                    _checkout.value = BookingCheckout(
                        ticketIds = pending.ticketIds,
                        seatNumbers = pending.seatNumbers,
                        totalPrice = pending.totalPrice,
                        paymentId = payment.paymentId,
                        paymentUrl = payment.paymentUrl,
                        qrContent = payment.qrContent,
                        qrImageBase64 = payment.qrImageBase64,
                        qrMimeType = payment.qrMimeType,
                        paymentExpiresAt = payment.expiresAt,
                        outboundSeatNumbers = pending.outboundSeatNumbers,
                        returnSeatNumbers = pending.returnSeatNumbers,
                        isRoundTrip = pending.isRoundTrip
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "createPaymentPayload failed paymentId=${pending.paymentId}: ${error.message}", error)
                    _error.value = error.message ?: "Kh\u00f4ng th\u1ec3 t\u1ea1o link thanh to\u00e1n VNPAY"
                }
            _isProcessing.value = false
        }
    }

    private fun removeUnavailableSelections() {
        val availableSeatIds = _seats.value
            .orEmpty()
            .filter { it.status == SeatStatus.AVAILABLE }
            .map { it.seat.id }
            .toSet()
        val selected = _selectedSeats.value.orEmpty()
        val validSelected = selected.filter { it.id in availableSeatIds }
        if (validSelected.size != selected.size) {
            _selectedSeats.value = validSelected
            recalcTotal()
        }
    }
}
