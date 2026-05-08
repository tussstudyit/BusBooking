package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.session.SessionManager
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.presentation.ui.state.AuthState
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill all fields")
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            when (val result = authRepository.loginUser(email, password)) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    SessionManager.saveSession(result.data)
                    _authState.value = AuthState.LoginSuccess(result.data)
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                else -> {
                    _authState.value = AuthState.Error("Unknown error")
                }
            }
        }
    }

    fun register(name: String, email: String, password: String, phone: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
            _authState.value = AuthState.Error("Please fill all fields")
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            when (val result = authRepository.registerUser(name, email, password, phone)) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _authState.value = AuthState.RegisterSuccess(result.data)
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
                else -> {
                    _authState.value = AuthState.Error("Unknown error")
                }
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
