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
        val rgTextSource: RadioGroup = findViewById(R.id.rgTextSource)
        val rgIconSource: RadioGroup = findViewById(R.id.rgIconSource)
        val btnSave: Button = findViewById(R.id.btnSaveConfig)

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
        tvRawTitle.text = "Title: $lastTitle"
        tvRawText.text = "Text: $lastText"

        // Load Settings
        val textSource = prefs.getString("${packageName}_text_source", "text")
        if (textSource == "title") rgTextSource.check(R.id.rbSourceTitle)
        else rgTextSource.check(R.id.rbSourceText)

        val iconSource = prefs.getString("${packageName}_icon_source", "notification")
        if (iconSource == "app") rgIconSource.check(R.id.rbIconApp)
        else rgIconSource.check(R.id.rbIconNotification)

        btnSave.setOnClickListener {
            val selectedTextSource = if (rgTextSource.checkedRadioButtonId == R.id.rbSourceTitle) "title" else "text"
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
}
