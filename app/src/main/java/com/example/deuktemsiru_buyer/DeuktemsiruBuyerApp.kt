package com.example.deuktemsiru_buyer

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class DeuktemsiruBuyerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, getString(R.string.kakao_app_key))
    }
}
