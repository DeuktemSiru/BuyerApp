package com.example.deuktemsiru_buyer.network

import retrofit2.http.*

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): UserApiResponse

    @GET("api/stores")
    suspend fun getStores(
        @Query("category") category: String? = null,
        @Query("userId") userId: Long? = null,
    ): List<StoreApiResponse>

    @GET("api/stores/{storeId}")
    suspend fun getStore(
        @Path("storeId") storeId: Long,
        @Query("userId") userId: Long? = null,
    ): StoreApiResponse

    @POST("api/wishlist/{storeId}")
    suspend fun toggleWishlist(
        @Path("storeId") storeId: Long,
        @Query("userId") userId: Long,
    ): Map<String, Any>

    @GET("api/wishlist")
    suspend fun getWishlist(@Query("userId") userId: Long): List<StoreApiResponse>

    @POST("api/orders")
    suspend fun createOrder(
        @Query("buyerId") buyerId: Long,
        @Body req: CreateOrderRequest,
    ): OrderApiResponse

    @GET("api/orders")
    suspend fun getOrders(@Query("buyerId") buyerId: Long): List<OrderApiResponse>

    @GET("api/orders/{orderId}")
    suspend fun getOrder(@Path("orderId") orderId: Long): OrderApiResponse

    @GET("api/users/{userId}")
    suspend fun getUser(@Path("userId") userId: Long): UserApiResponse

    @GET("api/notifications")
    suspend fun getNotifications(@Query("userId") userId: Long): List<NotificationApiResponse>
}
