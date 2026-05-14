package com.example.deuktemsiru_buyer.data

import com.example.deuktemsiru_buyer.network.ApiService
import com.example.deuktemsiru_buyer.util.Result
import retrofit2.HttpException

class StoreRepository(private val api: ApiService) {

    suspend fun getStores(category: String? = null): Result<List<Store>> = safeCall {
        val apiCategory = if (category != null) categoryToApi(category) else null
        api.getStores(category = apiCategory).data?.stores
            ?.map { item ->
                runCatching {
                    api.getStore(item.storeId).data?.toStore()
                }.getOrNull() ?: item.toStore()
            }
            ?: emptyList()
    }

    suspend fun getStore(storeId: Long): Result<Store> = safeCall {
        val response = api.getStore(storeId).data
            ?: error("Store $storeId not found")
        response.toStore()
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
    val msg = when (e.code()) {
        401, 403 -> "auth_error"
        404 -> "데이터를 찾을 수 없어요."
        in 500..599 -> "서버에 일시적인 문제가 있어요. 잠시 후 다시 시도해주세요."
        else -> "네트워크 오류가 발생했어요. (${e.code()})"
    }
    Result.Error(msg, e)
} catch (e: Exception) {
    Result.Error("네트워크에 연결할 수 없어요.", e)
}
