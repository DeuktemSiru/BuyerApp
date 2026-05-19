package com.example.deuktemsiru_buyer.data

import com.example.deuktemsiru_buyer.network.ApiService
import com.example.deuktemsiru_buyer.network.CreateOrderRequest
import com.example.deuktemsiru_buyer.network.CreateOrderResponse
import com.example.deuktemsiru_buyer.network.OrderDetailResponse
import com.example.deuktemsiru_buyer.network.OrderItemRequest
import com.example.deuktemsiru_buyer.network.OrderListItemResponse

class OrderRepository(private val api: ApiService) {
    suspend fun createOrder(items: List<OrderItemRequest>): CreateOrderResponse? =
        api.createOrder(CreateOrderRequest(items = items)).data

    suspend fun getOrders(): List<OrderListItemResponse> =
        api.getOrders().data.orEmpty()

    suspend fun getOrder(orderId: Long): OrderDetailResponse? =
        api.getOrder(orderId).data
}
