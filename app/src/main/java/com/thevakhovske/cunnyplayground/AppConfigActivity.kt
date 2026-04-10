package com.thevakhovske.cunnyplayground

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.File

class AppConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageName = intent.getStringExtra("package_name") ?: run { finish(); return }

        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                AppConfigScreen(packageName, onBack = { finish() }, onSave = { finish() })
            }
        }
    }
}

@Composable
fun AppConfigScreen(packageName: String, onBack: () -> Unit, onSave: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)

    var appLabel by remember { mutableStateOf(packageName) }
    var appIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(packageName) {
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appLabel = appInfo.loadLabel(pm).toString()
            appIcon = appInfo.loadIcon(pm)
        } catch (_: Exception) {}
    }

    val castMode = remember { prefs.getString("cast_mode", "live_updates") ?: "live_updates" }

    var iconSource by remember { mutableStateOf(prefs.getString("${packageName}_icon_source", "default") ?: "default") }

    var hypLeftSource by remember { mutableStateOf(prefs.getString("${packageName}_hyper_left_source", "title") ?: "title") }
    var hypMainSource by remember { mutableStateOf(prefs.getString("${packageName}_hyper_main_source", "text") ?: "text") }
    var hypLeftRegex by remember { mutableStateOf(prefs.getString("${packageName}_hyper_left_regex", "") ?: "") }
    var hypMainRegex by remember { mutableStateOf(prefs.getString("${packageName}_hyper_main_regex", "") ?: "") }

    var luTextSource by remember { mutableStateOf(prefs.getString("${packageName}_text_source", "text") ?: "text") }
    var luRegex by remember { mutableStateOf(prefs.getString("${packageName}_regex_filter", "") ?: "") }

    val lastTitle = remember { prefs.getString("${packageName}_last_title", "N/A") ?: "N/A" }
    val lastText = remember { prefs.getString("${packageName}_last_text", "N/A") ?: "N/A" }
    val lastSubText = remember { prefs.getString("${packageName}_last_subtext", "N/A") ?: "N/A" }
    val lastDump = remember { prefs.getString("${packageName}_last_raw_dump", "Waiting for next interception...") ?: "Waiting..." }

    val drawablesStr = remember { prefs.getString("${packageName}_last_drawables", "") ?: "" }
    val drawableIds = remember { drawablesStr.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct() }

    fun applyRegex(rawText: String, regexStr: String): String {
        if (regexStr.isEmpty()) return rawText
        return try {
            val regex = Regex(regexStr)
            val match = regex.find(rawText)
            if (match != null) {
                if (match.groups.size > 1) match.groupValues.drop(1).joinToString(" ") else match.value
            } else rawText
        } catch (_: Exception) { rawText }
    }

    fun getRawText(source: String): String = when (source) {
        "title" -> prefs.getString("${packageName}_last_title", "Title") ?: ""
        "subtext" -> prefs.getString("${packageName}_last_subtext", "SubText") ?: ""
        "titletext" -> {
            val t = prefs.getString("${packageName}_last_title", "Title") ?: ""
            val txt = prefs.getString("${packageName}_last_text", "Text") ?: ""
            "$t • $txt"
        }
        else -> prefs.getString("${packageName}_last_text", "Text") ?: ""
    }

    val previewText = if (castMode == "hyperisland") {
        val leftF = applyRegex(getRawText(hypLeftSource), hypLeftRegex)
        val mainF = applyRegex(getRawText(hypMainSource), hypMainRegex)
        "L: $leftF  |  M: $mainF"
    } else {
        applyRegex(getRawText(luTextSource), luRegex)
    }

    fun saveSettings() {
        prefs.edit().apply {
            putString("${packageName}_icon_source", iconSource)
            if (castMode == "hyperisland") {
                putString("${packageName}_hyper_left_source", hypLeftSource)
                putString("${packageName}_hyper_main_source", hypMainSource)
                putString("${packageName}_hyper_left_regex", hypLeftRegex)
                putString("${packageName}_hyper_main_regex", hypMainRegex)
            } else {
                putString("${packageName}_text_source", luTextSource)
                putString("${packageName}_regex_filter", luRegex)
            }
            apply()
        }
        Toast.makeText(context, "Configuration Saved", Toast.LENGTH_SHORT).show()
        onSave()
    }

    val topAppBarScrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = appLabel,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { saveSettings() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Save") }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
        ) {
            // App Info
            item {
                SmallTitle("App Information")
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    BasicComponent(
                        title = appLabel,
                        summary = packageName,
                        startAction = {
                            appIcon?.let {
                                Image(
                                    painter = BitmapPainter(it.toBitmap().asImageBitmap()),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Preview
            item {
                SmallTitle("Output Preview")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        appIcon?.let {
                            Image(
                                painter = BitmapPainter(it.toBitmap().asImageBitmap()),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Text(
                            text = previewText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Icon Source
            item {
                SmallTitle("Icon Source")
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    RadioButtonPreference(
                        selected = iconSource == "app",
                        onClick = { iconSource = "app" },
                        title = "Original App Icon"
                    )
                    RadioButtonPreference(
                        selected = iconSource == "notification",
                        onClick = { iconSource = "notification" },
                        title = "Default Notification Icon"
                    )
                    RadioButtonPreference(
                        selected = iconSource == "extracted",
                        onClick = { iconSource = "extracted" },
                        title = if (drawableIds.isEmpty()) "Extracted Resource (No icons discovered yet)" else "Extracted Resource",
                        enabled = drawableIds.isNotEmpty()
                    )
                }
            }

            // Mode-specific Config
            if (castMode == "hyperisland") {
                item {
                    SmallTitle("HyperIsland Mapping")

                    SmallTitle("Left Segment Source")
                    val sources = listOf("title", "text", "subtext", "titletext")
                    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                        sources.forEach { source ->
                            RadioButtonPreference(
                                selected = hypLeftSource == source,
                                onClick = { hypLeftSource = source },
                                title = source.replaceFirstChar { it.uppercase() }
                            )
                        }
                        TextField(
                            value = hypLeftRegex,
                            onValueChange = { hypLeftRegex = it },
                            label = "Left Segment Regex",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SmallTitle("Main Segment Source")
                    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                        sources.forEach { source ->
                            RadioButtonPreference(
                                selected = hypMainSource == source,
                                onClick = { hypMainSource = source },
                                title = source.replaceFirstChar { it.uppercase() }
                            )
                        }
                        TextField(
                            value = hypMainRegex,
                            onValueChange = { hypMainRegex = it },
                            label = "Main Segment Regex",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                item {
                    SmallTitle("Live Update Mapping")

                    SmallTitle("Text Source")
                    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                        RadioButtonPreference(
                            selected = luTextSource == "title",
                            onClick = { luTextSource = "title" },
                            title = "Title"
                        )
                        RadioButtonPreference(
                            selected = luTextSource == "text",
                            onClick = { luTextSource = "text" },
                            title = "Text"
                        )
                        RadioButtonPreference(
                            selected = luTextSource == "subtext",
                            onClick = { luTextSource = "subtext" },
                            title = "SubText"
                        )
                        RadioButtonPreference(
                            selected = luTextSource == "titletext",
                            onClick = { luTextSource = "titletext" },
                            title = "Title+Text"
                        )

                        TextField(
                            value = luRegex,
                            onValueChange = { luRegex = it },
                            label = "Text Regex Filter",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Discovered Resources
            if (drawableIds.isNotEmpty()) {
                item {
                    SmallTitle("Discovered Resources")

                    val sourceContext = remember(packageName) {
                        try { context.createPackageContext(packageName, 0) } catch (_: Exception) { null }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(drawableIds) { id ->
                            sourceContext?.let { ctx ->
                                val drawable = remember(id) {
                                    try { ResourcesCompat.getDrawable(ctx.resources, id, ctx.theme) } catch (_: Exception) { null }
                                }
                                drawable?.let {
                                    Image(
                                        painter = BitmapPainter(it.toBitmap().asImageBitmap()),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clickable {
                                                val resName = try { ctx.resources.getResourceEntryName(id) } catch (_: Exception) { id.toString() }
                                                Toast.makeText(context, "ID: $id\nName: $resName", Toast.LENGTH_SHORT).show()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notification Render Preview
            item {
                val renderFile = remember(packageName) { File(context.filesDir, "renders/${packageName}.png") }
                if (renderFile.exists()) {
                    SmallTitle("Last Notification Render")
                    val bitmap = remember(packageName) {
                        try { BitmapFactory.decodeFile(renderFile.absolutePath) } catch (_: Exception) { null }
                    }
                    if (bitmap != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Notification render preview",
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        }
                    }
                }
            }

            // Raw Data
            item {
                SmallTitle("Latest Raw Data")
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    BasicComponent(title = "Title: $lastTitle")
                    BasicComponent(title = "Text: $lastText")
                    BasicComponent(title = "SubText: $lastSubText")
                }
            }

            item {
                SmallTitle("Raw Dump")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = lastDump,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Notification Dump", lastDump))
                        Toast.makeText(context, "Dump copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("Copy Raw Dump") }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
