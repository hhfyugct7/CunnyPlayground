package com.thevakhovske.cunnyplayground

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import android.graphics.Canvas
import android.view.View
import java.io.File
import java.io.FileOutputStream

class NotificationCastListener : NotificationListenerService() {
    
    private val lastNotificationContent = HashMap<Int, String>()

    private val reloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.thevakhovske.cunnyplayground.RELOAD_NOTIFICATIONS") {
                Log.d("NotificationCast", "Reloading notifications due to config change...")
                lastNotificationContent.clear()
                reloadNotifications()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter("com.thevakhovske.cunnyplayground.RELOAD_NOTIFICATIONS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(reloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(reloadReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(reloadReceiver)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        captureExistingSuperX()
        reloadNotifications()
    }

    /** Inspector: snapshot any SuperX atomic notifications already present when we connect. */
    private fun captureExistingSuperX() {
        try {
            activeNotifications?.forEach { sbn ->
                if (sbn.packageName != packageName && SuperXInspectorStore.isSuperX(sbn.notification.extras)) {
                    SuperXInspectorStore.capture(this, sbn)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationCast", "captureExistingSuperX failed", e)
        }
    }

    private fun reloadNotifications() {
        val prefs = getSharedPreferences("experimental_prefs", MODE_PRIVATE)
        val isCastingEnabled = prefs.getBoolean("cast_notifications", false)
        val enabledApps = prefs.getStringSet("cast_enabled_apps", null)

        try {
            val notifications = activeNotifications ?: return
            for (sbn in notifications) {
                val isPackageAllowed = enabledApps == null || enabledApps.contains(sbn.packageName)
                val isAllowed = isCastingEnabled && sbn.packageName != packageName && isPackageAllowed
                
                if (isAllowed) {
                    onNotificationPosted(sbn)
                } else {
                    onNotificationRemoved(sbn)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationCast", "Failed to reload notifications", e)
        }
    }


    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Inspector: capture any SuperX atomic notification (from other apps) regardless of cast state.
        if (sbn.packageName != packageName && SuperXInspectorStore.isSuperX(sbn.notification.extras)) {
            SuperXInspectorStore.capture(this, sbn)
        }

        val prefs = getSharedPreferences("experimental_prefs", MODE_PRIVATE)
        val isCastingEnabled = prefs.getBoolean("cast_notifications", false)

        if (!isCastingEnabled) return

        // Ignore our own notifications
        if (sbn.packageName == packageName) return

        // Check app filter (if set)
        val enabledApps = prefs.getStringSet("cast_enabled_apps", null)
        if (enabledApps != null && !enabledApps.contains(sbn.packageName)) {
            //Log.d("NotificationCast", "Skipping notification from ${sbn.packageName} (not in filter)")
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
            //Log.d("NotificationCast", "Skipping notification from ${sbn.packageName} (no title/text)")
            return
        }

        // Edge case: if title or text is exactly the package name, ignore
        val pkg = sbn.packageName
        if (rawTitle.equals(pkg, ignoreCase = true) || rawText.equals(pkg, ignoreCase = true)) {
            //Log.d("NotificationCast", "Skipping notification from $pkg (content matches package name)")
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

        // Render and Save Notification Preview
        try {
            val remoteViews = sbn.notification.bigContentView 
                ?: sbn.notification.contentView 
            
            if (remoteViews != null) {
                val bitmap = renderRemoteViewsToBitmap(remoteViews)
                if (bitmap != null) {
                    val rendersDir = File(filesDir, "renders")
                    if (!rendersDir.exists()) rendersDir.mkdirs()
                    val renderFile = File(rendersDir, "${sbn.packageName}.png")
                    FileOutputStream(renderFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            }
            
            // Save Small Icon for preview
            if (Build.VERSION.SDK_INT >= 23) {
                val smallIcon = sbn.notification.smallIcon
                if (smallIcon != null) {
                    val smallIconDrawable = smallIcon.loadDrawable(this)
                    val smallIconBitmap = smallIconDrawable?.toBitmap()
                    if (smallIconBitmap != null) {
                        val rendersDir = File(filesDir, "renders")
                        if (!rendersDir.exists()) rendersDir.mkdirs()
                        val smallIconFile = File(rendersDir, "${sbn.packageName}_small_icon.png")
                        FileOutputStream(smallIconFile).use { out ->
                            smallIconBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationCast", "Failed to render notification preview", e)
        }

        val sourceApp = try {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        //Log.d("NotificationCast", "Casting notification from $sourceApp (${sbn.packageName})")

        // Read per-app customization
        val castMode = prefs.getString("cast_mode", "live_updates")
        
        fun resolveText(source: String?): String {
            return when (source) {
                "title" -> finalTitle
                "subtext" -> if (rawSubText.isNotEmpty()) rawSubText else finalText
                "titletext" -> "$finalTitle • $finalText"
                else -> finalText
            }
        }
        
        fun applyRegex(rawText: String, regexStr: String?): String {
            if (regexStr.isNullOrEmpty()) return rawText
            return try {
                val match = Regex(regexStr).find(rawText)
                if (match != null) {
                    if (match.groups.size > 1) match.groupValues.drop(1).joinToString(" ") else match.value
                } else rawText
            } catch (e: Exception) { rawText }
        }

        var finalChipText = ""
        var hyperLeftText = ""
        var hyperMainText = ""
        var originLeftText = ""
        var originRightText = ""
        var originTemplate = 0
        var originRightTemplate = 0

        if (castMode == "hyperisland") {
            val leftSource = prefs.getString("${sbn.packageName}_hyper_left_source", "title")
            val leftRegex = prefs.getString("${sbn.packageName}_hyper_left_regex", "")
            hyperLeftText = applyRegex(resolveText(leftSource), leftRegex)

            val mainSource = prefs.getString("${sbn.packageName}_hyper_main_source", "text")
            val mainRegex = prefs.getString("${sbn.packageName}_hyper_main_regex", "")
            hyperMainText = applyRegex(resolveText(mainSource), mainRegex)
        } else if (castMode == "originisland") {
            val leftSource = prefs.getString("${sbn.packageName}_origin_left_source", "title")
            val leftRegex = prefs.getString("${sbn.packageName}_origin_left_regex", "")
            originLeftText = applyRegex(resolveText(leftSource), leftRegex)

            val rightSource = prefs.getString("${sbn.packageName}_origin_right_source", "text")
            val rightRegex = prefs.getString("${sbn.packageName}_origin_right_regex", "")
            originRightText = applyRegex(resolveText(rightSource), rightRegex)

            // 0 = Auto: PlaygroundService picks the template by content (e.g. progress when present)
            originTemplate = prefs.getInt("${sbn.packageName}_origin_template", 0)
            originRightTemplate = prefs.getInt("${sbn.packageName}_origin_right_template", 0)
        } else {
            val textSource = prefs.getString("${sbn.packageName}_text_source", "text")
            val regexStr = prefs.getString("${sbn.packageName}_regex_filter", "")
            finalChipText = applyRegex(resolveText(textSource), regexStr)
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
        
        // Extract multi-segment progress
        val segments = if (Build.VERSION.SDK_INT >= 33) {
            extras.getParcelableArrayList("android.progressSegments", android.os.Bundle::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelableArrayList<android.os.Bundle>("android.progressSegments")
        }
        val segmentsCount = if (segments != null) (segments.size).coerceAtLeast(0) else 0

        // Extract Large Icon
        val largeIcon = extras.get("android.largeIcon")

        // aosp workaround
        val limit7Char = prefs.getBoolean("limit_chip_7char", false)
        var processedChipText = if (limit7Char && finalChipText.length > 7) {
            finalChipText.take(7)
        } else {
            finalChipText
        }

        // Override shortcriticaltext with progress percentage
        val showPercent = prefs.getBoolean("show_progress_percentage", false)
        if (showPercent && hasProgress && progressMax > 0 && !isIndeterminate) {
            val percent = (progress * 100) / progressMax
            processedChipText = "$percent%"
        }

        // Unique ID for this cast
        val castId = (sbn.key.hashCode() and 0x7FFFFFFF) % 10000 + 20000

        // Deduping: Generate a key based on content that effects the UI
        val contentKey = "T:$finalTitle|X:$finalText|C:$processedChipText|L:$hyperLeftText|M:$hyperMainText|OL:$originLeftText|OR:$originRightText|OT:$originTemplate/$originRightTemplate|P:$progress/$progressMax/$isIndeterminate"
        
        if (castMode == "hyperisland") {
            //Log.d("HyperIsland", "Extracted -> Left: '$hyperLeftText', Main: '$hyperMainText' [Key: $contentKey]")
        }

        if (lastNotificationContent[castId] == contentKey) {
            // //Log.d("NotificationCast", "Skipping redundant update for $sourceApp ($pkg)")
            return
        }
        lastNotificationContent[castId] = contentKey

        // Forward to PlaygroundService
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("title", finalTitle)
            putExtra("text", finalText)
            putExtra("subtext", rawSubText)
            putExtra("source_app", sourceApp)
            val limit7Char = prefs.getBoolean("limit_chip_7char", false)
            var processedChipText = if (limit7Char && finalChipText.length > 7 && castMode != "hyperisland") {
                finalChipText.take(7)
            } else {
                finalChipText
            }

            // Override shortcriticaltext with progress percentage
            val showPercent = prefs.getBoolean("show_progress_percentage", false)
            if (showPercent && hasProgress && progressMax > 0 && !isIndeterminate && castMode != "hyperisland") {
                val percent = (progress * 100) / progressMax
                processedChipText = "$percent%"
            }

            putExtra("status_chip_text", processedChipText)
            
            if (castMode == "hyperisland") {
                putExtra("hyper_left_text", hyperLeftText)
                putExtra("hyper_main_text", hyperMainText)
            }

            if (castMode == "originisland") {
                if (originLeftText.isNotBlank()) putExtra("oi_left_content", originLeftText)
                if (originRightText.isNotBlank()) putExtra("oi_right_content", originRightText)
                // 0 (or out-of-range) is treated as Auto by PlaygroundService
                putExtra("oi_template", originTemplate)
                putExtra("oi_right_template", originRightTemplate)
            }

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
                putExtra("progress_segments", segmentsCount)
            } else {
                putExtra("show_progress", false)
            }
            
            // Pass Large Icon
            if (largeIcon != null) {
                if (Build.VERSION.SDK_INT >= 23 && largeIcon is android.graphics.drawable.Icon) {
                    putExtra("large_icon_obj", largeIcon)
                } else if (largeIcon is android.graphics.Bitmap) {
                    putExtra("large_icon_bitmap", largeIcon)
                }
            }
            
            putExtra("cast_mode", castMode)

            putExtra("is_promoted", true)
            putExtra("when", sbn.notification.`when`)
            
            // Pass original RemoteViews for miui.focus.rv injection
            val sourceRv = sbn.notification.bigContentView ?: sbn.notification.contentView
            if (sourceRv != null) {
                putExtra("miui_rv", sourceRv)
            }
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
                //Log.d("NotificationCast", "Found interactive button in RemoteViews: $titleString")
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

    private fun renderRemoteViewsToBitmap(remoteViews: RemoteViews): Bitmap? {
        return try {
            val parent = FrameLayout(this)
            val view = remoteViews.apply(this, parent)
            
            // Measure based on screen width
            val displayMetrics = resources.displayMetrics
            val widthSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.AT_MOST)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            
            view.measure(widthSpec, heightSpec)
            val width = view.measuredWidth.coerceAtLeast(1)
            val height = view.measuredHeight.coerceAtLeast(1)
            
            view.layout(0, 0, width, height)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            Log.e("NotificationCast", "RemoteViews rendering failed", e)
            null
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
        val castId = (sbn.key.hashCode() and 0x7FFFFFFF) % 10000 + 20000
        
        // Clear deduping cache
        lastNotificationContent.remove(castId)

        //Log.d("NotificationCast", "Removing cast notification for ${sbn.packageName} (ID: $castId)")

        // Send cancel action to PlaygroundService
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_CANCEL
            putExtra("id", castId)
        }
        startService(intent)
    }
}
