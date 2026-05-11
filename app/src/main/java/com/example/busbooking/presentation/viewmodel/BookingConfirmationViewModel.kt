package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.TicketRepository
import kotlinx.coroutines.launch

/**
 * ViewModel cho BookingConfirmationFragment.
 * Chỉ load và hiển thị thông tin vé vừa đặt — không có hành động write.
 */
class BookingConfirmationViewModel(
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _ticket = MutableLiveData<TicketDetails?>(null)
    val ticket: LiveData<TicketDetails?> = _ticket

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadTicket(ticketId: Long) {
        viewModelScope.launch {
            when (val result = ticketRepository.getTicketById(ticketId)) {
                is Result.Success -> _ticket.value = result.data
                is Result.Error   -> _error.value  = result.message
                else              -> _error.value  = "Không tìm thấy thông tin vé"
            }
        }
    }
}