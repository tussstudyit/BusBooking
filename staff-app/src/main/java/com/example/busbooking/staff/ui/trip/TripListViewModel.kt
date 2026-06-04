package com.example.busbooking.staff.ui.trip

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.session.StaffSessionManager
import com.example.busbooking.staff.utils.StaffTripGroups
import com.example.busbooking.staff.utils.groupForStaffTripList
import kotlinx.coroutines.launch

sealed class TripListState {
    object Loading : TripListState()
    data class Success(val groups: StaffTripGroups) : TripListState()
    data class Error(val message: String) : TripListState()
}

class TripListViewModel(
    private val repository: StaffRepository
) : ViewModel() {
    private val _state = MutableLiveData<TripListState>()
    val state: LiveData<TripListState> = _state

    fun loadTrips() {
        val staffId = StaffSessionManager.currentStaffId()
        viewModelScope.launch {
            _state.value = TripListState.Loading
            runCatching { repository.loadTrips(staffId) }
                .onSuccess { _state.value = TripListState.Success(it.groupForStaffTripList()) }
                .onFailure { _state.value = TripListState.Error(it.message ?: "Không tải được danh sách chuyến") }
        }
    }
}
