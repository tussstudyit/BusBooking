package com.example.busbooking.presentation.ui.state

import com.example.busbooking.data.entity.User
import com.example.busbooking.data.relations.TicketDetails

sealed class UserState {
    object Idle : UserState()
    object Loading : UserState()
    data class ProfileLoaded(val user: User) : UserState()
    data class UpdateSuccess(val message: String = "Cập nhật thành công") : UserState()
    data class TicketsLoaded(val tickets: List<TicketDetails>) : UserState()
    data class Error(val message: String) : UserState()
}