package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.presentation.ui.state.AuthState
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

     fun login(phone: String, password: String) {
         if (phone.isBlank() || password.isBlank()) {
             _authState.value = AuthState.Error("Please fill all fields")
             return
         }

         _authState.value = AuthState.Loading

         viewModelScope.launch {
             when (val result = authRepository.loginUser(phone, password)) {
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
    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AuthViewModel(authRepository) as T
                }
            }
        }
    }
}
