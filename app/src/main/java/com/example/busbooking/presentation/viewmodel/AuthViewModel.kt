package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.User
import com.example.busbooking.domain.repository.UserRepository
import com.example.busbooking.utils.SessionManager
import com.example.busbooking.utils.UiState
import kotlinx.coroutines.launch

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _loginState = MutableLiveData<UiState<User>>()
    val loginState: LiveData<UiState<User>> = _loginState

    private val _registerState = MutableLiveData<UiState<Long>>()
    val registerState: LiveData<UiState<Long>> = _registerState

    private val _validationErrors = MutableLiveData<Map<String, String>>()
    val validationErrors: LiveData<Map<String, String>> = _validationErrors

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    fun loginUser(email: String, password: String) {
        val errors = validateLoginInput(email, password)
        if (errors.isNotEmpty()) {
            _validationErrors.value = errors
            return
        }

        _loginState.value = UiState.Loading("Logging in...")
        viewModelScope.launch {
            val result = userRepository.loginUser(email, password)
            result.onSuccess { user ->
                SessionManager.saveSession(user)
                _loginState.value = UiState.Success(user)
            }
            result.onFailure { exception ->
                _loginState.value = UiState.Error(exception.message ?: "Login failed")
            }
        }
    }

    fun registerUser(name: String, email: String, password: String, phone: String) {
        val errors = validateRegisterInput(name, email, password, phone)
        if (errors.isNotEmpty()) {
            _validationErrors.value = errors
            return
        }

        _registerState.value = UiState.Loading("Registering...")
        viewModelScope.launch {
            val result = userRepository.registerUser(name, email, password, phone)
            result.onSuccess { userId ->
                _registerState.value = UiState.Success(userId)
            }
            result.onFailure { exception ->
                _registerState.value = UiState.Error(exception.message ?: "Registration failed")
            }
        }
    }

    fun logoutUser() {
        SessionManager.clearSession()
        _logoutEvent.value = true
    }

    private fun validateLoginInput(email: String, password: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (email.isBlank()) errors["email"] = "Email is required"
        else if (!email.contains("@")) errors["email"] = "Invalid email format"
        if (password.isBlank()) errors["password"] = "Password is required"
        else if (password.length < 6) errors["password"] = "Password must be at least 6 characters"
        return errors
    }

    private fun validateRegisterInput(name: String, email: String, password: String, phone: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (name.isBlank()) errors["name"] = "Name is required"
        if (email.isBlank()) errors["email"] = "Email is required"
        else if (!email.contains("@")) errors["email"] = "Invalid email format"
        if (password.isBlank()) errors["password"] = "Password is required"
        else if (password.length < 6) errors["password"] = "Password must be at least 6 characters"
        if (phone.isBlank()) errors["phone"] = "Phone is required"
        else if (phone.length < 10) errors["phone"] = "Phone must be at least 10 digits"
        return errors
    }
}

