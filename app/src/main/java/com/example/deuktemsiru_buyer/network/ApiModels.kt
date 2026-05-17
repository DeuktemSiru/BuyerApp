package com.example.deuktemsiru_buyer.network

import com.google.gson.annotations.SerializedName

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

data class DebugLoginRequest(
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

// ── 가게 / 상품 ─────────────────────────────────────────────
data class StoreListResponse(
    val stores: List<StoreListItemResponse>,
    val hasNext: Boolean,
)

data class StoreListItemResponse(
    val storeId: Long,
    val name: String,
    val thumbnailUrl: String?,
    val distanceM: Int,
    val category: String,
    val ratingAvg: Double,
    val reviewCount: Int,
    val availableProductCount: Int,
    val representativeOriginalPrice: Int = 0,
    val representativeDiscountPrice: Int = 0,
    val representativeDiscountRate: Int = 0,
    val representativePickupEnd: String? = null,
)

data class StoreDetailApiResponse(
    val storeId: Long,
    val name: String,
    val description: String?,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val thumbnailUrl: String?,
    val images: List<String>,
    val categories: List<String>,
    val ratingAvg: Double,
    val reviewCount: Int,
    val products: List<StoreProductItem>,
)

data class StoreProductItem(
    val productId: Long,
    val name: String,
    val originalPrice: Int = 0,
    val discountPrice: Int,
    val quantityRemaining: Int,
    val pickupStart: String? = null,
    val pickupEnd: String,
    val status: String,
)

// ── 찜 ─────────────────────────────────────────────────────
data class WishlistToggleResponse(
    val storeId: Long,
    val isWishlisted: Boolean,
)

data class WishlistListResponse(
    val wishlists: List<WishlistItemResponse>,
)

data class WishlistItemResponse(
    val wishlistId: Long,
    val storeId: Long,
    val name: String,
    val thumbnailUrl: String?,
    val ratingAvg: Double,
    val availableProductCount: Int,
)

// ── 주문 ────────────────────────────────────────────────────
data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)

data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
    val paymentMethod: String = "SIRU",
)

data class PaymentInfo(
    val method: String,
    val status: String,
)

data class CreateOrderResponse(
    val orderId: Long,
    val pickupCode: String?,
    val status: String,
    val totalPrice: Int,
    val payment: PaymentInfo,
)

data class OrderListItemResponse(
    val orderId: Long,
    val storeName: String,
    val status: String,
    val totalPrice: Int,
    val pickupCode: String?,
    val createdAt: String,
    val itemCount: Int,
)

data class OrderItemDetailResponse(
    val productId: Long? = null,
    val menuItemId: Long? = null,
    val productName: String,
    val quantity: Int,
    val unitPrice: Int,
)

data class OrderDetailResponse(
    val orderId: Long,
    val orderNumber: String? = null,
    val customerName: String? = null,
    val pickupCode: String?,
    val pickupTime: String? = null,
    val status: String,
    val totalPrice: Int,
    val storeName: String,
    val items: List<OrderItemDetailResponse>,
    val payment: PaymentInfo,
    val createdAt: String? = null,
)

// ── 사용자 ───────────────────────────────────────────────────
data class MemberApiResponse(
    val memberId: Long,
    val email: String,
    val nickname: String,
    val name: String,
    val role: String,
    val profileImageUrl: String?,
    val phone: String?,
    val gender: String?,
    val birth: String?,
    val status: Int,
    @SerializedName(value = "isSiruLinked", alternate = ["siruLinked"])
    val isSiruLinked: Boolean = false,
    val siruBalance: Int = 0,
    val createdAt: String,
)

data class SiruLinkRequest(val siruAccessToken: String)

data class MemberStatsResponse(
    val totalSavedAmount: Int,
    val totalCarbonSavedKg: Double,
    val totalOrders: Int,
)

data class NotificationSettingsResponse(
    val newProduct: Boolean?,
    val pickupReminder: Boolean?,
    val orderConfirmed: Boolean?,
    val newOrder: Boolean?,
    val pickupComplete: Boolean?,
    val soldOut: Boolean?,
    val event: Boolean,
)

data class UpdateNotificationSettingsRequest(
    val newProduct: Boolean? = null,
    val pickupReminder: Boolean? = null,
    val orderConfirmed: Boolean? = null,
    val event: Boolean? = null,
)

// ── 알림 ────────────────────────────────────────────────────
data class NotificationListResponse(
    val notifications: List<NotificationApiResponse>,
    val unreadCount: Int,
)

data class NotificationApiResponse(
    val notificationId: Long,
    val type: String,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val relatedStoreId: Long?,
    val relatedOrderId: Long?,
    val relatedProductId: Long?,
    val createdAt: String,
)

// ── 장바구니 ────────────────────────────────────────────────
data class CartAddRequest(
    val productId: Long,
    val quantity: Int,
)

data class CartUpdateRequest(
    val quantity: Int,
)

data class CartApiItem(
    val cartItemId: Long,
    val productId: Long,
    val productName: String,
    val storeId: Long,
    val storeName: String,
    val storeLatitude: Double = 0.0,
    val storeLongitude: Double = 0.0,
    val originalPrice: Int = 0,
    val discountPrice: Int,
    val pickupStart: String = "",
    val pickupEnd: String = "",
    val quantity: Int,
    val imageUrl: String?,
)

data class CartResponse(
    val items: List<CartApiItem>,
    val totalPrice: Int,
)
