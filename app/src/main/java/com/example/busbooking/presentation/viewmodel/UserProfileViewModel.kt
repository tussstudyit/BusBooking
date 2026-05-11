package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.User
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * ViewModel cho UserProfileFragment.
 * Tách riêng khỏi UserViewModel để không chia sẻ state với MyTickets/History.
 */
class UserProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableLiveData<User?>(null)
    val user: LiveData<User?> = _user

    // null = chưa update, true = thành công, false = thất bại
    private val _updateResult = MutableLiveData<Boolean?>(null)
    val updateResult: LiveData<Boolean?> = _updateResult

    private var cachedUser: User? = null

    fun loadProfile(userId: Long) {
        // Ưu tiên session trước, tránh query DB không cần thiết
        val sessionUser = SessionManager.getCurrentUser()
        if (sessionUser != null) {
            cachedUser = sessionUser
            _user.value = sessionUser
            return
        }

        viewModelScope.launch {
            when (val result = authRepository.getUserProfile(userId)) {
                is Result.Success -> {
                    cachedUser = result.data
                    _user.value = result.data
                }
                is Result.Error -> {
                    // Giữ _user null để Fragment hiển thị lỗi
                }
                else -> {}
            }
        }
    }

    fun updateProfile(name: String, phone: String) {
        val user = cachedUser ?: return
        val updated = user.copy(name = name, phone = phone)

        viewModelScope.launch {
            when (val result = authRepository.updateUserProfile(updated)) {
                is Result.Success -> {
                    cachedUser = updated
                    _user.value = updated
                    SessionManager.saveSession(updated)
                    _updateResult.value = true
                }
                is Result.Error -> {
                    _updateResult.value = false
                }
                else -> {
                    _updateResult.value = false
                }
            }
        }
    }

    fun resetUpdateResult() {
        _updateResult.value = null
    }
}