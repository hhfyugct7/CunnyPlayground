package com.thevakhovske.cunnyplayground

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val CHANNEL_ID = "live_updates_channel"
        const val NOTIFICATION_ID = 1001
        const val PERMISSION_REQUEST_CODE = 101
        // The extra key mentioned in context - potentially related to new features
        const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.app.extra.PROMOTED_ONGOING" 
    }

    private lateinit var etTitle: EditText
    private lateinit var etText: EditText
    private lateinit var cbOngoing: CheckBox
    private lateinit var cbPromoted: CheckBox
    private lateinit var cbChronometer: CheckBox
    private lateinit var cbColorized: CheckBox
    private lateinit var rgStyle: RadioGroup
    private lateinit var btnPost: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnCancel: Button

    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        initViews()
        setupListeners()
        checkPermissions()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etText = findViewById(R.id.etText)
        cbOngoing = findViewById(R.id.cbOngoing)
        cbPromoted = findViewById(R.id.cbPromoted)
        cbChronometer = findViewById(R.id.cbChronometer)
        cbColorized = findViewById(R.id.cbColorized)
        rgStyle = findViewById(R.id.rgStyle)
        btnPost = findViewById(R.id.btnPost)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupListeners() {
        btnPost.setOnClickListener {
            postNotification()
        }

        btnUpdate.setOnClickListener {
            postNotification(update = true)
        }

        btnCancel.setOnClickListener {
            notificationManager.cancelAll()
            Toast.makeText(this, "Cleared all notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Live Updates"
            val descriptionText = "Channel for Live Updates Playground"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // Promoted notifications often imply high importance and sound/vibration might act weird with updates
                // separating them might be better, but for now standard is fine.
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun postNotification(update: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission missing!", Toast.LENGTH_SHORT).show()
                checkPermissions()
                return
            }
        }

        val title = etTitle.text.toString()
        val text = etText.text.toString() + (if (update) " (Updated: ${System.currentTimeMillis() % 1000})" else "")
        
        // Intent for clicking the notification
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Full screen intent (required for CallStyle if not FGS/UIJ)
        val fullScreenIntent = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Builder Setup
        // We use Notification.Builder for 'CallStyle' native support or NotificationCompat
        // Let's use NotificationCompat for ease, but map to native styles where essential.
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Fallback icon
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenIntent, true) // Required for CallStyle validity checks
            .setOngoing(cbOngoing.isChecked)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(cbChronometer.isChecked)
            
        // Colorized
        if (cbColorized.isChecked) {
            builder.setColorized(true)
            builder.setColor(Color.CYAN) // A distinct color
        }

        // Extras for "Promoted" logic (Unstable/Hidden APIs)
        if (cbPromoted.isChecked) {
             // Try to promote using extras. 
             // Note: Android 13+ 'CallStyle' is the official way, but checking if there's a hidden extra.
             // Based on user request history, we add this.
             builder.extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
        }

        
        // Styling
        when (rgStyle.checkedRadioButtonId) {
            R.id.rbCall -> {
                val person = androidx.core.app.Person.Builder()
                    .setName("Cunny Playground")
                    .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                    .setImportant(true)
                    .build()
                
                val hangupIntent = PendingIntent.getBroadcast(this, 1, Intent("ACTION_HANGUP"), PendingIntent.FLAG_IMMUTABLE)
                
                // CallStyle requires a 'verification' in some contexts, but acceptable for basic playground
                val style = NotificationCompat.CallStyle.forOngoingCall(
                    person,
                    hangupIntent
                )
                builder.setStyle(style)
                
                // CallStyle implies promoted chip in status bar
            }
            R.id.rbProgress -> {
                // Live Updates/Status Chips usually generally require a Foreground Service to be considered "Ongoing"
                // and to prevent system killing.
                val intent = Intent(this, PlaygroundService::class.java).apply {
                    action = PlaygroundService.ACTION_START
                    putExtra("title", title)
                    putExtra("text", text)
                }
                
                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                return
            }
            R.id.rbStandard -> {
                // No specific style, just standard
            }
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
