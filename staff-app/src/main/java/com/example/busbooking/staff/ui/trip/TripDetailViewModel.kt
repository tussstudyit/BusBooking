package com.example.busbooking.staff.ui.trip

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.staff.data.model.StaffTripDetail
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.session.StaffSessionManager
import kotlinx.coroutines.launch

sealed class TripDetailState {
    object Loading : TripDetailState()
    data class Success(val trip: StaffTripDetail) : TripDetailState()
    data class Error(val message: String) : TripDetailState()
}

class TripDetailViewModel(
    private val repository: StaffRepository
) : ViewModel() {
    private val _state = MutableLiveData<TripDetailState>()
    val state: LiveData<TripDetailState> = _state

    fun loadTrip(tripId: Long) {
        val staffId = StaffSessionManager.currentStaffId()
        viewModelScope.launch {
            _state.value = TripDetailState.Loading
            runCatching { repository.loadTripDetail(tripId, staffId) }
                .onSuccess { _state.value = TripDetailState.Success(it) }
                .onFailure { _state.value = TripDetailState.Error(it.message ?: "Không tải được chuyến") }
        }
    }

    private fun updateTrip(tripId: Long, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { loadTrip(tripId) }
                .onFailure { _state.value = TripDetailState.Error(it.message ?: "Cập nhật chuyến thất bại") }
        }
    }
}
