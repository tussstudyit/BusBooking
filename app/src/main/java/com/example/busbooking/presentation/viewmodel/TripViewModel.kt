package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.ITripRepository
import com.example.busbooking.presentation.ui.state.TripState
import kotlinx.coroutines.launch

class TripViewModel(
    private val tripRepository: ITripRepository
) : ViewModel() {

    private val _tripState = MutableLiveData<TripState>(TripState.Idle)
    val tripState: LiveData<TripState> = _tripState

    /**
     * Tìm kiếm chuyến xe theo điểm đi, điểm đến, ngày
     * Được gọi từ TripListFragment, nhận args từ RouteSearchFragment
     */
    fun searchTrips(origin: String, destination: String, tripDate: Long) {
        if (origin.isBlank() || destination.isBlank()) {
            _tripState.value = TripState.Error("Thông tin tìm kiếm không hợp lệ")
            return
        }

        _tripState.value = TripState.Loading

        viewModelScope.launch {
            when (val result = tripRepository.searchTrips(origin, destination, tripDate)) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        _tripState.value = TripState.Empty()
                    } else {
                        _tripState.value = TripState.SearchSuccess(result.data)
                    }
                }
                is Result.Error -> {
                    _tripState.value = TripState.Empty("Không tìm thấy chuyến xe phù hợp")
                }
                else -> {
                    _tripState.value = TripState.Error("Đã xảy ra lỗi")
                }
            }
        }
    }

    /**
     * Load chi tiết 1 chuyến xe
     * Được gọi từ TripDetailsFragment
     */
    fun loadTripDetail(tripId: Long) {
        _tripState.value = TripState.Loading

        viewModelScope.launch {
            when (val result = tripRepository.getTripById(tripId)) {
                is Result.Success -> {
                    _tripState.value = TripState.DetailSuccess(result.data)
                }
                is Result.Error -> {
                    _tripState.value = TripState.Error(result.message)
                }
                else -> {
                    _tripState.value = TripState.Error("Đã xảy ra lỗi")
                }
            }
        }
    }

    /**
     * Load tất cả chuyến xe (cho admin)
     */
    fun loadAllTrips() {
        _tripState.value = TripState.Loading

        viewModelScope.launch {
            when (val result = tripRepository.getAllTrips()) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        _tripState.value = TripState.Empty("Không có chuyến xe nào")
                    } else {
                        _tripState.value = TripState.SearchSuccess(result.data)
                    }
                }
                is Result.Error -> {
                    _tripState.value = TripState.Empty(result.message)
                }
                else -> {
                    _tripState.value = TripState.Error("Đã xảy ra lỗi")
                }
            }
        }
    }

    fun resetState() {
        _tripState.value = TripState.Idle
    }
}
