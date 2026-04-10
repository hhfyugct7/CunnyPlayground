package com.thevakhovske.cunnyplayground

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.NotesFill
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.overScrollVertical

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

class MainActivity : ComponentActivity() {

    companion object {
        const val CHANNEL_ID = "live_updates_channel"
        const val NOTIFICATION_ID = 1001
        const val PERMISSION_REQUEST_CODE = 101
        const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.app.extra.PROMOTED_ONGOING"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)
        checkPermissions()

        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                MainScreen()
            }
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
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
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val labels = listOf("Playground", "HyperIsland", "Re-Caster")

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = when (selectedTab) {
                    0 -> "Live Updates Playground"
                    1 -> "HyperIsland Playground"
                    else -> "Notification Re-Caster"
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                labels.forEachIndexed { index, label ->
                    val navIcon = when (index) {
                        0 -> MiuixIcons.Notes
                        1 -> MiuixIcons.NotesFill
                        else -> MiuixIcons.Send
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = navIcon,
                        label = label
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> PlaygroundScreen(paddingValues, scrollBehavior)
            1 -> HyperIslandScreen(paddingValues, scrollBehavior)
            2 -> RecasterScreen(paddingValues, scrollBehavior)
        }
    }
}

@Composable
fun PlaygroundScreen(paddingValues: PaddingValues, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("Live Update") }
    var text by remember { mutableStateOf("Ongoing task...") }
    var subtext by remember { mutableStateOf("") }
    var statusChipText by remember { mutableStateOf("50%") }
    var isPromoted by remember { mutableStateOf(true) }
    var isOngoing by remember { mutableStateOf(true) }
    var showProgress by remember { mutableStateOf(true) }
    var useChrono by remember { mutableStateOf(false) }
    var selectedIcon by remember { mutableIntStateOf(0) }

