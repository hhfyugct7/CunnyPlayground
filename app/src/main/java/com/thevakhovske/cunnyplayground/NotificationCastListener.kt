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

import java.util.concurrent.ConcurrentHashMap

class NotificationCastListener : NotificationListenerService() {

    companion object {
        val activeControllers = ConcurrentHashMap<Int, android.media.session.MediaController>()
        val activeRemoteViews = ConcurrentHashMap<Int, android.widget.RemoteViews>()
        val activeSmallIcons = ConcurrentHashMap<Int, android.graphics.drawable.Icon>()
        val activeLargeIcons = ConcurrentHashMap<Int, android.graphics.drawable.Icon>()
        val activeLargeBitmaps = ConcurrentHashMap<Int, android.graphics.Bitmap>()
        var lastActiveMediaCastId: Int? = null
    }
    
    private val lastNotificationContent = HashMap<Int, String>()
    // sbn.key -> layout parity, flipped each poll so the media RemoteViews alternates its layoutId and
    // OriginOS fully re-applies it (seekbar / play-state actually refresh). Keyed per session so two
    // simultaneous players each alternate independently.
    private val mediaRvFlip = HashMap<String, Boolean>()
    private val mediaCallbacks = HashMap<String, Pair<android.media.session.MediaController, android.media.session.MediaController.Callback>>()
    private val pollHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun <T : android.os.Parcelable> android.os.Bundle.getParcelableSafe(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= 33) {
            this.getParcelable(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            this.getParcelable(key) as? T
        }
    }

    private fun getComplexity(rv: RemoteViews?): Int {
        if (rv == null) return -1
        return try {
            val field = rv.javaClass.getDeclaredField("mActions")
            field.isAccessible = true
            (field.get(rv) as? List<*>)?.size ?: 0
        } catch (e: Throwable) {
            0
        }
    }

    private fun getComplexRemoteViews(notification: Notification): RemoteViews? {
        val options = listOfNotNull(
            notification.bigContentView,
            notification.headsUpContentView,
            notification.contentView
        )
        return options.maxByOrNull { getComplexity(it) }
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                val prefs = getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)
                val isCastingEnabled = prefs.getBoolean("cast_notifications", false)
                val isMediaCastingEnabled = prefs.getBoolean("cast_media_sessions", false)
                val ignoredMediaApps = prefs.getStringSet("cast_ignored_media_apps", emptySet()) ?: emptySet()

