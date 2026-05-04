package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.Trip
import com.example.busbooking.domain.repository.RouteRepository
import com.example.busbooking.domain.repository.TripRepository
import com.example.busbooking.utils.UiState
import kotlinx.coroutines.launch

data class SearchFilters(
    val origin: String = "",
    val destination: String = "",
    val date: String = "",
    val minPrice: Double = 0.0,
    val maxPrice: Double = 10000.0
)

class RouteSearchViewModel(
    private val routeRepository: RouteRepository,
    private val tripRepository: TripRepository
) : ViewModel() {
    private val _searchResults = MutableLiveData<UiState<List<Trip>>>()
    val searchResults: LiveData<UiState<List<Trip>>> = _searchResults

    private val _origins = MutableLiveData<List<String>>()
    val origins: LiveData<List<String>> = _origins

    private val _destinations = MutableLiveData<List<String>>()
    val destinations: LiveData<List<String>> = _destinations

    private val _appliedFilters = MutableLiveData<SearchFilters>(SearchFilters())
    val appliedFilters: LiveData<SearchFilters> = _appliedFilters

    init {
        loadOriginDestinations()
    }

    private fun loadOriginDestinations() {
        viewModelScope.launch {
            val originsResult = routeRepository.getAllOrigins()
            originsResult.onSuccess { _origins.value = it }

            val destResult = routeRepository.getAllDestinations()
            destResult.onSuccess { _destinations.value = it }
        }
    }

    fun searchTrips(origin: String, destination: String, date: String) {
        _searchResults.value = UiState.Loading("Searching for trips...")
        viewModelScope.launch {
            val routeResult = routeRepository.getRoutesByOriginAndDestination(origin, destination)
            routeResult.onSuccess { routes ->
                if (routes.isNotEmpty()) {
                    val route = routes[0]
                    val tripsResult = tripRepository.getTripsByRouteAndDate(route.id, date)
                    tripsResult.onSuccess { trips ->
                        _searchResults.value = UiState.Success(trips)
                    }
                    tripsResult.onFailure { error ->
                        _searchResults.value = UiState.Error(error.message ?: "Failed to fetch trips")
                    }
                } else {
                    _searchResults.value = UiState.Success(emptyList())
                }
            }
            routeResult.onFailure { error ->
                _searchResults.value = UiState.Error(error.message ?: "Failed to search routes")
            }
        }
    }

    fun applyFilters(minPrice: Double, maxPrice: Double) {
        val currentFilters = _appliedFilters.value ?: SearchFilters()
        _appliedFilters.value = currentFilters.copy(minPrice = minPrice, maxPrice = maxPrice)
    }

    fun clearFilters() {
        _appliedFilters.value = SearchFilters()
        _searchResults.value = UiState.Success(emptyList())
    }
}

