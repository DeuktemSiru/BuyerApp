package com.example.deuktemsiru_buyer.data

import com.example.deuktemsiru_buyer.network.ApiService
import com.example.deuktemsiru_buyer.network.CartAddRequest
import com.example.deuktemsiru_buyer.network.CartUpdateRequest

class CartRepository(
    private val api: ApiService,
    private val session: SessionManager,
) {
    suspend fun loadServerCart(): Boolean {
        if (!session.isLoggedIn()) return false
        return runCatching {
            val items = api.getCart().data?.items.orEmpty()
            if (items.isEmpty()) {
                CartManager.clear()
                return@runCatching true
            }

            val first = items.first()
            CartManager.replaceFromServer(
                storeId = first.storeId,
                storeName = first.storeName,
                storeLat = first.storeLatitude,
                storeLng = first.storeLongitude,
                items = items.map { it.toCartItem() },
                serverIds = items.associate { it.productId to it.cartItemId },
            )
            true
        }.getOrDefault(false)
    }

    suspend fun addProduct(productId: Long): Boolean {
        if (!session.isLoggedIn()) return true
        return runCatching {
            val item = api.addToCart(CartAddRequest(productId = productId, quantity = 1)).data
            if (item != null) CartManager.addServerCartItemId(item.productId, item.cartItemId)
            item != null
        }.getOrDefault(false)
    }

    suspend fun removeProduct(productId: Long): Boolean =
        withServerCartItem(productId) { api.removeCartItem(it) }

    suspend fun setQuantity(productId: Long, quantity: Int): Boolean =
        withServerCartItem(productId) { api.updateCartItem(it, CartUpdateRequest(quantity)) }

    suspend fun clearServerCart() {
        if (session.isLoggedIn()) runCatching { api.clearCart() }
    }

    private suspend fun withServerCartItem(productId: Long, action: suspend (Long) -> Unit): Boolean {
        val cartItemId = CartManager.serverCartItemIds[productId] ?: return true
        if (!session.isLoggedIn()) return true
        return runCatching { action(cartItemId) }
            .onFailure { loadServerCart() }
            .isSuccess
    }
}
