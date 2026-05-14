package com.example.deuktemsiru_buyer.network

import retrofit2.http.*

interface ApiService {

    // ── 인증 ────────────────────────────────────────────────
    @POST("api/v1/auth/kakao/login")
    suspend fun kakaoLogin(@Body req: KakaoLoginRequest): ApiResponse<LoginData>

    @POST("api/v1/auth/debug/login")
    suspend fun debugLogin(@Body req: DebugLoginRequest): ApiResponse<LoginData>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body req: TokenRefreshRequest): ApiResponse<TokenData>

    @POST("api/v1/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    // ── 가게 ────────────────────────────────────────────────
    @GET("api/v1/stores")
    suspend fun getStores(
        @Query("category") category: String? = null,
    ): ApiResponse<List<StoreApiResponse>>

    @GET("api/v1/stores/{storeId}")
    suspend fun getStore(
        @Path("storeId") storeId: Long,
    ): ApiResponse<StoreApiResponse>

    // ── 찜 ──────────────────────────────────────────────────
    @POST("api/v1/wishlist/{storeId}")
    suspend fun toggleWishlist(
        @Path("storeId") storeId: Long,
    ): ApiResponse<Map<String, Any>>

    @GET("api/v1/wishlist")
    suspend fun getWishlist(): ApiResponse<List<StoreApiResponse>>

    // ── 주문 ────────────────────────────────────────────────
    @POST("api/v1/orders")
    suspend fun createOrder(@Body req: CreateOrderRequest): ApiResponse<OrderApiResponse>

    @GET("api/v1/orders")
    suspend fun getOrders(): ApiResponse<List<OrderApiResponse>>

    @GET("api/v1/orders/{orderId}")
    suspend fun getOrder(@Path("orderId") orderId: Long): ApiResponse<OrderApiResponse>

    // ── 사용자 ───────────────────────────────────────────────
    @GET("api/v1/users/me")
    suspend fun getMe(): ApiResponse<UserApiResponse>

    // ── 알림 ────────────────────────────────────────────────
    @GET("api/v1/notifications")
    suspend fun getNotifications(): ApiResponse<List<NotificationApiResponse>>
}
