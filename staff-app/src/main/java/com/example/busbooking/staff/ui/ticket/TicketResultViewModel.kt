package com.example.busbooking.staff.ui.ticket

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.staff.data.model.TicketVerificationResult
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.session.StaffSessionManager
import kotlinx.coroutines.launch

sealed class TicketCheckInState {
    object Idle : TicketCheckInState()
    object Loading : TicketCheckInState()
    data class Success(val result: TicketVerificationResult) : TicketCheckInState()
    data class Error(val message: String) : TicketCheckInState()
}

class TicketResultViewModel(
    private val repository: StaffRepository
) : ViewModel() {
    private val _state = MutableLiveData<TicketCheckInState>(TicketCheckInState.Idle)
    val state: LiveData<TicketCheckInState> = _state

    fun checkIn(ticketId: Long) {
        viewModelScope.launch {
            _state.value = TicketCheckInState.Loading
            runCatching { repository.checkInTicket(ticketId, StaffSessionManager.currentStaffId()) }
                .onSuccess { _state.value = TicketCheckInState.Success(it) }
                .onFailure { _state.value = TicketCheckInState.Error(it.message ?: "Xác nhận lên xe thất bại") }
        }
    }
}