                activeNotifications?.forEach { sbn ->
                    val extras = sbn.notification.extras
                    val mediaSession = extras.getParcelableSafe("android.mediaSession", android.media.session.MediaSession.Token::class.java)
                    
                    if (mediaSession != null) {
                        if (isMediaCastingEnabled && !ignoredMediaApps.contains(sbn.packageName)) {
                            // Only poll/process if the controller is actively playing to save battery
                            val controller = mediaCallbacks[sbn.key]?.first
                            val isPlaying = controller?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
                            if (isPlaying) {
                                processMediaNotification(sbn, mediaSession)
                            }
                        }
                    } else if (isCastingEnabled && sbn.isOngoing && extras.getBoolean("android.showChronometer", false)) {
                        // Re-process regular ongoing notifications with chronometer
                        onNotificationPosted(sbn)
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationCast", "Error in pollRunnable", e)
            }
            pollHandler.postDelayed(this, 1000)
        }
    }

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
        pollHandler.post(pollRunnable)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        pollHandler.removeCallbacks(pollRunnable)
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
        val isMediaCastingEnabled = prefs.getBoolean("cast_media_sessions", false)

        if (!isCastingEnabled && !isMediaCastingEnabled) return

        val extras = sbn.notification.extras
        
        // --- MEDIA SESSION INTERCEPTION ---
        val mediaSession = extras.getParcelableSafe("android.mediaSession", android.media.session.MediaSession.Token::class.java)
        val template = extras.getString("android.template")
        val isMediaTemplate = template == "android.app.Notification\$MediaStyle"
        
        if (mediaSession != null || isMediaTemplate) {
            val ignoredMediaApps = prefs.getStringSet("cast_ignored_media_apps", emptySet()) ?: emptySet()
            if (isMediaCastingEnabled && !ignoredMediaApps.contains(sbn.packageName) && mediaSession != null) {
                processMediaNotification(sbn, mediaSession)
            }
            // Media notifications are exclusively handled by the Media Player Island if enabled.
            // If disabled or ignored, we also don't want them polluting the regular Live Updates.
            return 
        }

        if (!isCastingEnabled) return

        // Skip group SUMMARY notifications. They're invisible containers for a group of child
        // notifications and carry generic placeholder content (a bare "Notification" + app name).
        // An app like Google posts the summary alongside the real notification, so casting it spawns a
        // phantom second island next to the genuine one (the reported init/final double-island bug).
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            //Log.d("NotificationCast", "Skipping group summary from ${sbn.packageName}")
            return
        }

        // Check app filter (if set) for regular notifications
        val enabledApps = prefs.getStringSet("cast_enabled_apps", null)
        if (enabledApps != null && !enabledApps.contains(sbn.packageName)) {
            //Log.d("NotificationCast", "Skipping notification from ${sbn.packageName} (not in filter)")
            return
        }
        val titleExtra = extras.getCharSequence("android.title")
        val textExtra = extras.getCharSequence("android.text")
        val subTextExtra = extras.getCharSequence("android.subText")

        val rawTitle = titleExtra?.toString()?.trim() ?: ""
        val rawText = textExtra?.toString()?.trim() ?: ""
        val rawSubText = subTextExtra?.toString()?.trim() ?: ""

        // Intercept regardless of empty or package-name-matching content (previously these were
        // skipped, dropping otherwise-valid notifications). Substitute a sane title and allow empty
        // text: title falls back to "Notification", text stays "" when blank.
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
            val remoteViews = getComplexRemoteViews(sbn.notification)
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

        // Orange branch: OriginIsland is the only cast mode.
        val castMode = "originisland"
        
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
        var originLeftText = ""
        var originRightText = ""
        var originTemplate = 0
        var originRightTemplate = 0

        run {
            val leftSource = prefs.getString("${sbn.packageName}_origin_left_source", "title")
            val leftRegex = prefs.getString("${sbn.packageName}_origin_left_regex", "")
            originLeftText = applyRegex(resolveText(leftSource), leftRegex)

            val rightSource = prefs.getString("${sbn.packageName}_origin_right_source", "text")
            val rightRegex = prefs.getString("${sbn.packageName}_origin_right_regex", "")
            originRightText = applyRegex(resolveText(rightSource), rightRegex)

            // 0 = Auto: PlaygroundService picks the template by content (e.g. progress when present)
            originTemplate = prefs.getInt("${sbn.packageName}_origin_template", 0)
            originRightTemplate = prefs.getInt("${sbn.packageName}_origin_right_template", 0)
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

        val segmentColors = IntArray(segmentsCount)
        if (segments != null) {
            for (i in 0 until segmentsCount) {
                val bundle = segments[i]
                var c = bundle.getInt("color", 0)
                if (c == 0) c = bundle.getInt("android.color", 0)
                segmentColors[i] = c
            }
        }

        // Extract shortCriticalText
        val shortCriticalText = extras.getCharSequence("android.shortCriticalText")?.toString() ?: extras.getString("android.shortCriticalText")

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

        // Calculate chronometer or shortCriticalText BEFORE deduplication
        val showChronometer = extras.getBoolean("android.showChronometer", false)
        if (showChronometer && castMode != "hyperisland") {
            val base = sbn.notification.`when`
            if (base > 0) {
                val elapsedMs = System.currentTimeMillis() - base
                val totalSeconds = Math.abs(elapsedMs) / 1000
                val seconds = totalSeconds % 60
                val minutes = (totalSeconds / 60) % 60
                val hours = totalSeconds / 3600
                processedChipText = if (hours > 0) {
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%02d:%02d", minutes, seconds)
                }
            }
        } else if (!shortCriticalText.isNullOrEmpty() && castMode != "hyperisland") {
            processedChipText = shortCriticalText
        }

        // Unique ID for this cast
        val castId = (sbn.key.hashCode() and 0x7FFFFFFF) % 10000 + 20000

        // Bypassing deduplication completely to ensure 100% reliable updates on any notification change.

        // Forward to PlaygroundService
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("title", finalTitle)
            putExtra("text", finalText)
            putExtra("subtext", rawSubText)
            putExtra("source_app", sourceApp)
            putExtra("source_pkg", sbn.packageName)
            // Assign the finalized chip text
            putExtra("status_chip_text", processedChipText)
            putExtra("click_resp", sbn.notification.contentIntent)
            
            if (originLeftText.isNotBlank()) putExtra("oi_left_content", originLeftText)
            if (originRightText.isNotBlank()) putExtra("oi_right_content", originRightText)
            // 0 (or out-of-range) is treated as Auto by PlaygroundService
            putExtra("oi_template", originTemplate)
            putExtra("oi_right_template", originRightTemplate)

            putExtra("id", castId)
            putExtra("icon_res", R.drawable.ic_alert)
            putExtra("force_update_tick", System.currentTimeMillis())
            if (iconToUse != null) {
                activeSmallIcons[castId] = iconToUse
            } else {
                activeSmallIcons.remove(castId)
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
                putExtra("progress_segment_colors", segmentColors)
            } else {
                putExtra("show_progress", false)
            }
            putExtra("is_ongoing", sbn.isOngoing)
            
            // Pass Large Icon via memory
            if (largeIcon != null) {
                if (Build.VERSION.SDK_INT >= 23 && largeIcon is android.graphics.drawable.Icon) {
                    activeLargeIcons[castId] = largeIcon
                } else if (largeIcon is android.graphics.Bitmap) {
                    activeLargeBitmaps[castId] = largeIcon
                }
            } else {
                activeLargeIcons.remove(castId)
                activeLargeBitmaps.remove(castId)
            }
            
            putExtra("cast_mode", castMode)

            putExtra("is_promoted", true)
            putExtra("when", sbn.notification.`when`)
            putExtra("notification_color", sbn.notification.color)
            
            if (showChronometer) {
                putExtra("chronometer_base", sbn.notification.`when`)
                if (Build.VERSION.SDK_INT >= 24) {
                    putExtra("chronometer_count_down", extras.getBoolean("android.chronometerCountDown", false))
                }
            }

            // The source notification's own content intent → OriginIsland tap opens the source app.
            sbn.notification.contentIntent?.let { putExtra("source_content_intent", it) }

            // Pass original RemoteViews for miui.focus.rv injection via memory
            val sourceRv = getComplexRemoteViews(sbn.notification)
            if (sourceRv != null) {
                activeRemoteViews[castId] = sourceRv
            } else {
                activeRemoteViews.remove(castId)
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

        // Clean up media callbacks and controllers
        mediaCallbacks.remove(sbn.key)?.let { (oldController, oldCallback) ->
            try {
                oldController.unregisterCallback(oldCallback)
            } catch (e: Exception) {}
        }
        activeControllers.remove(castId)
        activeRemoteViews.remove(castId)
        activeSmallIcons.remove(castId)
        activeLargeIcons.remove(castId)
        activeLargeBitmaps.remove(castId)

        //Log.d("NotificationCast", "Removing cast notification for ${sbn.packageName} (ID: $castId)")

        // Send cancel action to PlaygroundService
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_CANCEL
            putExtra("id", castId)
        }
        startService(intent)
    }

    private fun processMediaNotification(sbn: StatusBarNotification, sessionToken: android.media.session.MediaSession.Token) {
      val castId = (sbn.key.hashCode() and 0x7FFFFFFF) % 10000 + 20000
      try {
        var controller = mediaCallbacks[sbn.key]?.first
        if (controller == null || controller.sessionToken != sessionToken) {
            mediaCallbacks[sbn.key]?.let { (oldController, oldCallback) ->
                try {
                    oldController.unregisterCallback(oldCallback)
                } catch (e: Exception) {}
            }
            try {
                val newController = android.media.session.MediaController(this, sessionToken)
                val callback = object : android.media.session.MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                        Log.d("MediaCast", "Callback onPlaybackStateChanged state=${state?.state}")
                        processMediaNotification(sbn, sessionToken)
                    }
                    override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                        Log.d("MediaCast", "Callback onMetadataChanged")
                        processMediaNotification(sbn, sessionToken)
                    }
                }
                newController.registerCallback(callback, pollHandler)
                mediaCallbacks[sbn.key] = Pair(newController, callback)
                controller = newController
            } catch (e: Exception) {
                Log.e("NotificationCast", "Failed to create/register MediaController", e)
            }
        }

        val activeController = controller ?: android.media.session.MediaController(this, sessionToken)
        activeControllers[castId] = activeController

        val metadata = activeController.metadata
        val playbackState = activeController.playbackState

        val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
            ?: sbn.notification.extras.getCharSequence("android.title")?.toString()
            ?: "Unknown"
        val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
            ?: sbn.notification.extras.getCharSequence("android.text")?.toString()
            ?: "Unknown"
        
        val durationMs = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val isPlaying = playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        
        if (isPlaying && lastActiveMediaCastId != castId) {
            lastActiveMediaCastId?.let { oldId ->
                // Cancel the old media island if a new one starts playing
                val cancelIntent = Intent(this, PlaygroundService::class.java).apply {
                    action = PlaygroundService.ACTION_CANCEL
                    putExtra("id", oldId)
                }
                startService(cancelIntent)
            }
            lastActiveMediaCastId = castId
        }
        
        // Dynamically calculate current position using elapsedRealtime, because playbackState.position is a static snapshot
        val positionMs = if (isPlaying && playbackState != null) {
            val timeDelta = android.os.SystemClock.elapsedRealtime() - playbackState.lastPositionUpdateTime
            (playbackState.position + (timeDelta * playbackState.playbackSpeed)).toLong()
        } else {
            playbackState?.position ?: 0L
        }

        // Diagnostic: confirms the 1s poll is firing and what live state it reads each tick. If pos/state
        // advance here but the card doesn't, the freeze is render-side; if this stops logging, the poll died.
        Log.d("MediaCast", "tick pkg=${sbn.packageName} state=${playbackState?.state} playing=$isPlaying pos=${positionMs}ms/${durationMs}ms")

        // Alternate the layoutId every tick so OriginOS fully re-applies the card (not reapply()),
        // guaranteeing the seekbar and play/pause icon refresh.
        val mediaFlip = !(mediaRvFlip[sbn.key] ?: false)
        mediaRvFlip[sbn.key] = mediaFlip
        val mediaLayout = if (mediaFlip) R.layout.layout_origin_media_player else R.layout.layout_origin_media_player_alt
        val rv = android.widget.RemoteViews(packageName, mediaLayout)
        // OriginOS only does a full re-apply of a custom SuperX template (template 7) when the PREVIOUS
        // RemoteViews carried FLAG_REAPPLY_DISALLOWED — see CustomSuperXTemplate.loadContentForRemoteViews,
        // which otherwise calls reapply() that no-ops on our card and freezes the seekbar/play-state.
        // Setting the flag on every frame forces the full apply() path each poll, exactly like OriginOS
        // does for its own live notifications (createBigContentView().addFlags(1)). addFlags()/
        // FLAG_REAPPLY_DISALLOWED are @hide in the public SDK, so set it reflectively (value 1).
        try {
            android.widget.RemoteViews::class.java
                .getMethod("addFlags", Int::class.javaPrimitiveType)
                .invoke(rv, 1)
        } catch (e: Throwable) {
            Log.w("NotificationCast", "RemoteViews.addFlags(FLAG_REAPPLY_DISALLOWED) unavailable", e)
        }
        rv.setTextViewText(R.id.media_track_title, title)
        rv.setTextViewText(R.id.media_artist_name, artist)
        
        fun formatTime(ms: Long): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

        rv.setTextViewText(R.id.media_time_current, formatTime(positionMs))
        rv.setTextViewText(R.id.media_time_total, formatTime(durationMs))
        if (durationMs > 0) {
            rv.setProgressBar(R.id.media_progress_bar, durationMs.toInt(), positionMs.toInt(), false)
        } else {
            rv.setProgressBar(R.id.media_progress_bar, 100, 0, false)
        }
        
        var waveColor = android.graphics.Color.parseColor("#3083F0")
        val rawAlbumArt = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: sbn.notification.extras.getParcelableSafe("android.picture", android.graphics.Bitmap::class.java)
            ?: sbn.notification.extras.getParcelableSafe("android.largeIcon", android.graphics.Bitmap::class.java)

        // Only accept a real, non-empty bitmap; anything else → placeholder (a missing/invalid/0-size
        // or hardware bitmap otherwise crashes when the RemoteViews is applied in the system process).
        var safeAlbumArt: android.graphics.Bitmap? = null
        if (rawAlbumArt != null && !rawAlbumArt.isRecycled && rawAlbumArt.width > 0 && rawAlbumArt.height > 0) {
            try {
                var bmp = rawAlbumArt
                // Hardware bitmaps can't be read back / parceled into a RemoteViews — software-copy first.
                if (bmp.config == android.graphics.Bitmap.Config.HARDWARE) {
                    bmp = bmp.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                }
                val maxDimen = Math.max(bmp.width, bmp.height)
                if (maxDimen > 256) {
                    val scale = 256f / maxDimen
                    bmp = android.graphics.Bitmap.createScaledBitmap(
                        bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true
                    )
                }
                // Normalize any remaining odd/unknown config (RGB_565, RGBA_F16, null) to a clean
                // ARGB_8888 copy so neither our process nor OriginOS chokes when applying the RemoteViews.
                if (bmp.config != android.graphics.Bitmap.Config.ARGB_8888) {
                    bmp = bmp.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                }
                safeAlbumArt = if (!bmp.isRecycled && bmp.width > 0 && bmp.height > 0) bmp else null
            } catch (e: Throwable) {
                Log.e("NotificationCast", "Failed to scale/convert album art", e)
                safeAlbumArt = null
            }
        }

        // Missing/weird cover → replace with the placeholder cover, rasterized to a real bitmap so it
        // is used uniformly for the card image AND the capsule icon (not just a card-only resource).
        val hasRealArt = safeAlbumArt != null
        if (safeAlbumArt == null) {
            safeAlbumArt = try {
                androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_media_placeholder)
                    ?.toBitmap(256, 256)
            } catch (e: Throwable) {
                Log.e("NotificationCast", "Placeholder cover render failed", e)
                null
            }
        }
        val cover = safeAlbumArt
        if (cover != null) {
            rv.setImageViewBitmap(R.id.media_album_art, cover)
            // Only derive the accent from a real cover; the neutral placeholder keeps the default blue.
            if (hasRealArt) {
                try {
                    val palette = androidx.palette.graphics.Palette.from(cover).generate()
                    waveColor = palette.getVibrantColor(palette.getDominantColor(waveColor))
                } catch (e: Exception) {
                    Log.e("NotificationCast", "Palette extraction failed", e)
                }
            }
        } else {
            rv.setImageViewResource(R.id.media_album_art, R.drawable.ic_media_placeholder)
        }

        // Tie the scrubber fill to the album-art accent (Apple Music-style). setColorStateList(...,
        // "setProgressTintList", ...) is API 31+; minSdk here is 36, but guard anyway.
        try {
            rv.setColorStateList(
                R.id.media_progress_bar, "setProgressTintList",
                android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            )
        } catch (e: Throwable) {
            Log.w("NotificationCast", "progress tint unavailable", e)
        }

        val createPi = { action: String ->
            android.app.PendingIntent.getBroadcast(
                this, action.hashCode(),
                android.content.Intent(this, MediaControlReceiver::class.java).apply {
                    this.action = action
                    putExtra("cast_id", castId)
                    putExtra(MediaControlReceiver.EXTRA_TOKEN, sessionToken)
                },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }
        
        rv.setOnClickPendingIntent(R.id.media_btn_play_pause, createPi(MediaControlReceiver.ACTION_PLAY_PAUSE))
        rv.setOnClickPendingIntent(R.id.media_btn_next, createPi(MediaControlReceiver.ACTION_NEXT))
        rv.setOnClickPendingIntent(R.id.media_btn_prev, createPi(MediaControlReceiver.ACTION_PREV))
        
        rv.setImageViewResource(R.id.media_btn_play_pause, if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play)
        
        val intent = android.content.Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", castId)
            putExtra("cast_mode", "originisland")
            putExtra("oi_template", 7)
            putExtra("oi_custom_template", rv)
            putExtra("oi_right_template", 6)
            
            // Capsule icon: the album cover, or the placeholder cover when art is missing/invalid.
            if (cover != null && Build.VERSION.SDK_INT >= 23) {
                val capsuleIcon = android.graphics.Bitmap.createScaledBitmap(cover, 64, 64, true)
                putExtra("small_icon_obj", android.graphics.drawable.Icon.createWithBitmap(capsuleIcon))
            }
            
            // This is CRITICAL: PlaygroundService deduplicates intents based on extra hash.
            // RemoteViews toString() does not change when we call setProgressBar. 
            // We MUST include positionMs so PlaygroundService sees a new update and posts it!
            putExtra("media_position", positionMs)
            
            val colors = java.util.ArrayList<String>()
            colors.add(String.format("#%06X", 0xFFFFFF and waveColor))
            putStringArrayListExtra("oi_wave_color", colors)
            
            putExtra("source_pkg", sbn.packageName)
            val timeText = formatTime(positionMs)
            
            putExtra("title", "\u200B")
            putExtra("text", "\u200B")
            putExtra("status_chip_text", timeText)
            putExtra("force_update_tick", System.currentTimeMillis())
            putExtra("click_resp", sbn.notification.contentIntent)
        }
        startService(intent)
      } catch (e: Throwable) {
        Log.e("NotificationCast", "processMediaNotification failed; skipping this frame", e)
      }
    }
}
