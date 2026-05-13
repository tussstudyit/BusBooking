package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.busbooking.data.model.Province
import com.example.busbooking.data.model.Stop
import com.example.busbooking.data.model.VietnameseProvinces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProvinceViewModel : ViewModel() {

    private val _provinces = MutableLiveData<List<Province>>()
    val provinces: LiveData<List<Province>> = _provinces

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Stop hiển thị trên UI (tên bến xe cụ thể)
    private val _selectedStop = MutableLiveData<Stop?>(null)
    val selectedStop: LiveData<Stop?> = _selectedStop

    // Province để query DB (tên tỉnh khớp với SeedData)
    private val _selectedProvince = MutableLiveData<Province?>(null)
    val selectedProvince: LiveData<Province?> = _selectedProvince

    private val _isEmpty = MutableLiveData<Boolean>(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private var searchJob: Job? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        loadProvinces()
    }

    fun loadProvinces() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = VietnameseProvinces.provinces
                _provinces.value = result
                _isEmpty.value = result.isEmpty()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchProvinces(query: String) {
        searchJob?.cancel()

        if (query.trim().isEmpty()) {
            _provinces.value = VietnameseProvinces.provinces
            _isEmpty.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            try {
                val results = VietnameseProvinces.searchProvinces(query)
                _provinces.value = results
                _isEmpty.value = results.isEmpty()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Gọi khi user chọn một Stop.
     * Tự động tìm Province cha để dùng khi query DB.
     */
    fun selectStop(stop: Stop) {
        _selectedStop.value = stop

        // Tìm Province chứa stop này → dùng province.name để query trips
        val province = VietnameseProvinces.provinces.find { province ->
            province.stops.any { it.id == stop.id }
        }
        _selectedProvince.value = province
    }

    /**
     * Lấy tên tỉnh để query DB (khớp với SeedData)
     * VD: "Đà Nẵng", "Hà Nội", "TP. Hồ Chí Minh"
     *
     * Lưu ý: SeedData dùng "TP. Hồ Chí Minh" nhưng Province.kt dùng
     * "Thành phố Hồ Chí Minh" → cần normalize nếu không khớp.
     */
    fun getProvinceNameForQuery(): String? {
        return _selectedProvince.value?.name?.let { normalizeProvinceName(it) }
    }

    /**
     * Normalize tên tỉnh cho khớp với SeedData.
     * Thêm các mapping nếu Province.kt và SeedData dùng tên khác nhau.
     */
    private fun normalizeProvinceName(name: String): String {
        return when (name) {
            "Thành phố Hồ Chí Minh" -> "TP. Hồ Chí Minh"
            else -> name
        }
    }

    fun clearSelectedStop() {
        _selectedStop.value = null
        _selectedProvince.value = null
    }

    fun clearSearchCache() {
        VietnameseProvinces.clearSearchCache()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
        searchJob?.cancel()
    }
}