package com.example.deuktemsiru_buyer.network

// ── 공통 응답 래퍼 ──────────────────────────────────────────
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
)

// ── 인증 ────────────────────────────────────────────────────
data class KakaoLoginRequest(
    val kakaoAccessToken: String,
    val role: String = "CONSUMER",
)

data class TokenRefreshRequest(val refreshToken: String)

data class MemberSummary(
    val memberId: Long,
    val nickname: String,
    val role: String,
)

data class LoginData(
    val accessToken: String,
    val refreshToken: String,
    val member: MemberSummary,
)

data class TokenData(val accessToken: String)

// ── 메뉴 / 상품 ─────────────────────────────────────────────
data class MenuItemApiResponse(
    val id: Long,
    val name: String,
    val emoji: String,
    val originalPrice: Int,
    val discountedPrice: Int,
    val discountRate: Int,
    val remainingItems: Int,
    val isSoldOut: Boolean,
    val pickupTimeSlot: String,
)

// ── 가게 ────────────────────────────────────────────────────
data class StoreApiResponse(
    val id: Long,
    val name: String,
    val category: String,
    val emoji: String,
    val rating: Float,
    val address: String,
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val closingTime: String,
    val isWishlisted: Boolean,
    val menus: List<MenuItemApiResponse>,
)

// ── 주문 ────────────────────────────────────────────────────
data class OrderItemApiResponse(
    val productId: Long,
    val menuItemId: Long,
    val name: String,
    val emoji: String,
    val quantity: Int,
    val price: Int,
)

data class OrderApiResponse(
    val id: Long,
    val orderNumber: String,
    val storeId: Long,
    val storeName: String,
    val status: String,
    val pickupCode: String,
    val pickupTime: String,
    val totalAmount: Int,
    val createdAt: String,
    val items: List<OrderItemApiResponse>,
)

// ── 사용자 ───────────────────────────────────────────────────
data class UserApiResponse(
    val id: Long,
    val nickname: String,
    val role: String,
    val grade: String,
    val totalSavings: Int,
    val points: Int,
    val couponCount: Int,
    val co2Saved: Float,
)

data class NotificationApiResponse(
    val id: Long,
    val storeId: Long,
    val storeName: String,
    val message: String,
    val sentAt: String,
    val recipientCount: Int,
)

// ── 주문 요청 ────────────────────────────────────────────────
data class CreateOrderRequest(
    val storeId: Long,
    val items: List<OrderItemRequest>,
    val pickupTime: String,
)

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)
