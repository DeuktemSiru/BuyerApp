package com.example.deuktemsiru_buyer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.deuktemsiru_buyer.network.RetrofitClient
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SessionManager(context: Context) {
    private val prefs = securePrefs(context, "buyer_session")

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

    var memberId: Long     by prefs.pref("memberId",    getter = { getLong(it, -1L) },      setter = { k, v -> putLong(k, v) })
    var nickname: String   by prefs.pref("nickname",    getter = { getString(it, "") ?: "" }, setter = { k, v -> putString(k, v) })
    var lastOrderId: Long  by prefs.pref("lastOrderId", getter = { getLong(it, -1L) },       setter = { k, v -> putLong(k, v) })
    var isSiruLinked: Boolean by prefs.pref("isSiruLinked", getter = { getBoolean(it, false) }, setter = { k, v -> putBoolean(k, v) })
    var siruBalance: Int   by prefs.pref("siruBalance", getter = { getInt(it, 0) },           setter = { k, v -> putInt(k, v) })

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

// --- SharedPreferences generic property delegate ---

private fun <T> SharedPreferences.pref(
    key: String,
    getter: SharedPreferences.(String) -> T,
    setter: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getter(key)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        edit().setter(key, value).apply()
    }
}

private fun securePrefs(context: Context, name: String): SharedPreferences {
    val appContext = context.applicationContext
    return runCatching {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse {
        appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    }
}
