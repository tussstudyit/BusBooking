package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.User
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.FirebaseTicketRepository
import com.example.busbooking.domain.repository.IAuthRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.presentation.ui.state.UserState
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.launch

class UserViewModel(
    private val authRepository: IAuthRepository,
    private val ticketRepository: TicketRepository,
    private val firebaseTicketRepository: FirebaseTicketRepository = FirebaseTicketRepository()
) : ViewModel() {

    private val _userState = MutableLiveData<UserState>(UserState.Idle)
    val userState: LiveData<UserState> = _userState

    fun loadProfile() {
        val userId = SessionManager.getCurrentUserId()
        if (userId <= 0) {
            _userState.value = UserState.Error("Vui lòng đăng nhập lại")
            return
        }

        viewModelScope.launch {
            _userState.value = UserState.Loading
            when (val result = authRepository.getUserProfile(userId)) {
                is Result.Success -> _userState.value = UserState.ProfileLoaded(result.data)
                is Result.Error -> _userState.value = UserState.Error(result.message)
                Result.Loading -> Unit
            }
        }
    }

    fun updateProfile(user: User) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            when (val result = authRepository.updateUserProfile(user)) {
                is Result.Success -> _userState.value = UserState.UpdateSuccess()
                is Result.Error -> _userState.value = UserState.Error(result.message)
                Result.Loading -> Unit
            }
        }
    }

    fun loadMyTickets() {
        val userId = SessionManager.getCurrentUserId()
        if (userId <= 0) {
            _userState.value = UserState.TicketsLoaded(emptyList())
            return
        }

        viewModelScope.launch {
            _userState.value = UserState.Loading
            when (val result = firebaseTicketRepository.getUserActiveTickets(userId)) {
                is Result.Success -> _userState.value = UserState.TicketsLoaded(result.data)
                is Result.Error -> loadLocalTickets(userId, result.message)
                Result.Loading -> Unit
            }
        }
    }

    fun loadTicketHistory() {
        val userId = SessionManager.getCurrentUserId()
        if (userId <= 0) {
            _userState.value = UserState.TicketsLoaded(emptyList())
            return
        }

        viewModelScope.launch {
            _userState.value = UserState.Loading
            when (val result = firebaseTicketRepository.getUserTicketHistory(userId)) {
                is Result.Success -> _userState.value = UserState.TicketsLoaded(result.data)
                is Result.Error -> loadLocalTicketHistory(userId, result.message)
                Result.Loading -> Unit
            }
        }
    }

    private suspend fun loadLocalTickets(userId: Long, fallbackError: String) {
        when (val result = ticketRepository.getUserActiveTickets(userId)) {
            is Result.Success -> _userState.value = UserState.TicketsLoaded(result.data)
            is Result.Error -> _userState.value = UserState.Error(fallbackError)
            Result.Loading -> Unit
        }
    }

    private suspend fun loadLocalTicketHistory(userId: Long, fallbackError: String) {
        when (val result = ticketRepository.getUserTicketHistory(userId)) {
            is Result.Success -> _userState.value = UserState.TicketsLoaded(result.data)
            is Result.Error -> _userState.value = UserState.Error(fallbackError)
            Result.Loading -> Unit
        }
    }
}
