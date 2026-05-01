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

object SampleData {

    val stores: List<Store> = listOf(
        Store(
            id = 1,
            name = "파리바게뜨 정왕점",
            category = "베이커리",
            emoji = "🥐",
            rating = 4.8f,
            walkingMinutes = 4,
            discountRate = 60,
            originalPrice = 15000,
            discountedPrice = 5900,
            remainingItems = 3,
            minutesUntilClose = 22,
            address = "경기도 시흥시 정왕동 1234-56",
            phone = "031-123-4567",
            menus = listOf(
                MenuItem(1, "아침 세트 A", "🥐", 12000, 4800, 60, 2),
                MenuItem(2, "크루아상 2개", "🥖", 7000, 2800, 60, 1),
                MenuItem(3, "식빵 세트", "🍞", 8000, 3200, 60, 3),
                MenuItem(4, "마감 랜덤 박스", "📦", 20000, 6000, 70, 1)
            )
        ),
        Store(
            id = 2,
            name = "한솥 도시락 시흥점",
            category = "도시락",
            emoji = "🍱",
            rating = 4.5f,
            walkingMinutes = 7,
            discountRate = 50,
            originalPrice = 8500,
            discountedPrice = 4200,
            remainingItems = 5,
            minutesUntilClose = 48,
            address = "경기도 시흥시 정왕동 789-12",
            phone = "031-234-5678",
            menus = listOf(
                MenuItem(5, "불고기 도시락", "🍱", 8500, 4200, 50, 3),
                MenuItem(6, "참치마요 도시락", "🐟", 7500, 3700, 51, 2),
                MenuItem(7, "제육볶음 도시락", "🥩", 9000, 4500, 50, 1, isSoldOut = true)
            )
        ),
        Store(
            id = 3,
            name = "그린샐러드 키친",
            category = "샐러드",
            emoji = "🥗",
            rating = 4.6f,
            walkingMinutes = 6,
            discountRate = 50,
            originalPrice = 12000,
            discountedPrice = 5900,
            remainingItems = 2,
            minutesUntilClose = 15,
            address = "경기도 시흥시 정왕동 456-78",
            phone = "031-345-6789",
            menus = listOf(
                MenuItem(8, "시저 샐러드", "🥗", 12000, 5900, 51, 1),
                MenuItem(9, "콥 샐러드", "🌽", 13000, 6400, 51, 1)
            )
        ),
        Store(
            id = 4,
            name = "스타벅스 정왕역점",
            category = "카페",
            emoji = "☕",
            rating = 4.3f,
            walkingMinutes = 3,
            discountRate = 30,
            originalPrice = 10000,
            discountedPrice = 6900,
            remainingItems = 4,
            minutesUntilClose = 55,
            address = "경기도 시흥시 정왕동 100-5",
            phone = "031-456-7890",
            menus = listOf(
                MenuItem(10, "오늘의 케이크 2종", "🍰", 10000, 6900, 31, 2),
                MenuItem(11, "샌드위치 세트", "🥪", 8500, 5900, 31, 2)
            )
        ),
        Store(
            id = 5,
            name = "뚜레쥬르 시흥중앙점",
            category = "베이커리",
            emoji = "🍩",
            rating = 4.4f,
            walkingMinutes = 9,
            discountRate = 70,
            originalPrice = 18000,
            discountedPrice = 5400,
            remainingItems = 6,
            minutesUntilClose = 35,
            address = "경기도 시흥시 중앙동 200-15",
            phone = "031-567-8901",
            menus = listOf(
                MenuItem(12, "케이크 조각 3개", "🍰", 9000, 2700, 70, 3),
                MenuItem(13, "마감 빵 봉투", "🥖", 12000, 3600, 70, 2),
                MenuItem(14, "도넛 세트", "🍩", 8000, 2400, 70, 1)
            )
        ),
        Store(
            id = 6,
            name = "오니기리 정왕",
            category = "도시락",
            emoji = "🍙",
            rating = 4.7f,
            walkingMinutes = 5,
            discountRate = 50,
            originalPrice = 9000,
            discountedPrice = 4500,
            remainingItems = 8,
            minutesUntilClose = 42,
            address = "경기도 시흥시 정왕동 333-22",
            phone = "031-678-9012",
            menus = listOf(
                MenuItem(15, "오니기리 3개 세트", "🍙", 9000, 4500, 50, 4),
                MenuItem(16, "유부초밥 세트", "🍣", 11000, 5500, 50, 2)
            )
        )
    )

    fun getStoreById(id: Int): Store? = stores.find { it.id == id }

    fun formatPrice(price: Int): String {
        return "%,d원".format(price)
    }
}

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

fun categoryToDisplay(category: String) = when (category) {
    "BAKERY" -> "베이커리"
    "LUNCHBOX" -> "도시락"
    "SALAD" -> "샐러드"
    "CAFE" -> "카페"
    else -> category
}

fun categoryToApi(display: String) = when (display) {
    "베이커리" -> "BAKERY"
    "도시락" -> "LUNCHBOX"
    "샐러드" -> "SALAD"
    "카페" -> "CAFE"
    else -> null
}

private fun minutesUntilClose(closingTime: String): Int {
    val parts = closingTime.split(":")
    val closeHour = parts.getOrNull(0)?.toIntOrNull() ?: return 60
    val closeMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val now = Calendar.getInstance()
    val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val closeMins = closeHour * 60 + closeMin
    return (closeMins - nowMins).coerceAtLeast(0)
}
