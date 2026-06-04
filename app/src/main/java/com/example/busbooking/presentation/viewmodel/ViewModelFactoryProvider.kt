package com.example.busbooking.presentation.viewmodel

import android.content.Context
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.domain.repository.RouteRepository

object ViewModelFactoryProvider {

    fun authFactory(context: Context) = ViewModelFactory {
        AuthViewModel(AuthRepository())
    }

    fun routeSearchFactory(context: Context) = ViewModelFactory {
        RouteSearchViewModel(RouteRepository())
    }
}
