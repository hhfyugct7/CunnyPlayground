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
            intent.getParcelableExtra<android.graphics.drawable.Icon>("small_icon_obj")
        } else null 
        val sourceApp = intent.getStringExtra("source_app")
        val isPromoted = intent.getBooleanExtra("is_promoted", true)
        val statusChipText = intent.getStringExtra("status_chip_text")
        val showProgress = intent.getBooleanExtra("show_progress", true)
        val timestamp = intent.getLongExtra("when", System.currentTimeMillis())
        
        val castMode = intent.getStringExtra("cast_mode") ?: "live_updates"
        val targetChannel = if (castMode == "hyperisland") HYPER_CHANNEL_ID else CHANNEL_ID

        val largeIconObj = if (Build.VERSION.SDK_INT >= 23) {
            intent.getParcelableExtra<android.graphics.drawable.Icon>("large_icon_obj")
        } else null
        val largeIconBitmap = intent.getParcelableExtra<android.graphics.Bitmap>("large_icon_bitmap")
        val sourceRv = intent.getParcelableExtra<android.widget.RemoteViews>("miui_rv")
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
                        val sourceRv = intent.getParcelableExtra<RemoteViews>("miui_rv")
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
                val obj = if (Build.VERSION.SDK_INT >= 23) intent.getParcelableExtra<Icon>("large_icon_obj") else null
                obj ?: intent.getParcelableExtra<Bitmap>("large_icon_bitmap")?.let { iconFromBitmapCapped(it) }
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
            val hasActions = originActions.isNotEmpty()

            // Island parameters: explicit (OriginIsland playground / per-app config) or Auto.
            // Auto picks templates by content — progress when present, else clickable actions, else priority.
            val templateExtra = intent.getIntExtra("oi_template", 0)
            val template = when {
                templateExtra in 1..5 -> templateExtra
                hasProgress -> OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL
                hasActions -> OriginIslandConstants.TEMPLATE_BASE
                else -> OriginIslandConstants.TEMPLATE_PRIORITY_INFO
            }
            val rightTemplateExtra = intent.getIntExtra("oi_right_template", 0)
            val rightTemplate = when {
                rightTemplateExtra in 1..6 -> rightTemplateExtra
                hasProgress -> OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS
                hasActions -> OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_ICON_TEXT
                else -> OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT
            }
            val leftContent = intent.getStringExtra("oi_left_content")
                ?: sourceApp?.takeIf { it.isNotBlank() } ?: title
            val rightContent = intent.getStringExtra("oi_right_content")
                ?: statusChipText?.takeIf { it.isNotBlank() } ?: text
            val extra1 = intent.getStringExtra("oi_extra1") ?: title
            val extra2 = intent.getStringExtra("oi_extra2") ?: text
            val extra3 = intent.getStringExtra("oi_extra3") ?: ""
            val extra4 = intent.getStringExtra("oi_extra4") ?: ""
            val scene = intent.getStringExtra("oi_scene") ?: "NAVIGATION"
            val navMsg = intent.getStringExtra("oi_nav_msg")
            val oiProgress = intent.getIntExtra("oi_progress", derivedProgress)
            val bgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_bg_color"), 0xFF363636.toInt())
            val fgColor = OriginIslandBuilder.parseColor(intent.getStringExtra("oi_fg_color"), 0xFF41DC8E.toInt())
            val keepDuration = intent.getIntExtra("oi_keep_duration", 0)
            val forceShow = intent.getBooleanExtra("oi_force_show", false)
            val dismissWhenKill = intent.getBooleanExtra("oi_dismiss_when_kill", true)
            val islandShowTime = intent.getIntExtra("oi_island_show_time", 0)
            val progressState = if (isIndeterminate) 0 else 1

            // Lifecycle: first post for this id = create (0); a repeat while still active = update (1).
            val operation = if (originScenes.containsKey(notificationId)) 1 else 0

            // Landing page fallback: launch our own app on tap when no action intent is available.
            val launch = packageManager.getLaunchIntentForPackage(packageName)
                ?: Intent(this, MainActivity::class.java)
            val clickResp = PendingIntent.getActivity(
                this, notificationId, launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

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
                islandShowTime = islandShowTime,
                forceShow = forceShow,
                progressState = progressState
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

            originScenes[notificationId] = scene
            // Post with the fixed SuperX tag so OriginOS can later match & dismiss it (§3.3).
            notificationManager.notify(OriginIslandConstants.SUPERX_TAG, notificationId, nb.build())
        } catch (e: Exception) {
            e.printStackTrace()
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
