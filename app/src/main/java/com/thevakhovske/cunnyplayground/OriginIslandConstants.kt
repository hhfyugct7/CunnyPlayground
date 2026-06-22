package com.thevakhovske.cunnyplayground

/**
 * Bundle keys and template constants for vivo OriginOS SuperX (原子通知 / 原子岛 / OriginIsland).
 *
 * Direct port of superx_demo's `LiveNotificationConstants.java`. The values MUST match the
 * originals byte-for-byte — OriginOS reads these exact keys out of the notification extras
 * bundle. Comments are kept from the source for reference.
 */
object OriginIslandConstants {
    // 原子通知核心参数 START
    const val BUNDLE_KEY_OPERATION = "notification.superx.operation" // 原子通知生命周期 0：创建 1：更新 2：结束
    const val BUNDLE_KEY_TEMPLATE = "notification.superx.template" // 原子通知类型模板 1：强调信息 2：进度可视化
    const val BUNDLE_KEY_SHOW_NOTIFY = "notification.superx.showNotify" // 展示失败时是否展示成普通通知，默认 true
    const val BUNDLE_KEY_VERSION_CODE = "notification.superx.versionCode" // 版本号，内部使用
    const val BUNDLE_KEY_SCENE = "notification.superx.scene" // 原子通知的场景，没有的话走异常逻辑
    const val BUNDLE_KEY_KEEP_DURATION = "notification.superx.keepDuration" // 结束后保留显示多久(s)，最长 1h
    const val BUNDLE_KEY_IS_V_SUGGESTION = "notification.superx.isVSuggestion" // 是否走小v建议逻辑
    const val BUNDLE_KEY_V_EXTRA_DATA = "notification.superx.vSuggestionExtraData" // 小v建议的额外数据
    const val BUNDLE_KEY_BASE_INFOS = "notification.superx.baseInfos" // 基础信息参数
    const val BUNDLE_KEY_INFOS = "notification.superx.infos" // 原子通知对应template的参数
    const val BUNDLE_KEY_SHORT_INFOS = "notification.superx.shortInfos" // 小卡和OriginB锁屏模版参数
    const val BUNDLE_KEY_ISLAND_INFOS = "notification.superx.island" // 原子岛参数信息
    const val BUNDLE_KEY_CLICK_RESP = "notification.superx.clickResp" // 落地页 / 胶囊点击落地页
    const val BUNDLE_KEY_CAPSULE = "notification.superx.capsule" // 原子胶囊模板参数
    const val BUNDLE_KEY_CHANGE_RECORD = "notification.superx.changedRecord" // 更新的顺序
    const val BUNDLE_KEY_REMOTE_IMAGE_ERROR = "remote_image_error"
    // 原子通知核心参数 END

    // 原子通知基础信息区参数 START
    const val BUNDLE_KEY_BASE_ICON = "notification.superx.baseInfos.icon"
    const val BUNDLE_KEY_BASE_TITLE = "notification.superx.baseInfos.title"
    const val BUNDLE_KEY_BASE_CONTENT = "notification.superx.baseInfos.content"
    const val BUNDLE_KEY_BASE_SUB_INFO = "notification.superx.baseInfos.subInfo" // 0:不展示 1:文本 2:胶囊文本 3:图片 4:多图片
    const val BUNDLE_KEY_BASE_SUB_TEXT = "notification.superx.baseInfos.subText"
    const val BUNDLE_KEY_BASE_SUB_TEXT_COLOR = "notification.superx.baseInfos.subTextColor"
    const val BUNDLE_KEY_BASE_SUB_CAPSULE_BG_COLOR = "notification.superx.baseInfos.subCapsuleBgColor"
    const val BUNDLE_KEY_BASE_SUB_IMAGE = "notification.superx.baseInfos.subImage"
    const val BUNDLE_KEY_BASE_SUB_INFO_CLICK_RESP = "notification.superx.baseInfos.subInfoClickResp"
    const val BUNDLE_KEY_BASE_SUB_IMAGE_LIST = "notification.superx.baseInfos.subImageList"
    const val BUNDLE_KEY_BASE_SUB_INFO_CLICK_RESP_LIST = "notification.superx.baseInfos.subInfoClickRespList"
    // 原子通知基础信息区参数 END

