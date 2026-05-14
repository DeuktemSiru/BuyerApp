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

    @POST("api/v1/auth/siru/link")
    suspend fun linkSiru(@Body req: SiruLinkRequest): ApiResponse<MemberApiResponse>

    @DELETE("api/v1/auth/siru/link")
    suspend fun unlinkSiru(): ApiResponse<MemberApiResponse>

    // ── 가게 ────────────────────────────────────────────────
    @GET("api/v1/stores")
    suspend fun getStores(
        @Query("latitude") latitude: Double = 37.3799,
        @Query("longitude") longitude: Double = 126.8031,
        @Query("radius") radius: Int = 20000,
        @Query("category") category: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("sort") sort: String = "distance",
    ): ApiResponse<StoreListResponse>

    @GET("api/v1/stores/{storeId}")
    suspend fun getStore(
        @Path("storeId") storeId: Long,
    ): ApiResponse<StoreDetailApiResponse>

    // ── 찜 ──────────────────────────────────────────────────
    @POST("api/v1/wishlist/{storeId}")
    suspend fun toggleWishlist(
        @Path("storeId") storeId: Long,
    ): ApiResponse<WishlistToggleResponse>

    @GET("api/v1/wishlist")
    suspend fun getWishlist(): ApiResponse<WishlistListResponse>

    // ── 주문 ────────────────────────────────────────────────
    @POST("api/v1/orders")
    suspend fun createOrder(@Body req: CreateOrderRequest): ApiResponse<CreateOrderResponse>

    @GET("api/v1/orders")
    suspend fun getOrders(): ApiResponse<List<OrderListItemResponse>>

    @GET("api/v1/orders/{orderId}")
    suspend fun getOrder(@Path("orderId") orderId: Long): ApiResponse<OrderDetailResponse>

    @PATCH("api/v1/orders/{orderId}/cancel")
    suspend fun cancelOrder(@Path("orderId") orderId: Long): ApiResponse<OrderDetailResponse>

    // ── 장바구니 ─────────────────────────────────────────────
    @POST("api/v1/cart")
    suspend fun addToCart(@Body req: CartAddRequest): ApiResponse<CartApiItem>

    @GET("api/v1/cart")
    suspend fun getCart(): ApiResponse<CartResponse>

    @DELETE("api/v1/cart/{cartItemId}")
    suspend fun removeCartItem(@Path("cartItemId") cartItemId: Long): ApiResponse<Unit>

    @DELETE("api/v1/cart")
    suspend fun clearCart(): ApiResponse<Unit>

    // ── 사용자 ───────────────────────────────────────────────
    @GET("api/v1/members/me")
    suspend fun getMe(): ApiResponse<MemberApiResponse>

    @GET("api/v1/members/me/stats")
    suspend fun getMyStats(): ApiResponse<MemberStatsResponse>

    @GET("api/v1/members/me/notification-settings")
    suspend fun getNotificationSettings(): ApiResponse<NotificationSettingsResponse>

    @PUT("api/v1/members/me/notification-settings")
    suspend fun updateNotificationSettings(
        @Body req: UpdateNotificationSettingsRequest,
    ): ApiResponse<NotificationSettingsResponse>

    // ── 알림 ────────────────────────────────────────────────
    @GET("api/v1/notifications")
    suspend fun getNotifications(): ApiResponse<NotificationListResponse>

    @PATCH("api/v1/notifications/{notificationId}/read")
    suspend fun markNotificationRead(@Path("notificationId") notificationId: Long): ApiResponse<Unit>

    @DELETE("api/v1/notifications/{notificationId}")
    suspend fun deleteNotification(@Path("notificationId") notificationId: Long): ApiResponse<Unit>
}
