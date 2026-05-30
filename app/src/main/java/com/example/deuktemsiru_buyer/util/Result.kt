package com.example.deuktemsiru_buyer.util

enum class AppError {
    AUTH_ERROR,
    NOT_FOUND,
    SERVER_ERROR,
    NETWORK_ERROR,
    UNKNOWN,
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(
        val error: AppError,
        /** HTTP status code when available, -1 otherwise. */
        val httpCode: Int = -1,
    ) : Result<Nothing>()
}
