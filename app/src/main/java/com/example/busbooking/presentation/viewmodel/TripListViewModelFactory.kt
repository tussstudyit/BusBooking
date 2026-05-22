package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.busbooking.domain.repository.ITripRepository

class TripListViewModelFactory(
    private val tripRepository: ITripRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripListViewModel(tripRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
