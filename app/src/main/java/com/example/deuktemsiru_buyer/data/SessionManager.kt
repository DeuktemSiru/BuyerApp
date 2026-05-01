package com.example.deuktemsiru_buyer.data

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("buyer_session", Context.MODE_PRIVATE)

    var userId: Long
        get() = prefs.getLong("userId", -1L)
        set(value) { prefs.edit().putLong("userId", value).apply() }

    var nickname: String
        get() = prefs.getString("nickname", "") ?: ""
        set(value) { prefs.edit().putString("nickname", value).apply() }

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(value) { prefs.edit().putString("token", value).apply() }

    var lastOrderId: Long
        get() = prefs.getLong("lastOrderId", -1L)
        set(value) { prefs.edit().putLong("lastOrderId", value).apply() }

    fun isLoggedIn() = userId > 0L && token.isNotBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
