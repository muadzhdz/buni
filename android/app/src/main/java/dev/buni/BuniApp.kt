package dev.buni

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BuniApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Buni Protection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status of Buni network shield"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "buni_protection_channel"
    }
}
