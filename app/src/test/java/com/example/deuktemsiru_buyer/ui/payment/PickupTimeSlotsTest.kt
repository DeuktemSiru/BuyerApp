package com.example.deuktemsiru_buyer.ui.payment

import org.junit.Assert.assertEquals
import org.junit.Test

class PickupTimeSlotsTest {

    @Test
    fun `creates inclusive half-hour pickup slots`() {
        assertEquals(
            listOf("18:30", "19:00", "19:30", "20:00", "20:30"),
            pickupTimeSlots("18:30:00", "20:30:00"),
        )
    }

    @Test
    fun `uses deadline when pickup start is absent`() {
        assertEquals(listOf("20:30"), pickupTimeSlots(null, "20:30:00"))
    }

    @Test
    fun `includes a deadline outside the half-hour cadence`() {
        assertEquals(
            listOf("18:00", "18:30", "19:00", "19:15"),
            pickupTimeSlots("18:00:00", "19:15:00"),
        )
    }

    @Test
    fun `rejects an invalid pickup window`() {
        assertEquals(emptyList<String>(), pickupTimeSlots("21:00:00", "20:30:00"))
    }

    @Test(timeout = 1000)
    fun `late pickup slots stop before wrapping to midnight`() {
        assertEquals(
            listOf("23:00", "23:30", "23:59"),
            pickupTimeSlots("23:00", "23:59"),
        )
        val allDay = pickupTimeSlots("00:00", "23:59")
        assertEquals(49, allDay.size)
        assertEquals("00:00", allDay.first())
        assertEquals("23:59", allDay.last())
    }

    @Test(timeout = 1000)
    fun `identical start and end produces one slot even near midnight`() {
        assertEquals(listOf("23:45"), pickupTimeSlots("23:45", "23:45"))
    }

    @Test
    fun `accepts date-time input and preserves invalid input fallbacks`() {
        assertEquals(
            listOf("18:00", "18:30", "18:45"),
            pickupTimeSlots("2026-09-08T18:00:00.000", "2026-09-08T18:45:00.000"),
        )
        assertEquals(emptyList<String>(), pickupTimeSlots("18:00", null))
        assertEquals(emptyList<String>(), pickupTimeSlots("18:00", "invalid"))
        assertEquals(listOf("18:30"), pickupTimeSlots("invalid", "18:30"))
    }
}
