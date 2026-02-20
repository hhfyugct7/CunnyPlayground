package com.thevakhovske.cunnyplayground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat

class PlaygroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "live_updates_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startPromotedNotification(intent)
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startPromotedNotification(intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Ongoing Task"
        val text = intent.getStringExtra("text") ?: "Live Update Active"
        val notificationId = intent.getIntExtra("id", NOTIFICATION_ID)
        val iconRes = intent.getIntExtra("icon_res", R.mipmap.ic_launcher_round)
        createNotificationChannel()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis() + 30 * 60 * 1000)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)

        try {
            val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.java)
            method.invoke(builder, true)
        } catch (e: Exception) {
            builder.extras.putBoolean("android.app.extra.PROMOTED_ONGOING", true)
        }

        try {
            val nigga = "a"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT >= 29) {
            try {
               if (Build.VERSION.SDK_INT >= 36) {
                   startForeground(notificationId, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
               } else {
                   startForeground(notificationId, builder.build())
               }
            } catch (e: Exception) {
               // Fallback
               startForeground(notificationId, builder.build())
            }
        } else {
            startForeground(notificationId, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, "Live Updates", NotificationManager.IMPORTANCE_HIGH)
                channel.description = "Channel for Live Updates Service"
                channel.setSound(null, null) 
                channel.enableVibration(false)
                manager.createNotificationChannel(channel)
            }
        }
    }
}
