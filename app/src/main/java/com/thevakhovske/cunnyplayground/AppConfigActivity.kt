package com.thevakhovske.cunnyplayground

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AppConfigActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var packageName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        setContentView(R.layout.activity_app_config)

        // Handle Window Insets
        val rootLayout = findViewById<android.view.View>(R.id.rootConfigContainer)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarConfig)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        packageName = intent.getStringExtra("package_name") ?: finish().run { return }
        prefs = getSharedPreferences("experimental_prefs", MODE_PRIVATE)

        setupUI()
    }

    private fun setupUI() {
        val ivIcon: ImageView = findViewById(R.id.ivConfigAppIcon)
        val tvName: TextView = findViewById(R.id.tvConfigAppName)
        val tvRawTitle: TextView = findViewById(R.id.tvRawTitle)
        val tvRawText: TextView = findViewById(R.id.tvRawText)
        val tvRawSubText: TextView = findViewById(R.id.tvRawSubText)
        val tvRawDump: TextView = findViewById(R.id.tvRawDump)

        // Load App Info
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            ivIcon.setImageDrawable(appInfo.loadIcon(packageManager))
            tvName.text = appInfo.loadLabel(packageManager)
        } catch (e: Exception) {
            tvName.text = packageName
        }

        // Load Raw Data
        val lastTitle = prefs.getString("${packageName}_last_title", "N/A")
        val lastText = prefs.getString("${packageName}_last_text", "N/A")
        val lastSubText = prefs.getString("${packageName}_last_subtext", "N/A")
        val lastDump = prefs.getString("${packageName}_last_raw_dump", "Waiting for next interception...")
        tvRawTitle.text = "Title: $lastTitle"
        tvRawText.text = "Text: $lastText"
        tvRawSubText.text = "SubText: $lastSubText"
        tvRawDump.text = lastDump

        // Load Drawables
        val drawablesStr = prefs.getString("${packageName}_last_drawables", "")
        if (drawablesStr != null && drawablesStr.isNotEmpty()) {
            val llDrawables: LinearLayout = findViewById(R.id.llNotificationDrawables)
            llDrawables.removeAllViews()
            val ids = drawablesStr.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct()
            
            val sourceContext = try {
                createPackageContext(packageName, 0)
            } catch (e: Exception) {
                null
            }

            if (sourceContext != null) {
                for (id in ids) {
                    try {
                        val imageView = ImageView(this).apply {
                            val size = (48 * resources.displayMetrics.density).toInt()
                            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                                marginEnd = (12 * resources.displayMetrics.density).toInt()
                            }
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageDrawable(androidx.core.content.res.ResourcesCompat.getDrawable(sourceContext.resources, id, sourceContext.theme))
                            setOnClickListener {
                                val name = try { sourceContext.resources.getResourceEntryName(id) } catch (e: Exception) { id.toString() }
                                Toast.makeText(context, "ID: $id\nName: $name", Toast.LENGTH_SHORT).show()
                            }
                        }
                        llDrawables.addView(imageView)
                    } catch (e: Exception) {
                        // Skip if resource not found or invalid
                    }
                }
            }
        }

        // Load Settings
        val textSource = prefs.getString("${packageName}_text_source", "text")
        val rgTextSource: RadioGroup = findViewById(R.id.rgTextSource)
        when (textSource) {
            "title" -> rgTextSource.check(R.id.rbSourceTitle)
            "subtext" -> rgTextSource.check(R.id.rbSourceSubText)
            else -> rgTextSource.check(R.id.rbSourceText)
        }

        val iconSource = prefs.getString("${packageName}_icon_source", "notification")
        val rgIconSource: RadioGroup = findViewById(R.id.rgIconSource)
        if (iconSource == "app") rgIconSource.check(R.id.rbIconApp)
        else rgIconSource.check(R.id.rbIconNotification)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_app_config, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveSettings() {
        val rgTextSource: RadioGroup = findViewById(R.id.rgTextSource)
        val rgIconSource: RadioGroup = findViewById(R.id.rgIconSource)

        val selectedTextSource = when (rgTextSource.checkedRadioButtonId) {
            R.id.rbSourceTitle -> "title"
            R.id.rbSourceSubText -> "subtext"
            else -> "text"
        }
        val selectedIconSource = if (rgIconSource.checkedRadioButtonId == R.id.rbIconApp) "app" else "notification"

        prefs.edit().apply {
            putString("${packageName}_text_source", selectedTextSource)
            putString("${packageName}_icon_source", selectedIconSource)
            apply()
        }
        Toast.makeText(this, "Configuration Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