    // 原子通知强调信息区参数 START
    const val BUNDLE_KEY_INFO_DESCRIBE = "notification.superx.infos.describe"
    const val BUNDLE_KEY_INFO_CORE_INFO = "notification.superx.infos.coreInfo"
    const val BUNDLE_KEY_INFO_IMAGE = "notification.superx.infos.image"
    const val BUNDLE_KEY_INFO_IMAGE_CLICK_RESP = "notification.superx.infos.imageClickResp"
    // 原子通知强调信息区参数 END

    // 原子通知进度可视化参数 START
    const val BUNDLE_KEY_INFO_NODE_ICON = "notification.superx.infos.nodeIcon" // 节点图标，2~5个
    const val BUNDLE_KEY_INFO_INDICATOR_ICON = "notification.superx.infos.indicatorIcon"
    const val BUNDLE_KEY_INFO_INDICATOR_LOC = "notification.superx.infos.indicatorLoc" // 1:覆盖 2:上方
    const val BUNDLE_KEY_INFO_PROGRESS = "notification.superx.infos.progress" // 最大值 100
    const val BUNDLE_KEY_INFO_PROGRESS_COLOR = "notification.superx.infos.progressColor"
    const val BUNDLE_KEY_INFO_BG_COLOR = "notification.superx.infos.BgColor"
    // 原子通知进度可视化参数 END

    // 原子通知左右对称信息区参数 START
    const val BUNDLE_KEY_INFO_LEFT_MAIN = "notification.superx.infos.leftMain"
    const val BUNDLE_KEY_INFO_LEFT_SUB = "notification.superx.infos.leftSub"
    const val BUNDLE_KEY_INFO_MID_MAIN_TYPE = "notification.superx.infos.midMainType" // 1.图片 2.文本
    const val BUNDLE_KEY_INFO_MID_MAIN_ICON = "notification.superx.infos.midMainIcon"
    const val BUNDLE_KEY_INFO_MID_MAIN_MSG = "notification.superx.infos.midMainMsg"
    const val BUNDLE_KEY_INFO_MID_SUB_TYPE = "notification.superx.infos.midSubType" // 1.图片 2.文本
    const val BUNDLE_KEY_INFO_MID_SUB_ICON = "notification.superx.infos.midSubIcon"
    const val BUNDLE_KEY_INFO_MID_SUB_MSG = "notification.superx.infos.midSubMsg"
    const val BUNDLE_KEY_INFO_RIGHT_MAIN = "notification.superx.infos.rightMain"
    const val BUNDLE_KEY_INFO_RIGHT_MAIN_EXTRA = "notification.superx.infos.rightMainExtra"
    const val BUNDLE_KEY_INFO_RIGHT_SUB = "notification.superx.infos.rightSub"
    // 原子通知左右对称信息区参数 END

    // 原子通知小卡和OriginB锁屏参数 START
    const val BUNDLE_KEY_SHORT_INFO_ICON = "notification.superx.shortInfos.icon"
    const val BUNDLE_KEY_SHORT_INFO_IMAGE = "notification.superx.shortInfos.image"
    const val BUNDLE_KEY_SHORT_INFO_IMAGE_CLICK_RESP = "notification.superx.shortInfos.imageClickResp"
    const val BUNDLE_KEY_SHORT_INFO_ORIGIN_IMAGE = "notification.superx.shortInfos.OriginBImage"
    const val BUNDLE_KEY_SHORT_INFO_DESCRIBE_SHORT = "notification.superx.shortInfos.describeShort"
    const val BUNDLE_KEY_SHORT_INFO_CORE_INFO_SHORT = "notification.superx.shortInfos.coreInfoShort"
    // 原子通知小卡和OriginB锁屏参数 END

    // 原子通知模板类型
    const val TEMPLATE_PRIORITY_INFO = 1 // 强调信息模板
    const val TEMPLATE_PROGRESS_VISUAL = 2 // 进度可视化模板
    const val TEMPLATE_TEXT_SYMMETRY = 3 // 左右文本对称模版
    const val TEMPLATE_BASE = 4 // 基础模版

    // 原子通知胶囊字段定义 START
    const val BUNDLE_KEY_CAPSULE_STATE = "notification.superx.capsule.state" // 0:不展示 1:展示
    const val BUNDLE_KEY_CAPSULE_ICON = "notification.superx.capsule.icon"
    const val BUNDLE_KEY_CAPSULE_CONTENT = "notification.superx.capsule.content"
    const val BUNDLE_KEY_CAPSULE_NEW_NODE = "notification.superx.capsule.newNode"
    const val BUNDLE_KEY_CAPSULE_CONTENT_COLOR = "notification.superx.capsule.contentColor"
    const val BUNDLE_KEY_CAPSULE_BG_COLOR = "notification.superx.capsule.bgColor"
    // 原子通知胶囊字段定义 END

