package com.example.deuktemsiru_buyer.data

import android.content.Context
import android.content.SharedPreferences
import com.example.deuktemsiru_buyer.network.RetrofitClient
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("buyer_session", Context.MODE_PRIVATE)

    init {
        restoreToken()
        // onTokenRefreshed holds a reference to this SessionManager. Clear it by calling
        // RetrofitClient.onTokenRefreshed = null when the session is no longer needed
        // (e.g., in Application.onTerminate or after clear()) to avoid a memory leak.
        RetrofitClient.onTokenRefreshed = { newAccessToken ->
            prefs.edit().putString("accessToken", newAccessToken).apply()
            RetrofitClient.accessToken = newAccessToken.takeIf { it.isNotBlank() }
        }
    }

    var memberId: Long by prefs.long("memberId", -1L)
    var nickname: String by prefs.string("nickname", "")
    var lastOrderId: Long by prefs.long("lastOrderId", -1L)
    var isSiruLinked: Boolean by prefs.boolean("isSiruLinked", false)
    var siruBalance: Int by prefs.int("siruBalance", 0)

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

    fun isLoggedIn() = memberId > 0L && accessToken.isNotBlank()

    fun restoreToken() {
        RetrofitClient.accessToken = accessToken.takeIf { it.isNotBlank() }
        RetrofitClient.refreshToken = refreshToken.takeIf { it.isNotBlank() }
    }

    fun clear() {
        prefs.edit().clear().apply()
        RetrofitClient.accessToken = null
        RetrofitClient.refreshToken = null
        RetrofitClient.onTokenRefreshed = null
    }
}

// --- SharedPreferences property delegates ---

private fun SharedPreferences.string(key: String, default: String): ReadWriteProperty<Any?, String> =
    object : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            getString(key, default) ?: default
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) =
            edit().putString(key, value).apply()
    }

private fun SharedPreferences.long(key: String, default: Long): ReadWriteProperty<Any?, Long> =
    object : ReadWriteProperty<Any?, Long> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = getLong(key, default)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) =
            edit().putLong(key, value).apply()
    }

private fun SharedPreferences.boolean(key: String, default: Boolean): ReadWriteProperty<Any?, Boolean> =
    object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = getBoolean(key, default)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
            edit().putBoolean(key, value).apply()
    }

private fun SharedPreferences.int(key: String, default: Int): ReadWriteProperty<Any?, Int> =
    object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = getInt(key, default)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) =
            edit().putInt(key, value).apply()
    }
