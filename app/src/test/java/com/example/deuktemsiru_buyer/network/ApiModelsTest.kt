package com.example.deuktemsiru_buyer.network

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiModelsTest {

    private val gson = Gson()

    @Test
    fun `order request sends selected pickup time`() {
        val json = gson.toJsonTree(
            CreateOrderRequest(
                items = listOf(OrderItemRequest(productId = 1L, quantity = 1)),
                pickupTime = "18:30",
            )
        ).asJsonObject

        assertEquals("18:30", json["pickupTime"].asString)
    }

    @Test
    fun `member stats reads rewards returned by backend`() {
        val stats = gson.fromJson(
            """{"totalSavedAmount":12340,"totalCarbonSavedKg":3.5,"totalOrders":15,"grade":"TREE","points":1234,"couponCount":0}""",
            MemberStatsResponse::class.java,
        )

        assertEquals("TREE", stats.grade)
        assertEquals(1_234, stats.points)
        assertEquals(0, stats.couponCount)
    }
}
