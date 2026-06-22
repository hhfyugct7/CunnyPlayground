package com.thevakhovske.cunnyplayground

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.thevakhovske.cunnyplayground.OriginIslandConstants as C

/** A ready-made OriginIsland configuration that posts itself on tap. */
data class OriginSample(val name: String, val summary: String, val post: (Context) -> Unit)

class OriginSamplesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            MiuixTheme(controller = controller) {
                OriginSamplesScreen(onBack = { finish() })
            }
        }
    }
}

private fun send(
    c: Context, title: String, content: String, left: String, right: String,
    template: Int, rightTemplate: Int, scene: String,
    extra1: String = "", extra2: String = "", extra3: String = "", extra4: String = "",
    progress: Int = 50, icon: Int = R.drawable.ic_alert, adv: OriginAdvanced = OriginAdvanced()
) = postOriginIslandNotification(
    c, title, content, left, right, extra1, extra2, extra3, extra4,
    template, rightTemplate, progress, scene, "", "", icon, adv
)

private fun samples(): List<OriginSample> = listOf(
    OriginSample("App download", "Progress card + island ring · 75%") { c ->
        send(c, "Downloading", "Genshin Impact · 75%", "Genshin", "75%",
            C.TEMPLATE_PROGRESS_VISUAL, C.TEMPLATE_RIGHT_ISLAND_PROGRESS, "NAVIGATION",
            progress = 75, icon = R.drawable.ic_timer)
    },
    OriginSample("Payment success", "Progress ring → green check + status icon") { c ->
        send(c, "Payment complete", "\$12.50 charged to Visa", "Wallet", "Paid",
            C.TEMPLATE_PROGRESS_VISUAL, C.TEMPLATE_RIGHT_ISLAND_PROGRESS, "NAVIGATION",
            progress = 100, icon = R.drawable.ic_alert,
            adv = OriginAdvanced(iconStatusType = C.ICON_STATUS_SUCCESS))
    },
    OriginSample("Food delivery", "TAKEOUT · base card + capsule") { c ->
        send(c, "Order on the way", "Arriving in about 8 min", "Sushi Place", "8 min",
            C.TEMPLATE_BASE, C.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, "NAVIGATION", icon = R.drawable.ic_alert)
    },
    OriginSample("Ride hailing", "TAXI · driver arriving") { c ->
        send(c, "Driver arriving", "Black Tesla · plate 7Z·123", "Uber", "2 min",
            C.TEMPLATE_BASE, C.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, "NAVIGATION", icon = R.drawable.ic_call)
    },
    OriginSample("Flight boarding", "FLIGHT · left/right symmetry") { c ->
        send(c, "Boarding", "Gate A12 · Seat 14C", "", "",
            C.TEMPLATE_TEXT_SYMMETRY, C.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, "NAVIGATION",
            extra1 = "PEK", extra2 = "09:30", extra3 = "SVO", extra4 = "14:20")
    },
    OriginSample("Train", "TRAIN · symmetry + capsule") { c ->
        send(c, "On time", "Car 7 · Seat 12F", "", "",
            C.TEMPLATE_TEXT_SYMMETRY, C.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, "NAVIGATION",
            extra1 = "Beijing", extra2 = "G123", extra3 = "Shanghai", extra4 = "18:40")
    },
    OriginSample("Navigation", "Nav template · 2-line message") { c ->
        send(c, "Turn left", "onto Main St", "", "",
            C.TEMPLATE_NAVIGATION, C.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, "NAVIGATION",
            adv = OriginAdvanced(navMsg = "Turn left#onto Main St · 200 m"))
    },
    OriginSample("Driving navi", "Driving navigation template (9)") { c ->
        send(c, "200 m", "Turn right onto Ring Rd", "", "",
            C.TEMPLATE_DRIVING_NAVI, C.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, "NAVIGATION")
    },
    OriginSample("Music player", "Wave island + media buttons") { c ->
        send(c, "Lofi beats to relax", "Chillhop Radio", "Music", "",
            C.TEMPLATE_BUTTONS, C.TEMPLATE_RIGHT_ISLAND_WAVE, "NAVIGATION",
            icon = R.drawable.ic_alert, adv = OriginAdvanced(buttonTitles = "Prev,Pause,Next"))
    },
    OriginSample("Incoming call", "VOIPCALL · Decline / Answer buttons") { c ->
        send(c, "Incoming call", "+1 555 0199", "Phone", "",
            C.TEMPLATE_BUTTONS, C.TEMPLATE_RIGHT_ISLAND_LOADING, "NAVIGATION",
            icon = R.drawable.ic_call, adv = OriginAdvanced(buttonTitles = "Decline,Answer"))
    },
    OriginSample("Two buttons", "Buttons template · Deny / Receive (vivoshare)") { c ->
        send(c, "vivo X200 FE", "About to send 1 image (2.6 MB)", "vivoshare", "",
            C.TEMPLATE_BUTTONS, C.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, "NAVIGATION",
            adv = OriginAdvanced(buttonTitles = "Deny,Receive"))
    },
    OriginSample("Countdown timer", "TIMER · progress 40%") { c ->
        send(c, "Timer", "04:32 remaining", "Timer", "04:32",
            C.TEMPLATE_PROGRESS_VISUAL, C.TEMPLATE_RIGHT_ISLAND_PROGRESS, "NAVIGATION",
            progress = 40, icon = R.drawable.ic_timer)
    },
    OriginSample("Loading", "Loading-dots island") { c ->
        send(c, "Syncing", "Uploading 12 photos…", "Cloud", "",
            C.TEMPLATE_BASE, C.TEMPLATE_RIGHT_ISLAND_LOADING, "NAVIGATION")
    },
    OriginSample("Double-line island", "Text-only 2-line island sides") { c ->
        send(c, "Trip in progress", "Express to downtown", "", "",
            C.TEMPLATE_BASE, C.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT, "NAVIGATION",
            adv = OriginAdvanced(leftDoubleLine = "Departed,09:30", rightDoubleLine = "Arrives,14:20"))
    },
    OriginSample("SOS / light effect", "CRITICAL · red AOD edge light") { c ->
        send(c, "Emergency SOS", "Sending your location…", "SOS", "",
            C.TEMPLATE_PRIORITY_INFO, C.TEMPLATE_RIGHT_ISLAND_LOADING, "NAVIGATION",
            extra1 = "Emergency", extra2 = "SOS activated", icon = R.drawable.ic_alert,
            adv = OriginAdvanced(lightColor = "#FF3B30", keepScreenOn = true))
    }
)

@androidx.compose.runtime.Composable
fun OriginSamplesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val list = remember { samples() }
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.title_origin_samples),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize().scrollEndHaptic()
        ) {
            item { SmallTitle(stringResource(R.string.origin_samples_hint)) }
            items(list) { s ->
                Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = s.name, fontSize = 16.sp)
                            Text(text = s.summary, fontSize = 13.sp)
                        }
                        IconButton(onClick = {
                            s.post(context)
                            Toast.makeText(context, context.getString(R.string.msg_sample_sent, s.name), Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(MiuixIcons.Send, contentDescription = stringResource(R.string.btn_post))
                        }
                    }
                }
            }
        }
    }
}
