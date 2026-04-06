package com.thevakhovske.cunnyplayground

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.io.File

class HyperCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "com.thevakhovske.cunnyplayground.SEND_HYPER") {
            val filePath = intent.getStringExtra("file")
            val rawJsonExtra = intent.getStringExtra("json")
            
            val jsonToUse = if (filePath != null) {
                try {
                    File(filePath).readText()
                } catch (e: Exception) {
                    Log.e("HyperCommand", "Failed to read file: $filePath", e)
                    null
                }
            } else {
                rawJsonExtra
            }

            if (!jsonToUse.isNullOrBlank()) {
                val notificationId = intent.getIntExtra("id", 80085)
                val channelId = "hyperslop_channel"
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (notificationManager.getNotificationChannel(channelId) == null) {
                        val channel = android.app.NotificationChannel(channelId, "HyperIsland", android.app.NotificationManager.IMPORTANCE_HIGH)
                        channel.setSound(null, null)
                        channel.enableVibration(false)
                        notificationManager.createNotificationChannel(channel)
                    }
                }

                val title = intent.getStringExtra("title") ?: "ADB Payload"
                val text = intent.getStringExtra("text") ?: "Raw JSON Injection"

                val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentTitle(title)
                    .setContentText(text)

                // Add HyperIsland specific resources
                val hyperBuilder = io.github.d4viddf.hyperisland_kit.HyperIslandNotification.Builder(
                    context,
                    "live_updates_recaster",
                    "Incoming Notification"
                )
                hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("default_icon", context, R.mipmap.ic_launcher_round))
                hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("file_preview", context, R.mipmap.ic_launcher_round))
                hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("upload_status", context, R.mipmap.ic_launcher_round))
                hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("big_icon", context, R.mipmap.ic_launcher_round))

                builder.extras.putString("miui.focus.param", jsonToUse)
                builder.extras.putAll(hyperBuilder.buildResourceBundle())
                builder.extras.putBoolean("android.app.extra.PROMOTED_ONGOING", true)

                notificationManager.notify(notificationId, builder.build())
                Log.d("HyperCommand", "Posted notification independently with ID: $notificationId")
            } else {
                Log.w("HyperCommand", "No JSON to process. Provide --es file <path> or --es json <string>")
            }
        }
    }
}
