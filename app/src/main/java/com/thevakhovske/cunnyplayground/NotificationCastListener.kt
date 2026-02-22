package com.thevakhovske.cunnyplayground

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationCastListener : NotificationListenerService() {
    
    private val lastNotificationContent = HashMap<Int, String>()


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
        val titleExtra = extras.getCharSequence("android.title")
        val textExtra = extras.getCharSequence("android.text")

        val rawTitle = titleExtra?.toString()?.trim() ?: ""
        val rawText = textExtra?.toString()?.trim() ?: sbn.packageName

        if (rawTitle.isEmpty() && rawText.isEmpty()) {
            Log.d("NotificationCast", "Skipping notification from ${sbn.packageName} (no title/text)")
            return
        }

        // Edge case: if title or text is exactly the package name, ignore
        val pkg = sbn.packageName
        if (rawTitle.equals(pkg, ignoreCase = true) || rawText.equals(pkg, ignoreCase = true)) {
            Log.d("NotificationCast", "Skipping notification from $pkg (content matches package name)")
            return
        }

        // Final values for casting (ensure title is never empty for the builder)
        val finalTitle = if (rawTitle.isEmpty()) "Notification" else rawTitle
        val finalText = rawText

        // Cache raw data for the customization page
        prefs.edit().apply {
            putString("${sbn.packageName}_last_title", finalTitle)
            putString("${sbn.packageName}_last_text", finalText)
            apply()
        }

        val sourceApp = try {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        Log.d("NotificationCast", "Casting notification from $sourceApp (${sbn.packageName})")

        // Read per-app customization
        val textSource = prefs.getString("${sbn.packageName}_text_source", "text")
        val chipText = if (textSource == "title") finalTitle else finalText

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

        // Extract Actions
        val actions = sbn.notification.actions
        val actionsList = if (actions != null) ArrayList(actions.toList()) else null

        // Extract Progress
        val progress = extras.getInt("android.progress", 0)
        val progressMax = extras.getInt("android.progressMax", 0)
        val isIndeterminate = extras.getBoolean("android.progressIndeterminate", false)
        val hasProgress = progressMax > 0 || isIndeterminate

        // Unique ID for this cast
        val castId = (pkg.hashCode() + sbn.id) % 10000 + 20000

        // Deduping: Generate a key based on content that effects the UI
        val contentKey = "T:$finalTitle|X:$finalText|C:$chipText|P:$progress/$progressMax/$isIndeterminate"
        if (lastNotificationContent[castId] == contentKey) {
            // Log.d("NotificationCast", "Skipping redundant update for $sourceApp ($pkg)")
            return
        }
        lastNotificationContent[castId] = contentKey

        // Forward to PlaygroundService
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("title", finalTitle)
            putExtra("text", finalText)
            putExtra("source_app", sourceApp)
            putExtra("status_chip_text", chipText) 
            putExtra("id", castId)
            putExtra("icon_res", R.drawable.ic_alert)
            if (iconToUse != null) {
                putExtra("small_icon_obj", iconToUse)
            }
            if (actionsList != null) {
                putParcelableArrayListExtra("actions", actionsList)
            }
            if (hasProgress) {
                putExtra("progress", progress)
                putExtra("progress_max", progressMax)
                putExtra("progress_indeterminate", isIndeterminate)
                putExtra("show_progress", true)
            } else {
                putExtra("show_progress", false)
            }
            putExtra("is_promoted", true)
            putExtra("when", sbn.notification.`when`)
        }
        startService(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Calculate the same unique ID used in onNotificationPosted
        val castId = (sbn.packageName.hashCode() + sbn.id) % 10000 + 20000
        
        // Clear deduping cache
        lastNotificationContent.remove(castId)

        Log.d("NotificationCast", "Removing cast notification for ${sbn.packageName} (ID: $castId)")

        // Send cancel action to PlaygroundService
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_CANCEL
            putExtra("id", castId)
        }
        startService(intent)
    }
}
