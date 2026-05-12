package com.example.deuktemsiru_buyer.data

import com.example.deuktemsiru_buyer.network.MenuItemApiResponse
import com.example.deuktemsiru_buyer.network.StoreApiResponse
import java.util.Calendar

data class Store(
    val id: Int,
    val name: String,
    val category: String,
    val emoji: String,
    val rating: Float,
    val walkingMinutes: Int,
    val discountRate: Int,
    val originalPrice: Int,
    val discountedPrice: Int,
    val remainingItems: Int,
    val minutesUntilClose: Int,
    val address: String,
    val phone: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var isWishlisted: Boolean = false,
    val menus: List<MenuItem> = emptyList()
)

data class MenuItem(
    val id: Int,
    val name: String,
    val emoji: String,
    val originalPrice: Int,
    val discountedPrice: Int,
    val discountRate: Int,
    val remainingItems: Int,
    val isSoldOut: Boolean = false
)

fun StoreApiResponse.toStore(): Store {
    val rep = menus.firstOrNull { !it.isSoldOut } ?: menus.firstOrNull()
    return Store(
        id = id.toInt(),
        name = name,
        category = categoryToDisplay(category),
        emoji = emoji,
        rating = rating,
        walkingMinutes = 5,
        discountRate = rep?.discountRate ?: 0,
        originalPrice = rep?.originalPrice ?: 0,
        discountedPrice = rep?.discountedPrice ?: 0,
        remainingItems = menus.sumOf { it.remainingItems },
        minutesUntilClose = minutesUntilClose(closingTime),
        address = address,
        phone = phone,
        latitude = latitude,
        longitude = longitude,
        isWishlisted = isWishlisted,
        menus = menus.map { it.toMenuItem() },
    )
}

fun MenuItemApiResponse.toMenuItem() = MenuItem(
    id = id.toInt(),
    name = name,
    emoji = emoji,
    originalPrice = originalPrice,
    discountedPrice = discountedPrice,
    discountRate = discountRate,
    remainingItems = remainingItems,
    isSoldOut = isSoldOut,
)

private val categoryLabels = mapOf(
    "BAKERY" to "베이커리",
    "RESTAURANT" to "음식점",
    "CAFE" to "카페",
)

fun categoryToDisplay(category: String) = categoryLabels[category] ?: category

fun categoryToApi(display: String) =
    categoryLabels.entries.firstOrNull { it.value == display }?.key

private fun minutesUntilClose(closingTime: String): Int {
    val parts = closingTime.split(":")
    val closeHour = parts.getOrNull(0)?.toIntOrNull() ?: return 60
    val closeMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val now = Calendar.getInstance()
    val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val closeMins = closeHour * 60 + closeMin
    return (closeMins - nowMins).coerceAtLeast(0)
}
