package com.example.deuktemsiru_buyer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.data.StoreRepository
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.util.AppError
import com.example.deuktemsiru_buyer.util.Result
import com.example.deuktemsiru_buyer.util.filterStores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val stores: List<Store> = emptyList(),
    val filteredStores: List<Store> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val authError: Boolean = false,
    val selectedCategory: String = "전체",
    val searchQuery: String = "",
)

class HomeViewModel(
    private val repository: StoreRepository = StoreRepository(RetrofitClient.api),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadStores()
    }

    fun loadStores(showLoading: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = showLoading, error = null) }
            when (val result = repository.getStores()) {
                is Result.Success -> updateAndRefilter { it.copy(stores = result.data, isLoading = false) }
                is Result.Error -> {
                    val isAuth = result.error == AppError.AUTH_ERROR
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authError = isAuth,
                            error = if (isAuth) null else errorMessage(result.error, result.httpCode),
                        )
                    }
                }
            }
        }
    }

    fun selectCategory(category: String) = updateAndRefilter { it.copy(selectedCategory = category) }

    fun updateSearch(query: String) = updateAndRefilter { it.copy(searchQuery = query) }

    fun toggleWishlist(store: Store) {
        viewModelScope.launch {
            val optimisticValue = !store.isWishlisted
            updateWishlistState(store.id, optimisticValue)
            when (val result = repository.toggleWishlist(store.id.toLong())) {
                is Result.Success -> {
                    updateWishlistState(store.id, result.data)
                }
                is Result.Error -> {
                    updateWishlistState(store.id, store.isWishlisted)
                    _uiState.update { it.copy(error = "찜 처리 중 오류가 발생했어요.") }
                }
            }
        }
    }

    private fun updateWishlistState(storeId: Long, isWishlisted: Boolean) = updateAndRefilter { state ->
        state.copy(
            stores = state.stores.map {
                if (it.id == storeId) it.copy(isWishlisted = isWishlisted) else it
            },
        )
    }

    /** Applies [change], then keeps filteredStores consistent with the resulting state. */
    private fun updateAndRefilter(change: (HomeUiState) -> HomeUiState) = _uiState.update { state ->
        change(state).let { it.copy(filteredStores = it.stores.filterStores(it.selectedCategory, it.searchQuery)) }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }

    fun authErrorHandled() {
        _uiState.update { it.copy(authError = false) }
    }

    // AUTH_ERROR never reaches here — callers surface it through the authError flag instead.
    private fun errorMessage(error: AppError, httpCode: Int): String = when (error) {
        AppError.NOT_FOUND -> "데이터를 찾을 수 없어요."
        AppError.SERVER_ERROR -> "서버에 일시적인 문제가 있어요. 잠시 후 다시 시도해주세요."
        AppError.NETWORK_ERROR -> "네트워크에 연결할 수 없어요."
        AppError.UNKNOWN, AppError.AUTH_ERROR -> "네트워크 오류가 발생했어요. ($httpCode)"
    }
}
