package com.example.busbooking.staff.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.session.StaffSessionManager
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(
    private val repository: StaffRepository
) : ViewModel() {
    private val _state = MutableLiveData<LoginState>(LoginState.Idle)
    val state: LiveData<LoginState> = _state

    fun login(login: String, password: String) {
        val email = login.trim().lowercase()
        if (email.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("Vui lòng nhập đầy đủ thông tin")
            return
        }
        if (!email.matches(Regex("^[a-z0-9._%+-]+@gmail\\.com$"))) {
            _state.value = LoginState.Error("Vui lòng nhập email Gmail hợp lệ")
            return
        }
        viewModelScope.launch {
            _state.value = LoginState.Loading
            runCatching { repository.login(email, password) }
                .onSuccess { response ->
                    if (!response.user.canUseStaffApp) {
                        _state.value = LoginState.Error("Tài khoản không có quyền STAFF")
                    } else {
                        StaffSessionManager.saveUser(response.user)
                        _state.value = LoginState.Success
                    }
                }
                .onFailure { error ->
                    _state.value = LoginState.Error(error.message ?: "Đăng nhập thất bại")
                }
        }
    }
}
