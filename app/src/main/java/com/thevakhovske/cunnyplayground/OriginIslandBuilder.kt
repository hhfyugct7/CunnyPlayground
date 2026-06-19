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

    /**
     * Builds the full SuperX extras bundle.
     *
     * Mirrors `SuperXTemplateDemo.buildImportantInfoTemplate`. Icons are passed in (rather than
     * resolved from fixed resources) so both the manual playground and the notification re-caster
     * can supply their own artwork.
     *
     * @param template        atomic notification template (1 强调信息 / 2 进度可视化 / 3 左右对称 / 4 基础)
     * @param rightTemplate   right-island template (1 wave / 2 progress / 3 loading / 4 text+icon / 5 icon+text / 6 capsule)
     * @param defaultIcon     icon used for base/info/short/node areas (superx_demo uses ic_launcher)
     * @param leftIcon        OriginIsland left-island icon
     * @param rightIcon       OriginIsland right-island / capsule icon
     * @param accentIcon      indicator icon for the progress-visual template (superx_demo uses car)
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
        clickResp: PendingIntent?
    ): Bundle {
        val bundle = Bundle()
        bundle.putInt(BUNDLE_KEY_OPERATION, 0)
        bundle.putBoolean(BUNDLE_KEY_SHOW_NOTIFY, true)
        bundle.putInt(BUNDLE_KEY_TEMPLATE, template)
        if (clickResp != null) bundle.putParcelable(BUNDLE_KEY_CLICK_RESP, clickResp)
        bundle.putString(BUNDLE_KEY_SCENE, scene)
        bundle.putInt(BUNDLE_KEY_CHANGE_RECORD, 0)

        // Base Infos
        val baseBundle = Bundle()
        baseBundle.putParcelable(BUNDLE_KEY_BASE_ICON, defaultIcon)
        baseBundle.putCharSequence(BUNDLE_KEY_BASE_TITLE, title)
        baseBundle.putCharSequence(BUNDLE_KEY_BASE_CONTENT, content)

        if (template == OriginIslandConstants.TEMPLATE_BASE) {
            // Template 4: Basic requires subInfo
            baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, 1)
            baseBundle.putString(BUNDLE_KEY_BASE_SUB_TEXT, extra1)
        } else {
            baseBundle.putInt(BUNDLE_KEY_BASE_SUB_INFO, 2)
            baseBundle.putString(BUNDLE_KEY_BASE_SUB_TEXT, "Waiting...")
            baseBundle.putInt(BUNDLE_KEY_BASE_SUB_TEXT_COLOR, Color.parseColor("#FFFFFF"))
            baseBundle.putInt(BUNDLE_KEY_BASE_SUB_CAPSULE_BG_COLOR, Color.parseColor("#FF0F24"))
        }
        bundle.putBundle(BUNDLE_KEY_BASE_INFOS, baseBundle)

        // Specific Template Infos
        val infoBundle = Bundle()
        when (template) {
            OriginIslandConstants.TEMPLATE_PRIORITY_INFO -> {
                infoBundle.putString(BUNDLE_KEY_INFO_DESCRIBE, extra1)
                infoBundle.putString(BUNDLE_KEY_INFO_CORE_INFO, extra2)
                infoBundle.putParcelable(BUNDLE_KEY_INFO_IMAGE, defaultIcon)
                if (clickResp != null) infoBundle.putParcelable(BUNDLE_KEY_INFO_IMAGE_CLICK_RESP, clickResp)
            }
            OriginIslandConstants.TEMPLATE_PROGRESS_VISUAL -> {
                val iconList = ArrayList<Icon>()
                iconList.add(defaultIcon)
                iconList.add(defaultIcon)
                iconList.add(defaultIcon)
                infoBundle.putParcelableArrayList(BUNDLE_KEY_INFO_NODE_ICON, iconList)
                infoBundle.putInt(BUNDLE_KEY_INFO_PROGRESS, progress)
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
                infoBundle.putParcelable(BUNDLE_KEY_INFO_MID_MAIN_ICON, defaultIcon)
                infoBundle.putInt(BUNDLE_KEY_INFO_MID_SUB_TYPE, 2)
                infoBundle.putString(BUNDLE_KEY_INFO_MID_SUB_MSG, "Mid Sub")
                infoBundle.putString(BUNDLE_KEY_INFO_RIGHT_MAIN, extra3)
                infoBundle.putString(BUNDLE_KEY_INFO_RIGHT_SUB, extra4)
            }
            OriginIslandConstants.TEMPLATE_BASE -> {
                // No extra info bundle for basic
            }
        }
        bundle.putBundle(BUNDLE_KEY_INFOS, infoBundle)

        // Short Infos
        val shortInfoBundle = Bundle()
        shortInfoBundle.putParcelable(BUNDLE_KEY_SHORT_INFO_IMAGE, defaultIcon)
        shortInfoBundle.putString(BUNDLE_KEY_SHORT_INFO_CORE_INFO_SHORT, content)
        shortInfoBundle.putString(BUNDLE_KEY_SHORT_INFO_DESCRIBE_SHORT, title)
        bundle.putBundle(BUNDLE_KEY_SHORT_INFOS, shortInfoBundle)

        // Island Infos
        val islandBundle = Bundle()
        islandBundle.putInt(BUNDLE_KEY_ISLAND_LEFT_TEMPLATE, 1)
        islandBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_TEMPLATE, rightTemplate)

        val leftBundle = Bundle()
        leftBundle.putParcelable(BUNDLE_KEY_ISLAND_LEFT_ICON, leftIcon)
        leftBundle.putString(BUNDLE_KEY_ISLAND_LEFT_CONTENT, leftContent)
        islandBundle.putBundle(BUNDLE_KEY_ISLAND_LEFT_INFO, leftBundle)

        val rightBundle = Bundle()
        when (rightTemplate) {
            TEMPLATE_RIGHT_ISLAND_WAVE -> {
                // 1: Wave
                val colors = ArrayList<String>()
                colors.add(String.format("#%06X", 0xFFFFFF and fgColor))
                rightBundle.putStringArrayList(BUNDLE_KEY_ISLAND_RIGHT_WAVE_COLOR, colors)
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_WAVE_STATE, 1)
            }
            TEMPLATE_RIGHT_ISLAND_PROGRESS -> {
                // 2: Progress
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS, progress)
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_COLOR, fgColor)
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_BG_COLOR, bgColor)
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_STATE, 1)
            }
            TEMPLATE_RIGHT_ISLAND_LOADING -> {
                // 3: Loading
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_LOADING_COLOR, fgColor)
            }
            TEMPLATE_RIGHT_ISLAND_TEXT_ICON, TEMPLATE_RIGHT_ISLAND_ICON_TEXT -> {
                // 4 or 5: Text+Icon / Icon+Text
                rightBundle.putParcelable(BUNDLE_KEY_ISLAND_RIGHT_ICON, rightIcon)
                val spannable = SpannableString(rightContent)
                if (rightContent.isNotEmpty()) {
                    spannable.setSpan(
                        ForegroundColorSpan(fgColor), 0, rightContent.length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                rightBundle.putCharSequence(BUNDLE_KEY_ISLAND_RIGHT_CONTENT, spannable)
            }
            TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT -> {
                // 6: Capsule
                val spannable = SpannableString(rightContent)
                if (rightContent.isNotEmpty()) {
                    spannable.setSpan(
                        ForegroundColorSpan(fgColor), 0, rightContent.length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                rightBundle.putCharSequence(BUNDLE_KEY_ISLAND_RIGHT_CAPSULE_CONTENT, spannable)
                rightBundle.putInt(BUNDLE_KEY_ISLAND_RIGHT_BG_COLOR, bgColor)
            }
        }

        islandBundle.putBundle(BUNDLE_KEY_ISLAND_RIGHT_INFO, rightBundle)
        bundle.putBundle(BUNDLE_KEY_ISLAND_INFOS, islandBundle)

        // Capsule
        val capsuleBundle = Bundle()
        capsuleBundle.putInt(BUNDLE_KEY_CAPSULE_STATE, 1)
        capsuleBundle.putParcelable(BUNDLE_KEY_CAPSULE_ICON, rightIcon)
        capsuleBundle.putString(BUNDLE_KEY_CAPSULE_CONTENT, rightContent)
        capsuleBundle.putInt(BUNDLE_KEY_CAPSULE_CONTENT_COLOR, fgColor)
        capsuleBundle.putInt(BUNDLE_KEY_CAPSULE_BG_COLOR, bgColor)
        bundle.putBundle(BUNDLE_KEY_CAPSULE, capsuleBundle)

        return bundle
    }
}
