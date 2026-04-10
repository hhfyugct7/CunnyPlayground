package com.thevakhovske.cunnyplayground

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

class AppPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                AppPickerScreen(onBack = { finish() })
            }
        }
    }
}

data class AppInfo(val name: String, val packageName: String, val icon: Drawable)

@Composable
fun AppPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = context.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)

    var searchQuery by remember { mutableStateOf("") }
    val selectedApps = remember {
        mutableStateListOf<String>().apply {
            addAll(prefs.getStringSet("cast_enabled_apps", emptySet()) ?: emptySet())
        }
    }

    val allApps = remember {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { AppInfo(it.loadLabel(pm).toString(), it.packageName, it.loadIcon(pm)) }
            .sortedBy { it.name.lowercase() }
    }

    val displayApps = remember(searchQuery) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val topAppBarScrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Select Apps",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = "Back")
                    }
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
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search apps...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    displayApps.forEach { app ->
                        val isSelected = selectedApps.contains(app.packageName)
                        BasicComponent(
                            title = app.name,
                            summary = app.packageName,
                            startAction = {
                                Image(
                                    painter = BitmapPainter(app.icon.toBitmap().asImageBitmap()),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            endActions = {
                                Checkbox(
                                    state = if (isSelected) ToggleableState.On else ToggleableState.Off,
                                    onClick = {
                                        if (isSelected) {
                                            selectedApps.remove(app.packageName)
                                        } else {
                                            selectedApps.add(app.packageName)
                                        }
                                        prefs.edit().putStringSet("cast_enabled_apps", selectedApps.toSet()).apply()
                                    }
                                )
                            },
                            onClick = {
                                if (isSelected) {
                                    selectedApps.remove(app.packageName)
                                } else {
                                    selectedApps.add(app.packageName)
                                }
                                prefs.edit().putStringSet("cast_enabled_apps", selectedApps.toSet()).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
