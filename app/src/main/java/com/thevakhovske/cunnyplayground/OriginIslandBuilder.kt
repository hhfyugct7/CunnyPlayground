package com.thevakhovske.cunnyplayground

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_BASE_CONTENT
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_BASE_ICON
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_BASE_INFOS
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_BASE_SUB_CAPSULE_BG_COLOR
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_BASE_SUB_INFO
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_BASE_SUB_TEXT
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_BASE_SUB_TEXT_COLOR
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_BASE_TITLE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_CAPSULE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_CAPSULE_BG_COLOR
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_CAPSULE_CONTENT
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_CAPSULE_CONTENT_COLOR
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_CAPSULE_ICON
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_CAPSULE_STATE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_CHANGE_RECORD
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_CLICK_RESP
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFOS
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_CORE_INFO
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_DESCRIBE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_IMAGE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_IMAGE_CLICK_RESP
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_INDICATOR_ICON
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_INDICATOR_LOC
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_LEFT_MAIN
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_LEFT_SUB
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_MID_MAIN_ICON
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_MID_MAIN_TYPE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_MID_SUB_MSG
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_MID_SUB_TYPE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_NODE_ICON
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_PROGRESS
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_PROGRESS_COLOR
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_RIGHT_MAIN
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_INFO_RIGHT_SUB
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_INFOS
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_LEFT_CONTENT
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_LEFT_ICON
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_LEFT_INFO
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_LEFT_TEMPLATE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_BG_COLOR
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CAPSULE_CONTENT
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CONTENT
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_ICON
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_INFO
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_LOADING_COLOR
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_PROGRESS
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_BG_COLOR
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_COLOR
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_STATE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_TEMPLATE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_WAVE_COLOR
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_WAVE_STATE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_OPERATION
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_SCENE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_SHORT_INFOS
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_SHORT_INFO_CORE_INFO_SHORT
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_SHORT_INFO_DESCRIBE_SHORT
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_SHORT_INFO_IMAGE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_SHOW_NOTIFY
import com.thevakhovske.cunnyplayground.OriginIslandConstants.BUNDLE_KEY_TEMPLATE
import com.thevakhovske.cunnyplayground.OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT
import com.thevakhovske.cunnyplayground.OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_ICON_TEXT
import com.thevakhovske.cunnyplayground.OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_LOADING
import com.thevakhovske.cunnyplayground.OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS
import com.thevakhovske.cunnyplayground.OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON
import com.thevakhovske.cunnyplayground.OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_WAVE

/**
 * Builds vivo OriginOS SuperX (原子通知 / OriginIsland) notification extras.
 *
 * This is a faithful Kotlin port of superx_demo's `SuperXTemplateDemo` (specifically
 * `buildImportantInfoTemplate`) and `MainActivity#getPermissionGrant`. The output [Bundle] is
 * meant to be attached verbatim to a notification's extras so OriginOS renders the atomic
 * notification, capsule and OriginIsland.
 */
object OriginIslandBuilder {
    private const val TAG = "OriginIslandBuilder"

    /**
     * Registers the SuperX scene whitelist for this package via the hidden
     * `NotificationManager#setSuperXInfosSceneList` API (reflection). OriginOS requires this to
     * be called before it will render any SuperX/OriginIsland notification.
     *
     * Direct port of superx_demo's `MainActivity#getPermissionGrant`.
     */
    fun grantScenes(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        try {
            val method = NotificationManager::class.java.getMethod(
                "setSuperXInfosSceneList",
                List::class.java, List::class.java, List::class.java, List::class.java
            )
            val sceneList = ArrayList(OriginIslandConstants.SUPERX_SCENES)
            val switchList = ArrayList<String>()
            val pkgList = ArrayList<String>()
            val pkgSwitchList = ArrayList<String>()
            for (i in sceneList.indices) {
                switchList.add("true")
                pkgList.add(context.packageName)
                pkgSwitchList.add("true")
            }
            method.invoke(nm, sceneList, switchList, pkgList, pkgSwitchList)
            Log.d(TAG, "setSuperXInfosSceneList granted for ${context.packageName}")
        } catch (e: Exception) {
            // Method only exists on OriginOS; harmless elsewhere.
            Log.w(TAG, "setSuperXInfosSceneList unavailable: ${e.message}")
        }
    }

