package com.example.busbooking.data.session

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore instance
private val Context.sessionDataStore by preferencesDataStore("user_session")

object SessionPreferences {
    val USER_ID = longPreferencesKey("user_id")
    val EMAIL = stringPreferencesKey("email")
    val NAME = stringPreferencesKey("name")
    val ROLE = stringPreferencesKey("role")
}

// SIMPLE SESSION MANAGER (ĐỦ CHO ĐỒ ÁN)
class SessionManager(private val context: Context) {

    private val dataStore = context.sessionDataStore

    // ================= LOGIN =================

    suspend fun saveSession(
        userId: Long,
        email: String,
        name: String,
        role: String
    ) {
        dataStore.edit {
            it[SessionPreferences.USER_ID] = userId
            it[SessionPreferences.EMAIL] = email
            it[SessionPreferences.NAME] = name
            it[SessionPreferences.ROLE] = role
        }
    }

    // ================= GET DATA =================

    suspend fun getUserId(): Long {
        return dataStore.data.map {
            it[SessionPreferences.USER_ID] ?: -1L
        }.first()
    }

    suspend fun getUserEmail(): String {
        return dataStore.data.map {
            it[SessionPreferences.EMAIL] ?: ""
        }.first()
    }

    suspend fun getUserRole(): String {
        return dataStore.data.map {
            it[SessionPreferences.ROLE] ?: "USER"
        }.first()
    }

    // ================= CHECK =================

    suspend fun isLoggedIn(): Boolean {
        return getUserId() != -1L
    }

    suspend fun isAdmin(): Boolean {
        return getUserRole() == "ADMIN"
    }

    // ================= LOGOUT =================

    suspend fun logout() {
        dataStore.edit { it.clear() }
    }
}