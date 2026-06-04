package com.example.busbooking.staff.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.staff.data.model.StaffHomeSummary
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.session.StaffSessionManager
import com.example.busbooking.staff.utils.todayOnlySummary
import kotlinx.coroutines.launch

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val summary: StaffHomeSummary) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel(
    private val repository: StaffRepository
) : ViewModel() {
    private val _state = MutableLiveData<HomeState>()
    val state: LiveData<HomeState> = _state

    fun loadHome() {
        val user = StaffSessionManager.currentUser()
        if (user == null) {
            _state.value = HomeState.Error("Phiên đăng nhập đã hết hạn")
            return
        }
        viewModelScope.launch {
            _state.value = HomeState.Loading
            runCatching { repository.loadHome(user.id) }
                .onSuccess { _state.value = HomeState.Success(it.todayOnlySummary()) }
                .onFailure {
                    _state.value = HomeState.Success(
                        StaffHomeSummary(
                            staffName = user.name,
                            companyName = user.companyName.ifBlank { "Nha xe" },
                            assignedTripsToday = 0,
                            checkedInPassengers = 0,
                            bookedPassengersToday = 0,
                            todayTrips = emptyList()
                        )
                    )
                }
        }
    }
}
