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

    private fun colorSpan(text: String, color: Int): CharSequence {
        val s = SpannableString(text)
        if (text.isNotEmpty()) {
            s.setSpan(ForegroundColorSpan(color), 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
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
        progressState: Int = 1
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

        // Card/capsule tap target: the first action's intent, else the card-level clickResp (opens app).
        val primaryClick = actions.firstOrNull { it.pendingIntent != null }?.pendingIntent ?: clickResp
        if (primaryClick != null) bundle.putParcelable(BUNDLE_KEY_CLICK_RESP, primaryClick)
        // A *real* action intent only (null when the notification has no action). Used for island
        // sub-surfaces so a plain recast doesn't turn an island tap into an app launch.
        val actionClick = actions.firstOrNull { it.pendingIntent != null }?.pendingIntent
        val clickableActions = actions.filter { it.pendingIntent != null }
        val baseIcon = largeIcon ?: defaultIcon

        // ── Base Infos ──
        val baseBundle = Bundle()
        baseBundle.putParcelable(BUNDLE_KEY_BASE_ICON, baseIcon)
        baseBundle.putCharSequence(BUNDLE_KEY_BASE_TITLE, title)
        baseBundle.putCharSequence(BUNDLE_KEY_BASE_CONTENT, content)
        when {
            // Base template + actions → up to 3 tappable action images (subInfo 4)
            template == OriginIslandConstants.TEMPLATE_BASE && clickableActions.isNotEmpty() -> {
                val imgs = ArrayList<Icon>()
                val pis = ArrayList<PendingIntent>()
                clickableActions.take(3).forEach { a ->
                    imgs.add(a.icon ?: defaultIcon)
                    pis.add(a.pendingIntent!!)
                }
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, OriginIslandConstants.BASE_SUB_INFO_IMAGE_LIST)
                baseBundle.putParcelableArrayList(OriginIslandConstants.BUNDLE_KEY_BASE_SUB_IMAGE_LIST, imgs)
                baseBundle.putParcelableArrayList(OriginIslandConstants.BUNDLE_KEY_BASE_SUB_INFO_CLICK_RESP_LIST, pis)
            }
            template == OriginIslandConstants.TEMPLATE_BASE -> {
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, OriginIslandConstants.BASE_SUB_INFO_TEXT)
                baseBundle.putString(BUNDLE_KEY_BASE_SUB_TEXT, subText?.takeIf { it.isNotBlank() } ?: extra1)
            }
            // Map the source notification's subtext into the aux text area when present
            !subText.isNullOrBlank() -> {
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, OriginIslandConstants.BASE_SUB_INFO_TEXT)
                baseBundle.putString(BUNDLE_KEY_BASE_SUB_TEXT, subText)
            }
            else -> {
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, OriginIslandConstants.BASE_SUB_INFO_CAPSULE)
                baseBundle.putString(BUNDLE_KEY_BASE_SUB_TEXT, rightContent.ifBlank { "Live" })
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_TEXT_COLOR, Color.parseColor("#FFFFFF"))
                baseBundle.putInt(BUNDLE_KEY_BASE_SUB_CAPSULE_BG_COLOR, Color.parseColor("#FF0F24"))
            }
        }
        bundle.putBundle(BUNDLE_KEY_BASE_INFOS, baseBundle)

        // ── Specific Template Infos ──
        val infoBundle = Bundle()
        when (template) {
            OriginIslandConstants.TEMPLATE_PRIORITY_INFO -> {
                infoBundle.putString(BUNDLE_KEY_INFO_DESCRIBE, extra1)
                infoBundle.putString(BUNDLE_KEY_INFO_CORE_INFO, extra2)
                infoBundle.putParcelable(BUNDLE_KEY_INFO_IMAGE, largeIcon ?: defaultIcon)
                primaryClick?.let { infoBundle.putParcelable(BUNDLE_KEY_INFO_IMAGE_CLICK_RESP, it) }
            }
            OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL -> {
                val iconList = ArrayList<Icon>()
                iconList.add(defaultIcon)
                iconList.add(defaultIcon)
                iconList.add(defaultIcon)
                infoBundle.putParcelableArrayList(BUNDLE_KEY_INFO_NODE_ICON, iconList)
                infoBundle.putInt(BUNDLE_KEY_INFO_PROGRESS, progress.coerceIn(0, 100))
                infoBundle.putParcelable(BUNDLE_KEY_INFO_INDICATOR_ICON, accentIcon)
                infoBundle.putInt(BUNDLE_KEY_INFO_INDICATOR_LOC, 2)
                infoBundle.putInt(
                    BUNDLE_KEY_INFO_PROGRESS_COLOR,
                    context.resources.getColor(R.color.vivo_super_x_progress_bar_default_color, null)
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
                infoBundle.putString(OriginIslandConstants.BUNDLE_KEY_INFO_NAV_MSG, navMsg ?: content)
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
        primaryClick?.let {
            shortInfoBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_SHORT_INFO_IMAGE_CLICK_RESP, it)
        }
        bundle.putBundle(BUNDLE_KEY_SHORT_INFOS, shortInfoBundle)

        // ── Island Infos ──
        val islandBundle = Bundle()
        islandBundle.putInt(BUNDLE_KEY_ISLAND_LEFT_TEMPLATE, 1)
        islandBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_TEMPLATE, rightTemplate)
        if (islandShowTime > 0) islandBundle.putInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_SHOW_TIME, islandShowTime)
        if (forceShow) islandBundle.putBoolean(OriginIslandConstants.BUNDLE_KEY_ISLAND_FORCE_SHOW, true)
        // Tapping the island EXPANDS the big card (出卡, islandClick=0) — the expected behaviour.
        // islandClick=1 would instead fire a landing PendingIntent, which collapses the shade.
        islandBundle.putInt(OriginIslandConstants.BUNDLE_KEY_ISLAND_CLICK, OriginIslandConstants.ISLAND_CLICK_SHOW_CARD)

        val leftBundle = Bundle()
        leftBundle.putParcelable(BUNDLE_KEY_ISLAND_LEFT_ICON, leftIcon)
        leftBundle.putString(BUNDLE_KEY_ISLAND_LEFT_CONTENT, leftContent)
        islandBundle.putBundle(BUNDLE_KEY_ISLAND_LEFT_INFO, leftBundle)

        val rightBundle = Bundle()
        when (rightTemplate) {
            TEMPLATE_RIGHT_ISLAND_WAVE -> {
                // 1: Wave (no resources; first color main, second accent)
                val colors = ArrayList<String>()
                colors.add(String.format("#%06X", 0xFFFFFF and fgColor))
                rightBundle.putStringArrayList(BUNDLE_KEY_ISLAND_RIGHT_WAVE_COLOR, colors)
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_WAVE_STATE, 1)
            }
            TEMPLATE_RIGHT_ISLAND_PROGRESS -> {
                // 2: Progress ring
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS, progress.coerceIn(0, 100))
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_COLOR, fgColor)
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_BG_COLOR, bgColor)
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_STATE, progressState)
            }
            TEMPLATE_RIGHT_ISLAND_LOADING -> {
                // 3: Loading dots
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_LOADING_COLOR, fgColor)
            }
            TEMPLATE_RIGHT_ISLAND_TEXT_ICON, TEMPLATE_RIGHT_ISLAND_ICON_TEXT -> {
                // 4 or 5: Text+Icon / Icon+Text
                rightBundle.putParcelable(BUNDLE_KEY_ISLAND_RIGHT_ICON, rightIcon)
                rightBundle.putCharSequence(BUNDLE_KEY_ISLAND_RIGHT_CONTENT, colorSpan(rightContent, fgColor))
                actionClick?.let { rightBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CLICK_RESP, it) }
            }
            TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT -> {
                // 6: Capsule text
                rightBundle.putCharSequence(BUNDLE_KEY_ISLAND_RIGHT_CAPSULE_CONTENT, colorSpan(rightContent, fgColor))
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_BG_COLOR, bgColor)
                actionClick?.let { rightBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_ISLAND_RIGHT_CLICK_RESP, it) }
            }
        }
        islandBundle.putBundle(BUNDLE_KEY_ISLAND_RIGHT_INFO, rightBundle)
        bundle.putBundle(BUNDLE_KEY_ISLAND_INFOS, islandBundle)

        // ── Capsule (always sent alongside the island for Flip outer-screen compatibility, §5.1) ──
        val capsuleBundle = Bundle()
        capsuleBundle.putInt(BUNDLE_KEY_CAPSULE_STATE, 1)
        capsuleBundle.putParcelable(BUNDLE_KEY_CAPSULE_ICON, rightIcon)
        capsuleBundle.putString(BUNDLE_KEY_CAPSULE_CONTENT, rightContent)
        capsuleBundle.putInt(BUNDLE_KEY_CAPSULE_CONTENT_COLOR, fgColor)
        capsuleBundle.putInt(BUNDLE_KEY_CAPSULE_BG_COLOR, bgColor)
        if (capsuleShowTime > 0) capsuleBundle.putInt(OriginIslandConstants.BUNDLE_KEY_CAPSULE_SHOW_TIME, capsuleShowTime)
        if (newNode > 0) capsuleBundle.putInt(OriginIslandConstants.BUNDLE_KEY_CAPSULE_NEW_NODE, newNode)
        primaryClick?.let { capsuleBundle.putParcelable(OriginIslandConstants.BUNDLE_KEY_CAPSULE_CLICK_RESP, it) }
        bundle.putBundle(BUNDLE_KEY_CAPSULE, capsuleBundle)

        return bundle
    }
}
