package com.example.deuktemsiru_buyer.network

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val email: String,
    val nickname: String,
    val password: String,
    val role: String = "BUYER",
)

data class LoginResponse(
    val userId: Long,
    val nickname: String,
    val role: String,
    val token: String,
)

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

data class OrderItemApiResponse(
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

data class UserApiResponse(
    val id: Long,
    val email: String,
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

data class CreateOrderRequest(
    val storeId: Long,
    val items: List<OrderItemRequest>,
    val pickupTime: String,
)

data class OrderItemRequest(
    val menuItemId: Long,
    val quantity: Int,
)
