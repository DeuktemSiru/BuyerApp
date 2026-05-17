package com.example.deuktemsiru_buyer.data

data class CartItem(
    val menuId: Long,
    val menuName: String,
    val emoji: String,
    val originalPrice: Int,
    val discountedPrice: Int,
    val pickupStart: String = "",
    val pickupEnd: String = "",
    var quantity: Int = 1,
)

object CartManager {
    @Volatile private var _serverCartItemIds: Map<Long, Long> = emptyMap()
    @Volatile private var _storeId: Long = 0L
    @Volatile private var _storeName: String = ""
    @Volatile private var _storeEmoji: String = ""
    @Volatile private var _storeLat: Double = 0.0
    @Volatile private var _storeLng: Double = 0.0
    private val _items = mutableListOf<CartItem>()

    val serverCartItemIds: Map<Long, Long> get() = _serverCartItemIds
    val storeId: Long get() = _storeId
    val storeName: String get() = _storeName
    val storeEmoji: String get() = _storeEmoji
    val storeLat: Double get() = _storeLat
    val storeLng: Double get() = _storeLng
    val items: List<CartItem> get() = synchronized(this) { _items.toList() }

    @Synchronized
    fun add(storeId: Long, storeName: String, storeEmoji: String, storeLat: Double, storeLng: Double, item: CartItem): Boolean {
        if (_storeId != 0L && _storeId != storeId) return false
        _storeId = storeId
        _storeName = storeName
        _storeEmoji = storeEmoji
        _storeLat = storeLat
        _storeLng = storeLng
        val existing = _items.find { it.menuId == item.menuId }
        if (existing != null) existing.quantity++ else _items.add(item.copy())
        return true
    }

    @Synchronized
    fun remove(menuId: Long) {
        _items.removeAll { it.menuId == menuId }
        if (_items.isEmpty()) clearInternal()
    }

    @Synchronized
    fun increaseQuantity(menuId: Long) {
        _items.find { it.menuId == menuId }?.quantity++
    }

    @Synchronized
    fun decreaseQuantity(menuId: Long) {
        val item = _items.find { it.menuId == menuId } ?: return
        if (item.quantity <= 1) {
            _items.removeAll { it.menuId == menuId }
            if (_items.isEmpty()) clearInternal()
        } else {
            item.quantity--
        }
    }

    @Synchronized
    fun addServerCartItemId(productId: Long, cartItemId: Long) {
        _serverCartItemIds = _serverCartItemIds + (productId to cartItemId)
    }

    @Synchronized
    fun clear() { clearInternal() }

    private fun clearInternal() {
        _storeId = 0L
        _storeName = ""
        _storeEmoji = ""
        _storeLat = 0.0
        _storeLng = 0.0
        _items.clear()
        _serverCartItemIds = emptyMap()
    }

    @Synchronized
    fun replaceFromServer(
        storeId: Long,
        storeName: String,
        storeLat: Double,
        storeLng: Double,
        items: List<CartItem>,
        serverIds: Map<Long, Long>,
    ) {
        clearInternal()
        _storeId = storeId
        _storeName = storeName
        _storeEmoji = "🛍️"
        _storeLat = storeLat
        _storeLng = storeLng
        _items.addAll(items)
        _serverCartItemIds = serverIds
    }

    val totalPrice: Int get() = synchronized(this) { _items.sumOf { it.discountedPrice * it.quantity } }
    val totalCount: Int get() = synchronized(this) { _items.sumOf { it.quantity } }
    val isEmpty: Boolean get() = synchronized(this) { _items.isEmpty() }
}
