package com.example.deuktemsiru_buyer.ui.payment

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val SLOT_LABEL = DateTimeFormatter.ofPattern("HH:mm")

internal fun pickupTimeSlots(startTime: String?, endTime: String?): List<String> {
    val end = endTime.toPickupLocalTime() ?: return emptyList()
    val start = startTime.toPickupLocalTime() ?: return listOf(end.format(SLOT_LABEL))
    if (start.isAfter(end)) return emptyList()

    val steps = Duration.between(start, end).toMinutes() / 30
    return buildList {
        for (step in 0..steps) add(start.plusMinutes(step * 30).format(SLOT_LABEL))
        val deadline = end.format(SLOT_LABEL)
        if (lastOrNull() != deadline) add(deadline)
    }
}

private fun String?.toPickupLocalTime(): LocalTime? {
    val value = this?.substringAfter("T")?.substringBefore(".") ?: return null
    return runCatching { LocalTime.parse(value) }.getOrNull()
}
