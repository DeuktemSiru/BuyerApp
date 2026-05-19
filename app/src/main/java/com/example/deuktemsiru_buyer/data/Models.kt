package com.example.deuktemsiru_buyer.data

import com.example.deuktemsiru_buyer.network.StoreDetailApiResponse
import com.example.deuktemsiru_buyer.network.StoreListItemResponse
import com.example.deuktemsiru_buyer.network.StoreProductItem
import com.example.deuktemsiru_buyer.network.WishlistItemResponse
import com.example.deuktemsiru_buyer.network.CartApiItem
import java.util.Calendar

data class Store(
    val id: Long,
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
    val id: Long,
    val name: String,
    val emoji: String,
    val originalPrice: Int,
    val discountedPrice: Int,
    val discountRate: Int,
    val remainingItems: Int,
    val pickupStart: String = "",
    val pickupEnd: String = "",
    val isSoldOut: Boolean = false
)

private object StoreDefaults {
    const val WALKING_MINUTES = 5
    const val CLOSE_MINUTES = 60
}

val storeCategoryFilters = listOf("전체", "한식", "양식", "카페·디저트", "베이커리", "카페")

fun StoreDetailApiResponse.toStore(isWishlisted: Boolean = this.isWishlisted): Store {
    val menus = products.map { it.toMenuItem() }
    val rep = menus.firstOrNull { !it.isSoldOut } ?: menus.firstOrNull()
    return Store(
        id = storeId,
        name = name,
        category = categoryToDisplay(categories.firstOrNull() ?: "OTHER"),
        emoji = categoryEmoji(categories.firstOrNull()),
        rating = ratingAvg.toFloat(),
        walkingMinutes = StoreDefaults.WALKING_MINUTES,
        discountRate = rep?.discountRate ?: 0,
        originalPrice = rep?.originalPrice ?: 0,
        discountedPrice = rep?.discountedPrice ?: 0,
        remainingItems = products.sumOf { it.quantityRemaining },
        minutesUntilClose = products.map { minutesUntilClose(it.pickupEnd) }.filter { it > 0 }.minOrNull() ?: 0,
        address = address,
        phone = phone.orEmpty(),
        latitude = latitude,
        longitude = longitude,
        isWishlisted = isWishlisted,
        menus = menus,
    )
}

fun StoreListItemResponse.toStore() = toStoreSummary(
    storeId = storeId,
    name = name,
    category = category,
    ratingAvg = ratingAvg,
    walkingMinutes = (distanceM / 80).coerceAtLeast(1),
    representativeDiscountRate = representativeDiscountRate,
    representativeOriginalPrice = representativeOriginalPrice,
    representativeDiscountPrice = representativeDiscountPrice,
    availableProductCount = availableProductCount,
    representativePickupEnd = representativePickupEnd,
    isWishlisted = isWishlisted,
)

fun WishlistItemResponse.toStore() = toStoreSummary(
    storeId = storeId,
    name = name,
    category = category,
    ratingAvg = ratingAvg,
    walkingMinutes = StoreDefaults.WALKING_MINUTES,
    representativeDiscountRate = representativeDiscountRate,
    representativeOriginalPrice = representativeOriginalPrice,
    representativeDiscountPrice = representativeDiscountPrice,
    availableProductCount = availableProductCount,
    representativePickupEnd = representativePickupEnd,
    isWishlisted = true,
)

private fun toStoreSummary(
    storeId: Long,
    name: String,
    category: String,
    ratingAvg: Double,
    walkingMinutes: Int,
    representativeDiscountRate: Int,
    representativeOriginalPrice: Int,
    representativeDiscountPrice: Int,
    availableProductCount: Int,
    representativePickupEnd: String?,
    isWishlisted: Boolean,
) = Store(
    id = storeId,
    name = name,
    category = categoryToDisplay(category),
    emoji = categoryEmoji(category),
    rating = ratingAvg.toFloat(),
    walkingMinutes = walkingMinutes,
    discountRate = representativeDiscountRate,
    originalPrice = representativeOriginalPrice,
    discountedPrice = representativeDiscountPrice,
    remainingItems = availableProductCount,
    minutesUntilClose = representativePickupEnd?.let { minutesUntilClose(it) } ?: StoreDefaults.CLOSE_MINUTES,
    address = "",
    phone = "",
    isWishlisted = isWishlisted,
)

fun StoreProductItem.toMenuItem(): MenuItem {
    val original = when {
        originalPrice > 0 -> originalPrice
        discountPrice > 0 -> (discountPrice / 0.7).toInt()
        else -> 0
    }
    return MenuItem(
        id = productId,
        name = name,
        emoji = "🍽",
        originalPrice = original,
        discountedPrice = discountPrice,
        discountRate = if (original > 0) ((original - discountPrice) * 100 / original) else 0,
        remainingItems = quantityRemaining,
        pickupStart = pickupStart.orEmpty(),
        pickupEnd = pickupEnd,
        isSoldOut = status == "SOLD_OUT" || quantityRemaining <= 0,
    )
}

fun StoreProductItem.pickupMinutesUntilClose() = minutesUntilClose(pickupEnd)

fun CartApiItem.toCartItem() = CartItem(
    menuId = productId,
    menuName = productName,
    emoji = "🛍️",
    originalPrice = originalPrice,
    discountedPrice = discountPrice,
    pickupStart = pickupStart,
    pickupEnd = pickupEnd,
    quantity = quantity,
)

private fun categoryEmoji(category: String?) = when (category) {
    "BAKERY" -> "🥐"
    "CAFE" -> "☕"
    "RESTAURANT" -> "🍱"
    "GROCERY" -> "🥦"
    else -> "🍽"
}

private val categoryLabels = mapOf(
    "BAKERY" to "베이커리",
    "RESTAURANT" to "음식점",
    "CAFE" to "카페",
    "GROCERY" to "식료품",
    "OTHER" to "기타",
)

private val categoryAliases = mapOf(
    "한식" to "RESTAURANT",
    "양식" to "RESTAURANT",
    "카페·디저트" to "CAFE",
)

fun categoryToDisplay(category: String) = categoryLabels[category] ?: category

fun categoryToApi(display: String) =
    categoryAliases[display] ?: categoryLabels.entries.firstOrNull { it.value == display }?.key

internal fun minutesUntilClose(closingTime: String): Int {
    val time = closingTime.substringAfter("T", closingTime).substringBefore(".")
    val parts = time.split(":")
    val closeHour = parts.getOrNull(0)?.toIntOrNull() ?: return 60
    val closeMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val now = Calendar.getInstance()
    val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val closeMins = closeHour * 60 + closeMin
    return if (closeMins >= nowMins) closeMins - nowMins
           else 24 * 60 - nowMins + closeMins
}
