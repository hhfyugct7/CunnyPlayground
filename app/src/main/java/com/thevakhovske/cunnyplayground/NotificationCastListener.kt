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
        val rawTitle = extras.getCharSequence("android.title")?.toString() ?: "No Title"
        val rawText = extras.getCharSequence("android.text")?.toString() ?: sbn.packageName

        // Cache raw data for the customization page
        prefs.edit().apply {
            putString("${sbn.packageName}_last_title", rawTitle)
            putString("${sbn.packageName}_last_text", rawText)
            apply()
        }

        Log.d("NotificationCast", "Casting notification from ${sbn.packageName}")

        // Read per-app customization
        val textSource = prefs.getString("${sbn.packageName}_text_source", "text")
        val chipText = if (textSource == "title") rawTitle else rawText

        val iconSource = prefs.getString("${sbn.packageName}_icon_source", "default")
        val globalUseAppIcon = prefs.getBoolean("use_app_icon", false)
        
        val shouldUseAppIcon = if (iconSource == "default") globalUseAppIcon else (iconSource == "app")

        val iconToUse = if (shouldUseAppIcon && android.os.Build.VERSION.SDK_INT >= 23) {
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
            putExtra("title", rawTitle)
            putExtra("text", rawText)
            putExtra("status_chip_text", chipText) 
            putExtra("id", (sbn.packageName.hashCode() + sbn.id) % 10000 + 20000)
            putExtra("icon_res", R.drawable.ic_alert)
            if (iconToUse != null) {
                putExtra("small_icon_obj", iconToUse)
            }
            putExtra("is_promoted", true)
            putExtra("show_progress", false)
            putExtra("when", sbn.notification.`when`)
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