    fun parseColor(colorStr: String?, defaultColor: Int): Int {
        return try {
            Color.parseColor(colorStr)
        } catch (e: Exception) {
            defaultColor
        }
    }

    /**
     * Builds an "end" bundle (operation = 2 结束原子通知) that tells OriginOS to dismiss a previously
     * posted SuperX atomic notification / OriginIsland. Post this with the SAME notification id the
     * island was created with, then cancel the host notification. Without this, cancelling the host
     * leaves the island/capsule lingering.
     */
    fun buildEndBundle(scene: String): Bundle {
        val bundle = Bundle()
        bundle.putInt(BUNDLE_KEY_OPERATION, 2) // 2 = 结束原子通知 (end)
        bundle.putBoolean(BUNDLE_KEY_SHOW_NOTIFY, false)
        bundle.putString(BUNDLE_KEY_SCENE, scene)
        // High change record so the end is never dropped as an out-of-order update.
        bundle.putInt(BUNDLE_KEY_CHANGE_RECORD, Int.MAX_VALUE)
        return bundle
    }

    /** A notification action mapped onto OriginIsland clickable surfaces (capsule/island/card/images). */
    data class OriginAction(val title: String, val icon: Icon?, val pendingIntent: PendingIntent?)

    /** A fully transparent 2x2 icon, used to hide the required nodeIcons on a plain progress bar. */
    private fun transparentIcon(): Icon {
        val bmp = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
        return Icon.createWithBitmap(bmp)
    }

    /** A color whose alpha is 0 (e.g. 0x0) means "use the system default" per 技术规范 §5.5. */
    private fun isDefaultColor(color: Int): Boolean = (color ushr 24) == 0

