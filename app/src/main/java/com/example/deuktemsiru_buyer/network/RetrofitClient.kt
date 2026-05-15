package com.example.deuktemsiru_buyer.network

import android.util.Log
import com.example.deuktemsiru_buyer.BuildConfig
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection
import java.net.URL

object RetrofitClient {

    private const val EMULATOR_BASE_URL = "http://10.0.2.2:8080/"
    val BASE_URL: String = BuildConfig.BASE_URL.ifBlank { EMULATOR_BASE_URL }
    var accessToken: String? = null
    var refreshToken: String? = null
    var onTokenRefreshed: ((String) -> Unit)? = null

    init {
        if (!BuildConfig.DEBUG && BASE_URL.contains("10.0.2.2")) {
            error("릴리스 빌드에서 에뮬레이터 URL을 사용할 수 없습니다. local.properties에 BACKEND_BASE_URL을 설정하세요.")
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                accessToken?.takeIf { it.isNotBlank() }?.let {
                    requestBuilder.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(requestBuilder.build())
            }
            .authenticator(TokenRefreshAuthenticator())
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private class TokenRefreshAuthenticator : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            if (response.request.header("Authorization").isNullOrBlank()) return null
            if (responseCount(response) >= 2) return null

            val savedRefreshToken = refreshToken?.takeIf { it.isNotBlank() } ?: return null
            val newAccessToken = synchronized(this) {
                val currentRequestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
                if (!accessToken.isNullOrBlank() && accessToken != currentRequestToken) {
                    accessToken
                } else {
                    refreshTokenSync(savedRefreshToken)?.also {
                        accessToken = it
                        onTokenRefreshed?.invoke(it)
                    }
                }
            } ?: return null

            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        }

        private fun refreshTokenSync(refreshToken: String): String? {
            return try {
                val url = URL("${BASE_URL}api/v1/auth/refresh")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val body = Gson().toJson(mapOf("refreshToken" to refreshToken))
                conn.outputStream.use { it.write(body.toByteArray()) }
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
                val json = conn.inputStream.bufferedReader().readText()
                try {
                    Gson().fromJson(json, Map::class.java)["data"]
                        ?.let { (it as? Map<*, *>)?.get("accessToken") as? String }
                } catch (e: Exception) {
                    Log.e("TokenRefresh", "Failed to parse token refresh response", e)
                    null
                }
            } catch (e: Exception) {
                Log.e("TokenRefresh", "Token refresh request failed", e)
                null
            }
        }

        private fun responseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }
    }
}
