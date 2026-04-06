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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
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
        const val CHANNEL_ID = "live_updates_channel"
        const val HYPER_CHANNEL_ID = "hyperslop_channel"
        const val NOTIFICATION_ID = 1001
    }

    private val activeIds = mutableSetOf<Int>()
    private var isForegroundActive = false
    private lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            ACTION_STOP -> {
                activeIds.forEach { notificationManager.cancel(it) }
                activeIds.clear()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun cancelNotification(intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (id != -1) {
            notificationManager.cancel(id)
            activeIds.remove(id)
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
        
        val hasRvRender = intent.getBooleanExtra("has_rv_render", false)
        val targetPkg = intent.getStringExtra("package_name") ?: ""
        val rvRenderBitmap = if (hasRvRender && targetPkg.isNotEmpty()) {
            try {
                val renderFile = File(filesDir, "renders/$targetPkg.png")
                if (renderFile.exists()) {
                    BitmapFactory.decodeFile(renderFile.absolutePath)
                } else null
            } catch (e: Exception) { null }
        } else {
            intent.getParcelableExtra<android.graphics.Bitmap>("rv_render")
        }

        activeIds.add(notificationId)
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
                
                // Register RemoteViews render as background if available
                if (rvRenderBitmap != null) {
                    hyperBuilder.addPicture(io.github.d4viddf.hyperisland_kit.HyperPicture("rv_view", rvRenderBitmap))
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
                        
                        // Inject bgInfo if RemoteViews render exists
                        if (rvRenderBitmap != null) {
                            paramV2.put("bgInfo", org.json.JSONObject().apply {
                                put("type", 1)
                                put("picBg", "miui.focus.pic_rv_view")
                            })
                        }
                        
                        // Implement Progress Bar Support (Manual Injection)
                        val progress = intent.getIntExtra("progress", 0)
                        val progressMax = intent.getIntExtra("progress_max", 0)
                        if (showProgress && progressMax > 0) {
                            val progressPercent = (progress * 100) / progressMax
                            
                            // 1. Root Progress
                            val rootProgressInfo = org.json.JSONObject().apply {
                                put("progress", progressPercent)
                                put("colorProgress", "#34C759")
                            }
                            paramV2.put("progressInfo", rootProgressInfo)
                            
                            // 2. Small Island Progress (requires combinePicInfo wrapper)
                            val paramIsland = paramV2.optJSONObject("param_island")
                            val smallArea = paramIsland?.optJSONObject("smallIslandArea")
                            val picInfo = smallArea?.optJSONObject("picInfo")
                            if (smallArea != null && picInfo != null) {
                                val combinePicInfo = org.json.JSONObject().apply {
                                    put("picInfo", picInfo)
                                    put("progressInfo", org.json.JSONObject().apply {
                                        put("progress", progressPercent)
                                        put("colorReach", "#34C759")
                                        put("isCCW", false)
                                    })
                                }
                                smallArea.remove("picInfo")
                                smallArea.put("combinePicInfo", combinePicInfo)
                            }
                            
                            // 3. Big Island Progress (requires progressTextInfo block)
                            val bigArea = paramIsland?.optJSONObject("bigIslandArea")
                            if (bigArea != null) {
                                val progressTextInfo = org.json.JSONObject().apply {
                                    put("progressInfo", org.json.JSONObject().apply {
                                        put("progress", progressPercent)
                                        put("colorReach", "#34C759")
                                        put("isCCW", true)
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                val channelName = if (channelId == HYPER_CHANNEL_ID) "HyperIsland" else "Live Updates"
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                channel.description = "Channel for $channelName Service"
                channel.setSound(null, null) 
                channel.enableVibration(false)
                manager.createNotificationChannel(channel)
            }
        }
    }
}
