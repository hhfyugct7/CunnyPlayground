package com.thevakhovske.cunnyplayground

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationCastListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("experimental_prefs", MODE_PRIVATE)
        val isCastingEnabled = prefs.getBoolean("cast_notifications", false)

        if (!isCastingEnabled) return

        // Ignore our own notifications
        if (sbn.packageName == packageName) return

        // Check app filter (if set)
        val enabledApps = prefs.getStringSet("cast_enabled_apps", null)
        if (enabledApps != null && !enabledApps.contains(sbn.packageName)) {
            Log.d("NotificationCast", "Skipping notification from ${sbn.packageName} (not in filter)")
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: "Cast Notification"
        val text = extras.getCharSequence("android.text")?.toString() ?: sbn.packageName

        Log.d("NotificationCast", "Casting notification from ${sbn.packageName}: $title - $text")

        val useAppIcon = prefs.getBoolean("use_app_icon", false)
        val iconToUse = if (useAppIcon && android.os.Build.VERSION.SDK_INT >= 23) {
            try {
                val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                android.graphics.drawable.Icon.createWithResource(sbn.packageName, appInfo.icon)
            } catch (e: Exception) {
                if (android.os.Build.VERSION.SDK_INT >= 23) sbn.notification.smallIcon else null
            }
        } else if (android.os.Build.VERSION.SDK_INT >= 23) {
            sbn.notification.smallIcon
        } else null

        // Forward to PlaygroundService
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("title", "$title")
            putExtra("text", text)
            putExtra("status_chip_text", text) 
            putExtra("id", (sbn.packageName.hashCode() + sbn.id) % 10000 + 20000) // Unique consistent ID
            putExtra("icon_res", R.drawable.ic_alert)
            if (iconToUse != null) {
                putExtra("small_icon_obj", iconToUse)
            }
            putExtra("is_promoted", true)
            putExtra("show_progress", false)
        }
        startService(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Calculate the same unique ID used in onNotificationPosted
        val castId = (sbn.packageName.hashCode() + sbn.id) % 10000 + 20000
        
        Log.d("NotificationCast", "Removing cast notification for ${sbn.packageName} (ID: $castId)")

        // Send cancel action to PlaygroundService
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_CANCEL
            putExtra("id", castId)
        }
        startService(intent)
    }
}
