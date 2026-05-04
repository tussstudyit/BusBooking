package com.example.busbooking.utils

sealed class UiState<out T> {
    data class Loading<T>(val message: String = "") : UiState<T>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error<T>(val message: String, val code: Int = -1, val throwable: Throwable? = null) : UiState<T>()

    fun getOrNull(): T? = (this as? Success)?.data
    fun isLoading() = this is Loading
    fun isSuccess() = this is Success
    fun isError() = this is Error
}

