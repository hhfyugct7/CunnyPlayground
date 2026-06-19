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

    /** Scenes registered via NotificationManager#setSuperXInfosSceneList (see superx_demo). */
    val SUPERX_SCENES = listOf(
        "NAVIGATION", "MOVIE", "HEALTH_REGISTER", "TAXI", "TAKEOUT",
        "DELIEVERY", "CAR_STATE", "METTING", "TRAIN", "FLIGHT"
    )
}
