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
        const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.app.extra.PROMOTED_ONGOING" 
    }

    private lateinit var etTitle: EditText
    private lateinit var etText: EditText
    private lateinit var etId: EditText
    private lateinit var cbOngoing: CheckBox
    private lateinit var cbPromoted: CheckBox
    private lateinit var cbChronometer: CheckBox
    private lateinit var cbColorized: CheckBox
    private lateinit var rgStyle: RadioGroup
    private lateinit var rgIcon: RadioGroup
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
        etId = findViewById(R.id.etId)
        cbOngoing = findViewById(R.id.cbOngoing)
        cbPromoted = findViewById(R.id.cbPromoted)
        cbChronometer = findViewById(R.id.cbChronometer)
        cbColorized = findViewById(R.id.cbColorized)
        rgStyle = findViewById(R.id.rgStyle)
        rgIcon = findViewById(R.id.rgIcon)
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
        val idStr = etId.text.toString()
        val notificationId = if (idStr.isNotEmpty()) idStr.toInt() else 1001
        
        val iconRes = when (rgIcon.checkedRadioButtonId) {
            R.id.rbIconTimer -> R.drawable.ic_timer
            R.id.rbIconCall -> R.drawable.ic_call
            R.id.rbIconAlert -> R.drawable.ic_alert
            else -> R.mipmap.ic_launcher_round
        }
        
        if (cbPromoted.isChecked || rgStyle.checkedRadioButtonId == R.id.rbProgress) {
            val intent = Intent(this, PlaygroundService::class.java).apply {
                action = PlaygroundService.ACTION_START
                putExtra("title", title)
                putExtra("text", text)
                putExtra("id", notificationId)
                putExtra("icon_res", iconRes)
            }
            
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
             Toast.makeText(this, "Select ProgressStyle", Toast.LENGTH_SHORT).show()
        }
    }
}
