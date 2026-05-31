package com.example.deuktemsiru_buyer.network

import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseUrlPolicyTest {
    @Test
    fun `release requires https`() {
        requireSecureBaseUrl("https://api.example.com/", isDebug = false)
        assertThrows(IllegalArgumentException::class.java) {
            requireSecureBaseUrl("http://api.example.com/", isDebug = false)
        }
    }

    @Test
    fun `debug permits emulator http`() {
        requireSecureBaseUrl("http://10.0.2.2:8080/", isDebug = true)
    }

    @Test
    fun `only invalid refresh credentials expire session`() {
        assertTrue(isDefinitiveRefreshFailure(400))
        assertTrue(isDefinitiveRefreshFailure(401))
        assertFalse(isDefinitiveRefreshFailure(500))
    }
}
