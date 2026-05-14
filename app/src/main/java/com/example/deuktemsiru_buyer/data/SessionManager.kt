package com.example.deuktemsiru_buyer.data

import android.content.Context
import com.example.deuktemsiru_buyer.network.RetrofitClient

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("buyer_session", Context.MODE_PRIVATE)

    init {
        restoreToken()
        RetrofitClient.onTokenRefreshed = { newAccessToken ->
            prefs.edit().putString("accessToken", newAccessToken).apply()
            RetrofitClient.accessToken = newAccessToken.takeIf { it.isNotBlank() }
        }
    }

    var memberId: Long
        get() = prefs.getLong("memberId", -1L)
        set(value) { prefs.edit().putLong("memberId", value).apply() }

    var nickname: String
        get() = prefs.getString("nickname", "") ?: ""
        set(value) { prefs.edit().putString("nickname", value).apply() }

    var accessToken: String
        get() = prefs.getString("accessToken", "") ?: ""
        set(value) {
            prefs.edit().putString("accessToken", value).apply()
            RetrofitClient.accessToken = value.takeIf { it.isNotBlank() }
        }

    var refreshToken: String
        get() = prefs.getString("refreshToken", "") ?: ""
        set(value) {
            prefs.edit().putString("refreshToken", value).apply()
            RetrofitClient.refreshToken = value.takeIf { it.isNotBlank() }
        }

    var lastOrderId: Long
        get() = prefs.getLong("lastOrderId", -1L)
        set(value) { prefs.edit().putLong("lastOrderId", value).apply() }

    var isSiruLinked: Boolean
        get() = prefs.getBoolean("isSiruLinked", false)
        set(value) { prefs.edit().putBoolean("isSiruLinked", value).apply() }

    var siruBalance: Int
        get() = prefs.getInt("siruBalance", 0)
        set(value) { prefs.edit().putInt("siruBalance", value).apply() }

    fun isLoggedIn() = memberId > 0L && accessToken.isNotBlank()

    fun restoreToken() {
        RetrofitClient.accessToken = accessToken.takeIf { it.isNotBlank() }
        RetrofitClient.refreshToken = refreshToken.takeIf { it.isNotBlank() }
    }

    fun clear() {
        prefs.edit().clear().apply()
        RetrofitClient.accessToken = null
        RetrofitClient.refreshToken = null
    }
}