    private fun colorSpan(text: String, color: Int): CharSequence {
        // No forced color when the caller didn't specify one — let OriginOS theme the text
        // (white on the dark island, etc.) instead of inheriting our bg/progress color.
        if (text.isEmpty() || isDefaultColor(color)) return text
        val s = SpannableString(text)
        s.setSpan(ForegroundColorSpan(color), 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }

    /**
     * Reflectively calls the hidden `NotificationManager#getSceneStatus(pkg, scene)` (技术规范 §5.6).
     * Returns true/false if the per-scene switch state is known, or null if the API is unavailable.
     */
    fun getSceneStatus(context: Context, scene: String): Boolean? {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
            val m = Class.forName("android.app.NotificationManager")
                .getDeclaredMethod("getSceneStatus", String::class.java, String::class.java)
            m.isAccessible = true
            m.invoke(nm, context.packageName, scene) as? Boolean
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reflectively calls the hidden `NotificationManager#isSupportCustomFun(pkg, scene)` (技术规范 §5.9)
     * to check whether the current OS version supports the atomic-notification module for a scene.
     */
    fun isSupportCustomFun(context: Context, scene: String): Boolean? {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
            val m = Class.forName("android.app.NotificationManager")
                .getDeclaredMethod("isSupportCustomFun", String::class.java, String::class.java)
            m.isAccessible = true
            m.invoke(nm, context.packageName, scene) as? Boolean
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Builds the full SuperX extras bundle covering the entire 原子通知技术规范 data protocol:
     * core params, base infos, the chosen template (强调/进度/左右对称/基础/导航), short infos,
     * the OriginIsland (left + right), and the status-bar capsule (always sent alongside the island
     * for Flip compatibility, per §5.1).
     *
     * Mirrors `SuperXTemplateDemo.buildImportantInfoTemplate` and extends it with every documented
     * field. Icons are passed in so both the manual playground and the re-caster supply their own art.
     *
     * @param operation   0 create / 1 update / 2 end (lifecycle, §3)
     * @param actions     notification action buttons → wired to card/island/capsule/right-island
     *                    clickResp and (base template) the clickable sub-image list
     * @param largeIcon   优先 used for base/info/nav/short artwork when present (e.g. source big icon)
     */
    fun buildBundle(
        context: Context,
        template: Int,
        title: String,
        content: String,
        leftContent: String,
        rightContent: String,
        extra1: String,
        extra2: String,
        extra3: String,
        extra4: String,
        rightTemplate: Int,
        defaultIcon: Icon,
        leftIcon: Icon,
        rightIcon: Icon,
        accentIcon: Icon,
        progress: Int,
        bgColor: Int,
        fgColor: Int,
        scene: String,
        clickResp: PendingIntent?,
        operation: Int = 0,
        largeIcon: Icon? = null,
        subText: String? = null,
        navMsg: String? = null,
        actions: List<OriginAction> = emptyList(),
        keepDuration: Int = 0,
        displays: Int = 0,
        sound: Boolean = false,
        dismissWhenKill: Boolean = true,
        newNode: Int = 0,
        islandShowTime: Int = 0,
        capsuleShowTime: Int = 0,
        forceShow: Boolean = false,
        progressState: Int = 1,
        progressMarkers: Boolean = false,
        showRightIcon: Boolean = true,
        cardBgColor: Int = 0,
        keepScreenOn: Boolean = false,
        disableInvertColor: Boolean = false,
        lightColor: Int = 0,
        lightMode: Int = 0,
        generatingStatus: Int = 0,
        iconStatusType: Int = -1,
        leftDoubleLine: List<String> = emptyList(),
        rightDoubleLine: List<String> = emptyList(),
        buttonTitles: List<String> = emptyList(),
        customTemplate: android.widget.RemoteViews? = null,
        waveState: Int = 1,
        waveColorList: List<String>? = null
    ): Bundle {
        val bundle = Bundle()
        bundle.putInt(BUNDLE_KEY_OPERATION, operation)
        bundle.putBoolean(BUNDLE_KEY_SHOW_NOTIFY, true)
        bundle.putInt(BUNDLE_KEY_TEMPLATE, template)
        bundle.putString(BUNDLE_KEY_SCENE, scene)
        bundle.putInt(BUNDLE_KEY_CHANGE_RECORD, 0)
        bundle.putBoolean(OriginIslandConstants.BUNDLE_KEY_SOUND, sound)
        bundle.putBoolean(OriginIslandConstants.BUNDLE_KEY_DISMISS_WHEN_KILL, dismissWhenKill)
        if (keepDuration > 0) bundle.putInt(OriginIslandConstants.BUNDLE_KEY_KEEP_DURATION, keepDuration)
        if (displays != 0) bundle.putInt(OriginIslandConstants.BUNDLE_KEY_DISPLAYS, displays)
        if (newNode > 0) bundle.putInt(OriginIslandConstants.BUNDLE_KEY_NEW_NODE, newNode)
        if (!isDefaultColor(cardBgColor)) bundle.putInt(OriginIslandConstants.BUNDLE_KEY_CARD_BG_COLOR, cardBgColor)
        if (keepScreenOn) bundle.putBoolean(OriginIslandConstants.BUNDLE_KEY_KEEP_SCREEN_ON, true)
        if (disableInvertColor) bundle.putBoolean(OriginIslandConstants.BUNDLE_KEY_DISABLE_INVERT_COLOR, true)
        if (!isDefaultColor(lightColor)) {
            // AOD edge light effect [RE] — isLight on root + a lightEffectInfo sub-bundle.
            bundle.putBoolean(OriginIslandConstants.BUNDLE_KEY_EFFECT_IS_LIGHT, true)
            bundle.putBundle(OriginIslandConstants.BUNDLE_KEY_EFFECT_LIGHT_INFO, Bundle().apply {
                putInt(OriginIslandConstants.BUNDLE_KEY_EFFECT_LIGHT_MODE, lightMode)
                putInt(OriginIslandConstants.BUNDLE_KEY_EFFECT_LIGHT_MAIN_COLOR, lightColor)
            })
        }

        // Tapping the island / card / capsule opens the cast SOURCE app: clickResp = the source
        // notification's content intent (supplied by the caller). The notification's own action is
        // surfaced separately as a single tappable "button" chip (subInfo) — it must NOT hijack the
        // whole-island tap.
        clickResp?.let { bundle.putParcelable(BUNDLE_KEY_CLICK_RESP, it) }
        val primaryAction = actions.firstOrNull { it.pendingIntent != null }
        val actionClick = primaryAction?.pendingIntent
        val actionTitle = primaryAction?.title?.takeIf { it.isNotBlank() }
        val baseIcon = largeIcon ?: defaultIcon

        // ── Base Infos ──
        val baseBundle = Bundle()
        baseBundle.putParcelable(BUNDLE_KEY_BASE_ICON, baseIcon)
        baseBundle.putCharSequence(BUNDLE_KEY_BASE_TITLE, title)
        baseBundle.putCharSequence(BUNDLE_KEY_BASE_CONTENT, content)
        when {
            // The buttons template renders its own real button row from infos.btn* — no chip needed.
            template == OriginIslandConstants.TEMPLATE_BUTTONS -> {
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, OriginIslandConstants.BASE_SUB_INFO_NONE)
            }
            // Otherwise the primary action → a tappable chip (the single in-card button on non-button
            // templates). It sits in baseInfos (separate from the progress bar), so it shows on the
            // progress card too. Forced light so it stays readable (system default is dark-on-dark).
            actionClick != null && actionTitle != null -> {
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, OriginIslandConstants.BASE_SUB_INFO_CAPSULE)
                baseBundle.putString(BUNDLE_KEY_BASE_SUB_TEXT, actionTitle)
                baseBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_BASE_SUB_INFO_CLICK_RESP, actionClick)
                baseBundle.putInt(
                    BUNDLE_KEY_BASE_SUB_TEXT_COLOR,
                    if (!isDefaultColor(fgColor)) fgColor else Color.parseColor("#FF1C1C1E")
                )
                baseBundle.putInt(
                    BUNDLE_KEY_BASE_SUB_CAPSULE_BG_COLOR,
                    if (!isDefaultColor(bgColor)) bgColor else Color.parseColor("#FFF2F2F2")
                )
            }
            // Progress-visual card with no action: keep the aux area empty — no text beside the bar.
            template == OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL -> {
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, OriginIslandConstants.BASE_SUB_INFO_NONE)
            }
            // Base template requires a subInfo; otherwise show the source subtext as plain text.
            template == OriginIslandConstants.TEMPLATE_BASE -> {
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, OriginIslandConstants.BASE_SUB_INFO_TEXT)
                baseBundle.putString(BUNDLE_KEY_BASE_SUB_TEXT, subText?.takeIf { it.isNotBlank() } ?: extra1)
            }
            !subText.isNullOrBlank() -> {
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, OriginIslandConstants.BASE_SUB_INFO_TEXT)
                baseBundle.putString(BUNDLE_KEY_BASE_SUB_TEXT, subText)
            }
            else -> {
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, OriginIslandConstants.BASE_SUB_INFO_NONE)
            }
        }
        if (generatingStatus > 0) baseBundle.putInt(OriginIslandConstants.BUNDLE_KEY_BASE_GENERATING_STATUS, generatingStatus)
        if (iconStatusType >= 0) baseBundle.putInt(OriginIslandConstants.BUNDLE_KEY_BASE_ICON_STATUS_TYPE, iconStatusType)
        bundle.putBundle(BUNDLE_KEY_BASE_INFOS, baseBundle)

        // ── Specific Template Infos ──
        val infoBundle = Bundle()
        when (template) {
            OriginIslandConstants.TEMPLATE_NOTIF_CUSTOM -> {
                customTemplate?.let {
                    bundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_CUSTOM_TEMPLATE, it)
                }
            }
            OriginIslandConstants.TEMPLATE_PRIORITY_INFO -> {
                infoBundle.putString(BUNDLE_KEY_INFO_DESCRIBE, extra1)
                infoBundle.putString(BUNDLE_KEY_INFO_CORE_INFO, extra2)
                infoBundle.putParcelable(BUNDLE_KEY_INFO_IMAGE, largeIcon ?: defaultIcon)
                clickResp?.let { infoBundle.putParcelable(BUNDLE_KEY_INFO_IMAGE_CLICK_RESP, it) }
            }
            OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL -> {
                // nodeIcon is required (2~5). For a clean linear bar (no markers) we pass two fully
                // transparent nodes and omit indicatorIcon entirely (it only shows when set).
                val iconList = ArrayList<Icon>()
                if (progressMarkers) {
                    iconList.add(accentIcon)
                    iconList.add(defaultIcon)
                    iconList.add(accentIcon)
                    infoBundle.putParcelable(BUNDLE_KEY_INFO_INDICATOR_ICON, accentIcon)
                    infoBundle.putInt(BUNDLE_KEY_INFO_INDICATOR_LOC, 2)
                } else {
                    val t = transparentIcon()
                    iconList.add(t)
                    iconList.add(t)
                }
                infoBundle.putParcelableArrayList(BUNDLE_KEY_INFO_NODE_ICON, iconList)
                infoBundle.putInt(BUNDLE_KEY_INFO_PROGRESS, progress.coerceIn(0, 100))
                infoBundle.putInt(
                    BUNDLE_KEY_INFO_PROGRESS_COLOR,
                    context.resources.getColor(R.color.vivo_super_x_progress_bar_default_color, null)
                )
                infoBundle.putInt(
                    OriginIslandConstants.BUNDLE_KEY_INFO_BG_COLOR,
                    context.resources.getColor(R.color.vivo_super_x_progress_bar_bkg_default_color, null)
                )
            }
            OriginIslandConstants.TEMPLATE_TEXT_SYMMETRY -> {
                infoBundle.putString(BUNDLE_KEY_INFO_LEFT_MAIN, extra1)
                infoBundle.putString(BUNDLE_KEY_INFO_LEFT_SUB, extra2)
                infoBundle.putInt(BUNDLE_KEY_INFO_MID_MAIN_TYPE, 1)
                infoBundle.putParcelable(BUNDLE_KEY_INFO_MID_MAIN_ICON, largeIcon ?: defaultIcon)
                infoBundle.putInt(BUNDLE_KEY_INFO_MID_SUB_TYPE, 2)
                infoBundle.putString(BUNDLE_KEY_INFO_MID_SUB_MSG, content)
                infoBundle.putString(BUNDLE_KEY_INFO_RIGHT_MAIN, extra3)
                infoBundle.putString(BUNDLE_KEY_INFO_RIGHT_SUB, extra4)
            }
            OriginIslandConstants.TEMPLATE_NAVIGATION -> {
                infoBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_INFO_NAV_ICON, largeIcon ?: defaultIcon)
                // Default the nav text to "{Title} • {Text}".
                val defaultNav = listOf(title, content).filter { it.isNotBlank() }.joinToString(" • ")
                infoBundle.putString(OriginIslandConstants.BUNDLE_KEY_INFO_NAV_MSG, navMsg ?: defaultNav)
            }
            OriginIslandConstants.TEMPLATE_BUTTONS -> {
                // Real multi-button row (undocumented, recovered from ButtonsSuperXTemplate).
                // btnType 1 = up to 3 filled buttons. All five lists must be the same length and
                // btnIconList must be non-empty (null entries render as text-only buttons).
                // Source: the notification's own actions, else explicit playground button titles
                // (each wired to clickResp so they're tappable).
                val btnSource: List<Triple<String, Icon?, PendingIntent>> = when {
                    actions.any { it.pendingIntent != null } ->
                        actions.filter { it.pendingIntent != null }.map { Triple(it.title, it.icon, it.pendingIntent!!) }
                    buttonTitles.isNotEmpty() && clickResp != null ->
                        buttonTitles.map { Triple(it, null as Icon?, clickResp) }
                    else -> emptyList()
                }.take(3)
                if (btnSource.isNotEmpty()) {
                    val textColor = if (!isDefaultColor(fgColor)) fgColor else Color.parseColor("#FF1A1A1A")
                    val fillColor = if (!isDefaultColor(bgColor)) bgColor else Color.parseColor("#FFEFEFEF")
                    infoBundle.putInt(OriginIslandConstants.BUNDLE_KEY_INFO_BTN_TYPE, OriginIslandConstants.BTN_TYPE_FILLED)
                    infoBundle.putStringArrayList(OriginIslandConstants.BUNDLE_KEY_INFO_BTN_TEXT_LIST, ArrayList(btnSource.map { it.first }))
                    infoBundle.putParcelableArrayList(OriginIslandConstants.BUNDLE_KEY_INFO_BTN_ICON_LIST, ArrayList(btnSource.map { it.second }))
                    infoBundle.putIntegerArrayList(OriginIslandConstants.BUNDLE_KEY_INFO_BTN_TEXT_COLOR_LIST, ArrayList(btnSource.map { textColor }))
                    infoBundle.putIntegerArrayList(OriginIslandConstants.BUNDLE_KEY_INFO_BTN_COLOR_LIST, ArrayList(btnSource.map { fillColor }))
                    infoBundle.putParcelableArrayList(OriginIslandConstants.BUNDLE_KEY_INFO_BTN_CLICK_RESP_LIST, ArrayList(btnSource.map { it.third }))
                }
            }
            OriginIslandConstants.TEMPLATE_DRIVING_NAVI -> {
                // Driving-navi card [RE] — turn icon + main text (highlight/normal) + assist text.
                infoBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_INFO_DIR_ICON, largeIcon ?: defaultIcon)
                infoBundle.putString(OriginIslandConstants.BUNDLE_KEY_INFO_MAIN_HIGHLIGHT_TEXT, title)
                infoBundle.putString(OriginIslandConstants.BUNDLE_KEY_INFO_MAIN_NORMAL_TEXT, content)
                infoBundle.putCharSequence(OriginIslandConstants.BUNDLE_KEY_INFO_DIR_SUB_TEXT, navMsg ?: content)
            }
            OriginIslandConstants.TEMPLATE_BASE -> {
                // No extra info bundle for basic
            }
        }
        bundle.putBundle(BUNDLE_KEY_INFOS, infoBundle)

        // ── Short Infos (small card & OriginB lockscreen) ──
        val shortInfoBundle = Bundle()
        shortInfoBundle.putParcelable(BUNDLE_KEY_SHORT_INFO_IMAGE, largeIcon ?: defaultIcon)
        shortInfoBundle.putString(BUNDLE_KEY_SHORT_INFO_CORE_INFO_SHORT, content)
        shortInfoBundle.putString(BUNDLE_KEY_SHORT_INFO_DESCRIBE_SHORT, title)
        clickResp?.let {
            shortInfoBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_SHORT_INFO_IMAGE_CLICK_RESP, it)
        }
        bundle.putBundle(BUNDLE_KEY_SHORT_INFOS, shortInfoBundle)

        // ── Island Infos ──
        val islandBundle = Bundle()
        islandBundle.putInt(BUNDLE_KEY_ISLAND_LEFT_TEMPLATE, 1)
        islandBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_TEMPLATE, rightTemplate)
        if (islandShowTime > 0) islandBundle.putInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_SHOW_TIME, islandShowTime)
        if (forceShow) islandBundle.putBoolean(OriginIslandConstants.BUNDLE_KEY_ISLAND_FORCE_SHOW, true)
        // Tapping the island pill EXPANDS the big card (出卡, islandClick=0) — the expected behaviour.
        // islandClick=1 would instead fire a landing PendingIntent, which collapses the shade.
        islandBundle.putInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_CLICK, OriginIslandConstants.ISLAND_CLICK_SHOW_CARD)
        // Tapping the card's blank area → the cast source app.
        clickResp?.let { islandBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_ISLAND_CLICK_RESP, it) }

        val leftBundle = Bundle()
        leftBundle.putParcelable(BUNDLE_KEY_ISLAND_LEFT_ICON, leftIcon)
        leftBundle.putString(BUNDLE_KEY_ISLAND_LEFT_CONTENT, leftContent)
        if (leftDoubleLine.isNotEmpty()) {
            leftBundle.putCharSequenceArrayList(OriginIslandConstants.BUNDLE_KEY_ISLAND_LEFT_DOUBLELINE, ArrayList<CharSequence>(leftDoubleLine))
        }
        islandBundle.putBundle(BUNDLE_KEY_ISLAND_LEFT_INFO, leftBundle)

        val rightBundle = Bundle()
        when (rightTemplate) {
            TEMPLATE_RIGHT_ISLAND_WAVE -> {
                if (waveColorList != null && waveColorList.isNotEmpty()) {
                    rightBundle.putStringArrayList(BUNDLE_KEY_ISLAND_RIGHT_WAVE_COLOR, ArrayList(waveColorList))
                } else if (!isDefaultColor(fgColor)) {
                    val colors = ArrayList<String>()
                    colors.add(String.format("#%06X", 0xFFFFFF and fgColor))
                    rightBundle.putStringArrayList(BUNDLE_KEY_ISLAND_RIGHT_WAVE_COLOR, colors)
                }
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_WAVE_STATE, waveState)
            }
            TEMPLATE_RIGHT_ISLAND_PROGRESS -> {
                // 2: Progress ring. Always give it a visible color (a default-themed ring can come out
                // invisible), and let progressState drive whether the ring or a success/fail icon shows.
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS, progress.coerceIn(0, 100))
                val ringColor = if (!isDefaultColor(fgColor)) fgColor
                    else context.resources.getColor(R.color.vivo_super_x_progress_bar_default_color, null)
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_COLOR, ringColor)
                if (!isDefaultColor(bgColor)) rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_BG_COLOR, bgColor)
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_STATE, progressState)
            }
            TEMPLATE_RIGHT_ISLAND_LOADING -> {
                // 3: Loading dots
                if (!isDefaultColor(fgColor)) rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_LOADING_COLOR, fgColor)
            }
            TEMPLATE_RIGHT_ISLAND_TEXT_ICON, TEMPLATE_RIGHT_ISLAND_ICON_TEXT -> {
                // 4 or 5: Text+Icon / Icon+Text. "至少选一个" — omit the icon for plain right-side text.
                if (showRightIcon) rightBundle.putParcelable(BUNDLE_KEY_ISLAND_RIGHT_ICON, rightIcon)
                rightBundle.putCharSequence(BUNDLE_KEY_ISLAND_RIGHT_CONTENT, colorSpan(rightContent, fgColor))
                clickResp?.let { rightBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CLICK_RESP, it) }
            }
            TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT -> {
                // 6: Capsule text (text-only — no redundant right-side icon)
                rightBundle.putCharSequence(BUNDLE_KEY_ISLAND_RIGHT_CAPSULE_CONTENT, colorSpan(rightContent, fgColor))
                if (!isDefaultColor(bgColor)) rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_BG_COLOR, bgColor)
                clickResp?.let { rightBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CLICK_RESP, it) }
            }
        }
        if (rightDoubleLine.isNotEmpty()) {
            rightBundle.putCharSequenceArrayList(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_DOUBLELINE, ArrayList<CharSequence>(rightDoubleLine))
        }
        islandBundle.putBundle(BUNDLE_KEY_ISLAND_RIGHT_INFO, rightBundle)
        bundle.putBundle(BUNDLE_KEY_ISLAND_INFOS, islandBundle)

        // ── Capsule (always sent alongside the island for Flip outer-screen compatibility, §5.1) ──
        val capsuleBundle = Bundle()
        capsuleBundle.putInt(BUNDLE_KEY_CAPSULE_STATE, 1)
        capsuleBundle.putParcelable(BUNDLE_KEY_CAPSULE_ICON, rightIcon)
        capsuleBundle.putString(BUNDLE_KEY_CAPSULE_CONTENT, rightContent)
        if (!isDefaultColor(fgColor)) capsuleBundle.putInt(BUNDLE_KEY_CAPSULE_CONTENT_COLOR, fgColor)
        if (!isDefaultColor(bgColor)) capsuleBundle.putInt(BUNDLE_KEY_CAPSULE_BG_COLOR, bgColor)
        if (capsuleShowTime > 0) capsuleBundle.putInt(OriginIslandConstants.BUNDLE_KEY_CAPSULE_SHOW_TIME, capsuleShowTime)
        if (newNode > 0) capsuleBundle.putInt(OriginIslandConstants.BUNDLE_KEY_CAPSULE_NEW_NODE, newNode)
        clickResp?.let { capsuleBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_CAPSULE_CLICK_RESP, it) }
        bundle.putBundle(BUNDLE_KEY_CAPSULE, capsuleBundle)

        return bundle
    }
}
