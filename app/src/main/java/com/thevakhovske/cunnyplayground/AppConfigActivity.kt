package com.thevakhovske.cunnyplayground

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import android.graphics.BitmapFactory
import java.io.File
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
        toolbar.navigationIcon?.setTint(android.graphics.Color.WHITE)
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

        // Copy Dump Logic
        findViewById<Button>(R.id.btnCopyDump).setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Notification Dump", lastDump)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Dump copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Load Render Preview
        val renderFile = File(filesDir, "renders/${packageName}.png")
        val ivPreview: ImageView = findViewById(R.id.ivNotificationPreview)
        val cvPreview: androidx.cardview.widget.CardView = findViewById(R.id.cvNotificationPreview)
        val tvLabelPreview: TextView = findViewById(R.id.tvLabelPreview)

        if (renderFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(renderFile.absolutePath)
                ivPreview.setImageBitmap(bitmap)
                cvPreview.visibility = android.view.View.VISIBLE
                tvLabelPreview.visibility = android.view.View.VISIBLE
            } catch (e: Exception) {
                cvPreview.visibility = android.view.View.GONE
                tvLabelPreview.visibility = android.view.View.GONE
            }
        } else {
            cvPreview.visibility = android.view.View.GONE
            tvLabelPreview.visibility = android.view.View.GONE
        }

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

        val castMode = prefs.getString("cast_mode", "live_updates")
        val llLiveUpdates = findViewById<LinearLayout>(R.id.llLiveUpdatesConfig)
        val llHyperIsland = findViewById<LinearLayout>(R.id.llHyperIslandConfig)

        if (castMode == "hyperisland") {
            llLiveUpdates.visibility = android.view.View.GONE
            llHyperIsland.visibility = android.view.View.VISIBLE

            fun getIndex(source: String): Int = when (source) {
                "title" -> 0
                "text" -> 1
                "subtext" -> 2
                "titletext" -> 3
                else -> 1
            }

            val leftSource = prefs.getString("${packageName}_hyper_left_source", "title") ?: "title"
            findViewById<Spinner>(R.id.spnHypLeftSource).setSelection(getIndex(leftSource))

            val mainSource = prefs.getString("${packageName}_hyper_main_source", "text") ?: "text"
            findViewById<Spinner>(R.id.spnHypMainSource).setSelection(getIndex(mainSource))

            findViewById<EditText>(R.id.etHypLeftRegex).setText(prefs.getString("${packageName}_hyper_left_regex", ""))
            findViewById<EditText>(R.id.etHypMainRegex).setText(prefs.getString("${packageName}_hyper_main_regex", ""))

            val filterWatcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updatePreview() }
                override fun afterTextChanged(s: android.text.Editable?) {}
            }
            findViewById<EditText>(R.id.etHypLeftRegex).addTextChangedListener(filterWatcher)
            findViewById<EditText>(R.id.etHypMainRegex).addTextChangedListener(filterWatcher)

            val spinListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) { updatePreview() }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            findViewById<Spinner>(R.id.spnHypLeftSource).onItemSelectedListener = spinListener
            findViewById<Spinner>(R.id.spnHypMainSource).onItemSelectedListener = spinListener

        } else {
            llLiveUpdates.visibility = android.view.View.VISIBLE
            llHyperIsland.visibility = android.view.View.GONE

            val textSource = prefs.getString("${packageName}_text_source", "text")
            val rgTextSource: RadioGroup = findViewById(R.id.rgTextSource)
            when (textSource) {
                "title" -> rgTextSource.check(R.id.rbSourceTitle)
                "subtext" -> rgTextSource.check(R.id.rbSourceSubText)
                "titletext" -> rgTextSource.check(R.id.rbSourceTitleText)
                else -> rgTextSource.check(R.id.rbSourceText)
            }
            rgTextSource.setOnCheckedChangeListener { _, _ -> updatePreview() }

            val etRegex: EditText = findViewById(R.id.etRegexFilter)
            etRegex.setText(prefs.getString("${packageName}_regex_filter", ""))
            etRegex.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updatePreview() }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }

        // Load Global Icon Settings
        val currentIconSource = prefs.getString("${packageName}_icon_source", "default")
        val rbExtracted = findViewById<com.google.android.material.radiobutton.MaterialRadioButton>(R.id.rbIconExtracted)
        val hasDrawables = !prefs.getString("${packageName}_last_drawables", "").isNullOrEmpty()

        if (!hasDrawables) {
            rbExtracted.isEnabled = false
            rbExtracted.text = rbExtracted.text.toString() + " (No icons discovered yet)"
        }

        when (currentIconSource) {
            "app" -> findViewById<com.google.android.material.radiobutton.MaterialRadioButton>(R.id.rbIconApp).isChecked = true
            "notification" -> findViewById<com.google.android.material.radiobutton.MaterialRadioButton>(R.id.rbIconNotification).isChecked = true
            "extracted" -> rbExtracted.isChecked = true
            else -> {
                // Default: uses global toggle
            }
        }

        updatePreview()

        findViewById<RadioGroup>(R.id.rgIconSource).setOnCheckedChangeListener { _, _ -> updatePreview() }
    }

    private fun updatePreview() {
        val tvPreviewText: TextView = findViewById(R.id.tvPreviewText)
        val ivPreviewIcon: ImageView = findViewById(R.id.ivPreviewIcon)

        fun applyRegex(rawText: String, regexStr: String): String {
            if (regexStr.isEmpty()) return rawText
            return try {
                val regex = Regex(regexStr)
                val match = regex.find(rawText)
                if (match != null) {
                    if (match.groups.size > 1) match.groupValues.drop(1).joinToString(" ") else match.value
                } else rawText
            } catch (e: Exception) { rawText }
        }

        fun getRawText(sourceString: String): String {
            return when (sourceString) {
                "title" -> prefs.getString("${packageName}_last_title", "Title") ?: ""
                "subtext" -> prefs.getString("${packageName}_last_subtext", "SubText") ?: ""
                "titletext" -> {
                    val t = prefs.getString("${packageName}_last_title", "Title") ?: ""
                    val txt = prefs.getString("${packageName}_last_text", "Text") ?: ""
                    "$t • $txt"
                }
                else -> prefs.getString("${packageName}_last_text", "Text") ?: ""
            }
        }

        val castMode = prefs.getString("cast_mode", "live_updates")
        if (castMode == "hyperisland") {
            fun getSourceForIndex(index: Int): String = when (index) { 0 -> "title" 1 -> "text" 2 -> "subtext" 3 -> "titletext" else -> "text" }
            val leftSource = getSourceForIndex(findViewById<Spinner>(R.id.spnHypLeftSource).selectedItemPosition)
            val mainSource = getSourceForIndex(findViewById<Spinner>(R.id.spnHypMainSource).selectedItemPosition)
            
            val leftRaw = getRawText(leftSource)
            val mainRaw = getRawText(mainSource)
            
            val leftF = applyRegex(leftRaw, findViewById<EditText>(R.id.etHypLeftRegex).text.toString())
            val mainF = applyRegex(mainRaw, findViewById<EditText>(R.id.etHypMainRegex).text.toString())
            
            tvPreviewText.text = "L: $leftF  |  M: $mainF"
        } else {
            val rgTextSource = findViewById<RadioGroup>(R.id.rgTextSource)
            val selectedTextSource = when (rgTextSource.checkedRadioButtonId) {
                R.id.rbSourceTitle -> "title"
                R.id.rbSourceSubText -> "subtext"
                R.id.rbSourceTitleText -> "titletext"
                else -> "text"
            }
            val raw = getRawText(selectedTextSource)
            val regexStr = findViewById<EditText>(R.id.etRegexFilter).text.toString()
            tvPreviewText.text = applyRegex(raw, regexStr)
        }

        // Update Icon
        val checkedIconId = findViewById<RadioGroup>(R.id.rgIconSource).checkedRadioButtonId
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            when (checkedIconId) {
                R.id.rbIconApp -> {
                    ivPreviewIcon.setImageDrawable(appInfo.loadIcon(packageManager))
                }
                R.id.rbIconNotification -> {
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
        val castMode = prefs.getString("cast_mode", "live_updates")

        prefs.edit().apply {
            if (castMode == "hyperisland") {
                fun getSourceForIndex(index: Int): String = when (index) { 0 -> "title" 1 -> "text" 2 -> "subtext" 3 -> "titletext" else -> "text" }
                val spLeft = findViewById<Spinner>(R.id.spnHypLeftSource)
                val spMain = findViewById<Spinner>(R.id.spnHypMainSource)
                putString("${packageName}_hyper_left_source", getSourceForIndex(spLeft.selectedItemPosition))
                putString("${packageName}_hyper_main_source", getSourceForIndex(spMain.selectedItemPosition))
                putString("${packageName}_hyper_left_regex", findViewById<EditText>(R.id.etHypLeftRegex).text.toString())
                putString("${packageName}_hyper_main_regex", findViewById<EditText>(R.id.etHypMainRegex).text.toString())
            } else {
                val rgTextSource: RadioGroup = findViewById(R.id.rgTextSource)
                val selectedTextSource = when (rgTextSource.checkedRadioButtonId) {
                    R.id.rbSourceTitle -> "title"
                    R.id.rbSourceSubText -> "subtext"
                    R.id.rbSourceTitleText -> "titletext"
                    else -> "text"
                }
                val regexFilter = findViewById<EditText>(R.id.etRegexFilter).text.toString()
                putString("${packageName}_text_source", selectedTextSource)
                putString("${packageName}_regex_filter", regexFilter)
            }

            val iconSource = when {
                findViewById<com.google.android.material.radiobutton.MaterialRadioButton>(R.id.rbIconApp).isChecked -> "app"
                findViewById<com.google.android.material.radiobutton.MaterialRadioButton>(R.id.rbIconNotification).isChecked -> "notification"
                findViewById<com.google.android.material.radiobutton.MaterialRadioButton>(R.id.rbIconExtracted).isChecked -> "extracted"
                else -> "default"
            }
            putString("${packageName}_icon_source", iconSource)
            apply()
        }
        Toast.makeText(this, "Configuration Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
