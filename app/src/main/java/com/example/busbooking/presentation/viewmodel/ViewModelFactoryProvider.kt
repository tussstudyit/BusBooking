package com.example.busbooking.presentation.viewmodel

import android.content.Context
import com.example.busbooking.data.db.BusBookingDatabase
import com.example.busbooking.domain.repository.*

object ViewModelFactoryProvider {

    fun authFactory(context: Context) = ViewModelFactory {
        val db = BusBookingDatabase.getInstance(context)
        AuthViewModel(AuthRepository(db.userDao()))
    }

    fun routeSearchFactory(context: Context) = ViewModelFactory {
        val db = BusBookingDatabase.getInstance(context)
        RouteSearchViewModel(RouteRepository(db.routeDao()))
    }

    // thêm các ViewModel khác ở đây sau
}