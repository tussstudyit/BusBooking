package com.example.busbooking.presentation.ui.state

import com.example.busbooking.data.entity.User

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class LoginSuccess(val user: User) : AuthState()
    data class RegisterSuccess(val userId: Long) : AuthState()
    data class Error(val message: String) : AuthState()
}
