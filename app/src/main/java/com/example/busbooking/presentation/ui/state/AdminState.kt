package com.example.busbooking.presentation.ui.state

import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.entity.User

sealed class AdminState {
    object Idle : AdminState()
    object Loading : AdminState()
    data class UsersLoaded(val users: List<User>) : AdminState()
    data class RoutesLoaded(val routes: List<Route>) : AdminState()
    data class ActionSuccess(val message: String) : AdminState()
    data class Error(val message: String) : AdminState()
}