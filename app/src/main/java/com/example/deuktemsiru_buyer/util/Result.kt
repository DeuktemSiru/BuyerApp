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
        val cause: Throwable? = null,
    ) : Result<Nothing>() {
        /** Legacy convenience accessor so existing call sites keep compiling. */
        val message: String get() = error.name.lowercase()
    }
    data object Loading : Result<Nothing>()
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (String, Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) action(message, cause)
    return this
}
