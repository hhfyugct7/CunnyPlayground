package com.thevakhovske.cunnyplayground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import android.graphics.Bitmap
import android.widget.RemoteViews
import org.json.JSONObject
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperPicture

class PlaygroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_REPLICATE = "ACTION_REPLICATE"
        const val CHANNEL_ID = "live_updates_channel"
        const val HYPER_CHANNEL_ID = "hyperslop_channel"
        const val ORIGIN_CHANNEL_ID = "originisland_channel"
        const val NOTIFICATION_ID = 1001
    }

    private val activeIds = mutableSetOf<Int>()
    // notificationId -> SuperX scene, for OriginIsland notifications that need an explicit "end".
    private val originScenes = HashMap<Int, String>()
    // notificationId -> monotonically increasing changedRecord so OriginOS accepts each update.
    private val originChangeRecord = HashMap<Int, Int>()
    // notificationId -> parity flag, flipped each post so the relayed custom template alternates its
    // layoutId. That forces OriginOS to fully re-inflate (apply, not reapply) so a nested addView()
    // source RemoteViews actually refreshes its live internals every update.
    private val originRvToggle = HashMap<Int, Boolean>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isForegroundActive = false
    private lateinit var notificationManager: NotificationManager

    private val isMiuiGlobalBuild: Boolean by lazy {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            val region = get.invoke(null, "ro.miui.region") as String
            region.isNotBlank() && region != "CN"
        } catch (e: Exception) {
            false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // OriginOS requires the SuperX scene whitelist to be registered before any island shows.
        OriginIslandBuilder.grantScenes(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isForegroundActive) {
            createNotificationChannel(CHANNEL_ID)
            val anchorNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background notification")
                .setContentText("Ignore")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setSilent(true)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= 29) {
                try {
                    if (Build.VERSION.SDK_INT >= 34) {
                        startForeground(9999, anchorNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(9999, anchorNotification)
                    }
                } catch (e: Exception) {
                    startForeground(9999, anchorNotification)
                }
            } else {
                startForeground(9999, anchorNotification)
            }
            isForegroundActive = true
        }

        when (intent?.action) {
            ACTION_START -> startPromotedNotification(intent)
            ACTION_CANCEL -> cancelNotification(intent)
            ACTION_REPLICATE -> replicateOrigin(intent)
            ACTION_STOP -> {
                // End any active OriginIsland atomic notifications so their islands don't linger.
                val hadOrigins = originScenes.isNotEmpty()
                HashMap(originScenes).forEach { (id, scene) -> endOrigin(id, scene) }
                originScenes.clear()
                originChangeRecord.clear()
                originRvToggle.clear()
                val toCancel = activeIds.toList()
                activeIds.clear()
                val tearDown = {
                    toCancel.forEach {
                        notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, it)
                        notificationManager.cancel(it)
                    }
                    stopForeground(true)
                    stopSelf()
                }
                // Let the SuperX engine process the end(s) before pulling the host notifications.
                if (hadOrigins) mainHandler.postDelayed(tearDown, 350) else tearDown()
            }
        }
        return START_NOT_STICKY
    }

    private fun cancelNotification(intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (id == -1) return
        // If this id was an OriginIsland notification, tell OriginOS to end the atomic notification
        // first (a plain cancel leaves the island/capsule on screen). The scene comes from our map
        // or, for swipe-to-dismiss delete intents, from the intent itself.
        val scene = originScenes.remove(id) ?: intent.getStringExtra("oi_scene")
        if (scene != null) {
            endOrigin(id, scene)
            // Remove the host shortly after, giving the SuperX engine time to process the end.
            // Must cancel with the fixed tag — that's how OriginOS tracks the atomic notification.
            mainHandler.postDelayed({ notificationManager.cancel(OriginIslandConstants.SUPERX_TAG, id) }, 350)
        } else {
            notificationManager.cancel(id)
        }
        activeIds.remove(id)
        originChangeRecord.remove(id)
        originRvToggle.remove(id)
    }

    /**
     * Posts an operation=2 SuperX bundle on [id] so OriginOS dismisses the atomic notification/island.
     * Per 技术规范 §3.3 the end/cancel must use the fixed tag "VIVO_SUPERX_TAG"; a plain untagged
     * cancel creates a separate notification and leaves the island lingering.
     */
    private fun endOrigin(id: Int, scene: String) {
        try {
            OriginIslandBuilder.grantScenes(this)
            createNotificationChannel(ORIGIN_CHANNEL_ID)
            val endNb = NotificationCompat.Builder(this, ORIGIN_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("")
                .setContentText("")
                .setSilent(true)
                .setExtras(OriginIslandBuilder.buildEndBundle(scene))
            notificationManager.notify(OriginIslandConstants.SUPERX_TAG, id, endNb.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Inspector "Replicate": rebuilds a captured SuperX bundle (scene forced to NAVIGATION) and
     * re-posts it under our own package, so a notification sniffed from another app can be re-emitted
     * within the whitelisted NAVIGATION scene. Captured cross-app PendingIntents can't be reused, so
     * the card/capsule click falls back to launching our app.
     */
    private fun replicateOrigin(intent: Intent) {
        try {
            val recordId = intent.getStringExtra("record_id") ?: return
            val bundle = SuperXInspectorStore.rebuildBundle(this, recordId) ?: return
            OriginIslandBuilder.grantScenes(this)
            createNotificationChannel(ORIGIN_CHANNEL_ID)

            val notifId = 30000 + (recordId.hashCode() and 0x7FFF)
            activeIds.add(notifId)
            originScenes[notifId] = "NAVIGATION"

            val launch = packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(this, MainActivity::class.java)
            val launchPi = PendingIntent.getActivity(
                this, notifId, launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            bundle.putParcelable("notification.superx.clickResp", launchPi)

            val title = intent.getStringExtra("title")?.takeIf { it.isNotBlank() } ?: "SuperX Replica"
            val text = intent.getStringExtra("text") ?: ""

            val deleteIntent = Intent(this, PlaygroundService::class.java).apply {
                action = ACTION_CANCEL
                putExtra("id", notifId)
                putExtra("oi_scene", "NAVIGATION")
            }
            val deletePi = PendingIntent.getService(
                this, notifId, deleteIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val nb = NotificationCompat.Builder(this, ORIGIN_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setAutoCancel(false)
                .setDeleteIntent(deletePi)
                .setExtras(bundle)
            notificationManager.notify(OriginIslandConstants.SUPERX_TAG, notifId, nb.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPromotedNotification(intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Ongoing Task"
        val text = intent.getStringExtra("text") ?: "Live Update Active"
        val subtext = intent.getStringExtra("subtext")
        val notificationId = intent.getIntExtra("id", NOTIFICATION_ID)
        val iconRes = intent.getIntExtra("icon_res", R.mipmap.ic_launcher_round)
        val iconObj = if (Build.VERSION.SDK_INT >= 23) {
            NotificationCastListener.activeSmallIcons[notificationId] ?: intent.getParcelableExtraSafe("small_icon_obj", android.graphics.drawable.Icon::class.java)
        } else null 
        val sourceApp = intent.getStringExtra("source_app")
        val isPromoted = intent.getBooleanExtra("is_promoted", true)
        val statusChipText = intent.getStringExtra("status_chip_text")
        val showProgress = intent.getBooleanExtra("show_progress", true)
        val timestamp = intent.getLongExtra("when", System.currentTimeMillis())
        
        val castMode = intent.getStringExtra("cast_mode") ?: "live_updates"
        val targetChannel = if (castMode == "hyperisland") HYPER_CHANNEL_ID else CHANNEL_ID

        val largeIconObj = if (Build.VERSION.SDK_INT >= 23) {
            NotificationCastListener.activeLargeIcons[notificationId] ?: intent.getParcelableExtraSafe("large_icon_obj", android.graphics.drawable.Icon::class.java)
        } else null
        val largeIconBitmap = NotificationCastListener.activeLargeBitmaps[notificationId] ?: intent.getParcelableExtraSafe("large_icon_bitmap", android.graphics.Bitmap::class.java)
        val sourceRv = NotificationCastListener.activeRemoteViews[notificationId] ?: intent.getParcelableExtraSafe("miui_rv", android.widget.RemoteViews::class.java)
        val segmentsCount = intent.getIntExtra("progress_segments", 0)

        activeIds.add(notificationId)

        // OriginOS / vivo SuperX path: build the atomic notification + OriginIsland exactly like
        // superx_demo, on a clean builder, then bail out of the generic Live-Updates/HyperIsland flow.
        if (castMode == "originisland") {
            postOriginIsland(intent, notificationId, title, text, subtext, sourceApp, statusChipText, iconObj, iconRes)
            return
        }

        createNotificationChannel(targetChannel)

        val builder = NotificationCompat.Builder(this, targetChannel)
        
        if (largeIconObj != null && Build.VERSION.SDK_INT >= 23) {
            builder.setLargeIcon(largeIconObj)
        } else if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }
        
        if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
            builder.setSmallIcon(IconCompat.createFromIcon(this, iconObj))
        } else {
            builder.setSmallIcon(iconRes)
        }

        builder.setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(timestamp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)

        if (!sourceApp.isNullOrEmpty()) {
            builder.setSubText(sourceApp)
        }

        // Apply Actions
        val actions = intent.getParcelableArrayListExtra<Notification.Action>("actions")
        actions?.forEach { action ->
            val icon = if (Build.VERSION.SDK_INT >= 23) {
                action.getIcon()?.let { IconCompat.createFromIcon(this, it) }
            } else null
            
            val builderAction = NotificationCompat.Action.Builder(
                icon,
                action.title,
                action.actionIntent
            ).build()
            builder.addAction(builderAction)
        }

        if (castMode == "live_updates") {
            // Apply Progress
            val progress = intent.getIntExtra("progress", 0)
            val progressMax = intent.getIntExtra("progress_max", 0)
            val isIndeterminate = intent.getBooleanExtra("progress_indeterminate", false)
            
            if (showProgress) {
                builder.setProgress(progressMax, progress, isIndeterminate)
            }
    
            if (isPromoted) {
                try {
                    val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.java)
                    method.invoke(builder, true)
                } catch (e: Exception) {
                    builder.extras.putBoolean("android.app.extra.PROMOTED_ONGOING", true)
                }
            }
    
            if (!statusChipText.isNullOrEmpty()) {
                try {
                    val method = builder.javaClass.getMethod("setShortCriticalText", String::class.java)
                    method.invoke(builder, statusChipText)
                } catch (e: Exception) {
                    // Fail silently if API not available
                }
            }
    
            // Progress Style (Status Chip)
            if (showProgress && isPromoted && progressMax > 0) {
                try {
                    val progressStyle = NotificationCompat.ProgressStyle()
                    val totalDuration = 100000 // arbitrary base for percentage (Int)
                    val currentProgress = (progress.toDouble() / progressMax * totalDuration).toInt()
                    
                    // Apply Monet dynamic color accent to progress bar and icons
                    try {
                        val dynamicContext = com.google.android.material.color.DynamicColors.wrapContextIfAvailable(this)
                        val primaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.purple_500)
                        progressStyle.addProgressSegment(
                        NotificationCompat.ProgressStyle.Segment(totalDuration).setColor(primaryColor)
                    )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    progressStyle.setProgress(currentProgress)
                    builder.setStyle(progressStyle)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (showProgress) {
                // Standard progress only
                try {
                    val progressStyle = NotificationCompat.ProgressStyle()
                    builder.setStyle(progressStyle)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (castMode == "hyperisland") {
            try {
                // Initialize builder early to supply resources unconditionally
                val hyperBuilder = io.github.d4viddf.hyperisland_kit.HyperIslandNotification.Builder(
                    this,
                    "live_updates_recaster",
                    "Incoming Notification"
                )
                
                val hPic = if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                    io.github.d4viddf.hyperisland_kit.HyperPicture("default_icon", iconObj)
                } else {
                    io.github.d4viddf.hyperisland_kit.HyperPicture("default_icon", this, iconRes)
                }
                hyperBuilder.addPicture(hPic)
                
                // Register LargeIcon if available for heads-up/expanded views
                if (largeIconObj != null && Build.VERSION.SDK_INT >= 23) {
                    hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("big_icon", largeIconObj))
                } else if (largeIconBitmap != null) {
                    hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("big_icon", largeIconBitmap))
                }

                // Inject dummy resources so user's manual notif.json testing doesn't break HyperOS rendering
                hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("file_preview", this, iconRes))
                hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("upload_status", this, iconRes))

                val rawJson = intent.getStringExtra("raw_hyper_json")
                if (!rawJson.isNullOrBlank()) {
                    builder.extras.putString("miui.focus.param", rawJson)
                    builder.extras.putAll(hyperBuilder.buildResourceBundle())
                    
                    // We still need to set some defaults for the notification shade part
                    builder.setSmallIcon(iconRes)
                    builder.setContentTitle(title)
                    builder.setContentText(text)
                } else if (io.github.d4viddf.hyperisland_kit.HyperIslandNotification.isSupported(this)) {

                    hyperBuilder.setBaseInfo(
                        title = title,
                        content = text,
                        pictureKey = null
                    )
                    val islandText = statusChipText?.takeIf { it.isNotBlank() } ?: title
                    
                    val hyperLeftExtra = intent.getStringExtra("hyper_left_text")
                    val hyperMainExtra = intent.getStringExtra("hyper_main_text")

                    // Structure the Big Island Area to populate both pill sides properly
                    val picInfo = io.github.d4viddf.hyperisland_kit.models.PicInfo(1, "default_icon", false, false, 0, null, null, null)
                    
                    val leftTextInfoObj = io.github.d4viddf.hyperisland_kit.models.TextInfo(
                        title = hyperLeftExtra?.takeIf { it.isNotBlank() } ?: title,
                        content = null,
                        showHighlightColor = false,
                        narrowFont = null
                    )
                    
                    val imageTextInfoLeft = io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft(
                        1, picInfo, leftTextInfoObj, null
                    )
                    
                    val rootTextInfo = io.github.d4viddf.hyperisland_kit.models.TextInfo(
                        title = hyperMainExtra?.takeIf { it.isNotBlank() } ?: text, 
                        content = null, 
                        showHighlightColor = false, 
                        narrowFont = null
                    )
                    
                    // Pass rootTextInfo as the 3rd parameter mapping natively to "textInfo" block
                    hyperBuilder.setBigIslandInfo(imageTextInfoLeft, null, rootTextInfo, null, null, null)
                    
                    // Use setSmallIslandIcon to cleanly map a solo picInfo block matching tethering smallIslandArea
                    hyperBuilder.setSmallIslandIcon("default_icon")
                    
                    // The icon appearing in param_v2
                    hyperBuilder.setPicInfo(2, "default_icon")
                    
                    // Auto-popup priority
                    hyperBuilder.setIslandConfig(priority = 2)

                    //if (sourceRv != null) {
                        //builder.extras.putParcelable("miui.focus.rv", sourceRv)
                    //}
                    
                    // Add Interactive Actions (Buttons)
                    val originalActions = if (Build.VERSION.SDK_INT >= 34) {
                        intent.getParcelableArrayListExtra("actions", Notification.Action::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra<Notification.Action>("actions")
                    }

                    originalActions?.forEachIndexed { index, action ->
                        val actionIntent = action.actionIntent
                        if (actionIntent != null) {
                            val type = when {
                                actionIntent.isActivity -> 1
                                actionIntent.isBroadcast -> 2
                                actionIntent.isForegroundService || actionIntent.isService -> 3
                                else -> 2 // Default to broadcast
                            }

                            val actionIcon = action.getIcon()
                            val actionBitmap: Bitmap? = if (actionIcon != null) {
                                try {
                                    actionIcon.loadDrawable(this)?.toBitmap(128, 128)
                                } catch (e: Exception) {
                                    null
                                }
                            } else null

                            val hAction = if (actionBitmap != null) {
                                HyperAction(
                                    key = "action_$index",
                                    title = action.title?.toString() ?: "Action",
                                    bitmap = actionBitmap,
                                    pendingIntent = actionIntent,
                                    actionIntentType = type
                                )
                            } else {
                                HyperAction(
                                    key = "action_$index",
                                    title = action.title?.toString() ?: "Action",
                                    pendingIntent = actionIntent,
                                    actionIntentType = type
                                )
                            }
                            hyperBuilder.addAction(hAction)
                        }
                    }
                    
                    val jsonPayloadRaw = hyperBuilder.buildJsonParam()
                    val jsonObj = org.json.JSONObject(jsonPayloadRaw)
                    val paramV2 = jsonObj.optJSONObject("param_v2")
                    val useLargeIcon = (largeIconObj != null || largeIconBitmap != null)
                    if (paramV2 != null) {
                        if (useLargeIcon) {
                            // we love json injections
                            val iconTextInfo = org.json.JSONObject().apply {
                                val animIconInfo = org.json.JSONObject().apply {
                                    put("type", 0)
                                    put("src", "miui.focus.pic_big_icon")
                                    put("loop", true)
                                    put("autoplay", true)
                                }
                                put("animIconInfo", animIconInfo)
                                put("title", title)
                                put("content", text)
                            }
                            paramV2.put("iconTextInfo", iconTextInfo)
                            paramV2.remove("picInfo")
                        }
                        
                        paramV2.put("enableFloat", false)
                        paramV2.put("islandFirstFloat", false)
                        
                        // Inject hintInfo if subtext exists
                        if (!subtext.isNullOrBlank()) {
                            paramV2.put("hintInfo", org.json.JSONObject().apply {
                                put("type", 1)
                                put("title", subtext)
                            })
                        }
                        
                        // Implement Progress Bar Support (Manual Injection)
                        val progress = intent.getIntExtra("progress", 0)
                        val progressMax = intent.getIntExtra("progress_max", 0)
                        if (showProgress && progressMax > 0 && progress < progressMax) {
                            val progressPercent = (progress * 100) / progressMax
                            val hasSegments = segmentsCount > 0
                            
                            // 1. Root Progress
                            if (hasSegments) {
                                val multiProgressInfo = org.json.JSONObject().apply {
                                    put("progress", progressPercent)
                                    put("points", segmentsCount)
                                    put("color", "#34C759")
                                }
                                paramV2.put("multiProgressInfo", multiProgressInfo)
                            } else {
                                val rootProgressInfo = org.json.JSONObject().apply {
                                    put("progress", progressPercent)
                                    put("colorProgress", "#34C759")
                                }
                                paramV2.put("progressInfo", rootProgressInfo)
                            }
                            
                            // 2. Small Island Progress (requires combinePicInfo wrapper)
                            val paramIsland = paramV2.optJSONObject("param_island")
                            val smallArea = paramIsland?.optJSONObject("smallIslandArea")
                            val picInfo = smallArea?.optJSONObject("picInfo")
                            if (smallArea != null && picInfo != null) {
                                val combinePicInfo = org.json.JSONObject().apply {
                                    put("picInfo", picInfo)
                                    val progKey = "progressInfo"
                                    put(progKey, org.json.JSONObject().apply {
                                        put("progress", progressPercent)
                                        put("colorReach", "#34C759")
                                    })
                                }
                                smallArea.remove("picInfo")
                                smallArea.put("combinePicInfo", combinePicInfo)
                            }
                            
                            // 3. Big Island Progress (requires progressTextInfo block)
                            val bigArea = paramIsland?.optJSONObject("bigIslandArea")
                            if (bigArea != null) {
                                val progressTextInfo = org.json.JSONObject().apply {
                                    val progKey = "progressInfo"
                                    put(progKey, org.json.JSONObject().apply {
                                        put("progress", progressPercent)
                                        put("colorReach", "#34C759")
                                    })
                                }
                                bigArea.put("progressTextInfo", progressTextInfo)
                            }
                        }
                    }
                    val jsonPayload = jsonObj.toString()
                    val resBundle = hyperBuilder.buildResourceBundle()
                    
                    builder.extras.putString("miui.focus.param", jsonPayload)
                    builder.extras.putAll(resBundle)

                    if (isMiuiGlobalBuild) {
                        // inject original remoteview (from preliminary impl)
                        val sourceRv = NotificationCastListener.activeRemoteViews[notificationId] ?: intent.getParcelableExtra<RemoteViews>("miui_rv")
                        if (sourceRv != null) {
                            val wrappedRv = RemoteViews(packageName, R.layout.focus_rv_wrapper)
                            wrappedRv.removeAllViews(R.id.rv_wrapper_container)
                            wrappedRv.addView(R.id.rv_wrapper_container, sourceRv)
                            builder.extras.putParcelable("miui.focus.rv", wrappedRv)
                        }

                        // miui.focus.pic_ticker needs to exists according to notificationfocusmanager
                        val tPic = if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                            io.github.d4viddf.hyperisland_kit.HyperPicture("ticker", iconObj)
                        } else {
                            io.github.d4viddf.hyperisland_kit.HyperPicture("ticker", this, iconRes)
                        }
                        hyperBuilder.addPicture(tPic)
                        
                        builder.extras.putAll(hyperBuilder.buildResourceBundle())

                        // miui.focus.param.custom
                        val customJson = JSONObject().apply {
                            put("ticker", title)
                            put("tickerPic", "miui.focus.pic_ticker")
                            put("enableFloat", false)
                            put("updatable", true)
                            put("isShowNotification", true)
                            put("islandFirstFloat", false)
                            put("timeout", 10000)

                            val paramIsland = JSONObject().apply {
                                put("islandProperty", 1)
                                put("islandPriority", 2)
                                put("islandOrder", false)
                                put("dismissIsland", false)
                                put("maxSize", false)
                                put("needCloseAnimation", true)

                                val bigIslandArea = JSONObject().apply {
                                    val imageTextInfoLeft = JSONObject().apply {
                                        put("type", 1)
                                        put("picInfo", JSONObject().apply {
                                            put("type", 1)
                                            put("pic", "miui.focus.pic_default_icon")
                                            put("loop", false)
                                            put("autoplay", false)
                                            put("number", 0)
                                        })
                                        put("textInfo", JSONObject().apply {
                                            put("title", title)
                                            put("showHighlightColor", false)
                                        })
                                    }
                                    put("imageTextInfoLeft", imageTextInfoLeft)
                                    
                                    put("textInfo", JSONObject().apply {
                                        put("title", text)
                                        put("showHighlightColor", false)
                                    })
                                }
                                put("bigIslandArea", bigIslandArea)

                                val smallIslandArea = JSONObject().apply {
                                    put("picInfo", JSONObject().apply {
                                        put("type", 1)
                                        put("pic", "miui.focus.pic_default_icon")
                                        put("loop", false)
                                        put("autoplay", false)
                                        put("number", 0)
                                    })
                                }
                                put("smallIslandArea", smallIslandArea)
                            }
                            put("param_island", paramIsland)
                        }
                        builder.extras.putString("miui.focus.param.custom", customJson.toString())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Posts a vivo OriginOS SuperX atomic notification + OriginIsland, replicating
     * superx_demo's `SuperXTemplateDemo.sendVivoSuperXNotification`. Island parameters are taken
     * from explicit `oi_*` intent extras (OriginIsland playground) or derived from the captured
     * notification (re-caster).
     */
    private fun postOriginIsland(
        intent: Intent,
        notificationId: Int,
        title: String,
        text: String,
        subtext: String?,
        sourceApp: String?,
        statusChipText: String?,
        iconObj: Icon?,
        iconRes: Int
    ) {
        try {
            // Ensure scenes are granted (idempotent) and the channel exists.
            OriginIslandBuilder.grantScenes(this)
            createNotificationChannel(ORIGIN_CHANNEL_ID)

            // Resolve artwork: prefer the captured/notification icon, fall back to launcher res.
            val islandIcon: Icon = iconObj ?: Icon.createWithResource(this, iconRes)

            // Progress (0..100) for the progress island / progress-visual template.
            val progress = intent.getIntExtra("progress", 0)
            val progressMax = intent.getIntExtra("progress_max", 0)
            val isIndeterminate = intent.getBooleanExtra("progress_indeterminate", false)
            val showProgress = intent.getBooleanExtra("show_progress", false)
            val hasProgress = showProgress && progressMax > 0
            val derivedProgress = if (progressMax > 0) (progress * 100 / progressMax).coerceIn(0, 100) else 50

            // Large icon (source big icon) for richer base / info / nav / short artwork.
            val largeIcon: Icon? = run {
                val obj = if (Build.VERSION.SDK_INT >= 23) (NotificationCastListener.activeLargeIcons[notificationId] ?: intent.getParcelableExtraSafe("large_icon_obj", Icon::class.java)) else null
                obj ?: (NotificationCastListener.activeLargeBitmaps[notificationId] ?: intent.getParcelableExtraSafe("large_icon_bitmap", Bitmap::class.java))?.let { iconFromBitmapCapped(it) }
            }

            // Notification action buttons → OriginIsland clickable surfaces (capsule/island/card/images).
            val rawActions = if (Build.VERSION.SDK_INT >= 34) {
                intent.getParcelableArrayListExtra("actions", Notification.Action::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Notification.Action>("actions")
            }
            val originActions = rawActions.orEmpty().mapNotNull { a ->
                val pi = a.actionIntent ?: return@mapNotNull null
                OriginIslandBuilder.OriginAction(
                    title = a.title?.toString() ?: "Action",
                    icon = if (Build.VERSION.SDK_INT >= 23) a.getIcon() else null,
                    pendingIntent = pi
                )
            }
            // Island parameters: explicit (OriginIsland playground / per-app config) or Auto.
            // Auto: real button card when the source has actions (template 8 → multi-button row,
            // recovered from the decompiled SystemUI); progress card when it has progress; else base.
            // Buttons + progress coexist: buttons in the card, progress moves to the island ring.
            // Only the buttons template (8) is used for 2+ actions — but ButtonsSuperXTemplate does NOT
            // wire a whole-card click, so 0–1 action notifications stay on a tappable template (the lone
            // action shows as the in-card chip) so tapping the card still opens the source app.
            val sourceRv = NotificationCastListener.activeRemoteViews[notificationId] ?: intent.getParcelableExtraSafe("miui_rv", android.widget.RemoteViews::class.java)
            val isOngoing = intent.getBooleanExtra("is_ongoing", false)
            val shouldGenerateLiveUpdate = sourceRv == null && (isOngoing || hasProgress)

            val templateExtra = intent.getIntExtra("oi_template", 0)
            val template = when {
                templateExtra in 1..9 -> templateExtra
                sourceRv != null || shouldGenerateLiveUpdate -> OriginIslandConstants.TEMPLATE_NOTIF_CUSTOM
                originActions.size >= 2 -> OriginIslandConstants.TEMPLATE_BUTTONS
                hasProgress -> OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL
                else -> OriginIslandConstants.TEMPLATE_BASE
            }
            val rightTemplateExtra = intent.getIntExtra("oi_right_template", 0)
            // Right island: progress ring when there's progress, otherwise plain text (template 4
            // with no icon → "double-sided text", no capsule pill, no redundant second icon).
            val rightTemplate = when {
                rightTemplateExtra in 1..6 -> rightTemplateExtra
                !statusChipText.isNullOrEmpty() -> OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT
                hasProgress -> OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS
                else -> OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON
            }
            // Auto right-island text is icon-less; honor the icon only when a template was set explicitly.
            val showRightIcon = rightTemplateExtra in 1..6
            val leftContent = intent.getStringExtra("oi_left_content")
                ?: sourceApp?.takeIf { it.isNotBlank() } ?: title
            val rightContent = if (!statusChipText.isNullOrEmpty()) {
                statusChipText
            } else {
                intent.getStringExtra("oi_right_content")?.takeIf { it.isNotBlank() } ?: text
            }
            val extra1 = intent.getStringExtra("oi_extra1") ?: title
            val extra2 = intent.getStringExtra("oi_extra2") ?: text
            val extra3 = intent.getStringExtra("oi_extra3") ?: ""
            val extra4 = intent.getStringExtra("oi_extra4") ?: ""
            val scene = intent.getStringExtra("oi_scene") ?: "NAVIGATION"
            val navMsg = intent.getStringExtra("oi_nav_msg")
            val oiProgress = intent.getIntExtra("oi_progress", derivedProgress)
            // 0 (alpha 0) tells OriginOS to theme colors itself (§5.5) — avoids the right-side text
            // inheriting our bg/progress color. The playground passes explicit #RRGGBB colors.
            val bgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_bg_color"), 0)
            val fgColorRaw = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_fg_color"), 0)
            val notificationColor = intent.getIntExtra("notification_color", 0)
            val fgColor = if (fgColorRaw != 0) {
                fgColorRaw
            } else if (rightTemplate == OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) {
                notificationColor // Only inherit to fgColor for native progress ring so text isn't tinted
            } else {
                0
            }
            val keepDuration = intent.getIntExtra("oi_keep_duration", 0)
            val forceShow = intent.getBooleanExtra("oi_force_show", false)
            val dismissWhenKill = intent.getBooleanExtra("oi_dismiss_when_kill", true)
            val islandShowTime = intent.getIntExtra("oi_island_show_time", 0)
            val displaysVal = intent.getIntExtra("oi_displays", 0)
            val computedDisplays = if (displaysVal == 0) {
                OriginIslandConstants.DISPLAY_NOTIFICATION or OriginIslandConstants.DISPLAY_LOCKSCREEN or OriginIslandConstants.DISPLAY_STATUSBAR or OriginIslandConstants.DISPLAY_AOD
            } else {
                displaysVal
            }
            // Advanced / decompiled extras (playground custom controls)
            val cardBgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_card_bg_color"), 0)
            val keepScreenOn = intent.getBooleanExtra("oi_keep_screen_on", false)
            val disableInvert = intent.getBooleanExtra("oi_disable_invert", true)
            val lightColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_light_color"), 0)
            val lightMode = intent.getIntExtra("oi_light_mode", 0)
            val generatingStatus = intent.getIntExtra("oi_generating_status", 0)
            val iconStatusType = intent.getIntExtra("oi_icon_status_type", -1)
            val leftDoubleLine = intent.getStringArrayListExtra("oi_left_doubleline") ?: arrayListOf()
            val rightDoubleLine = intent.getStringArrayListExtra("oi_right_doubleline") ?: arrayListOf()
            val buttonTitles = intent.getStringArrayListExtra("oi_button_titles") ?: arrayListOf()
            // Compute source click response early so we can attach it to custom views
            val sourceClick = intent.getParcelableExtraSafe("source_content_intent", PendingIntent::class.java)
            val sourcePkg = intent.getStringExtra("source_pkg")
            val launch = (sourcePkg?.let { packageManager.getLaunchIntentForPackage(it) })
                ?: packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(this, MainActivity::class.java)
            val clickResp = sourceClick ?: PendingIntent.getActivity(
                this, notificationId, launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val explicitCustomTemplate = intent.getParcelableExtraSafe("oi_custom_template", android.widget.RemoteViews::class.java)
            val waveState = intent.getIntExtra("oi_wave_state", 1)
            val waveColorList = intent.getStringArrayListExtra("oi_wave_color")

            val customTemplate = if (sourceRv != null) {
                // Alternate the wrapper layoutId every post. OriginOS's CustomSuperXTemplate only does a
                // full apply() (which re-runs our addView and re-inflates the fresh source RemoteViews)
                // when the layoutId changes between updates; otherwise it calls reapply(), which skips
                // addView and freezes the nested card. Flipping between two identical layouts guarantees
                // the full-apply branch on every update — no hidden-API addFlags needed.
                val flip = !(originRvToggle[notificationId] ?: false)
                originRvToggle[notificationId] = flip
                val wrapperLayout = if (flip) R.layout.focus_rv_wrapper else R.layout.focus_rv_wrapper_alt
                val wrappedRv = android.widget.RemoteViews(packageName, wrapperLayout)
                wrappedRv.removeAllViews(R.id.rv_wrapper_container)
                wrappedRv.addView(R.id.rv_wrapper_container, sourceRv)
                wrappedRv.setOnClickPendingIntent(R.id.rv_wrapper_container, clickResp)
                wrappedRv.forceFullReapply()
                wrappedRv
            } else if (shouldGenerateLiveUpdate) {
                val flip = !(originRvToggle[notificationId] ?: false)
                originRvToggle[notificationId] = flip
                val layoutId = if (flip) R.layout.layout_origin_live_update else R.layout.layout_origin_live_update_alt
                val rv = android.widget.RemoteViews(packageName, layoutId)
                rv.forceFullReapply()
                rv.setOnClickPendingIntent(R.id.live_update_container, clickResp)
                rv.setTextViewText(R.id.live_update_title, title)
                rv.setTextViewText(R.id.live_update_text, text)
                
                // Icon
                if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                    val roundedIcon = createRoundedIcon(this, iconObj, 12f, 44f, 44f) ?: iconObj
                    rv.setImageViewIcon(R.id.live_update_icon, roundedIcon)
                } else {
                    rv.setImageViewResource(R.id.live_update_icon, iconRes)
                }

                // Large Icon
                if (largeIcon != null) {
                    rv.setViewVisibility(R.id.live_update_large_icon, android.view.View.VISIBLE)
                    if (Build.VERSION.SDK_INT >= 23) {
                        val roundedLargeIcon = createRoundedIcon(this, largeIcon, 12f, 44f, 44f) ?: largeIcon
                        rv.setImageViewIcon(R.id.live_update_large_icon, roundedLargeIcon)
                    }
                } else {
                    rv.setViewVisibility(R.id.live_update_large_icon, android.view.View.GONE)
                }

                // Chip Text / Chronometer
                val chronometerBase = intent.getLongExtra("chronometer_base", 0L)
                if (chronometerBase > 0) {
                    val countDown = intent.getBooleanExtra("chronometer_count_down", false)
                    val elapsedRealtimeBase = android.os.SystemClock.elapsedRealtime() - (System.currentTimeMillis() - chronometerBase)
                    
                    rv.setViewVisibility(R.id.live_update_chip, android.view.View.GONE)
                    rv.setViewVisibility(R.id.live_update_chronometer, android.view.View.VISIBLE)
                    rv.setChronometer(R.id.live_update_chronometer, elapsedRealtimeBase, "%s", true)
                    if (Build.VERSION.SDK_INT >= 24) {
                        rv.setChronometerCountDown(R.id.live_update_chronometer, countDown)
                    }
                } else if (!statusChipText.isNullOrEmpty()) {
                    rv.setViewVisibility(R.id.live_update_chronometer, android.view.View.GONE)
                    rv.setViewVisibility(R.id.live_update_chip, android.view.View.VISIBLE)
                    rv.setTextViewText(R.id.live_update_chip, statusChipText)
                } else {
                    rv.setViewVisibility(R.id.live_update_chronometer, android.view.View.GONE)
                    rv.setViewVisibility(R.id.live_update_chip, android.view.View.GONE)
                }

                // Progress Segments & Regular Progress
                val segmentsCount = intent.getIntExtra("progress_segments", 0)
                val segmentColors = intent.getIntArrayExtra("progress_segment_colors")
                if (hasProgress) {
                    if (segmentsCount > 0) {
                        rv.setViewVisibility(R.id.live_update_progress, android.view.View.GONE)
                        rv.setViewVisibility(R.id.live_update_segments_container, android.view.View.VISIBLE)
                        rv.removeAllViews(R.id.live_update_segments_container)
                        val activeColor = if (notificationColor != 0) notificationColor else android.graphics.Color.parseColor("#34C759")
                        val inactiveColor = android.graphics.Color.parseColor("#33FFFFFF")
                        val activeSegments = if (progressMax > 0) (progress * segmentsCount) / progressMax else 0
                        for (i in 0 until segmentsCount) {
                            val segRv = android.widget.RemoteViews(packageName, R.layout.layout_origin_progress_segment)
                            val specificColor = if (segmentColors != null && i < segmentColors.size && segmentColors[i] != 0) segmentColors[i] else activeColor
                            segRv.setInt(R.id.progress_segment_view, "setColorFilter", if (i < activeSegments) specificColor else inactiveColor)
                            rv.addView(R.id.live_update_segments_container, segRv)
                        }
                    } else {
                        rv.setViewVisibility(R.id.live_update_segments_container, android.view.View.GONE)
                        rv.setViewVisibility(R.id.live_update_progress, android.view.View.VISIBLE)
                        rv.setProgressBar(R.id.live_update_progress, progressMax, progress, isIndeterminate)
                    }
                } else {
                    rv.setViewVisibility(R.id.live_update_progress, android.view.View.GONE)
                    rv.setViewVisibility(R.id.live_update_segments_container, android.view.View.GONE)
                }

                // Action Buttons
                if (originActions.isNotEmpty()) {
                    rv.setViewVisibility(R.id.live_update_actions_container, android.view.View.VISIBLE)
                    rv.removeAllViews(R.id.live_update_actions_container)
                    originActions.take(3).forEach { action ->
                        val btnRv = android.widget.RemoteViews(packageName, R.layout.layout_origin_action_button)
                        btnRv.setTextViewText(R.id.action_button_text, action.title)
                        if (action.pendingIntent != null) {
                            btnRv.setOnClickPendingIntent(R.id.action_button_text, action.pendingIntent)
                        }
                        rv.addView(R.id.live_update_actions_container, btnRv)
                    }
                } else {
                    rv.setViewVisibility(R.id.live_update_actions_container, android.view.View.GONE)
                }

                rv
            } else {
                explicitCustomTemplate
            }
            // 0 = updating (show the ring), 1 = success (shows a checkmark, NOT the ring). An ongoing
            // download must stay "updating" or the right island hides the progress entirely.
            val progressState = if (oiProgress >= 100 && !isIndeterminate) 1 else 0

            // Lifecycle: first post for this id = create (0); a repeat while still active = update (1).
            val operation = if (originScenes.containsKey(notificationId)) 1 else 0
            // Strictly-increasing per-id record so OriginOS doesn't drop the update as stale.
            val changeRecord = ((originChangeRecord[notificationId] ?: 0) + 1).also { originChangeRecord[notificationId] = it }

            val bundle = OriginIslandBuilder.buildBundle(
                context = this,
                template = template,
                title = title,
                content = text,
                leftContent = leftContent,
                rightContent = rightContent,
                extra1 = extra1, extra2 = extra2, extra3 = extra3, extra4 = extra4,
                rightTemplate = rightTemplate,
                defaultIcon = islandIcon,
                leftIcon = islandIcon,
                rightIcon = islandIcon,
                accentIcon = islandIcon,
                progress = oiProgress,
                bgColor = bgColor,
                fgColor = fgColor,
                scene = scene,
                clickResp = clickResp,
                operation = operation,
                largeIcon = largeIcon,
                subText = subtext,
                navMsg = navMsg,
                actions = originActions,
                keepDuration = keepDuration,
                sound = false,
                dismissWhenKill = dismissWhenKill,
                displays = computedDisplays,
                islandShowTime = islandShowTime,
                forceShow = forceShow,
                progressState = progressState,
                showRightIcon = showRightIcon,
                cardBgColor = cardBgColor,
                keepScreenOn = keepScreenOn,
                disableInvertColor = disableInvert,
                lightColor = lightColor,
                lightMode = lightMode,
                generatingStatus = generatingStatus,
                iconStatusType = iconStatusType,
                leftDoubleLine = leftDoubleLine,
                rightDoubleLine = rightDoubleLine,
                buttonTitles = buttonTitles,
                customTemplate = customTemplate,
                waveState = waveState,
                waveColorList = waveColorList,
                changeRecord = changeRecord
            )

            // When the user swipes the host away, end the OriginIsland too (a plain dismiss leaves
            // the island lingering). Routed back through ACTION_CANCEL with the scene attached.
            val deleteIntent = Intent(this, PlaygroundService::class.java).apply {
                action = ACTION_CANCEL
                putExtra("id", notificationId)
                putExtra("oi_scene", scene)
            }
            val deletePi = PendingIntent.getService(
                this, notificationId, deleteIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build like superx_demo: NOT ongoing (stays dismissable), NOT autoCancel (§5.7 caution).
            val nb = NotificationCompat.Builder(this, ORIGIN_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setAutoCancel(false)
                .setDeleteIntent(deletePi)
                .setExtras(bundle)
            if (iconObj != null && Build.VERSION.SDK_INT >= 23) {
                nb.setSmallIcon(IconCompat.createFromIcon(this, iconObj))
            } else {
                nb.setSmallIcon(iconRes)
            }
            if (!sourceApp.isNullOrEmpty()) nb.setSubText(sourceApp)

            // (Standard Android actions are ignored on SuperX cards — buttons come from template 8's
            // infos.btn* lists instead, built above.)

            originScenes[notificationId] = scene
            // Post with the fixed SuperX tag so OriginOS can later match & dismiss it (§3.3).
            notificationManager.notify(OriginIslandConstants.SUPERX_TAG, notificationId, nb.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sets RemoteViews FLAG_REAPPLY_DISALLOWED (=1). OriginOS's CustomSuperXTemplate only does a full
     * apply() (which actually re-renders the card) when the previous RemoteViews carried this flag;
     * otherwise it calls reapply(), which no-ops on a relayed card so live RemoteViews updates (nav
     * card internals, etc.) never show. addFlags() is @hide, so invoke it reflectively.
     */
    private fun android.widget.RemoteViews.forceFullReapply() {
        try {
            android.widget.RemoteViews::class.java
                .getMethod("addFlags", Int::class.javaPrimitiveType)
                .invoke(this, 1)
        } catch (e: Throwable) {
            // Flag unavailable on this build — reapply() path will be used; harmless.
        }
    }

    private fun createRoundedIcon(context: Context, icon: android.graphics.drawable.Icon, radiusDp: Float, widthDp: Float, heightDp: Float): android.graphics.drawable.Icon? {
        try {
            val drawable = icon.loadDrawable(context) ?: return null
            val density = context.resources.displayMetrics.density
            val width = (widthDp * density).toInt()
            val height = (heightDp * density).toInt()
            val output = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val rect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
            val radius = radiusDp * density
            canvas.drawRoundRect(rect, radius, radius, paint)
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
            drawable.setBounds(0, 0, width, height)
            
            // To ensure the drawable fits nicely
            val tempBmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val tempCanvas = android.graphics.Canvas(tempBmp)
            drawable.draw(tempCanvas)
            canvas.drawBitmap(tempBmp, 0f, 0f, paint)
            return android.graphics.drawable.Icon.createWithBitmap(output)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /** Builds an Icon from a bitmap, scaled so neither side exceeds 1000px (技术规范 §5.15). */
    private fun iconFromBitmapCapped(bmp: Bitmap): Icon {
        val max = 1000
        val scaled = if (bmp.width > max || bmp.height > max) {
            val ratio = minOf(max.toFloat() / bmp.width, max.toFloat() / bmp.height)
            Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * ratio).toInt().coerceAtLeast(1),
                (bmp.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else bmp
        return Icon.createWithBitmap(scaled)
    }

    private fun <T : android.os.Parcelable> Intent.getParcelableExtraSafe(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= 33) {
            this.getParcelableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            this.getParcelableExtra(key) as? T
        }
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                val channelName = when (channelId) {
                    HYPER_CHANNEL_ID -> "HyperIsland"
                    ORIGIN_CHANNEL_ID -> "OriginIsland"
                    else -> "Live Updates"
                }
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                channel.description = "Channel for $channelName Service"
                channel.setSound(null, null) 
                channel.enableVibration(false)
                manager.createNotificationChannel(channel)
            }
        }
    }
}
