package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.User
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.presentation.ui.state.UserState
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.launch

class UserViewModel(
    private val authRepository: AuthRepository,
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _userState = MutableLiveData<UserState>(UserState.Idle)
    val userState: LiveData<UserState> = _userState

    // Giữ user hiện tại để dùng khi update
    private var cachedUser: User? = null

    /**
     * Load thông tin profile
     * Ưu tiên SessionManager trước, fallback về DB
     */
    fun loadProfile() {
        val sessionUser = SessionManager.getCurrentUser()
        if (sessionUser != null) {
            cachedUser = sessionUser
            _userState.value = UserState.ProfileLoaded(sessionUser)
            return
        }

        _userState.value = UserState.Loading

        val userId = SessionManager.getCurrentUserId() ?: run {
            _userState.value = UserState.Error("Không tìm thấy thông tin người dùng")
            return
        }

        viewModelScope.launch {
            when (val result = authRepository.getUserProfile(userId)) {
                is Result.Success -> {
                    cachedUser = result.data
                    _userState.value = UserState.ProfileLoaded(result.data)
                }
                is Result.Error -> {
                    _userState.value = UserState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Cập nhật tên và số điện thoại
     */
    fun updateProfile(name: String, phone: String) {
        if (name.isBlank() || phone.isBlank()) {
            _userState.value = UserState.Error("Vui lòng điền đầy đủ thông tin")
            return
        }

        val user = cachedUser
        if (user == null) {
            _userState.value = UserState.Error("Không tìm thấy thông tin người dùng")
            return
        }

        _userState.value = UserState.Loading

        val updatedUser = user.copy(name = name, phone = phone)

        viewModelScope.launch {
            when (val result = authRepository.updateUserProfile(updatedUser)) {
                is Result.Success -> {
                    cachedUser = updatedUser
                    SessionManager.saveSession(updatedUser)
                    _userState.value = UserState.UpdateSuccess()
                }
                is Result.Error -> {
                    _userState.value = UserState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Load danh sách vé còn hiệu lực (CONFIRMED / PENDING)
     * Dùng trong UserDashboardFragment / MyTicketsFragment
     */
    fun loadMyTickets() {
        val userId = SessionManager.getCurrentUserId() ?: run {
            _userState.value = UserState.Error("Phiên đăng nhập hết hạn")
            return
        }

        _userState.value = UserState.Loading

        viewModelScope.launch {
            when (val result = ticketRepository.getUserActiveTickets(userId)) {
                is Result.Success -> {
                    _userState.value = UserState.TicketsLoaded(result.data)
                }
                is Result.Error -> {
                    _userState.value = UserState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    fun resetState() {
        _userState.value = UserState.Idle
    }
}