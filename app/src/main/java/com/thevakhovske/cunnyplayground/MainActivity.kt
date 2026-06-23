package com.thevakhovske.cunnyplayground

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
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
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.NotesFill
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Settings
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
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_desc)
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
    val context = LocalContext.current

    val isMiui = remember { isMiuiRegion() }
    val isOrigin = remember { isOriginOs() }
    // OriginOS takes precedence for the middle "island" tab on this branch.
    val showOrigin = isOrigin
    val showHyper = isMiui && !isOrigin

    // Tab set: Playground, [island], Re-Caster, Inspector
    val tabIds = buildList {
        add("playground")
        if (showOrigin) add("originisland") else if (showHyper) add("hyperisland")
        add("recaster")
        add("inspector")
    }
    if (selectedTab >= tabIds.size) selectedTab = 0
    val current = tabIds[selectedTab]

    fun titleFor(id: String) = when (id) {
        "playground" -> R.string.title_playground
        "originisland" -> R.string.title_originisland
        "hyperisland" -> R.string.title_hyperisland
        "recaster" -> R.string.title_recaster
        else -> R.string.title_inspector
    }
    fun labelFor(id: String) = when (id) {
        "playground" -> R.string.tab_playground
        "originisland" -> R.string.tab_originisland
        "hyperisland" -> R.string.tab_hyperisland
        "recaster" -> R.string.tab_recaster
        else -> R.string.tab_inspector
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(titleFor(current)),
                actions = {
                    if (current == "hyperisland") {
                        IconButton(onClick = { context.startActivity(Intent(context, ExamplesActivity::class.java)) }) {
                            Icon(imageVector = MiuixIcons.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                    if (current == "originisland") {
                        IconButton(onClick = { context.startActivity(Intent(context, OriginSamplesActivity::class.java)) }) {
                            Icon(imageVector = MiuixIcons.SelectAll, contentDescription = stringResource(R.string.title_origin_samples))
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                tabIds.forEachIndexed { index, id ->
                    val navIcon = when (id) {
                        "playground" -> MiuixIcons.Notes
                        "originisland", "hyperisland" -> MiuixIcons.NotesFill
                        "recaster" -> MiuixIcons.Send
                        else -> MiuixIcons.SelectAll
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = navIcon,
                        label = stringResource(labelFor(id))
                    )
                }
            }
        }
    ) { paddingValues ->
        when (current) {
            "playground" -> PlaygroundScreen(paddingValues, scrollBehavior)
            "originisland" -> OriginIslandScreen(paddingValues, scrollBehavior)
            "hyperisland" -> HyperIslandScreen(paddingValues, scrollBehavior)
            "recaster" -> RecasterScreen(paddingValues, scrollBehavior)
            "inspector" -> InspectorScreen(paddingValues, scrollBehavior)
        }
    }
}

@Composable
fun InspectorScreen(paddingValues: PaddingValues, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current
    val records = remember { mutableStateListOf<SuperXRecordMeta>() }
    var accessGranted by remember { mutableStateOf(false) }

    fun reload() {
        records.clear()
        records.addAll(SuperXInspectorStore.list(context))
        accessGranted = androidx.core.app.NotificationManagerCompat
            .getEnabledListenerPackages(context).contains(context.packageName)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // Live refresh whenever the listener captures a new SuperX notification.
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { reload() }
        }
        val filter = android.content.IntentFilter(SuperXInspectorStore.ACTION_UPDATED)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose { try { context.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }
    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
    ) {
        item {
            SmallTitle(stringResource(R.string.section_inspector_about))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                BasicComponent(
                    title = stringResource(R.string.inspector_desc_title),
                    summary = stringResource(R.string.inspector_desc_summary)
                )
                BasicComponent(
                    title = stringResource(R.string.inspector_access_status),
                    summary = if (accessGranted) stringResource(R.string.inspector_access_on)
                              else stringResource(R.string.inspector_access_off)
                )
                ArrowPreference(
                    title = stringResource(R.string.action_grant_access),
                    summary = stringResource(R.string.action_grant_access_summary),
                    onClick = { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
                )
                ArrowPreference(
                    title = stringResource(R.string.action_rescan),
                    summary = stringResource(R.string.action_rescan_summary),
                    onClick = {
                        try {
                            android.service.notification.NotificationListenerService.requestRebind(
                                android.content.ComponentName(context, NotificationCastListener::class.java)
                            )
                        } catch (_: Exception) {}
                        reload()
                        Toast.makeText(context, context.getString(R.string.msg_rescan), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        if (records.isEmpty()) {
            item {
                Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.inspector_empty),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item { SmallTitle(stringResource(R.string.section_captured, records.size)) }
            items(records.toList()) { rec ->
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth()) {
                    val bmp = remember(rec.iconPath) {
                        rec.iconPath?.let {
                            try { android.graphics.BitmapFactory.decodeFile(it) } catch (_: Exception) { null }
                        }
                    }
                    BasicComponent(
                        title = rec.title.ifBlank { rec.appLabel },
                        summary = "${rec.appLabel} • ${rec.scene.ifBlank { "—" }} • tpl ${rec.template}${if (rec.pinned) "  ★" else ""}",
                        startAction = {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        },
                        onClick = {
                            context.startActivity(
                                Intent(context, InspectorDetailActivity::class.java).apply {
                                    putExtra("record_id", rec.id)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaygroundScreen(paddingValues: PaddingValues, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(context.getString(R.string.mode_live_updates)) }
    var text by remember { mutableStateOf("") }
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
            SmallTitle(stringResource(R.string.section_notif_info))
            TextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.label_title),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.label_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = subtext,
                onValueChange = { subtext = it },
                label = stringResource(R.string.label_subtext),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = statusChipText,
                onValueChange = { statusChipText = it },
                label = stringResource(R.string.label_chip_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            SmallTitle(stringResource(R.string.section_settings))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                CheckboxPreference(
                    checked = isOngoing,
                    onCheckedChange = { isOngoing = it },
                    title = stringResource(R.string.pref_ongoing)
                )
                CheckboxPreference(
                    checked = isPromoted,
                    onCheckedChange = { isPromoted = it },
                    title = stringResource(R.string.pref_promoted)
                )
                CheckboxPreference(
                    checked = useChrono,
                    onCheckedChange = { useChrono = it },
                    title = stringResource(R.string.pref_chrono)
                )
                CheckboxPreference(
                    checked = showProgress,
                    onCheckedChange = { showProgress = it },
                    title = stringResource(R.string.pref_show_progress)
                )
            }
        }

        item {
            SmallTitle(stringResource(R.string.section_icon))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = selectedIcon == 0,
                    onClick = { selectedIcon = 0 },
                    title = stringResource(R.string.icon_timer)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 1,
                    onClick = { selectedIcon = 1 },
                    title = stringResource(R.string.icon_call)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 2,
                    onClick = { selectedIcon = 2 },
                    title = stringResource(R.string.icon_alert)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 3,
                    onClick = { selectedIcon = 3 },
                    title = stringResource(R.string.icon_default)
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
                ) { Text(stringResource(R.string.btn_post)) }
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
                ) { Text(stringResource(R.string.btn_update)) }
                Button(
                    onClick = {
                        stopService(context)
                        notifications.clear()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_clear_all)) }
            }
        }

        if (notifications.isNotEmpty()) {
            item { SmallTitle(stringResource(R.string.section_posted_notifs)) }
            items(notifications.toList()) { notif ->
                BasicComponent(
                    title = notif.title,
                    summary = "ID: ${notif.id} • ${if (notif.isPromoted) stringResource(R.string.pref_promoted).substringBefore("(") else "Standard"}${if (!notif.statusChipText.isNullOrEmpty()) " • Chip: ${notif.statusChipText}" else ""}",
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
    var hTitle by remember { mutableStateOf(context.getString(R.string.mode_hyperisland)) }
    var hText by remember { mutableStateOf("") }
    var hSubText by remember { mutableStateOf("") }
    var hLeftText by remember { mutableStateOf("") }
    var hMainText by remember { mutableStateOf("") }
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
            SmallTitle(stringResource(R.string.section_hyper_payload))
            TextField(
                value = hTitle,
                onValueChange = { hTitle = it },
                label = stringResource(R.string.label_title),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hText,
                onValueChange = { hText = it },
                label = stringResource(R.string.label_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hSubText,
                onValueChange = { hSubText = it },
                label = stringResource(R.string.label_subtext),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hLeftText,
                onValueChange = { hLeftText = it },
                label = stringResource(R.string.label_left_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = hMainText,
                onValueChange = { hMainText = it },
                label = stringResource(R.string.label_main_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            SmallTitle(stringResource(R.string.section_raw_json))
            TextField(
                value = rawJson,
                onValueChange = { rawJson = it },
                label = stringResource(R.string.label_custom_json),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(120.dp)
            )
        }

        item {
            SmallTitle(stringResource(R.string.section_icon))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = selectedIcon == 0,
                    onClick = { selectedIcon = 0 },
                    title = stringResource(R.string.icon_timer)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 1,
                    onClick = { selectedIcon = 1 },
                    title = stringResource(R.string.icon_call)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 2,
                    onClick = { selectedIcon = 2 },
                    title = stringResource(R.string.icon_alert)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 3,
                    onClick = { selectedIcon = 3 },
                    title = stringResource(R.string.icon_default)
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
                ) { Text(stringResource(R.string.btn_post_hyper)) }
                Button(
                    onClick = { stopService(context) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_clear_all)) }
            }
        }
    }
}

@Composable
fun OriginIslandScreen(paddingValues: PaddingValues, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(context.getString(R.string.mode_originisland)) }
    var content by remember { mutableStateOf("") }
    var leftContent by remember { mutableStateOf("") }
    var rightContent by remember { mutableStateOf("") }
    var extra1 by remember { mutableStateOf("") }
    var extra2 by remember { mutableStateOf("") }
    var extra3 by remember { mutableStateOf("") }
    var extra4 by remember { mutableStateOf("") }
    var template by remember { mutableIntStateOf(OriginIslandConstants.TEMPLATE_PRIORITY_INFO) }
    var rightTemplate by remember { mutableIntStateOf(OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) }
    var progress by remember { mutableStateOf("50") }
    var scene by remember { mutableStateOf("NAVIGATION") }
    var bgColor by remember { mutableStateOf("#FFFFFF") }
    var fgColor by remember { mutableStateOf("#000000") }
    var selectedIcon by remember { mutableIntStateOf(2) }
    // Advanced / decompiled knobs
    var advSubText by remember { mutableStateOf("") }
    var advNavMsg by remember { mutableStateOf("") }
    var advButtonTitles by remember { mutableStateOf("") }
    var advCardBg by remember { mutableStateOf("") }
    var advLightColor by remember { mutableStateOf("") }
    var advLeftDouble by remember { mutableStateOf("") }
    var advRightDouble by remember { mutableStateOf("") }
    var advKeepScreenOn by remember { mutableStateOf(false) }
    var advForceShow by remember { mutableStateOf(false) }
    var advIconStatus by remember { mutableIntStateOf(-1) }
    var advKeepDuration by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = paddingValues,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic()
    ) {
        item {
            SmallTitle(stringResource(R.string.section_origin_payload))
            TextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.label_title),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = content,
                onValueChange = { content = it },
                label = stringResource(R.string.label_text),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = leftContent,
                onValueChange = { leftContent = it },
                label = stringResource(R.string.label_left_content),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = rightContent,
                onValueChange = { rightContent = it },
                label = stringResource(R.string.label_right_content),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            SmallTitle(stringResource(R.string.section_origin_template))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = template == OriginIslandConstants.TEMPLATE_PRIORITY_INFO,
                    onClick = { template = OriginIslandConstants.TEMPLATE_PRIORITY_INFO },
                    title = stringResource(R.string.template_priority)
                )
                RadioButtonPreference(
                    selected = template == OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL,
                    onClick = { template = OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL },
                    title = stringResource(R.string.template_progress)
                )
                RadioButtonPreference(
                    selected = template == OriginIslandConstants.TEMPLATE_TEXT_SYMMETRY,
                    onClick = { template = OriginIslandConstants.TEMPLATE_TEXT_SYMMETRY },
                    title = stringResource(R.string.template_symmetry)
                )
                RadioButtonPreference(
                    selected = template == OriginIslandConstants.TEMPLATE_BASE,
                    onClick = { template = OriginIslandConstants.TEMPLATE_BASE },
                    title = stringResource(R.string.template_base)
                )
                RadioButtonPreference(
                    selected = template == OriginIslandConstants.TEMPLATE_NAVIGATION,
                    onClick = { template = OriginIslandConstants.TEMPLATE_NAVIGATION },
                    title = stringResource(R.string.template_navigation)
                )
                RadioButtonPreference(
                    selected = template == OriginIslandConstants.TEMPLATE_BUTTONS,
                    onClick = { template = OriginIslandConstants.TEMPLATE_BUTTONS },
                    title = stringResource(R.string.template_buttons)
                )
                RadioButtonPreference(
                    selected = template == OriginIslandConstants.TEMPLATE_DRIVING_NAVI,
                    onClick = { template = OriginIslandConstants.TEMPLATE_DRIVING_NAVI },
                    title = stringResource(R.string.template_driving_navi)
                )
                RadioButtonPreference(
                    selected = template == OriginIslandConstants.TEMPLATE_NOTIF_CUSTOM,
                    onClick = { template = OriginIslandConstants.TEMPLATE_NOTIF_CUSTOM },
                    title = stringResource(R.string.template_custom)
                )
            }
        }

        // Advanced (decompiled) custom controls
        item {
            SmallTitle(stringResource(R.string.section_origin_advanced))
            TextField(
                value = advButtonTitles,
                onValueChange = { advButtonTitles = it },
                label = stringResource(R.string.label_button_titles),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = advSubText,
                onValueChange = { advSubText = it },
                label = stringResource(R.string.label_subtext),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = advNavMsg,
                onValueChange = { advNavMsg = it },
                label = stringResource(R.string.label_nav_msg),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = advLeftDouble,
                onValueChange = { advLeftDouble = it },
                label = stringResource(R.string.label_left_doubleline),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = advRightDouble,
                onValueChange = { advRightDouble = it },
                label = stringResource(R.string.label_right_doubleline),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = advCardBg,
                onValueChange = { advCardBg = it },
                label = stringResource(R.string.label_card_bg_color),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = advLightColor,
                onValueChange = { advLightColor = it },
                label = stringResource(R.string.label_light_color),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = advKeepDuration,
                onValueChange = { advKeepDuration = it },
                label = stringResource(R.string.label_keep_duration),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                CheckboxPreference(
                    checked = advKeepScreenOn,
                    onCheckedChange = { advKeepScreenOn = it },
                    title = stringResource(R.string.label_keep_screen_on)
                )
                CheckboxPreference(
                    checked = advForceShow,
                    onCheckedChange = { advForceShow = it },
                    title = stringResource(R.string.label_force_show)
                )
                RadioButtonPreference(
                    selected = advIconStatus == -1,
                    onClick = { advIconStatus = -1 },
                    title = stringResource(R.string.status_none)
                )
                RadioButtonPreference(
                    selected = advIconStatus == OriginIslandConstants.ICON_STATUS_SUCCESS,
                    onClick = { advIconStatus = OriginIslandConstants.ICON_STATUS_SUCCESS },
                    title = stringResource(R.string.status_success)
                )
                RadioButtonPreference(
                    selected = advIconStatus == OriginIslandConstants.ICON_STATUS_FAIL,
                    onClick = { advIconStatus = OriginIslandConstants.ICON_STATUS_FAIL },
                    title = stringResource(R.string.status_fail)
                )
                RadioButtonPreference(
                    selected = advIconStatus == OriginIslandConstants.ICON_STATUS_ERROR,
                    onClick = { advIconStatus = OriginIslandConstants.ICON_STATUS_ERROR },
                    title = stringResource(R.string.status_error)
                )
            }
        }

        item {
            SmallTitle(stringResource(R.string.section_origin_right_template))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                val rightOptions = listOf(
                    OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE to stringResource(R.string.ritmpl_wave),
                    OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS to stringResource(R.string.ritmpl_progress),
                    OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_LOADING to stringResource(R.string.ritmpl_loading),
                    OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON to stringResource(R.string.ritmpl_text_icon),
                    OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_ICON_TEXT to stringResource(R.string.ritmpl_icon_text),
                    OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT to stringResource(R.string.ritmpl_capsule)
                )
                rightOptions.forEach { (value, label) ->
                    RadioButtonPreference(
                        selected = rightTemplate == value,
                        onClick = { rightTemplate = value },
                        title = label
                    )
                }
            }
        }

        item {
            SmallTitle(stringResource(R.string.section_origin_extras))
            TextField(
                value = extra1,
                onValueChange = { extra1 = it },
                label = stringResource(R.string.label_extra1),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = extra2,
                onValueChange = { extra2 = it },
                label = stringResource(R.string.label_extra2),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = extra3,
                onValueChange = { extra3 = it },
                label = stringResource(R.string.label_extra3),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = extra4,
                onValueChange = { extra4 = it },
                label = stringResource(R.string.label_extra4),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = progress,
                onValueChange = { progress = it },
                label = stringResource(R.string.label_progress),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = scene,
                onValueChange = { scene = it },
                label = stringResource(R.string.label_scene),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = bgColor,
                onValueChange = { bgColor = it },
                label = stringResource(R.string.label_bg_color),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            TextField(
                value = fgColor,
                onValueChange = { fgColor = it },
                label = stringResource(R.string.label_fg_color),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            SmallTitle(stringResource(R.string.section_icon))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = selectedIcon == 0,
                    onClick = { selectedIcon = 0 },
                    title = stringResource(R.string.icon_timer)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 1,
                    onClick = { selectedIcon = 1 },
                    title = stringResource(R.string.icon_call)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 2,
                    onClick = { selectedIcon = 2 },
                    title = stringResource(R.string.icon_alert)
                )
                RadioButtonPreference(
                    selected = selectedIcon == 3,
                    onClick = { selectedIcon = 3 },
                    title = stringResource(R.string.icon_default)
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
                        postOriginIslandNotification(
                            context, title, content, leftContent, rightContent,
                            extra1, extra2, extra3, extra4,
                            template, rightTemplate, progress.toIntOrNull() ?: 50,
                            scene, bgColor, fgColor, getIconRes(selectedIcon),
                            OriginAdvanced(
                                subText = advSubText,
                                navMsg = advNavMsg,
                                cardBgColor = advCardBg,
                                lightColor = advLightColor,
                                keepScreenOn = advKeepScreenOn,
                                forceShow = advForceShow,
                                keepDuration = advKeepDuration.toIntOrNull() ?: 0,
                                iconStatusType = advIconStatus,
                                leftDoubleLine = advLeftDouble,
                                rightDoubleLine = advRightDouble,
                                buttonTitles = advButtonTitles
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_post_origin)) }
                Button(
                    onClick = { stopService(context) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_clear_all)) }
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
            SmallTitle(stringResource(R.string.section_casting_settings))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                SwitchPreference(
                    checked = castEnabled,
                    onCheckedChange = {
                        castEnabled = it
                        prefs.edit().putBoolean("cast_notifications", it).apply()
                    },
                    title = stringResource(R.string.pref_cast_notifs),
                    summary = stringResource(R.string.pref_cast_notifs_summary)
                )
                SwitchPreference(
                    checked = useAppIcon,
                    onCheckedChange = {
                        useAppIcon = it
                        prefs.edit().putBoolean("use_app_icon", it).apply()
                    },
                    title = stringResource(R.string.pref_use_app_icon),
                    summary = stringResource(R.string.pref_use_app_icon_summary)
                )
                SwitchPreference(
                    checked = showProgressPercent,
                    onCheckedChange = {
                        showProgressPercent = it
                        prefs.edit().putBoolean("show_progress_percentage", it).apply()
                    },
                    title = stringResource(R.string.pref_show_progress_percent),
                    summary = stringResource(R.string.pref_show_progress_percent_summary)
                )
                SwitchPreference(
                    checked = limitChipText,
                    onCheckedChange = {
                        limitChipText = it
                        prefs.edit().putBoolean("limit_chip_7char", it).apply()
                    },
                    title = stringResource(R.string.pref_limit_chip),
                    summary = stringResource(R.string.pref_limit_chip_summary)
                )
            }
        }

        item {
            SmallTitle(stringResource(R.string.section_cast_as))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                RadioButtonPreference(
                    selected = castMode == "live_updates",
                    onClick = {
                        castMode = "live_updates"
                        prefs.edit().putString("cast_mode", "live_updates").apply()
                    },
                    title = stringResource(R.string.mode_live_updates)
                )
                if (isMiuiRegion()) {
                    RadioButtonPreference(
                        selected = castMode == "hyperisland",
                        onClick = {
                            castMode = "hyperisland"
                            prefs.edit().putString("cast_mode", "hyperisland").apply()
                        },
                        title = stringResource(R.string.mode_hyperisland)
                    )
                }
                if (isOriginOs()) {
                    RadioButtonPreference(
                        selected = castMode == "originisland",
                        onClick = {
                            castMode = "originisland"
                            prefs.edit().putString("cast_mode", "originisland").apply()
                        },
                        title = stringResource(R.string.mode_originisland),
                        summary = stringResource(R.string.pref_cast_notifs_origin_summary)
                    )
                }
            }
        }

        if (isMiuiCN() && castMode == "hyperisland") {
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = Color(0xFFFEE2E2) // Light red background
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = MiuixIcons.Settings, // Using Settings icon as fallback for warning
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.warning_cn_rom_title),
                                color = Color.Red,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.warning_cn_rom_msg),
                            color = Color(0xFF991B1B), // Darker red text
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        item {
            SmallTitle(stringResource(R.string.section_actions))
            Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                ArrowPreference(
                    title = stringResource(R.string.action_grant_access),
                    summary = stringResource(R.string.action_grant_access_summary),
                    onClick = {
                        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
                )
                ArrowPreference(
                    title = stringResource(R.string.action_select_apps),
                    summary = if (enabledApps.size == 1) stringResource(R.string.summary_app_selected) else stringResource(R.string.summary_apps_selected, enabledApps.size),
                    onClick = {
                        context.startActivity(Intent(context, AppPickerActivity::class.java))
                    }
                )
            }
        }

        if (enabledApps.isNotEmpty()) {
            item { 
                SmallTitle(stringResource(R.string.section_enabled_apps, enabledApps.size)) 
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

/** Advanced (decompiled) OriginIsland knobs exposed in the playground. Blank strings = unset. */
data class OriginAdvanced(
    val subText: String = "",
    val navMsg: String = "",
    val cardBgColor: String = "",
    val lightColor: String = "",
    val lightMode: Int = 0,
    val keepScreenOn: Boolean = false,
    val disableInvert: Boolean = false,
    val forceShow: Boolean = false,
    val dismissWhenKill: Boolean = true,
    val keepDuration: Int = 0,
    val islandShowTime: Int = 0,
    val displays: Int = 0,
    val generatingStatus: Int = 0,
    val iconStatusType: Int = -1,
    val leftDoubleLine: String = "",
    val rightDoubleLine: String = "",
    val buttonTitles: String = "",
    val customTemplate: android.widget.RemoteViews? = null
)

private fun splitCsv(s: String): ArrayList<String> =
    ArrayList(s.split(",").map { it.trim() }.filter { it.isNotEmpty() })

fun postOriginIslandNotification(
    context: Context, title: String, content: String, leftContent: String, rightContent: String,
    extra1: String, extra2: String, extra3: String, extra4: String,
    template: Int, rightTemplate: Int, progress: Int, scene: String,
    bgColor: String, fgColor: String, iconRes: Int, adv: OriginAdvanced = OriginAdvanced()
) {
    val intent = Intent(context, PlaygroundService::class.java).apply {
        action = PlaygroundService.ACTION_START
        putExtra("title", title)
        putExtra("text", content)
        putExtra("subtext", adv.subText)
        putExtra("id", (System.currentTimeMillis() % 100000).toInt())
        putExtra("icon_res", iconRes)
        putExtra("source_app", "Manual-Origin-Compose")
        putExtra("cast_mode", "originisland")
        putExtra("oi_template", template)
        putExtra("oi_right_template", rightTemplate)
        putExtra("oi_left_content", leftContent)
        putExtra("oi_right_content", rightContent)
        putExtra("oi_extra1", extra1)
        putExtra("oi_extra2", extra2)
        putExtra("oi_extra3", extra3)
        putExtra("oi_extra4", extra4)
        putExtra("oi_progress", progress)
        putExtra("oi_scene", scene)
        putExtra("oi_bg_color", bgColor)
        putExtra("oi_fg_color", fgColor)
        // advanced / decompiled knobs
        if (adv.navMsg.isNotBlank()) putExtra("oi_nav_msg", adv.navMsg)
        if (adv.cardBgColor.isNotBlank()) putExtra("oi_card_bg_color", adv.cardBgColor)
        if (adv.lightColor.isNotBlank()) putExtra("oi_light_color", adv.lightColor)
        putExtra("oi_light_mode", adv.lightMode)
        putExtra("oi_keep_screen_on", adv.keepScreenOn)
        putExtra("oi_disable_invert", adv.disableInvert)
        putExtra("oi_force_show", adv.forceShow)
        putExtra("oi_dismiss_when_kill", adv.dismissWhenKill)
        putExtra("oi_keep_duration", adv.keepDuration)
        putExtra("oi_island_show_time", adv.islandShowTime)
        putExtra("oi_displays", adv.displays)
        putExtra("oi_generating_status", adv.generatingStatus)
        putExtra("oi_icon_status_type", adv.iconStatusType)
        putStringArrayListExtra("oi_left_doubleline", splitCsv(adv.leftDoubleLine))
        putStringArrayListExtra("oi_right_doubleline", splitCsv(adv.rightDoubleLine))
        putStringArrayListExtra("oi_button_titles", splitCsv(adv.buttonTitles))
        if (adv.customTemplate != null) {
            putExtra("oi_custom_template", adv.customTemplate)
        }
        putExtra("show_progress", template == OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL ||
            rightTemplate == OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS)
        putExtra("progress", progress)
        putExtra("progress_max", 100)
    }
    if (Build.VERSION.SDK_INT >= 26) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

fun stopService(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        context.startForegroundService(Intent(context, PlaygroundService::class.java).apply { action = PlaygroundService.ACTION_STOP })
    } else {
        context.startService(Intent(context, PlaygroundService::class.java).apply { action = PlaygroundService.ACTION_STOP })
    }
}

fun isMiuiRegion(): Boolean {
    return try {
        val buildClass = Class.forName("android.os.SystemProperties")
        val method = buildClass.getMethod("get", String::class.java)
        val value = method.invoke(buildClass, "ro.miui.region") as String
        value.isNotEmpty()
    } catch (_: Exception) {
        false
    }
}

fun isMiuiCN(): Boolean {
    return try {
        val buildClass = Class.forName("android.os.SystemProperties")
        val method = buildClass.getMethod("get", String::class.java)
        val value = method.invoke(buildClass, "ro.miui.region") as String
        value == "CN"
    } catch (_: Exception) {
        false
    }
}

fun isOriginOs(): Boolean {
    return try {
        val buildClass = Class.forName("android.os.SystemProperties")
        val method = buildClass.getMethod("get", String::class.java)
        val osName = (method.invoke(buildClass, "ro.vivo.os.name") as? String).orEmpty()
        osName.isNotEmpty() || Build.MANUFACTURER.equals("vivo", ignoreCase = true)
    } catch (_: Exception) {
        Build.MANUFACTURER.equals("vivo", ignoreCase = true)
    }
}
