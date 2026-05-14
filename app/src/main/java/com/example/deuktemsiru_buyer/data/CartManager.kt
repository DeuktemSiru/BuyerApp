package com.example.deuktemsiru_buyer.data

data class CartItem(
    val menuId: Long,
    val menuName: String,
    val emoji: String,
    val originalPrice: Int,
    val discountedPrice: Int,
    var quantity: Int = 1,
)

object CartManager {
    var serverCartItemIds: Map<Long, Long> = emptyMap()
    var storeId: Long = 0L
    var storeName: String = ""
    var storeEmoji: String = ""
    var storeLat: Double = 0.0
    var storeLng: Double = 0.0
    private val _items = mutableListOf<CartItem>()
    val items: List<CartItem> get() = _items.toList()

    fun add(storeId: Long, storeName: String, storeEmoji: String, storeLat: Double, storeLng: Double, item: CartItem): Boolean {
        if (this.storeId != 0L && this.storeId != storeId) return false
        this.storeId = storeId
        this.storeName = storeName
        this.storeEmoji = storeEmoji
        this.storeLat = storeLat
        this.storeLng = storeLng
        val existing = _items.find { it.menuId == item.menuId }
        if (existing != null) existing.quantity++ else _items.add(item.copy())
        return true
    }

    fun remove(menuId: Long) {
        _items.removeAll { it.menuId == menuId }
        if (_items.isEmpty()) clear()
    }

    fun increaseQuantity(menuId: Long) {
        _items.find { it.menuId == menuId }?.quantity++
    }

    fun decreaseQuantity(menuId: Long) {
        val item = _items.find { it.menuId == menuId } ?: return
        if (item.quantity <= 1) remove(menuId) else item.quantity--
    }

    fun clear() {
        storeId = 0L
        storeName = ""
        storeEmoji = ""
        storeLat = 0.0
        storeLng = 0.0
        _items.clear()
        serverCartItemIds = emptyMap()
    }

    fun replaceFromServer(
        storeId: Long,
        storeName: String,
        items: List<CartItem>,
        serverIds: Map<Long, Long>,
    ) {
        clear()
        this.storeId = storeId
        this.storeName = storeName
        this.storeEmoji = "🛍️"
        _items.addAll(items)
        serverCartItemIds = serverIds
    }

    val totalPrice: Int get() = _items.sumOf { it.discountedPrice * it.quantity }
    val totalCount: Int get() = _items.sumOf { it.quantity }
    val isEmpty: Boolean get() = _items.isEmpty()
}
