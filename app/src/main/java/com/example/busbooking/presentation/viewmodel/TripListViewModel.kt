package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.ITripRepository
import com.example.busbooking.domain.repository.TripSeatAvailability
import com.example.busbooking.data.relations.TripWithRouteAndBus
import kotlinx.coroutines.launch

data class TripListItem(
    val tripDetails: TripWithRouteAndBus,
    val seatAvailability: TripSeatAvailability
) {
    val isBookingOpen: Boolean
        get() = tripDetails.trip.status == "SCHEDULED" &&
            tripDetails.trip.departureTime > System.currentTimeMillis() &&
            seatAvailability.availableSeats > 0
}

/**
 * ViewModel cho TripListFragment.
 * Tách riêng khỏi TripViewModel để Fragment không cần quan tâm
 * đến TripState.DetailSuccess vốn chỉ dành cho TripDetailsFragment.
 */
class TripListViewModel(
    private val tripRepository: ITripRepository
) : ViewModel() {

    private val _trips = MutableLiveData<List<TripListItem>>(emptyList())
    val trips: LiveData<List<TripListItem>> = _trips

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun searchTrips(origin: String, destination: String, tripDate: Long) {
        if (origin.isBlank() || destination.isBlank()) {
            _error.value = "Thông tin tìm kiếm không hợp lệ"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            when (val result = tripRepository.searchTrips(origin, destination, tripDate)) {
                is Result.Success -> {
                    _trips.value = result.data.bookableDepartures().withSeatAvailability()
                }
                is Result.Error -> {
                    _trips.value = emptyList()
                    _error.value = result.message
                }
                else -> {
                    _trips.value = emptyList()
                    _error.value = "Đã xảy ra lỗi"
                }
            }
            _isLoading.value = false
        }
    }

    // Dùng cho TripListAdminFragment — load toàn bộ không filter
    fun loadAllTrips() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            when (val result = tripRepository.getAllTrips()) {
                is Result.Success -> {
                    _trips.value = result.data.withSeatAvailability()
                }
                is Result.Error -> {
                    _trips.value = emptyList()
                    _error.value = result.message
                }
                else -> {
                    _trips.value = emptyList()
                }
            }
            _isLoading.value = false
        }
    }

    private suspend fun List<TripWithRouteAndBus>.withSeatAvailability(): List<TripListItem> {
        return map { trip ->
            val availability = when (val seats = tripRepository.getSeatAvailability(trip.trip.id, trip.bus.id)) {
                is Result.Success -> seats.data
                else -> TripSeatAvailability(trip.bus.totalSeats, 0)
            }
            TripListItem(trip, availability)
        }
    }

    private fun List<TripWithRouteAndBus>.bookableDepartures(): List<TripWithRouteAndBus> {
        val now = System.currentTimeMillis()
        return filter { it.trip.status == "SCHEDULED" && it.trip.departureTime > now }
            .sortedBy { it.trip.departureTime }
    }
}
