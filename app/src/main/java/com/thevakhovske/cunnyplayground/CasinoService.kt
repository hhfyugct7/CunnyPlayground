package com.thevakhovske.cunnyplayground

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class CasinoService : Service() {

    companion object {
        const val ACTION_SPIN = "com.thevakhovske.cunnyplayground.ACTION_SPIN"
        const val ACTION_LAUNCH = "com.thevakhovske.cunnyplayground.ACTION_LAUNCH"
        const val NOTIFICATION_ID = 777
        val SYMBOLS = arrayOf("🍒", "🍋", "🔔", "💎", "7️⃣", "💀", "💰")
        const val SPIN_COST = 10
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isSpinning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_LAUNCH -> updateCasinoUI(false)
            ACTION_SPIN -> startSpinning()
        }
        return START_NOT_STICKY
    }

    private fun startSpinning() {
        if (isSpinning) return

        val prefs = getSharedPreferences("casino_prefs", Context.MODE_PRIVATE)
        var balance = prefs.getInt("balance", 1000)

        if (balance < SPIN_COST) {
            updateCasinoUI(false, "Not enough balance! Here's a free $1000 loan.", forceBalance = 1000)
            return
        }

        balance -= SPIN_COST
        prefs.edit().putInt("balance", balance).apply()

        isSpinning = true

        // Push rapid updates to simulate spinning
        var spinCount = 0
        val maxSpins = 12
        val spinRunnable = object : Runnable {
            override fun run() {
                spinCount++
                if (spinCount < maxSpins) {
                    val s1 = SYMBOLS[Random.nextInt(SYMBOLS.size)]
                    val s2 = SYMBOLS[Random.nextInt(SYMBOLS.size)]
                    val s3 = SYMBOLS[Random.nextInt(SYMBOLS.size)]
                    updateCasinoUI(true, "Spinning...", s1, s2, s3, balance)
                    handler.postDelayed(this, 120)
                } else {
                    // Final Result
                    val f1 = SYMBOLS[Random.nextInt(SYMBOLS.size)]
                    val f2 = SYMBOLS[Random.nextInt(SYMBOLS.size)]
                    val f3 = SYMBOLS[Random.nextInt(SYMBOLS.size)]
                    
                    var win = 0
                    if (f1 == f2 && f2 == f3) {
                        win = 500
                    } else if (f1 == f2 || f2 == f3 || f1 == f3) {
                        win = 20
                    }

                    if (win > 0) {
                        balance += win
                        prefs.edit().putInt("balance", balance).apply()
                        updateCasinoUI(false, "WINNER! You won $$win!", f1, f2, f3, balance)
                    } else {
                        updateCasinoUI(false, "You lost! Try again.", f1, f2, f3, balance)
                    }
                    isSpinning = false
                }
            }
        }
        handler.post(spinRunnable)
    }

    private fun updateCasinoUI(
        spinning: Boolean,
        statusText: String? = null,
        s1: String = "🍒",
        s2: String = "🍒",
        s3: String = "🍒",
        forceBalance: Int? = null
    ) {
        val prefs = getSharedPreferences("casino_prefs", Context.MODE_PRIVATE)
        val balance = forceBalance ?: prefs.getInt("balance", 1000)
        
        if (forceBalance != null) {
            prefs.edit().putInt("balance", forceBalance).apply()
        }

        val rv = RemoteViews(packageName, R.layout.layout_casino)
        rv.setTextViewText(R.id.casino_balance, "Balance: $$balance")
        rv.setTextViewText(R.id.casino_slot_1, s1)
        rv.setTextViewText(R.id.casino_slot_2, s2)
        rv.setTextViewText(R.id.casino_slot_3, s3)

        if (statusText != null) {
            rv.setTextViewText(R.id.casino_status, statusText)
        } else {
            rv.setTextViewText(R.id.casino_status, "Press SPIN to play! ($$SPIN_COST per spin)")
        }

        if (spinning) {
            rv.setTextViewText(R.id.casino_btn_spin, "SPINNING...")
            val emptyIntent = PendingIntent.getService(this, 2, Intent(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(R.id.casino_btn_spin, emptyIntent)
        } else {
            rv.setTextViewText(R.id.casino_btn_spin, "SPIN")
            val spinIntent = Intent(this, CasinoService::class.java).apply {
                action = ACTION_SPIN
            }
            val pi = PendingIntent.getService(this, 1, spinIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(R.id.casino_btn_spin, pi)
        }

        val launchIntent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", NOTIFICATION_ID)
            putExtra("title", "🎰 Cunny Casino")
            putExtra("text", statusText ?: "Press SPIN to play!")
            putExtra("source_app", "Cunny Casino")
            putExtra("cast_mode", "originisland")
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", 7) // OriginIslandConstants.TEMPLATE_NOTIF_CUSTOM
            putExtra("oi_custom_template", rv)
            
            // Set capsule parameters for the small island
            putExtra("status_chip_text", if (spinning) "SPINNING" else "🎰")
            putExtra("oi_right_content", if (spinning) "..." else "Ready")
        }
        startService(launchIntent)
    }
}
