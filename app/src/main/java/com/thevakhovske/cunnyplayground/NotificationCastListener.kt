package com.thevakhovske.cunnyplayground

import android.app.Notification
import android.content.Intent
import android.os.Build
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
        val subTextExtra = extras.getCharSequence("android.subText")

        val rawTitle = titleExtra?.toString()?.trim() ?: ""
        val rawText = textExtra?.toString()?.trim() ?: sbn.packageName
        val rawSubText = subTextExtra?.toString()?.trim() ?: ""

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

        // Cache raw data and full dump for the customization page
        val dump = StringBuilder()
        val drawableIds = mutableSetOf<Int>()
        val discoveredActions = mutableListOf<android.app.Notification.Action>()
        
        // Populate dump and collect tech data
        val dumpText = dumpNotification(sbn, drawableIds, discoveredActions)

        prefs.edit().apply {
            putString("${sbn.packageName}_last_title", finalTitle)
            putString("${sbn.packageName}_last_text", finalText)
            putString("${sbn.packageName}_last_subtext", rawSubText)
            putString("${sbn.packageName}_last_raw_dump", dumpText)
            putString("${sbn.packageName}_last_drawables", drawableIds.joinToString(","))
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
        val chipText = when (textSource) {
            "title" -> finalTitle
            "subtext" -> if (rawSubText.isNotEmpty()) rawSubText else finalText
            "titletext" -> "$finalTitle • $finalText"
            else -> finalText
        }

        // Apply Regex Filter
        val regexStr = prefs.getString("${sbn.packageName}_regex_filter", "") ?: ""
        val finalChipText = if (regexStr.isNotEmpty()) {
            try {
                val regex = Regex(regexStr)
                val match = regex.find(chipText)
                if (match != null) {
                    if (match.groups.size > 1) {
                        match.groupValues.drop(1).joinToString(" ")
                    } else {
                        match.value
                    }
                } else {
                    chipText
                }
            } catch (e: Exception) {
                chipText
            }
        } else {
            chipText
        }

        val iconSource = prefs.getString("${sbn.packageName}_icon_source", "default")
        val iconToUse = when (iconSource) {
            "app" -> {
                if (Build.VERSION.SDK_INT >= 23) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                        android.graphics.drawable.Icon.createWithResource(sbn.packageName, appInfo.icon)
                    } catch (e: Exception) {
                        sbn.notification.smallIcon
                    }
                } else null
            }
            "notification" -> {
                if (Build.VERSION.SDK_INT >= 23) sbn.notification.smallIcon else null
            }
            "extracted" -> {
                if (Build.VERSION.SDK_INT >= 23) {
                    val firstExtracted = drawableIds.firstOrNull()
                    if (firstExtracted != null) {
                        try {
                            android.graphics.drawable.Icon.createWithResource(sbn.packageName, firstExtracted)
                        } catch (e: Exception) {
                            sbn.notification.smallIcon
                        }
                    } else sbn.notification.smallIcon
                } else null
            }
            else -> {
                // Default logic (global toggle)
                val globalUseAppIcon = prefs.getBoolean("use_app_icon", false)
                if (globalUseAppIcon && Build.VERSION.SDK_INT >= 23) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                        android.graphics.drawable.Icon.createWithResource(sbn.packageName, appInfo.icon)
                    } catch (e: Exception) {
                        sbn.notification.smallIcon
                    }
                } else if (Build.VERSION.SDK_INT >= 23) {
                    sbn.notification.smallIcon
                } else null
            }
        }

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
            putExtra("status_chip_text", finalChipText) 
            putExtra("id", castId)
            putExtra("icon_res", R.drawable.ic_alert)
            if (iconToUse != null) {
                putExtra("small_icon_obj", iconToUse)
            }
            // Combine original actions with discovered ones
            val allActions = ArrayList<Notification.Action>()
            sbn.notification.actions?.let { allActions.addAll(it) }
            allActions.addAll(discoveredActions)
            putParcelableArrayListExtra("actions", allActions)
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

    private fun dumpNotification(sbn: StatusBarNotification, drawableIds: MutableSet<Int>, discoveredActions: MutableList<Notification.Action>): String {
        val sb = StringBuilder()
        val n = sbn.notification
        val extras = n.extras

        sb.append("--- EXHAUSTIVE NOTIFICATION DUMP ---\n")
        sb.append("Package: ${sbn.packageName}\n")
        sb.append("ID: ${sbn.id}\n")
        sb.append("Tag: ${sbn.tag}\n")
        sb.append("Post Time: ${sbn.postTime}\n")
        
        sb.append("\n[EXTRAS]\n")
        for (key in extras.keySet()) {
            val value = extras.get(key)
            sb.append("$key: $value (${value?.javaClass?.simpleName ?: "null"})\n")
        }

        // Try to get package resources for ID resolution
        val res = try {
            packageManager.getResourcesForApplication(sbn.packageName)
        } catch (e: Exception) {
            null
        }

        sb.append("\n[REMOTEVIEWS DEEP INSPECTION]\n")
        exhaustiveDumpRemoteViews(n.contentView, "contentView", res, sb, drawableIds, discoveredActions)
        exhaustiveDumpRemoteViews(n.bigContentView, "bigContentView", res, sb, drawableIds, discoveredActions)
        exhaustiveDumpRemoteViews(n.headsUpContentView, "headsUpContentView", res, sb, drawableIds, discoveredActions)

        return sb.toString()
    }

    private fun exhaustiveDumpRemoteViews(
        rv: android.widget.RemoteViews?, 
        label: String, 
        res: android.content.res.Resources?, 
        sb: StringBuilder, 
        drawableIds: MutableSet<Int>,
        discoveredActions: MutableList<Notification.Action>
    ) {
        if (rv == null) {
            sb.append("$label: null\n")
            return
        }
        sb.append("\n$label Instruction List:\n")
        
        val viewIdToText = mutableMapOf<Int, CharSequence>()
        val viewIdToIntent = mutableMapOf<Int, android.app.PendingIntent>()

        try {
            val mActionsField = rv.javaClass.getDeclaredField("mActions")
            mActionsField.isAccessible = true
            val actions = mActionsField.get(rv) as? List<*> ?: return

            for (action in actions) {
                if (action == null) continue
                sb.append("  [Action: ${action.javaClass.simpleName}]\n")
                
                val allFields = mutableListOf<java.lang.reflect.Field>()
                var currClass: Class<*>? = action.javaClass
                while (currClass != null && currClass != Object::class.java) {
                    allFields.addAll(currClass.declaredFields)
                    currClass = currClass.superclass
                }

                var actionMethodName: String? = null
                val actionFields = mutableMapOf<String, Any?>()
                var currentViewId = -1

                for (field in allFields) {
                    field.isAccessible = true
                    try {
                        val name = field.name
                        val value = field.get(action)
                        actionFields[name] = value

                        if (name == "methodName" || name == "mMethodName") actionMethodName = value as? String
                        if (name == "viewId" || name == "mViewId") currentViewId = value as? Int ?: -1

                        // Resolve resource IDs or Dump recursive layouts
                        var displayValue = value.toString()
                        if (value is Int && value >= 0x7f000000 && res != null) {
                            val resName = try { res.getResourceEntryName(value) } catch (e: Exception) { null }
                            displayValue = if (resName != null) "$value ($resName)" else "$value (0x${Integer.toHexString(value)})"
                        } else if (value is android.widget.RemoteViews) {
                            val innerSb = StringBuilder()
                            exhaustiveDumpRemoteViews(value, "InnerRV", res, innerSb, drawableIds, discoveredActions)
                            displayValue = "\n" + innerSb.toString().prependIndent("      ")
                        }
                        sb.append("    - $name: $displayValue\n")
                    } catch (e: Exception) {
                        sb.append("    - ${field.name}: (Error: ${e.message})\n")
                    }
                }

                if (currentViewId != -1) {
                    val methodName = actionMethodName?.lowercase() ?: ""
                    
                    // 1. Capture Text labels
                    if (methodName == "settext") {
                        val textValue = actionFields["value"] ?: actionFields["mValue"]
                        if (textValue is CharSequence) viewIdToText[currentViewId] = textValue
                    }
                    
                    // 2. Scan for ANY PendingIntent in this action (Handles SetOnClickPendingIntent, SetOnClickResponse, etc)
                    val discoveredPi = findPendingIntent(action)
                    if (discoveredPi != null) {
                        viewIdToIntent[currentViewId] = discoveredPi
                    }
                }

                // Process drawables
                if (res != null) {
                    for ((name, value) in actionFields) {
                        if (value is Int && value > 0) {
                            val normalizedName = name.removePrefix("m").lowercase()
                            if (normalizedName == "resid" || normalizedName == "value") {
                                val isDrawable = actionMethodName?.lowercase()?.let { it.contains("icon") || it.contains("image") || it.contains("drawable") } ?: false
                                if (isDrawable || normalizedName == "resid") drawableIds.add(value)
                            }
                        }
                    }
                }
            }
            
            // Finalize Discovered Actions: Pair Intent with the nearest Text
            for ((vid, intent) in viewIdToIntent) {
                val titleString = viewIdToText[vid] ?: "Action"
                Log.d("NotificationCast", "Found interactive button in RemoteViews: $titleString")
                val action = if (Build.VERSION.SDK_INT >= 23) {
                    Notification.Action.Builder(null, titleString, intent).build()
                } else {
                    @Suppress("DEPRECATION")
                    Notification.Action(0, titleString, intent)
                }
                discoveredActions.add(action)
            }

        } catch (e: Exception) {
            sb.append("  - (Dump failed: ${e.message})\n")
        }
    }

    private fun findPendingIntent(obj: Any?, depth: Int = 0): android.app.PendingIntent? {
        if (obj == null || depth > 2) return null
        if (obj is android.app.PendingIntent) return obj
        
        try {
            var currClass: Class<*>? = obj.javaClass
            while (currClass != null && currClass != Object::class.java) {
                val fields = currClass.declaredFields
                for (f in fields) {
                    f.isAccessible = true
                    val value = f.get(obj)
                    if (value is android.app.PendingIntent) return value
                    if (value != null && !value.javaClass.isPrimitive && value !is String && value !is Number) {
                        val found = findPendingIntent(value, depth + 1)
                        if (found != null) return found
                    }
                }
                currClass = currClass.superclass
            }
        } catch (e: Exception) {}
        return null
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