    // 原子岛字段定义 START
    const val BUNDLE_KEY_ISLAND_LEFT_TEMPLATE = "island.superx.leftTemplate" // 1：图片+文本
    const val BUNDLE_KEY_ISLAND_LEFT_INFO = "island.superx.leftInfo"
    const val BUNDLE_KEY_ISLAND_RIGHT_TEMPLATE = "island.superx.rightTemplate" // 1律动 2进度 3加载 4文本+图片 5图片+文本 6胶囊文本
    const val BUNDLE_KEY_ISLAND_RIGHT_INFO = "island.superx.rightInfo"
    const val BUNDLE_KEY_ISLAND_FORCE_SHOW = "island.superx.forceShow"
    const val BUNDLE_KEY_ISLAND_FORCE_SHOW_CARD = "island.superx.forceShowCard"
    const val BUNDLE_KEY_ISLAND_CALLBACK_INFOS = "island.superx.callbackInfos"
    const val BUNDLE_KEY_ISLAND_CARD_TEMPLATE = "island.superx.template"
    const val BUNDLE_KEY_ISLAND_SHOW_TIME = "island.superx.showTime" // 最长 180 秒
    const val BUNDLE_KEY_ISLAND_LEFT_ICON = "island.superx.leftInfo.icon"
    const val BUNDLE_KEY_ISLAND_LEFT_CONTENT = "island.superx.leftInfo.content"
    const val BUNDLE_KEY_ISLAND_RIGHT_WAVE_COLOR = "island.superx.rightInfo.waveColor" // 主色, 辅色
    const val BUNDLE_KEY_ISLAND_RIGHT_WAVE_STATE = "island.superx.rightInfo.waveState" // 0暂停 1播放
    const val BUNDLE_KEY_ISLAND_RIGHT_PROGRESS = "island.superx.rightInfo.progressValue"
    const val BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_COLOR = "island.superx.rightInfo.progressColor"
    const val BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_BG_COLOR = "island.superx.rightInfo.progressBgColor"
    const val BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_STATE = "island.superx.rightInfo.progressState" // -1失败 0更新中 1成功
    const val BUNDLE_KEY_ISLAND_RIGHT_LOADING_COLOR = "island.superx.rightInfo.loadingColor"
    const val BUNDLE_KEY_ISLAND_RIGHT_ICON = "island.superx.rightInfo.icon" // rightTemplate 4 或 5
    const val BUNDLE_KEY_ISLAND_RIGHT_CONTENT = "island.superx.rightInfo.content" // rightTemplate 4 或 5
    const val BUNDLE_KEY_ISLAND_RIGHT_CAPSULE_CONTENT = "island.superx.rightInfo.capsuleContent" // rightTemplate 6
    const val BUNDLE_KEY_ISLAND_RIGHT_BG_COLOR = "island.superx.rightInfo.capsuleBgColor"
    const val BUNDLE_KEY_ISLAND_RIGHT_CLICK_RESP = "island.superx.rightInfo.clickResp"

    const val TEMPLATE_RIGHT_ISLAND_WAVE = 1 // 律动动画
    const val TEMPLATE_RIGHT_ISLAND_PROGRESS = 2 // 进度动画
    const val TEMPLATE_RIGHT_ISLAND_LOADING = 3 // loading动画
    const val TEMPLATE_RIGHT_ISLAND_TEXT_ICON = 4 // 文本+图片
    const val TEMPLATE_RIGHT_ISLAND_ICON_TEXT = 5 // 图片+文本
    const val TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT = 6 // 胶囊文本
    // 原子岛字段定义 END

    // ── Additional core params (原子通知技术规范 2.1) ──
    const val BUNDLE_KEY_NEW_NODE = "notification.superx.newNode" // 新节点，胶囊等处展示重要节点（如接单1，送外卖2）
    const val BUNDLE_KEY_DISPLAYS = "notification.superx.displays" // 触点 bitmask（不传为默认触点）
    const val BUNDLE_KEY_SOUND = "notification.superx.sound" // 创建时是否响铃，默认true
    const val BUNDLE_KEY_DISMISS_WHEN_KILL = "notification.superx.dismissWhenKill" // 杀进程时是否清除，默认false
    const val BUNDLE_KEY_CUSTOM_SUPERX = "notification.superx.customSuperx" // 扩展信息 json（小v建议）

