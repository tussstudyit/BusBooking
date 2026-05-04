package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.Ticket
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.utils.UiState
import kotlinx.coroutines.launch

data class TicketFilter(
    val status: String = "ALL" // ALL, UPCOMING, PAST, CANCELLED
)

class MyTicketsViewModel(private val ticketRepository: TicketRepository) : ViewModel() {
    private val _tickets = MutableLiveData<UiState<List<Ticket>>>()
    val tickets: LiveData<UiState<List<Ticket>>> = _tickets

    private val _appliedFilter = MutableLiveData<TicketFilter>(TicketFilter())
    val appliedFilter: LiveData<TicketFilter> = _appliedFilter

    private val _refreshing = MutableLiveData<Boolean>(false)
    val refreshing: LiveData<Boolean> = _refreshing

    fun loadUserTickets(userId: Long, filter: TicketFilter = TicketFilter()) {
        _refreshing.value = true
        _tickets.value = UiState.Loading("Loading tickets...")
        viewModelScope.launch {
            val result = ticketRepository.getTicketsByUserId(userId)
            result.onSuccess { allTickets ->
                val filtered = if (filter.status == "ALL") {
                    allTickets
                } else {
                    allTickets.filter { it.status == filter.status }
                }
                _tickets.value = UiState.Success(filtered.sortedByDescending { it.bookingTime })
                _refreshing.value = false
            }
            result.onFailure { error ->
                _tickets.value = UiState.Error(error.message ?: "Failed to load tickets")
                _refreshing.value = false
            }
        }
    }

    fun applyFilter(newFilter: TicketFilter) {
        _appliedFilter.value = newFilter
    }
}

