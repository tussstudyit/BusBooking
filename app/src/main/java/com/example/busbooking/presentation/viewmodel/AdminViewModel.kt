package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.RouteRepository
import com.example.busbooking.domain.repository.UserRepository
import com.example.busbooking.presentation.ui.state.AdminState
import kotlinx.coroutines.launch

class AdminViewModel(
    private val userRepository: UserRepository,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _adminState = MutableLiveData<AdminState>(AdminState.Idle)
    val adminState: LiveData<AdminState> = _adminState

    // ===================== USER MANAGEMENT =====================

    /**
     * Load tất cả users (phân trang mặc định 20)
     */
    fun loadAllUsers(limit: Int = 20, offset: Int = 0) {
        _adminState.value = AdminState.Loading

        viewModelScope.launch {
            when (val result = userRepository.getAllUsers(limit, offset)) {
                is Result.Success -> {
                    _adminState.value = AdminState.UsersLoaded(result.data)
                }
                is Result.Error -> {
                    _adminState.value = AdminState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Tìm kiếm user theo tên hoặc email
     */
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            loadAllUsers()
            return
        }

        _adminState.value = AdminState.Loading

        viewModelScope.launch {
            when (val result = userRepository.searchUsers(query)) {
                is Result.Success -> {
                    _adminState.value = AdminState.UsersLoaded(result.data)
                }
                is Result.Error -> {
                    _adminState.value = AdminState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Chặn / bỏ chặn tài khoản user
     */
    fun toggleBlockUser(userId: Long, isCurrentlyBlocked: Boolean) {
        viewModelScope.launch {
            val result = if (isCurrentlyBlocked) {
                userRepository.unblockUser(userId)
            } else {
                userRepository.blockUser(userId)
            }

            when (result) {
                is Result.Success -> {
                    val message = if (isCurrentlyBlocked) "Đã bỏ chặn tài khoản" else "Đã chặn tài khoản"
                    _adminState.value = AdminState.ActionSuccess(message)
                }
                is Result.Error -> {
                    _adminState.value = AdminState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    // ===================== ROUTE MANAGEMENT =====================

    /**
     * Load tất cả tuyến đường
     */
    fun loadAllRoutes() {
        _adminState.value = AdminState.Loading

        viewModelScope.launch {
            when (val result = routeRepository.getAllOrigins()) {
                is Result.Success -> {
                    // Load routes bằng cách search với query rỗng → trả về tất cả
                    loadRoutesInternal()
                }
                is Result.Error -> {
                    _adminState.value = AdminState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    private suspend fun loadRoutesInternal() {
        // Dùng searchRoutes với ký tự đại diện để lấy tất cả route
        when (val result = routeRepository.searchRoutes("")) {
            is Result.Success -> {
                _adminState.value = AdminState.RoutesLoaded(result.data)
            }
            is Result.Error -> {
                // Khi không có route nào, trả về list rỗng
                _adminState.value = AdminState.RoutesLoaded(emptyList())
            }
            else -> {}
        }
    }

    /**
     * Tìm kiếm tuyến đường
     */
    fun searchRoutes(query: String) {
        _adminState.value = AdminState.Loading

        viewModelScope.launch {
            if (query.isBlank()) {
                loadRoutesInternal()
                return@launch
            }

            when (val result = routeRepository.searchRoutes(query)) {
                is Result.Success -> {
                    _adminState.value = AdminState.RoutesLoaded(result.data)
                }
                is Result.Error -> {
                    _adminState.value = AdminState.RoutesLoaded(emptyList())
                }
                else -> {}
            }
        }
    }

    /**
     * Tạo tuyến đường mới
     */
    fun createRoute(origin: String, destination: String, distance: Int) {
        if (origin.isBlank() || destination.isBlank()) {
            _adminState.value = AdminState.Error("Vui lòng nhập điểm đi và điểm đến")
            return
        }
        if (distance <= 0) {
            _adminState.value = AdminState.Error("Khoảng cách phải lớn hơn 0")
            return
        }

        _adminState.value = AdminState.Loading

        viewModelScope.launch {
            when (val result = routeRepository.createRoute(origin, destination, distance)) {
                is Result.Success -> {
                    _adminState.value = AdminState.ActionSuccess("Đã tạo tuyến đường thành công")
                }
                is Result.Error -> {
                    _adminState.value = AdminState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Vô hiệu hóa tuyến đường
     */
    fun deactivateRoute(routeId: Long) {
        viewModelScope.launch {
            when (val result = routeRepository.deactivateRoute(routeId)) {
                is Result.Success -> {
                    _adminState.value = AdminState.ActionSuccess("Đã vô hiệu hóa tuyến đường")
                }
                is Result.Error -> {
                    _adminState.value = AdminState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    fun resetState() {
        _adminState.value = AdminState.Idle
    }
}