package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.ApiTicketRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.domain.repository.VnpayPaymentPayload
import com.example.busbooking.domain.repository.VnpayRepository
import kotlinx.coroutines.launch

class BookingConfirmationViewModel(
    private val ticketRepository: TicketRepository,
    private val ApiTicketRepository: ApiTicketRepository = ApiTicketRepository(),
    private val vnpayRepository: VnpayRepository = VnpayRepository()
) : ViewModel() {

    private val _ticket = MutableLiveData<TicketDetails?>(null)
    val ticket: LiveData<TicketDetails?> = _ticket

    private val _paymentPayload = MutableLiveData<VnpayPaymentPayload?>(null)
    val paymentPayload: LiveData<VnpayPaymentPayload?> = _paymentPayload

    private val _cancelSuccess = MutableLiveData(false)
    val cancelSuccess: LiveData<Boolean> = _cancelSuccess

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadTicket(ticketId: Long) {
        viewModelScope.launch {
            when (val result = ApiTicketRepository.getTicketById(ticketId)) {
                is Result.Success -> _ticket.value = result.data
                is Result.Error -> loadLocalTicket(ticketId, result.message)
                Result.Loading -> Unit
            }
        }
    }

    fun loadPendingPayment(ticketId: Long) {
        _paymentPayload.value = null
        viewModelScope.launch {
            when (val session = ApiTicketRepository.getPendingPaymentSession(ticketId)) {
                is Result.Success -> {
                    vnpayRepository.createPaymentPayload(session.data.paymentId)
                        .onSuccess { payload -> _paymentPayload.value = payload }
                        .onFailure { error ->
                            _error.value = error.message ?: "Kh\u00f4ng th\u1ec3 t\u1ea1o l\u1ea1i m\u00e3 QR VNPAY"
                        }
                }
                is Result.Error -> {
                    _error.value = session.message
                    loadTicket(ticketId)
                }
                Result.Loading -> Unit
            }
        }
    }

    fun cancelPendingPayment(ticketId: Long) {
        viewModelScope.launch {
            when (val session = ApiTicketRepository.getPendingPaymentSession(ticketId)) {
                is Result.Success -> {
                    vnpayRepository.cancelPayment(session.data.paymentId)
                        .onSuccess {
                            _cancelSuccess.value = true
                            loadTicket(ticketId)
                        }
                        .onFailure { error ->
                            _error.value = error.message ?: "Kh\u00f4ng th\u1ec3 h\u1ee7y thanh to\u00e1n"
                        }
                }
                is Result.Error -> _error.value = session.message
                Result.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun loadLocalTicket(ticketId: Long, fallbackError: String) {
        when (val result = ticketRepository.getTicketById(ticketId)) {
            is Result.Success -> _ticket.value = result.data
            is Result.Error -> _error.value = fallbackError
            Result.Loading -> Unit
        }
    }
}

