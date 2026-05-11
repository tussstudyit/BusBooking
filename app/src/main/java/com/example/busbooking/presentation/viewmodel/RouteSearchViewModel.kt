package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.RouteRepository
import kotlinx.coroutines.launch

class RouteSearchViewModel(
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _origins = MutableLiveData<List<String>>(emptyList())
    val origins: LiveData<List<String>> = _origins

    private val _destinations = MutableLiveData<List<String>>(emptyList())
    val destinations: LiveData<List<String>> = _destinations

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // Lưu lựa chọn hiện tại để truyền sang TripListFragment
    private val _selectedOrigin = MutableLiveData<String?>(null)
    val selectedOrigin: LiveData<String?> = _selectedOrigin

    private val _selectedDestination = MutableLiveData<String?>(null)
    val selectedDestination: LiveData<String?> = _selectedDestination

    private val _selectedDate = MutableLiveData<Long?>(null)
    val selectedDate: LiveData<Long?> = _selectedDate

    /**
     * Load danh sách tỉnh/thành phố cho 2 dropdown
     */
    fun loadLocations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val originsResult = routeRepository.getAllOrigins()) {
                is Result.Success -> _origins.value = originsResult.data
                is Result.Error -> _error.value = originsResult.message
                else -> {}
            }

            when (val destResult = routeRepository.getAllDestinations()) {
                is Result.Success -> _destinations.value = destResult.data
                is Result.Error -> _error.value = destResult.message
                else -> {}
            }

            _isLoading.value = false
        }
    }

    fun selectOrigin(origin: String) {
        _selectedOrigin.value = origin
    }

    fun selectDestination(destination: String) {
        _selectedDestination.value = destination
    }

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
    }

    /**
     * Kiểm tra input hợp lệ trước khi navigate sang TripListFragment
     * @return error message nếu invalid, null nếu ok
     */
    fun validate(): String? {
        if (_selectedOrigin.value.isNullOrBlank()) return "Vui lòng chọn điểm đi"
        if (_selectedDestination.value.isNullOrBlank()) return "Vui lòng chọn điểm đến"
        if (_selectedOrigin.value == _selectedDestination.value) return "Điểm đi và điểm đến không được trùng nhau"
        if (_selectedDate.value == null) return "Vui lòng chọn ngày đi"
        return null
    }

    fun clearError() {
        _error.value = null
    }
}