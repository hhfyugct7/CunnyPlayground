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
        
        // Ensure channel exists
        createNotificationChannel()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis() + 30 * 60 * 1000)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true) // Tomato sets silent

        // Attempt Promoted
        try {
            val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.java)
            method.invoke(builder, true)
        } catch (e: Exception) {
            builder.extras.putBoolean("android.app.extra.PROMOTED_ONGOING", true)
        }

        // Progress Style
        try {
            val progressStyle = NotificationCompat.ProgressStyle()
            progressStyle.addProgressSegment(
                NotificationCompat.ProgressStyle.Segment(30 * 60 * 1000).setColor(Color.GREEN)
            )
            progressStyle.addProgressSegment(
                NotificationCompat.ProgressStyle.Segment(10 * 60 * 1000).setColor(Color.YELLOW)
            )
            progressStyle.setProgress(15 * 60 * 1000)
            builder.setStyle(progressStyle)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Start Foreground
        if (Build.VERSION.SDK_INT >= 29) { // And definitely for 14+ specific types
            // For Android 14+, we need to declare the type in manifest and pass it here
            // We use 'specialUse' as per Tomato reference if SDK 34+
            // Using a generic fallback for compile safety if variables missing
            try {
               // ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE might be available if compiling against 36
               // If not, we pass the int value if we can find it, or just use 0/default if older.
               // Actually, let's try to access the field or just use a standard type that works universally like MEDIA_PLAYBACK or DATA_SYNC for test
               // But Tomato uses specialUse.
               // check for api 34
               if (Build.VERSION.SDK_INT >= 34) {
                   // 32 = FOREGROUND_SERVICE_TYPE_SPECIAL_USE (approx, need to check constant)
                   // Actually, if we compile against 36, we can access ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                   startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
               } else {
                   startForeground(NOTIFICATION_ID, builder.build())
               }
            } catch (e: Exception) {
               // Fallback
               startForeground(NOTIFICATION_ID, builder.build())
            }
        } else {
            startForeground(NOTIFICATION_ID, builder.build())
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