    // 触点 (displays) bitmask
    const val DISPLAY_NOTIFICATION = 0x001
    const val DISPLAY_LOCKSCREEN = 0x010
    const val DISPLAY_STATUSBAR = 0x100
    const val DISPLAY_WIDGET = 0x1000
    const val DISPLAY_AOD = 0x10000
    const val DISPLAY_MAGICBOX = 0x100000
    const val DISPLAY_V_SUGGESTION = 0x1000000

    // Extra template ids (template 5 = navigation; island.template 6 = custom)
    const val TEMPLATE_NAVIGATION = 5 // 导航模版
    const val TEMPLATE_CUSTOM = 6 // 自定义模版（仅 island.template）

    // ── Base infos extras (subInfo 4 多图片 / 5 进度 / 6 加载). subImage/subImageList/clickResp keys above. ──
    const val BUNDLE_KEY_BASE_SUB_PROGRESS = "notification.superx.baseInfos.subProgress" // subInfo 5 进度值
    const val BUNDLE_KEY_BASE_SUB_PROGRESS_COLOR = "notification.superx.baseInfos.subProgressColor"
    const val BUNDLE_KEY_BASE_SUB_PROGRESS_BG_COLOR = "notification.superx.baseInfos.subProgressBgColor"
    const val BUNDLE_KEY_BASE_PROGRESS_STATE = "notification.superx.baseInfos.progressState" // -1失败 0更新中 1成功
    const val BUNDLE_KEY_BASE_PROGRESS_CONTENT = "notification.superx.baseInfos.progressContent" // 进度环内文本
    const val BUNDLE_KEY_BASE_LOADING_COLOR = "notification.superx.baseInfos.loadingColor" // subInfo 6 loading 圆点颜色
    const val BASE_SUB_INFO_NONE = 0
    const val BASE_SUB_INFO_TEXT = 1
    const val BASE_SUB_INFO_CAPSULE = 2
    const val BASE_SUB_INFO_IMAGE = 3
    const val BASE_SUB_INFO_IMAGE_LIST = 4
    const val BASE_SUB_INFO_PROGRESS = 5
    const val BASE_SUB_INFO_LOADING = 6

    // ── Capsule extras ──
    const val BUNDLE_KEY_CAPSULE_SHOW_TIME = "notification.superx.capsule.showTime" // 显示秒数，默认一直显示
    const val BUNDLE_KEY_CAPSULE_CLICK_RESP = "notification.superx.capsule.clickResp" // 默认取 clickResp

    // ── Island extras ──
    const val BUNDLE_KEY_ISLAND_CLICK = "island.superx.islandClick" // 0出卡[默认] 1跳落地页 2点击反馈
    const val BUNDLE_KEY_ISLAND_CLICK_RESP = "island.superx.clickResp" // islandClick=1 时落地页
    const val BUNDLE_KEY_ISLAND_BASE_INFOS = "island.superx.baseInfos" // 大卡基础区，不设置复用核心 baseInfos
    const val BUNDLE_KEY_ISLAND_CARD_INFOS = "island.superx.infos" // 大卡扩展区，不设置复用核心 infos
    const val ISLAND_CLICK_SHOW_CARD = 0
    const val ISLAND_CLICK_LANDING = 1
    const val ISLAND_CLICK_FEEDBACK = 2

    // ── Priority info extras (2.5) ──
    const val BUNDLE_KEY_INFO_SUB_REVERSE = "notification.superx.infos.subReverse" // 与辅助信息区调换
    const val BUNDLE_KEY_VCARD_MAIN_TEXT = "notification.vcard.infos.mainText" // 组件卡片主信息
    const val BUNDLE_KEY_VCARD_SUB_TEXT = "notification.vcard.infos.subText" // 组件卡片辅信息

    // ── Symmetry extra (2.7) ──
    const val BUNDLE_KEY_INFO_MID_TOP_MSG = "notification.superx.infos.midTopMsg" // 中间顶部信息

    // ── Navigation template (2.9) ──
    const val BUNDLE_KEY_INFO_NAV_ICON = "notification.superx.infos.navIcon" // 导航左侧图标
    const val BUNDLE_KEY_INFO_NAV_MSG = "notification.superx.infos.navMsg" // 导航信息，两行用 # 分隔

