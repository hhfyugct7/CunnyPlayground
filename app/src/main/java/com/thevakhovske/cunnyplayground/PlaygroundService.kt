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
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat

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
    private lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startPromotedNotification(intent)
            ACTION_CANCEL -> cancelNotification(intent)
            ACTION_STOP -> {
                activeIds.forEach { notificationManager.cancel(it) }
                activeIds.clear()
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
            if (activeIds.isEmpty()) {
                stopSelf()
            }
        }
    }

    private fun startPromotedNotification(intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Ongoing Task"
        val text = intent.getStringExtra("text") ?: "Live Update Active"
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

        if (castMode == "hyperisland") {
            try {
                if (io.github.d4viddf.hyperisland_kit.HyperIslandNotification.isSupported(this)) {
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
                    hyperBuilder.setBaseInfo(
                        title = title,
                        content = text,
                        pictureKey = "default_icon"
                    )
                    val islandText = statusChipText?.takeIf { it.isNotBlank() } ?: title
                    
                    // Structure the Big Island Area to populate both pill sides properly
                    val picInfo = io.github.d4viddf.hyperisland_kit.models.PicInfo(1, "default_icon", false, false, 0, null, null, null)
                    
                    val leftTextInfoObj = io.github.d4viddf.hyperisland_kit.models.TextInfo(
                        title = title,
                        content = null,
                        showHighlightColor = false,
                        narrowFont = null
                    )
                    
                    val imageTextInfoLeft = io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft(
                        1, picInfo, leftTextInfoObj, null
                    )
                    
                    val rootTextInfo = io.github.d4viddf.hyperisland_kit.models.TextInfo(
                        title = text, 
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
                    
                    val jsonPayload = hyperBuilder.buildJsonParam()
                    val resBundle = hyperBuilder.buildResourceBundle()
                    
                    builder.extras.putString("miui.focus.param", jsonPayload)
                    builder.extras.putAll(resBundle)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val notification = builder.build()
        
        // Use startForeground for the first one to keep service alive
        if (activeIds.size == 1) {
            if (Build.VERSION.SDK_INT >= 29) {
                try {
                    if (Build.VERSION.SDK_INT >= 34) {
                        startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(notificationId, notification)
                    }
                } catch (e: Exception) {
                    startForeground(notificationId, notification)
                }
            } else {
                startForeground(notificationId, notification)
            }
        } else {
            // Just notify for others
            notificationManager.notify(notificationId, notification)
        }
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
