package com.example.deuktemsiru_buyer.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

fun countdownFlow(startSeconds: Long) = flow {
    var remaining = startSeconds
    while (remaining >= 0) {
        emit(remaining)
        if (remaining == 0L) break
        delay(1_000)
        remaining--
    }
}