    // ── Undocumented, recovered by decompiling OriginOS SystemUI (SuperXTemplateUtils v2) ──
    // v2 template ids: 1 major / 2 progress / 3 horizontal-pair / 4 normal / 5 navi
    const val TEMPLATE_NOTIF_CUSTOM = 7    // 自定义模版 (card-level custom; cf. island TEMPLATE_CUSTOM=6)
    const val TEMPLATE_BUTTONS = 8         // 按钮模版 (multi-button card)
    const val TEMPLATE_DRIVING_NAVI = 9    // 驾车导航模版

    // Multi-button API (notification.superx.infos.btn*) — ButtonsSuperXTemplate.
    // btnType 1 = up to 3 filled VButtons in a row; 2 = 2–5 icon+text items.
    // All five lists must be the same length; btnIconList must be non-empty (entries may be null).
    const val BUNDLE_KEY_INFO_BTN_TYPE = "notification.superx.infos.btnType"
    const val BUNDLE_KEY_INFO_BTN_TEXT_LIST = "notification.superx.infos.btnTextList"            // ArrayList<String>
    const val BUNDLE_KEY_INFO_BTN_ICON_LIST = "notification.superx.infos.btnIconList"            // ArrayList<Icon>
    const val BUNDLE_KEY_INFO_BTN_TEXT_COLOR_LIST = "notification.superx.infos.btnTextColorList" // ArrayList<Int>
    const val BUNDLE_KEY_INFO_BTN_COLOR_LIST = "notification.superx.infos.btnColorList"          // ArrayList<Int> (fill, type 1)
    const val BUNDLE_KEY_INFO_BTN_CLICK_RESP_LIST = "notification.superx.infos.btnClickRespList" // ArrayList<PendingIntent>
    const val BTN_TYPE_FILLED = 1
    const val BTN_TYPE_ICON_TEXT = 2

    // Other recovered core/card fields
    const val BUNDLE_KEY_CARD_BG_COLOR = "notification.superx.cardBgColor"               // int
    const val BUNDLE_KEY_KEEP_SCREEN_ON = "notification.superx.keepScreenOn"             // boolean
    const val BUNDLE_KEY_DISABLE_INVERT_COLOR = "notification.superx.disableInvertColor" // boolean

    // Island double-line text (ArrayList<CharSequence>) — text-only island side, no icon needed
    const val BUNDLE_KEY_ISLAND_LEFT_DOUBLELINE = "island.superx.leftInfo.doublelineText"
    const val BUNDLE_KEY_ISLAND_RIGHT_DOUBLELINE = "island.superx.rightInfo.doublelineText"
    const val BUNDLE_KEY_ISLAND_LEFT_GENERATING = "island.superx.leftInfo.generatingStatus"
    const val BUNDLE_KEY_ISLAND_RIGHT_PLAY_PAG = "island.superx.rightInfo.playPAG"
    const val BUNDLE_KEY_ISLAND_RIGHT_PROGRESS_CONTENT = "island.superx.rightInfo.progressContent"

    // AOD edge light effect (EffectLightInfo: mode, mainColor, sosWarnAnim)
    const val BUNDLE_KEY_EFFECT_IS_LIGHT = "notification.superx.lightEffect.isLight"           // boolean
    const val BUNDLE_KEY_EFFECT_LIGHT_INFO = "notification.superx.lightEffectInfo"             // Bundle
    const val BUNDLE_KEY_EFFECT_LIGHT_MAIN_COLOR = "notification.superx.lightEffectInfo.mainColor" // int
    const val BUNDLE_KEY_EFFECT_LIGHT_MODE = "notification.superx.lightEffectInfo.mode"        // int
    const val BUNDLE_KEY_EFFECT_LIGHT_SOS_WARN = "notification.superx.lightEffectInfo.sosWarn" // int
    const val BUNDLE_KEY_EFFECT_LIGHT_REPEAT = "notification.superx.lightEffectInfo.sosWarnRepeatCount"

