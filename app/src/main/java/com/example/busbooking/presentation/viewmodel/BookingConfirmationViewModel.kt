package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.FirebaseTicketRepository
import com.example.busbooking.domain.repository.TicketRepository
import kotlinx.coroutines.launch

class BookingConfirmationViewModel(
    private val ticketRepository: TicketRepository,
    private val firebaseTicketRepository: FirebaseTicketRepository = FirebaseTicketRepository()
) : ViewModel() {

    private val _ticket = MutableLiveData<TicketDetails?>(null)
    val ticket: LiveData<TicketDetails?> = _ticket

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadTicket(ticketId: Long) {
        viewModelScope.launch {
            when (val result = firebaseTicketRepository.getTicketById(ticketId)) {
                is Result.Success -> _ticket.value = result.data
                is Result.Error -> loadLocalTicket(ticketId, result.message)
                Result.Loading -> Unit
            }
        }
    }

    private suspend fun loadLocalTicket(ticketId: Long, fallbackError: String) {
        when (val result = ticketRepository.getTicketById(ticketId)) {
            is Result.Success -> _ticket.value = result.data
            is Result.Error -> _error.value = fallbackError
            Result.Loading -> Unit
        }
    }
}
