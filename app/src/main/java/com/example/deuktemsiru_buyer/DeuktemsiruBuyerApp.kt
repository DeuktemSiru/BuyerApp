package com.example.deuktemsiru_buyer

import android.app.Application
import com.example.deuktemsiru_buyer.data.SessionManager
import com.kakao.sdk.common.KakaoSdk

class DeuktemsiruBuyerApp : Application() {

    val session: SessionManager by lazy { SessionManager(this) }

    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        session // initialise early so RetrofitClient tokens are restored before first request
    }

    companion object {
        private lateinit var instance: DeuktemsiruBuyerApp
        fun get(): DeuktemsiruBuyerApp = instance
    }

    init {
        instance = this
    }
}
