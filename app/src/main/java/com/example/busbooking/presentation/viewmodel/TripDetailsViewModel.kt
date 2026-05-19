package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.TripRepository
import kotlinx.coroutines.launch

/**
 * ViewModel cho TripDetailsFragment.
 */
class TripDetailsViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _trip = MutableLiveData<TripWithRouteAndBus?>(null)
    val trip: LiveData<TripWithRouteAndBus?> = _trip

    // ✅ Số ghế còn trống (-1 = chưa load)
    private val _availableSeats = MutableLiveData<Int>(-1)
    val availableSeats: LiveData<Int> = _availableSeats

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadTrip(tripId: Long) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            when (val result = tripRepository.getTripById(tripId)) {
                is Result.Success -> {
                    _trip.value = result.data

                    // ✅ Sau khi có trip, load luôn số ghế trống
                    val busId = result.data.bus.id
                    when (val seatsResult = tripRepository.getAvailableSeatsCount(tripId, busId)) {
                        is Result.Success -> _availableSeats.value = seatsResult.data
                        else -> _availableSeats.value = 0
                    }
                }
                is Result.Error -> {
                    _error.value = result.message
                }
                else -> {
                    _error.value = "Không tìm thấy thông tin chuyến xe"
                }
            }
            _isLoading.value = false
        }
    }
}