package com.example.busbooking.staff.session

import android.content.Context
import com.example.busbooking.staff.data.model.StaffUser

object StaffSessionManager {
    private const val PREFS = "staff_session"
    private const val KEY_ID = "id"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHONE = "phone"
    private const val KEY_ROLE = "role"
    private const val KEY_COMPANY = "company"

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun saveUser(user: StaffUser) {
        prefs().edit()
            .putLong(KEY_ID, user.id)
            .putString(KEY_NAME, user.name)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, user.phone)
            .putString(KEY_ROLE, user.role)
            .putString(KEY_COMPANY, user.companyName)
            .apply()
    }

    fun currentUser(): StaffUser? {
        val id = prefs().getLong(KEY_ID, 0L)
        if (id <= 0L) return null
        return StaffUser(
            id = id,
            name = prefs().getString(KEY_NAME, "").orEmpty(),
            email = prefs().getString(KEY_EMAIL, "").orEmpty(),
            phone = prefs().getString(KEY_PHONE, "").orEmpty(),
            role = prefs().getString(KEY_ROLE, "").orEmpty(),
            companyName = prefs().getString(KEY_COMPANY, "").orEmpty()
        )
    }

    fun currentStaffId(): Long = prefs().getLong(KEY_ID, 0L)

    fun clear() {
        prefs().edit().clear().apply()
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
