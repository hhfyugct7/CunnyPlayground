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

data class EnabledApp(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
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
    private lateinit var etSubText: EditText
    private lateinit var etStatusChipText: EditText
    private lateinit var cbOngoing: com.google.android.material.checkbox.MaterialCheckBox
    private lateinit var cbPromoted: com.google.android.material.checkbox.MaterialCheckBox
    private lateinit var cbChronometer: com.google.android.material.checkbox.MaterialCheckBox
    private lateinit var cbShowProgress: com.google.android.material.checkbox.MaterialCheckBox
    private lateinit var rgIcon: RadioGroup
    private lateinit var btnPost: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnCancel: Button
    private lateinit var rvNotifications: RecyclerView
    private lateinit var llEnabledApps: LinearLayout
    private lateinit var tvEnabledAppsCount: TextView

    // HyperIsland Fields
    private lateinit var etHTitle: EditText
    private lateinit var etHText: EditText
    private lateinit var etHSubText: EditText
    private lateinit var etHLeftText: EditText
    private lateinit var etHMainText: EditText
    private lateinit var etHyperRawJson: EditText
    private lateinit var rgHIcon: RadioGroup
    private lateinit var btnHPost: Button
    private lateinit var btnHCancel: Button
    private lateinit var rootScrollMain: View
    private lateinit var rootScrollRecaster: View
    private lateinit var rootScrollHPlayground: View

    private val notifications = mutableListOf<NotificationInfo>()
    private val enabledAppsList = mutableListOf<EnabledApp>()
    private lateinit var adapter: NotificationAdapter
    private var lastId = 1000
    private var editingId: Int? = null

    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        setContentView(R.layout.activity_main)

        // Handle Window Insets
        val rootLayout = findViewById<View>(R.id.rootMainContainer)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarMain)
        setSupportActionBar(toolbar)

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_playground -> {
                    rootScrollMain.visibility = View.VISIBLE
                    rootScrollHPlayground.visibility = View.GONE
                    rootScrollRecaster.visibility = View.GONE
                    toolbar.title = "Live Updates Playground"
                    true
                }
                R.id.nav_hplayground -> {
                    rootScrollMain.visibility = View.GONE
                    rootScrollHPlayground.visibility = View.VISIBLE
                    rootScrollRecaster.visibility = View.GONE
                    toolbar.title = "HyperIsland Playground"
                    true
                }
                R.id.nav_recaster -> {
                    rootScrollMain.visibility = View.GONE
                    rootScrollHPlayground.visibility = View.GONE
                    rootScrollRecaster.visibility = View.VISIBLE
                    toolbar.title = "Notification Re-Caster"
                    true
                }
                else -> false
            }
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        initViews()
        setupRecasterUI()
        setupListeners()
        loadEnabledApps()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        loadEnabledApps()
    }

    private fun loadEnabledApps() {
        val prefs = getSharedPreferences("experimental_prefs", MODE_PRIVATE)
        val selectedPackages = prefs.getStringSet("cast_enabled_apps", emptySet()) ?: emptySet()
        
        val pm = packageManager
        val newList = selectedPackages.mapNotNull { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                EnabledApp(
                    name = appInfo.loadLabel(pm).toString(),
                    packageName = pkg,
                    icon = appInfo.loadIcon(pm)
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.name.lowercase() }

        enabledAppsList.clear()
        enabledAppsList.addAll(newList)
        
        if (::tvEnabledAppsCount.isInitialized) {
            tvEnabledAppsCount.text = "Apps enabled for casting (${enabledAppsList.size})"
        }

        if (::llEnabledApps.isInitialized) {
            llEnabledApps.removeAllViews()
            enabledAppsList.forEach { app ->
                val itemView = layoutInflater.inflate(R.layout.item_enabled_app, llEnabledApps, false)
                itemView.findViewById<ImageView>(R.id.ivEnabledAppIcon).setImageDrawable(app.icon)
                itemView.findViewById<TextView>(R.id.tvEnabledAppName).text = app.name
                itemView.findViewById<Button>(R.id.btnAppEnabledConfig).setOnClickListener {
                    val intent = Intent(this, AppConfigActivity::class.java).apply {
                        putExtra("package_name", app.packageName)
                    }
                    startActivity(intent)
                }
                llEnabledApps.addView(itemView)
            }
        }
    }

    private fun setupRecasterUI() {
        val prefs = getSharedPreferences("experimental_prefs", MODE_PRIVATE)
        val swCast = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.swCastNotifications)
        val swUseAppIcon = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.swUseAppIcon)
        val btnAppFilter = findViewById<Button>(R.id.btnAppFilter)
        val btnPermission = findViewById<Button>(R.id.btnNotificationAccess)

        val rgCastAs = findViewById<RadioGroup>(R.id.rgCastAs)
        val castMode = prefs.getString("cast_mode", "live_updates")
        if (castMode == "hyperisland") {
            rgCastAs.check(R.id.rbCastAsHyperIsland)
        } else {
            rgCastAs.check(R.id.rbCastAsLiveUpdates)
        }
        
        rgCastAs.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == R.id.rbCastAsHyperIsland) "hyperisland" else "live_updates"
            prefs.edit().putString("cast_mode", mode).apply()
        }

        swCast.isChecked = prefs.getBoolean("cast_notifications", false)
        swCast.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("cast_notifications", isChecked).apply()
        }

        swUseAppIcon.isChecked = prefs.getBoolean("use_app_icon", false)
        swUseAppIcon.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("use_app_icon", isChecked).apply()
        }

        val swShowProgressPercent = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.swShowProgressPercent)
        swShowProgressPercent.isChecked = prefs.getBoolean("show_progress_percentage", false)
        swShowProgressPercent.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_progress_percentage", isChecked).apply()
        }

        val swLimitChipText = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.swLimitChipText)
        swLimitChipText.isChecked = prefs.getBoolean("limit_chip_7char", false)
        swLimitChipText.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("limit_chip_7char", isChecked).apply()
        }

        btnAppFilter.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }

        btnPermission.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }

    private fun initViews() {
        rootScrollMain = findViewById(R.id.rootScrollMain)
        rootScrollRecaster = findViewById(R.id.rootScrollRecaster)
        rootScrollHPlayground = findViewById(R.id.rootScrollHPlayground)

        etTitle = findViewById(R.id.etTitle)
        etText = findViewById(R.id.etText)
        etSubText = findViewById(R.id.etSubText)
        etStatusChipText = findViewById(R.id.etStatusChipText)
        cbOngoing = findViewById(R.id.cbOngoing)
        cbPromoted = findViewById(R.id.cbPromoted)
        cbChronometer = findViewById(R.id.cbChronometer)
        cbShowProgress = findViewById(R.id.cbShowProgress)
        rgIcon = findViewById(R.id.rgIcon)
        btnPost = findViewById(R.id.btnPost)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnCancel = findViewById(R.id.btnCancel)
        rvNotifications = findViewById(R.id.rvNotifications)
        llEnabledApps = findViewById(R.id.llEnabledApps)
        tvEnabledAppsCount = findViewById(R.id.tvEnabledAppsCount)

        // HyperIsland
        etHTitle = findViewById(R.id.etHTitle)
        etHText = findViewById(R.id.etHText)
        etHSubText = findViewById(R.id.etHSubText)
        etHLeftText = findViewById(R.id.etHLeftText)
        etHMainText = findViewById(R.id.etHMainText)
        etHyperRawJson = findViewById(R.id.etHyperRawJson)
        rgHIcon = findViewById(R.id.rgHIcon)
        btnHPost = findViewById(R.id.btnHPost)
        btnHCancel = findViewById(R.id.btnHCancel)

        adapter = NotificationAdapter(notifications, ::onNotificationMenuClick)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter
    }



    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
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
            stopPlaygroundService()
        }

        btnHPost.setOnClickListener {
            postHyperNotification()
        }

        btnHCancel.setOnClickListener {
            stopPlaygroundService()
        }
    }

    private fun stopPlaygroundService() {
        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_STOP
        }
        startService(intent)
        notifications.clear()
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Cleared all notifications", Toast.LENGTH_SHORT).show()
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
        val subtext = etSubText.text.toString()
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
            putExtra("subtext", subtext)
            putExtra("status_chip_text", statusChipText)
            putExtra("id", notificationId)
            putExtra("icon_res", iconRes)
            putExtra("is_promoted", cbPromoted.isChecked)
            putExtra("show_progress", cbShowProgress.isChecked)
            if (cbShowProgress.isChecked) {
                putExtra("progress", 50)
                putExtra("progress_max", 100)
            }
            putExtra("when", notification.timestamp)
            putExtra("source_app", "Manual")
            putExtra("cast_mode", "live_updates")
        }
        
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun postHyperNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission missing!", Toast.LENGTH_SHORT).show()
                checkPermissions()
                return
            }
        }

        val title = etHTitle.text.toString()
        val text = etHText.text.toString()
        val subtext = etHSubText.text.toString()
        val leftText = etHLeftText.text.toString()
        val hyperMainText = etHMainText.text.toString()
        val rawJson = etHyperRawJson.text.toString()
        
        val notificationId = ++lastId
        
        val iconRes = when (rgHIcon.checkedRadioButtonId) {
            R.id.rbHIconTimer -> R.drawable.ic_timer
            R.id.rbHIconCall -> R.drawable.ic_call
            R.id.rbHIconAlert -> R.drawable.ic_alert
            else -> R.mipmap.ic_launcher_round
        }

        val intent = Intent(this, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("title", title)
            putExtra("text", text)
            putExtra("subtext", subtext)
            putExtra("hyper_left_text", leftText)
            putExtra("hyper_main_text", hyperMainText)
            putExtra("raw_hyper_json", rawJson)
            putExtra("id", notificationId)
            putExtra("icon_res", iconRes)
            putExtra("is_promoted", true)
            putExtra("source_app", "Manual-Hyper")
            putExtra("cast_mode", "hyperisland")
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