    // baseInfos extras
    const val BUNDLE_KEY_BASE_CONTENT_ICON = "notification.superx.baseInfos.contentIcon"       // Icon
    const val BUNDLE_KEY_BASE_GENERATING_STATUS = "notification.superx.baseInfos.generatingStatus" // int
    const val BUNDLE_KEY_BASE_ICON_STATUS_TYPE = "notification.superx.baseInfos.iconStatusType"    // 0 success/1 fail/2 error
    const val BUNDLE_KEY_BASE_ICON_ROUND_CORNER = "notification.superx.baseInfos.iconRoundCorner"  // boolean
    const val ICON_STATUS_SUCCESS = 0
    const val ICON_STATUS_FAIL = 1
    const val ICON_STATUS_ERROR = 2
    val STATUS_MAIN_COLOR = intArrayOf(
        0xFF32BF55.toInt(), // success rgb(50,191,85)
        0xFFED4D47.toInt(), // fail    rgb(237,77,71)
        0xFFFF9D58.toInt()  // error   rgb(255,157,88)
    )

    // Driving-navi template (9) — infos.*
    const val BUNDLE_KEY_INFO_DIR_ICON = "notification.superx.infos.dirIcon"               // Icon
    const val BUNDLE_KEY_INFO_DIR_SUB_TEXT = "notification.superx.infos.dirSubText"         // CharSequence
    const val BUNDLE_KEY_INFO_DIR_ASSIST_TEXT = "notification.superx.infos.dirAssistText"   // String
    const val BUNDLE_KEY_INFO_DIR_ASSIST_ICON = "notification.superx.infos.dirAssistIcon"   // Icon
    const val BUNDLE_KEY_INFO_DIR_ASSIST_TYPE = "notification.superx.infos.dirAssistType"   // int
    const val BUNDLE_KEY_INFO_DIR_ASSIST_TEXT_COLOR = "notification.superx.infos.dirAssistTextColor"
    const val BUNDLE_KEY_INFO_DIR_ASSIST_BTN_COLOR = "notification.superx.infos.dirAssistBtnColor"
    const val BUNDLE_KEY_INFO_LANE_LIST = "notification.superx.infos.laneList"               // ArrayList<Parcelable>
    const val BUNDLE_KEY_INFO_MAIN_HIGHLIGHT_TEXT = "notification.superx.infos.mainHighlightText"
    const val BUNDLE_KEY_INFO_MAIN_NORMAL_TEXT = "notification.superx.infos.mainNormalText"

    // Custom template (7)
    const val BUNDLE_KEY_CUSTOM_TEMPLATE = "notification.superx.customTemplate"             // Parcelable
    const val BUNDLE_KEY_CUSTOM_TEMPLATE_AOD = "notification.superx.customTemplate.aodAdapter"

    // Island control extras
    const val BUNDLE_KEY_ISLAND_PRIORITY = "island.superx.priority"
    const val BUNDLE_KEY_ISLAND_TYPE = "island.superx.islandType"
    const val BUNDLE_KEY_ISLAND_SHOW_TYPE = "island.superx.islandShowType"
    const val BUNDLE_KEY_ISLAND_STATE = "island.superx.state"
    const val BUNDLE_KEY_ISLAND_PERMANENT = "island.superx.permanent"
    const val BUNDLE_KEY_ISLAND_DISMISS_CARD = "island.superx.dismissCard"
    const val BUNDLE_KEY_ISLAND_SHOW_BAR_WHEN_CARD = "island.superx.showBarWhenCard"
    const val BUNDLE_KEY_ISLAND_LANDING_INFO = "island.superx.landingInfo"
    const val BUNDLE_KEY_ISLAND_LANDING_PKG = "island.superx.landingPkg"
    const val BUNDLE_KEY_ISLAND_CAPSULE_WIDTH = "island.superx.capsule.width"

    // Capsule extra
    const val BUNDLE_KEY_CAPSULE_LANDING_AUTO_HIDE = "notification.superx.capsule.landingAutoHide"

    /** Fixed tag required to cancel/end a SuperX atomic notification (see 技术规范 demo 3.3). */
    const val SUPERX_TAG = "VIVO_SUPERX_TAG"

    /**
     * Scenes registered via NotificationManager#setSuperXInfosSceneList. The first block is from
     * superx_demo / the public doc; the rest were recovered by decompiling SystemUI (INCALLING,
     * VOIPCALL, TIMER, RIDE_GUIDE, CRITICAL — needed for call / timer / SOS style notifications).
     */
    val SUPERX_SCENES = listOf(
        "NAVIGATION", "MOVIE", "HEALTH_REGISTER", "TAXI", "TAKEOUT",
        "DELIEVERY", "CAR_STATE", "METTING", "TRAIN", "FLIGHT",
        "INCALLING", "VOIPCALL", "TIMER", "RIDE_GUIDE", "CRITICAL"
    )
}
