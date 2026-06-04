package com.example.busbooking.staff.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.busbooking.staff.data.model.StaffUser
import com.example.busbooking.staff.session.StaffSessionManager

class ProfileViewModel : ViewModel() {
    private val _user = MutableLiveData<StaffUser?>()
    val user: LiveData<StaffUser?> = _user

    fun loadProfile() {
        _user.value = StaffSessionManager.currentUser()
    }

    fun logout() {
        StaffSessionManager.clear()
    }
}
