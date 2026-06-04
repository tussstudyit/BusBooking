package com.example.busbooking.staff.ui.ticket

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.staff.data.model.TicketVerificationResult
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.session.StaffSessionManager
import kotlinx.coroutines.launch

sealed class TicketVerifyState {
    object Idle : TicketVerifyState()
    object Loading : TicketVerifyState()
    data class Success(val result: TicketVerificationResult) : TicketVerifyState()
    data class Error(val message: String) : TicketVerifyState()
}

class TicketScannerViewModel(
    private val repository: StaffRepository
) : ViewModel() {
    private val _state = MutableLiveData<TicketVerifyState>(TicketVerifyState.Idle)
    val state: LiveData<TicketVerifyState> = _state

    fun verify(qrContent: String) {
        if (qrContent.isBlank()) {
            _state.value = TicketVerifyState.Error("Mã vé không hợp lệ")
            return
        }
        viewModelScope.launch {
            _state.value = TicketVerifyState.Loading
            runCatching { repository.verifyTicket(qrContent.trim(), StaffSessionManager.currentStaffId()) }
                .onSuccess { result ->
                    if (result.valid) _state.value = TicketVerifyState.Success(result)
                    else _state.value = TicketVerifyState.Error(result.message.ifBlank { "Vé không hợp lệ" })
                }
                .onFailure { _state.value = TicketVerifyState.Error(it.message ?: "Không kiểm tra được vé") }
        }
    }
}
