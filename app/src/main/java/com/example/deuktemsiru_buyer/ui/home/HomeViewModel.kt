package com.example.deuktemsiru_buyer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.data.StoreRepository
import com.example.deuktemsiru_buyer.util.AppError
import com.example.deuktemsiru_buyer.util.Result
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

class HomeViewModel(private val repository: StoreRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadStores(null)
    }

    fun loadStores(category: String?) {
        val cat = if (category == "전체") null else category
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getStores(cat)) {
                is Result.Success -> {
                    val stores = result.data
                    _uiState.update { state ->
                        state.copy(
                            stores = stores,
                            filteredStores = filterStores(stores, state.searchQuery),
                            isLoading = false,
                        )
                    }
                }
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
                is Result.Loading -> Unit
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadStores(if (category == "전체") null else category)
    }

    fun updateSearch(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredStores = filterStores(state.stores, query),
            )
        }
    }

    fun toggleWishlist(store: Store) {
        viewModelScope.launch {
            when (val result = repository.toggleWishlist(store.id.toLong())) {
                is Result.Success -> {
                    val isWishlisted = result.data
                    _uiState.update { state ->
                        val updated = state.stores.map {
                            if (it.id == store.id) it.copy(isWishlisted = isWishlisted) else it
                        }
                        state.copy(
                            stores = updated,
                            filteredStores = filterStores(updated, state.searchQuery),
                        )
                    }
                }
                is Result.Error -> _uiState.update { it.copy(error = "찜 처리 중 오류가 발생했어요.") }
                is Result.Loading -> Unit
            }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }

    fun authErrorHandled() {
        _uiState.update { it.copy(authError = false) }
    }

    private fun errorMessage(error: AppError, httpCode: Int): String = when (error) {
        AppError.NOT_FOUND -> "데이터를 찾을 수 없어요."
        AppError.SERVER_ERROR -> "서버에 일시적인 문제가 있어요. 잠시 후 다시 시도해주세요."
        AppError.NETWORK_ERROR -> "네트워크에 연결할 수 없어요."
        AppError.UNKNOWN -> "네트워크 오류가 발생했어요. ($httpCode)"
        AppError.AUTH_ERROR -> null // handled as authError flag, never shown as message
    } ?: "알 수 없는 오류가 발생했어요."

    private fun filterStores(stores: List<Store>, query: String): List<Store> {
        if (query.isBlank()) return stores
        return stores.filter { store ->
            store.name.contains(query, ignoreCase = true) ||
                store.category.contains(query, ignoreCase = true) ||
                store.address.contains(query, ignoreCase = true) ||
                store.menus.orEmpty().any { it.name.contains(query, ignoreCase = true) }
        }
    }

    class Factory(private val repository: StoreRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}
