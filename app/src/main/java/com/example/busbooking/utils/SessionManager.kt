package com.example.busbooking.utils

import android.content.Context
import com.example.busbooking.data.entity.User

object SessionManager {
    private const val PREFS_NAME = "bus_booking_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHONE = "phone"
    private const val KEY_ROLE = "role"
    private const val KEY_LOGIN_TIME = "login_time"

    private lateinit var prefs: android.content.SharedPreferences
    private var _currentUser: User? = null

    fun getCurrentUser(): User? = _currentUser

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(user: User) {
        _currentUser = user
        prefs.edit().apply {
            putLong(KEY_USER_ID, user.id)
            putString(KEY_NAME, user.name)
            putString(KEY_EMAIL, user.email)
            putString(KEY_PHONE, user.phone)
            putString(KEY_ROLE, user.role)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }

    fun isSessionActive(): Boolean {
        return prefs.getLong(KEY_USER_ID, -1L) != -1L
    }

    fun getCurrentUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1L)
    }

    fun getCurrentUserRole(): String {
        return prefs.getString(KEY_ROLE, "USER") ?: "USER"
    }

    fun getCurrentUserEmail(): String {
        return prefs.getString(KEY_EMAIL, "") ?: ""
    }

    fun getCurrentUserName(): String {
        return prefs.getString(KEY_NAME, "") ?: ""
    }

    fun getCurrentUserPhone(): String {
        return prefs.getString(KEY_PHONE, "") ?: ""
    }

    fun clearSession() {
        _currentUser = null
        prefs.edit().clear().apply()
    }

    fun restoreSession(): Boolean {
        val userId = prefs.getLong(KEY_USER_ID, -1L)
        return if (userId != -1L) {
            _currentUser = User(
                id = userId,
                name = getCurrentUserName(),
                email = getCurrentUserEmail(),
                password = "",
                phone = getCurrentUserPhone(),
                role = getCurrentUserRole()
            )
            true
        } else {
            false
        }
    }
}
