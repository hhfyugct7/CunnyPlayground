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
            "titletext" -> rgTextSource.check(R.id.rbSourceTitleText)
            else -> rgTextSource.check(R.id.rbSourceText)
        }

        val currentIconSource = prefs.getString("${packageName}_icon_source", "default")
        val rbExtracted = findViewById<RadioButton>(R.id.rbIconExtracted)
        val hasDrawables = !prefs.getString("${packageName}_last_drawables", "").isNullOrEmpty()

        if (!hasDrawables) {
            rbExtracted.isEnabled = false
            rbExtracted.text = rbExtracted.text.toString() + " (No icons discovered yet)"
        }

        when (currentIconSource) {
            "app" -> findViewById<RadioButton>(R.id.rbIconApp).isChecked = true
            "notification" -> findViewById<RadioButton>(R.id.rbIconNotification).isChecked = true
            "extracted" -> rbExtracted.isChecked = true
            else -> {
                // Default: none checked, uses global toggle
            }
        }

        updatePreview()

        rgTextSource.setOnCheckedChangeListener { _, _ -> updatePreview() }
        findViewById<RadioGroup>(R.id.rgIconSource).setOnCheckedChangeListener { _, _ -> updatePreview() }
    }

    private fun updatePreview() {
        val tvPreviewText: TextView = findViewById(R.id.tvPreviewText)
        val ivPreviewIcon: ImageView = findViewById(R.id.ivPreviewIcon)

        // Update Text
        val rgTextSource = findViewById<RadioGroup>(R.id.rgTextSource)
        val selectedText = when (rgTextSource.checkedRadioButtonId) {
            R.id.rbSourceTitle -> prefs.getString("${packageName}_last_title", "Title")
            R.id.rbSourceSubText -> prefs.getString("${packageName}_last_subtext", "SubText")
            R.id.rbSourceTitleText -> {
                val t = prefs.getString("${packageName}_last_title", "Title")
                val txt = prefs.getString("${packageName}_last_text", "Text")
                "$t: $txt"
            }
            else -> prefs.getString("${packageName}_last_text", "Text")
        }
        tvPreviewText.text = selectedText

        // Update Icon
        val checkedIconId = findViewById<RadioGroup>(R.id.rgIconSource).checkedRadioButtonId
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            when (checkedIconId) {
                R.id.rbIconApp -> {
                    ivPreviewIcon.setImageDrawable(appInfo.loadIcon(packageManager))
                }
                R.id.rbIconNotification -> {
                    // Try to load the "small icon" from the pacakge if possible, else fallback to app icon
                    ivPreviewIcon.setImageDrawable(appInfo.loadIcon(packageManager))
                }
                R.id.rbIconExtracted -> {
                    val drawablesStr = prefs.getString("${packageName}_last_drawables", "")
                    val firstId = drawablesStr?.split(",")?.firstOrNull()?.trim()?.toIntOrNull()
                    if (firstId != null) {
                        try {
                            val sourceContext = createPackageContext(packageName, 0)
                            val drawable = androidx.core.content.res.ResourcesCompat.getDrawable(sourceContext.resources, firstId, sourceContext.theme)
                            ivPreviewIcon.setImageDrawable(drawable)
                        } catch (e: Exception) {
                            ivPreviewIcon.setImageDrawable(appInfo.loadIcon(packageManager))
                        }
                    } else {
                        ivPreviewIcon.setImageDrawable(appInfo.loadIcon(packageManager))
                    }
                }
                else -> {
                    // Default case
                    ivPreviewIcon.setImageDrawable(appInfo.loadIcon(packageManager))
                }
            }
        } catch (e: Exception) {
            ivPreviewIcon.setImageDrawable(null)
        }
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
            R.id.rbSourceTitleText -> "titletext"
            else -> "text"
        }
        val iconSource = when {
            findViewById<RadioButton>(R.id.rbIconApp).isChecked -> "app"
            findViewById<RadioButton>(R.id.rbIconNotification).isChecked -> "notification"
            findViewById<RadioButton>(R.id.rbIconExtracted).isChecked -> "extracted"
            else -> "default"
        }

        prefs.edit().apply {
            putString("${packageName}_text_source", selectedTextSource)
            putString("${packageName}_icon_source", iconSource)
            apply()
        }
        Toast.makeText(this, "Configuration Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