    val notifications = remember { mutableStateListOf<NotificationInfo>() }
    var lastId by remember { mutableIntStateOf(1000) }
    var editingId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
    ) {
        item {
            SmallTitle("Notification Info")
            TextField(
                value = title,
                onValueChange = { title = it },
                label = "Title",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = text,
                onValueChange = { text = it },
                label = "Text",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = subtext,
                onValueChange = { subtext = it },
                label = "SubText",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = statusChipText,
                onValueChange = { statusChipText = it },
                label = "Status Chip Text",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            SmallTitle("Settings")
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                CheckboxPreference(
                    checked = isOngoing,
                    onCheckedChange = { isOngoing = it },
                    title = "Ongoing"
                )
                CheckboxPreference(
                    checked = isPromoted,
                    onCheckedChange = { isPromoted = it },
                    title = "Promoted (Status Chip)"
                )
                CheckboxPreference(
                    checked = useChrono,
                    onCheckedChange = { useChrono = it },
                    title = "Chronometer"
                )
                CheckboxPreference(
                    checked = showProgress,
                    onCheckedChange = { showProgress = it },
                    title = "Show Progress Bar"
                )
            }
        }

        item {
            SmallTitle("Icon")
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = selectedIcon == 0,
                    onClick = { selectedIcon = 0 },
                    title = "Timer"
                )
                RadioButtonPreference(
                    selected = selectedIcon == 1,
                    onClick = { selectedIcon = 1 },
                    title = "Call"
                )
                RadioButtonPreference(
                    selected = selectedIcon == 2,
                    onClick = { selectedIcon = 2 },
                    title = "Alert"
                )
                RadioButtonPreference(
                    selected = selectedIcon == 3,
                    onClick = { selectedIcon = 3 },
                    title = "Default"
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        editingId = null
                        val iconRes = getIconRes(selectedIcon)
                        val nId = ++lastId
                        notifications.add(NotificationInfo(nId, title, text, iconRes, isPromoted, statusChipText, showProgress))
                        postNotification(context, title, text, subtext, statusChipText, nId, iconRes, isPromoted, showProgress)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Post") }
                Button(
                    onClick = {
                        val updateId = editingId ?: notifications.lastOrNull()?.id
                        if (updateId != null) {
                            val iconRes = getIconRes(selectedIcon)
                            val updatedText = "$text (Updated)"
                            val idx = notifications.indexOfFirst { it.id == updateId }
                            if (idx != -1) {
                                notifications[idx] = notifications[idx].copy(title = title, text = updatedText, iconRes = iconRes, isPromoted = isPromoted, statusChipText = statusChipText, showProgress = showProgress)
                            }
                            postNotification(context, title, updatedText, subtext, statusChipText, updateId, iconRes, isPromoted, showProgress)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Update") }
                Button(
                    onClick = {
                        stopService(context)
                        notifications.clear()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear All") }
            }
        }

        if (notifications.isNotEmpty()) {
            item { SmallTitle("Posted Notifications") }
            items(notifications.toList()) { notif ->
                BasicComponent(
                    title = notif.title,
                    summary = "ID: ${notif.id} • ${if (notif.isPromoted) "Promoted" else "Standard"}${if (!notif.statusChipText.isNullOrEmpty()) " • Chip: ${notif.statusChipText}" else ""}",
                    onClick = {
                        editingId = notif.id
                        title = notif.title
                        text = notif.text
                        statusChipText = notif.statusChipText ?: ""
                        isPromoted = notif.isPromoted
                        showProgress = notif.showProgress
                    }
                )
            }
        }
    }
}

@Composable
fun HyperIslandScreen(paddingValues: PaddingValues, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current
    var hTitle by remember { mutableStateOf("Hyper Island") }
    var hText by remember { mutableStateOf("Dynamic Payload") }
    var hSubText by remember { mutableStateOf("") }
    var hLeftText by remember { mutableStateOf("") }
    var hMainText by remember { mutableStateOf("Main Content") }
    var rawJson by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableIntStateOf(0) }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
    ) {
        item {
            SmallTitle("HyperIsland Payload")
            TextField(
                value = hTitle,
                onValueChange = { hTitle = it },
                label = "Title",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hText,
                onValueChange = { hText = it },
                label = "Text",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hSubText,
                onValueChange = { hSubText = it },
                label = "SubText",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hLeftText,
                onValueChange = { hLeftText = it },
                label = "Left Text",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hMainText,
                onValueChange = { hMainText = it },
                label = "Main Text",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            SmallTitle("Raw JSON (Optional)")
            TextField(
                value = rawJson,
                onValueChange = { rawJson = it },
                label = "Custom JSON Payload",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(120.dp)
            )
        }

        item {
            SmallTitle("Icon")
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = selectedIcon == 0,
                    onClick = { selectedIcon = 0 },
                    title = "Timer"
                )
                RadioButtonPreference(
                    selected = selectedIcon == 1,
                    onClick = { selectedIcon = 1 },
                    title = "Call"
                )
                RadioButtonPreference(
                    selected = selectedIcon == 2,
                    onClick = { selectedIcon = 2 },
                    title = "Alert"
                )
                RadioButtonPreference(
                    selected = selectedIcon == 3,
                    onClick = { selectedIcon = 3 },
                    title = "Default"
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        postHyperNotification(context, hTitle, hText, hSubText, hLeftText, hMainText, rawJson, getIconRes(selectedIcon))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Post HyperIsland") }
                Button(
                    onClick = { stopService(context) },
                    modifier = Modifier.weight(1f)
                ) { Text("Clear All") }
            }
        }
    }
}

@Composable
fun RecasterScreen(paddingValues: PaddingValues, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)

    var castEnabled by remember { mutableStateOf(prefs.getBoolean("cast_notifications", false)) }
    var useAppIcon by remember { mutableStateOf(prefs.getBoolean("use_app_icon", false)) }
    var showProgressPercent by remember { mutableStateOf(prefs.getBoolean("show_progress_percentage", false)) }
    var limitChipText by remember { mutableStateOf(prefs.getBoolean("limit_chip_7char", false)) }
    var castMode by remember { mutableStateOf(prefs.getString("cast_mode", "live_updates") ?: "live_updates") }

    val pm = context.packageManager
    val enabledApps = remember { mutableStateListOf<EnabledApp>() }

    fun loadEnabledApps() {
        val selectedPackages = prefs.getStringSet("cast_enabled_apps", emptySet()) ?: emptySet()
        val newList = selectedPackages.mapNotNull { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                EnabledApp(
                    name = appInfo.loadLabel(pm).toString(),
                    packageName = pkg,
                    icon = appInfo.loadIcon(pm)
                )
            } catch (_: Exception) { null }
        }.sortedBy { it.name.lowercase() }
        enabledApps.clear()
        enabledApps.addAll(newList)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                loadEnabledApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { loadEnabledApps() }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
    ) {
        item {
            SmallTitle("Casting Settings")
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                SwitchPreference(
                    checked = castEnabled,
                    onCheckedChange = {
                        castEnabled = it
                        prefs.edit().putBoolean("cast_notifications", it).apply()
                    },
                    title = "Cast Notifications",
                    summary = "Re-cast intercepted notifications as Live Updates"
                )
                SwitchPreference(
                    checked = useAppIcon,
                    onCheckedChange = {
                        useAppIcon = it
                        prefs.edit().putBoolean("use_app_icon", it).apply()
                    },
                    title = "Use Original App Icons",
                    summary = "Use source app icon instead of notification icon"
                )
                SwitchPreference(
                    checked = showProgressPercent,
                    onCheckedChange = {
                        showProgressPercent = it
                        prefs.edit().putBoolean("show_progress_percentage", it).apply()
                    },
                    title = "Show Progress Percentage",
                    summary = "Display progress percentage in status chip"
                )
                SwitchPreference(
                    checked = limitChipText,
                    onCheckedChange = {
                        limitChipText = it
                        prefs.edit().putBoolean("limit_chip_7char", it).apply()
                    },
                    title = "(AOSP only) Limit Chip Text",
                    summary = "Limit status chip text to 7 characters"
                )
            }
        }

        item {
            SmallTitle("Cast As")
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = castMode == "live_updates",
                    onClick = {
                        castMode = "live_updates"
                        prefs.edit().putString("cast_mode", "live_updates").apply()
                    },
                    title = "Live Updates"
                )
                RadioButtonPreference(
                    selected = castMode == "hyperisland",
                    onClick = {
                        castMode = "hyperisland"
                        prefs.edit().putString("cast_mode", "hyperisland").apply()
                    },
                    title = "HyperIsland"
                )
            }
        }

        item {
            SmallTitle("Actions")
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                ArrowPreference(
                    title = "Grant Notification Access",
                    summary = "Required for intercepting notifications",
                    onClick = {
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
                )
                ArrowPreference(
                    title = "Select Apps to Cast",
                    summary = "${enabledApps.size} app${if (enabledApps.size != 1) "s" else ""} selected",
                    onClick = {
                        context.startActivity(Intent(context, AppPickerActivity::class.java))
                    }
                )
            }
        }

        if (enabledApps.isNotEmpty()) {
            item { 
                SmallTitle("Enabled Apps (${enabledApps.size})") 
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    enabledApps.forEach { app ->
                        BasicComponent(
                            title = app.name,
                            summary = app.packageName,
                            startAction = {
                                Image(
                                    painter = BitmapPainter(app.icon.toBitmap().asImageBitmap()),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            onClick = {
                                val intent = Intent(context, AppConfigActivity::class.java).apply {
                                    putExtra("package_name", app.packageName)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ──

private fun getIconRes(index: Int): Int = when (index) {
    0 -> R.drawable.ic_timer
    1 -> R.drawable.ic_call
    2 -> R.drawable.ic_alert
    else -> R.mipmap.ic_launcher_round
}

fun postNotification(
    context: Context, title: String, text: String, subtext: String,
    chipText: String, id: Int, iconRes: Int, isPromoted: Boolean, showProgress: Boolean
) {
    val intent = Intent(context, PlaygroundService::class.java).apply {
        action = PlaygroundService.ACTION_START
        putExtra("title", title)
        putExtra("text", text)
        putExtra("subtext", subtext)
        putExtra("status_chip_text", chipText)
        putExtra("id", id)
        putExtra("icon_res", iconRes)
        putExtra("is_promoted", isPromoted)
        putExtra("show_progress", showProgress)
        if (showProgress) {
            putExtra("progress", 50)
            putExtra("progress_max", 100)
        }
        putExtra("when", System.currentTimeMillis())
        putExtra("source_app", "Manual-Compose")
        putExtra("cast_mode", "live_updates")
    }
    if (Build.VERSION.SDK_INT >= 26) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

fun postHyperNotification(
    context: Context, title: String, text: String, subtext: String,
    leftText: String, mainText: String, rawJson: String, iconRes: Int
) {
    val intent = Intent(context, PlaygroundService::class.java).apply {
        action = PlaygroundService.ACTION_START
        putExtra("title", title)
        putExtra("text", text)
        putExtra("subtext", subtext)
        putExtra("hyper_left_text", leftText)
        putExtra("hyper_main_text", mainText)
        putExtra("raw_hyper_json", rawJson)
        putExtra("id", (System.currentTimeMillis() % 100000).toInt())
        putExtra("icon_res", iconRes)
        putExtra("is_promoted", true)
        putExtra("source_app", "Manual-Hyper-Compose")
        putExtra("cast_mode", "hyperisland")
    }
    if (Build.VERSION.SDK_INT >= 26) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

fun stopService(context: Context) {
    val intent = Intent(context, PlaygroundService::class.java).apply {
        action = PlaygroundService.ACTION_STOP
    }
    context.startService(intent)
}
