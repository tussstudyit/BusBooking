package com.example.busbooking.presentation.ui.state

import com.example.busbooking.data.relations.TripWithRouteAndBus

sealed class TripState {
    object Idle : TripState()
    object Loading : TripState()
    data class SearchSuccess(val trips: List<TripWithRouteAndBus>) : TripState()
    data class DetailSuccess(val trip: TripWithRouteAndBus) : TripState()
    data class Empty(val message: String = "Không tìm thấy chuyến xe") : TripState()
    data class Error(val message: String) : TripState()
}