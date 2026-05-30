package com.example.deuktemsiru_buyer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.deuktemsiru_buyer.network.Push
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** 포그라운드 수신 시에만 직접 알림을 띄운다(백그라운드는 FCM이 알아서 표시). */
class DeuktemsiruMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) = Push.registerToken(token)

    override fun onMessageReceived(message: RemoteMessage) {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val notification = NotificationCompat.Builder(this, Push.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(message.notification?.title ?: message.data["title"] ?: "득템시루")
            .setContentText(message.notification?.body ?: message.data["body"] ?: "")
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()
        getSystemService(NotificationManager::class.java)
            ?.notify(System.currentTimeMillis().toInt(), notification)
    }
}
