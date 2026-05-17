package com.example.deuktemsiru_buyer.data

import com.example.deuktemsiru_buyer.network.ApiService
import com.example.deuktemsiru_buyer.util.AppError
import com.example.deuktemsiru_buyer.util.Result
import retrofit2.HttpException

class StoreRepository(private val api: ApiService) {

    private val storeCache = mutableMapOf<Long, Store>()

    suspend fun getStores(category: String? = null): Result<List<Store>> = safeCall {
        val apiCategory = if (category != null) categoryToApi(category) else null
        api.getStores(category = apiCategory).data?.stores
            ?.map { item ->
                val listStore = item.toStore()
                val detailedStore = runCatching { api.getStore(item.storeId).data?.toStore() }.getOrNull()
                val displayStore = detailedStore?.takeIf { it.remainingItems > 0 && it.discountedPrice > 0 } ?: listStore
                displayStore.also { storeCache[item.storeId] = it }
            }
            ?: error("Failed to load stores: API returned null data")
    }

    suspend fun getStore(storeId: Long): Result<Store> = safeCall {
        storeCache[storeId] ?: run {
            val response = api.getStore(storeId).data
                ?: error("Store $storeId not found")
            response.toStore().also { storeCache[storeId] = it }
        }
    }

    fun invalidateCache(storeId: Long? = null) {
        if (storeId == null) storeCache.clear() else storeCache.remove(storeId)
    }

    suspend fun toggleWishlist(storeId: Long): Result<Boolean> = safeCall {
        api.toggleWishlist(storeId).data?.isWishlisted ?: false
    }

    suspend fun getWishlist(): Result<List<Store>> = safeCall {
        api.getWishlist().data?.wishlists?.map { it.toStore() } ?: emptyList()
    }
}

private inline fun <T> safeCall(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: HttpException) {
    val appError = when (e.code()) {
        401, 403 -> AppError.AUTH_ERROR
        404 -> AppError.NOT_FOUND
        in 500..599 -> AppError.SERVER_ERROR
        else -> AppError.UNKNOWN
    }
    Result.Error(appError, httpCode = e.code(), cause = e)
} catch (e: Exception) {
    Result.Error(AppError.NETWORK_ERROR, cause = e)
}
