package com.thevakhovske.cunnyplayground

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog

data class NotificationInfo(
    var id: Int,
    var title: String,
    var text: String,
    var iconRes: Int,
    var isPromoted: Boolean,
    var statusChipText: String?,
    var showProgress: Boolean,
    var timestamp: Long = System.currentTimeMillis()
)

class MainActivity : AppCompatActivity() {

    companion object {
        const val CHANNEL_ID = "live_updates_channel"
        const val NOTIFICATION_ID = 1001
        const val PERMISSION_REQUEST_CODE = 101
        const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.app.extra.PROMOTED_ONGOING" 
    }

    private lateinit var etTitle: EditText
    private lateinit var etText: EditText
    private lateinit var etStatusChipText: EditText
    private lateinit var cbOngoing: CheckBox
    private lateinit var cbPromoted: CheckBox
    private lateinit var cbChronometer: CheckBox
    private lateinit var cbColorized: CheckBox
    private lateinit var cbShowProgress: CheckBox
    private lateinit var rgStyle: RadioGroup
    private lateinit var rgIcon: RadioGroup
    private lateinit var btnPost: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnCancel: Button
    private lateinit var rvNotifications: RecyclerView

    private val notifications = mutableListOf<NotificationInfo>()
    private lateinit var adapter: NotificationAdapter
    private var lastId = 1000
    private var editingId: Int? = null

    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Handle Window Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        initViews()
        setupListeners()
        checkPermissions()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etText = findViewById(R.id.etText)
        etStatusChipText = findViewById(R.id.etStatusChipText)
        cbOngoing = findViewById(R.id.cbOngoing)
        cbPromoted = findViewById(R.id.cbPromoted)
        cbChronometer = findViewById(R.id.cbChronometer)
        cbColorized = findViewById(R.id.cbColorized)
        cbShowProgress = findViewById(R.id.cbShowProgress)
        rgStyle = findViewById(R.id.rgStyle)
        rgIcon = findViewById(R.id.rgIcon)
        btnPost = findViewById(R.id.btnPost)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnCancel = findViewById(R.id.btnCancel)
        rvNotifications = findViewById(R.id.rvNotifications)

        adapter = NotificationAdapter(notifications, ::onNotificationMenuClick)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            showExperimentalSettingsDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showExperimentalSettingsDialog() {
        val prefs = getSharedPreferences("experimental_prefs", MODE_PRIVATE)
        val view = layoutInflater.inflate(R.layout.dialog_experimental, null)
        val swCast = view.findViewById<Switch>(R.id.swCastNotifications)
        val swUseAppIcon = view.findViewById<Switch>(R.id.swUseAppIcon)
        val btnAppFilter = view.findViewById<Button>(R.id.btnAppFilter)
        val btnPermission = view.findViewById<Button>(R.id.btnNotificationAccess)

        swCast.isChecked = prefs.getBoolean("cast_notifications", false)
        swCast.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("cast_notifications", isChecked).apply()
        }

        swUseAppIcon.isChecked = prefs.getBoolean("use_app_icon", false)
        swUseAppIcon.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("use_app_icon", isChecked).apply()
        }

        btnAppFilter.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }

        btnPermission.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Experimental Features")
            .setView(view)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun setupListeners() {
        btnPost.setOnClickListener {
            editingId = null
            postNotification()
        }

        btnUpdate.setOnClickListener {
            if (editingId != null) {
                postNotification(idToUpdate = editingId)
            } else if (notifications.isNotEmpty()) {
                postNotification(idToUpdate = notifications.last().id)
            } else {
                Toast.makeText(this, "No notification to update", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            val intent = Intent(this, PlaygroundService::class.java).apply {
                action = PlaygroundService.ACTION_STOP
            }
            startService(intent)
            notifications.clear()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Cleared all notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onNotificationMenuClick(view: View, notification: NotificationInfo) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Edit")
        popup.menu.add("Delete")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Edit" -> {
                    editingId = notification.id
                    etTitle.setText(notification.title)
                    etText.setText(notification.text)
                    etStatusChipText.setText(notification.statusChipText ?: "")
                    cbPromoted.isChecked = notification.isPromoted
                    cbShowProgress.isChecked = notification.showProgress
                    // Could also set icon/style radio groups if tracked
                    Toast.makeText(this, "Editing ID: ${notification.id}", Toast.LENGTH_SHORT).show()
                }
                "Delete" -> {
                    cancelNotification(notification.id)
                }
            }
            true
        }
        popup.show()
    }

    private fun cancelNotification(id: Int) {
        // Send cancel action to service
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_CANCEL
            putExtra("id", id)
        }
        startService(intent)
        
        val index = notifications.indexOfFirst { it.id == id }
        if (index != -1) {
            notifications.removeAt(index)
            adapter.notifyItemRemoved(index)
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

    private fun postNotification(idToUpdate: Int? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission missing!", Toast.LENGTH_SHORT).show()
                checkPermissions()
                return
            }
        }

        val title = etTitle.text.toString()
        val text = etText.text.toString() + (if (idToUpdate != null) " (Updated)" else "")
        val statusChipText = etStatusChipText.text.toString()
        
        val notificationId = idToUpdate ?: ++lastId
        
        val iconRes = when (rgIcon.checkedRadioButtonId) {
            R.id.rbIconTimer -> R.drawable.ic_timer
            R.id.rbIconCall -> R.drawable.ic_call
            R.id.rbIconAlert -> R.drawable.ic_alert
            else -> R.mipmap.ic_launcher_round
        }
        
        // Update local list
        val existingIndex = notifications.indexOfFirst { it.id == notificationId }
        val notification = if (existingIndex != -1) {
            notifications[existingIndex].copy(
                title = title, 
                text = text, 
                iconRes = iconRes, 
                isPromoted = cbPromoted.isChecked,
                statusChipText = statusChipText,
                showProgress = cbShowProgress.isChecked
            ).also { 
                notifications[existingIndex] = it
                adapter.notifyItemChanged(existingIndex)
            }
        } else {
            NotificationInfo(notificationId, title, text, iconRes, cbPromoted.isChecked, statusChipText, cbShowProgress.isChecked).also {
                notifications.add(it)
                adapter.notifyItemInserted(notifications.size - 1)
            }
        }

        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("title", title)
            putExtra("text", text)
            putExtra("status_chip_text", statusChipText)
            putExtra("id", notificationId)
            putExtra("icon_res", iconRes)
            putExtra("is_promoted", cbPromoted.isChecked)
            putExtra("show_progress", cbShowProgress.isChecked)
            putExtra("when", notification.timestamp)
            putExtra("source_app", "Manual")
        }
        
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private inner class NotificationAdapter(
        private val items: List<NotificationInfo>,
        private val onMenuClick: (View, NotificationInfo) -> Unit
    ) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivItemIcon)
            val tvTitle: TextView = view.findViewById(R.id.tvItemTitle)
            val tvInfo: TextView = view.findViewById(R.id.tvItemInfo)
            val btnMenu: ImageButton = view.findViewById(R.id.btnItemMenu)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.ivIcon.setImageResource(item.iconRes)
            holder.tvTitle.text = item.title
            holder.tvInfo.text = "ID: ${item.id} • ${if (item.isPromoted) "Promoted" else "Standard"}${if (!item.statusChipText.isNullOrEmpty()) " • Chip: ${item.statusChipText}" else ""}"
            holder.btnMenu.setOnClickListener { onMenuClick(it, item) }
        }

        override fun getItemCount() = items.size
    }
}
