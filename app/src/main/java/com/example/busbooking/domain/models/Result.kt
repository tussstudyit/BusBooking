package com.example.busbooking.domain.models

/**
 * Sealed Result class for handling success/error states in repositories
 * Prevents null pointer exceptions and enforces exhaustive when expressions
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String = exception.message ?: "Unknown error") : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * Specific result type for booking operations
 * Distinguishes between:
 * - AlreadyBooked: Seat was taken (should show specific message)
 * - Failure: Other errors (network, database, etc.)
 * - Success: Booking created successfully
 */
sealed class BookingResult {
    data class Success(val ticketId: Long) : BookingResult()
    object AlreadyBooked : BookingResult()
    object InvalidSeat : BookingResult()
    object InvalidTrip : BookingResult()
    data class Failure(val exception: Exception) : BookingResult()
}

/**
 * Generic operation result for actions that don't return data
 */
sealed class OperationResult {
    object Success : OperationResult()
    data class Failure(val exception: Exception) : OperationResult()
}

/**
 * Enhanced result with loading state
 */
sealed class AsyncResult<out T> {
    object Loading : AsyncResult<Nothing>()
    data class Success<T>(val data: T) : AsyncResult<T>()
    data class Error(val exception: Exception) : AsyncResult<Nothing>()
}

