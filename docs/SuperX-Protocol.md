# vivo OriginOS SuperX (原子通知 / OriginIsland) — Complete Protocol Reference

> The definitive, reverse-engineered specification of vivo's SuperX atomic-notification API as
> implemented on **OriginOS 6 (SystemUI 16.0.7.x)**. This merges the public doc
> (`dev.vivo.com.cn/documentCenter/doc/896`, "原子通知技术规范") with everything recovered by
> decompiling `SystemUI.apk` and `System UI kits.apk` (jadx). Fields the public doc omits are tagged
> **[RE]**. This is the reference behind `OriginIslandConstants.kt` / `OriginIslandBuilder.kt`.

---

## 1. Architecture

A SuperX notification is an **ordinary Android `Notification`** whose `extras` bundle carries a tree
of `notification.superx.*` and `island.superx.*` keys. OriginOS's SystemUI plugin
(`com.vivo.notification.template.v2.*`) parses these extras and renders, in order of capability:

- **状态栏胶囊 (status-bar capsule)** — the pill near the status bar. From `notification.superx.capsule`.
- **原子岛 (OriginIsland)** — the Dynamic-Island-style pill around the camera; expands to a big card.
  From `notification.superx.island` (+ `island.superx.*` sub-bundles).
- **大卡 (big card)** — the expanded card. Template chosen by `notification.superx.template`.
- **小卡 / OriginB 锁屏 (small card / lockscreen)** — from `notification.superx.shortInfos`.
- **AOD light effect** — edge lighting on AOD. From `notification.superx.lightEffectInfo` **[RE]**.

> **Compatibility (§5.1):** always send BOTH `capsule` and `island` — Flip phones show the capsule on
> the outer screen while the main screen shows the island.

### Engine versions
Two renderers coexist: the **legacy** templates (`com.vivo.notification.template.legacy.*`, what the
public doc describes) and the newer **v2** templates (`com.vivo.notification.template.v2.*`, OriginOS 6).
v2 reads the same `notification.superx.*` keys and adds new templates/fields. The numbers below are v2.

---

## 2. Lifecycle, channel & whitelist

### Sending (local)
```kotlin
// 1. The scene must be whitelisted first (else nothing renders):
NotificationManager.setSuperXInfosSceneList(sceneList, switchList, pkgList, pkgSwitchList) // reflection
// 2. A NotificationChannel must exist BEFORE posting (§5.8/§5.11). Deleting it clears the island.
// 3. Build a normal NotificationCompat.Builder, attach the bundle via setExtras(bundle), then:
notificationManager.notify("VIVO_SUPERX_TAG", id, notification)
```

### `notification.superx.operation` lifecycle
| value | meaning |
|------:|---------|
| 0 | 创建 — create |
| 1 | 更新 — update (re-post same `id`, all other data same as create) |
| 2 | 结束 — end. With no `keepDuration`, only `operation=2` is required (§5.4). |

### Ending / cancelling
- **End (respect archive):** post `operation=2` (optionally with `keepDuration`).
- **Immediate cancel (§3.3):** `notificationManager.cancel("VIVO_SUPERX_TAG", id)` — **must use the fixed
  tag `VIVO_SUPERX_TAG`**. An untagged `cancel(id)` leaves the island lingering.

### Reflection APIs (`android.app.NotificationManager`, hidden)
| method | purpose |
|--------|---------|
| `setSuperXInfosSceneList(List, List, List, List)` | register scene whitelist (scenes, switches, pkgs, pkgSwitches). Stored in SP `SuperXInfos` **[RE]**. |
| `getSceneStatus(String pkg, String scene): boolean` **[RE §5.6]** | is the per-scene switch on? |
| `isSupportCustomFun(String pkg, String scene): boolean` **[RE §5.9]** | does this OS version support the module for the scene? |

### Cautions (§5)
- **Do not `setAutoCancel(true)`** (§5.7/§5.10) — clicking from the shade would let the system cancel it.
- Icons ≤ **1000×1000 px** (§5.15).
- Color **`0x0`** (alpha 0) = "use system default" (§5.5).
- §5.16: do **not** gate sending on the user's notification-switch state.

---

## 3. Core params — `notification.superx.*`

| key | type | req | values / notes |
|-----|------|:--:|----------------|
| `operation` | int | Y | 0 create / 1 update / 2 end |
| `template` | int | Y | template id — see §4 |
| `scene` | String | Y | see §11 scene list |
| `showNotify` | boolean | N | show as a normal notification if SuperX render fails (default true) |
| `versionCode` | int | N | internal version field |
| `changedRecord` | int | N | update ordering; an update with a value < current is dropped |
| `keepDuration` | int | N | seconds to keep showing after `end`; max 3600 (1h); default 0 (no archive) |
| `sound` | boolean | N | ring on create (default true) |
| `clickResp` | PendingIntent | Y | tap target for big card / small card / capsule |
| `baseInfos` | Bundle | Y | §5 |
| `infos` | Bundle | Y | template-specific, §6 |
| `shortInfos` | Bundle | N | small card / OriginB lockscreen, §7 |
| `island` | Bundle | N | OriginIsland data, §8 (devices with island use this as the status-bar touchpoint) |
| `capsule` | Bundle | N | status-bar capsule, §9 (devices without island use this) |
| `isVSuggestion` | boolean | N | route through 小v suggestion logic |
| `vSuggestionExtraData` | — | N | extra data when `isVSuggestion` |
| `newNode` | int | N | bump on each new node; capsule etc. can show the node |
| `displays` | int | N | touchpoint bitmask — see §11 |
| `dismissWhenKill` | boolean | N | clear when process killed (default false) |
| `customSuperx` | String(json) | N | extension info (小v suggestion); needs business onboarding |
| **`cardBgColor`** | int | N | **[RE]** big-card background color |
| **`keepScreenOn`** | boolean | N | **[RE]** keep the screen on while shown |
| **`disableInvertColor`** | boolean | N | **[RE]** opt out of automatic color inversion |
| **`islandNotify`** | boolean | N | **[RE]** |
| **`animationsConfig`** | String | N | **[RE]** animation config json |
| **`eventInfo`** | String | N | **[RE]** |
| **`legacy` / `legacy.businessKey` / `legacy.package` / `legacy.scene`** | bool/String | N | **[RE]** legacy-bridge fields |
| **`old.baseInfos.icon`** | Parcelable | N | **[RE]** legacy icon |
| **`lightEffectInfo` + sub-keys** | Bundle | N | **[RE]** AOD light effect, §10 |

---

## 4. Templates — `notification.superx.template`

From the v2 `SuperXTemplateUtils.TEMPLATE_TYPE_MAP` **[RE]** (public doc only lists 1–5):

| id | template | doc? |
|---:|----------|:----:|
| 1 | 强调信息 / major info (priority) | ✓ |
| 2 | 进度可视化 / progress visual | ✓ |
| 3 | 左右文本对称 / horizontal pair | ✓ |
| 4 | 基础 / normal (base) | ✓ |
| 5 | 导航 / navi | ✓ |
| **7** | **自定义 / custom** | **[RE]** |
| **8** | **按钮 / buttons** | **[RE]** |
| **9** | **驾车导航 / driving-navi** | **[RE]** |

---

## 5. `notification.superx.baseInfos` (base info area)

| key | type | req | notes |
|-----|------|:--:|-------|
| `title` | CharSequence | Y | brief state description |
| `content` | CharSequence | Y | auxiliary description |
| `icon` | Icon | N | defaults to the app's notification icon |
| `subInfo` | int | N* | aux-area mode (see below). **Required when `template=4`.** |
| `subText` | String | N | aux text (subInfo 1/2) |
| `subTextColor` | int | N | text color for subInfo 1/2 |
| `subCapsuleBgColor` | int | N | capsule bg for subInfo 2 |
| `subImage` | Icon | N | image for subInfo 3 |
| `subImageList` | ArrayList\<Icon\> | N | subInfo 4, ≤3 images; >1 image ⇒ each needs a click |
| `subInfoClickResp` | PendingIntent | N | click for subInfo 2/3 |
| `subInfoClickRespList` | ArrayList\<PendingIntent\> | N | clicks for subInfo 4 |
| `subProgress` | int | N | subInfo 5 progress value |
| `subProgressColor` | int | N | subInfo 5 ring color |
| `subProgressBgColor` | int | N | subInfo 5 ring bg |
| `progressState` | int | N | subInfo 5 state: -1 fail / 0 updating / 1 success |
| `progressContent` | CharSequence | N | text inside the big-card progress ring (default = value) |
| `loadingColor` | int | N | subInfo 6 loading-dot color |
| **`contentIcon`** | Parcelable | N | **[RE]** icon beside content |
| **`generatingStatus`** | int | N | **[RE]** AI "generating" animation state |
| **`iconStatusType`** | int | N | **[RE]** icon status overlay: 0 success / 1 fail / 2 error (colors §11) |
| **`iconRoundCorner`** | boolean | N | **[RE]** round the base icon corners |

**`subInfo` values:** 0 none · 1 text · 2 capsule text · 3 image · 4 multi-image (template 4 only) · 5 progress · 6 loading.

---

## 6. `notification.superx.infos` (template-specific)

### Template 1 — 强调信息 (priority / major)
| key | type | req |
|-----|------|:--:|
| `describe` | String | Y |
| `coreInfo` | String | Y |
| `image` | Icon | Y |
| `imageClickResp` | PendingIntent | N |
| `subReverse` | boolean | N — swap emphasis/aux image positions (new ROMs) |
| `notification.vcard.infos.mainText` / `.subText` | CharSequence | N — 小v component card |

### Template 2 — 进度可视化 (progress visual)
| key | type | req | notes |
|-----|------|:--:|-------|
| `nodeIcon` | ArrayList\<Icon\> | Y | 2–5 nodes (4–5 ⇒ system hides the indicator). Pass transparent icons for a markerless bar. |
| `progress` | int | Y | 0–100 |
| `indicatorIcon` | Icon | N | omit ⇒ no indicator rides the bar |
| `indicatorLoc` | int | N | 1 over the line (default) / 2 above it |
| `progressColor` | int | N | |
| `BgColor` | int | N | bar background |

### Template 3 — 左右文本对称 (horizontal pair)
`leftMain`(Y) `leftSub`(Y) `midTopMsg`(N) `midMainType`(Y:1 img/2 text) `midMainIcon` `midMainMsg`
`midSubType`(1/2) `midSubIcon` `midSubMsg` `rightMain`(Y) `rightMainExtra`(N, forced red, e.g. "+1天") `rightSub`(Y).

### Template 5 — 导航 (navi)
`navIcon`(Y, Icon) · `navMsg`(Y, String — max 2 lines, split lines with `#`).

### Template 8 — 按钮 (buttons) **[RE]** — `ButtonsSuperXTemplate`
The undocumented multi-button row. **All five lists must be the same length.** `btnIconList` must be
non-empty; null entries render as text-only buttons.
| key | type | notes |
|-----|------|-------|
| `btnType` | int | **1** = up to **3** filled `VButton`s in a row (Deny/Receive style); **2** = **2–5** icon+text items |
| `btnTextList` | ArrayList\<String\> | labels |
| `btnIconList` | ArrayList\<Icon\> | per-button icon (null ⇒ text only) |
| `btnTextColorList` | ArrayList\<Integer\> | text colors |
| `btnColorList` | ArrayList\<Integer\> | fill colors (type 1) |
| `btnClickRespList` | ArrayList\<PendingIntent\> | per-button click |

> Layouts: `vivo_super_x_v2_template_buttons_info` (card) / `..._island` (island). Standard Android
> `addAction` buttons are **ignored** on SuperX cards — this is the only button mechanism.

### Template 9 — 驾车导航 (driving navi) **[RE]** — `DrivingNaviSuperXTemplate`
`dirIcon`(Parcelable, turn icon) · `dirSubText`(CharSequence) · `dirAssistText`(String) ·
`dirAssistIcon`(Parcelable) · `dirAssistType`(int) · `dirAssistTextColor`(int) · `dirAssistBtnColor`(int) ·
`laneList`(ArrayList\<Parcelable\>, lane guidance) · `mainHighlightText`/`mainNormalText`(String).

### Template 7 — 自定义 (custom) **[RE]** — `CustomSuperXTemplate`
Driven by `notification.superx.customTemplate` (Parcelable, a `RemoteViews`-like payload) and
`customTemplate.aodAdapter` (String). For fully bespoke cards.

---

## 7. `notification.superx.shortInfos` (small card / OriginB lockscreen)
`icon`(N) · `image`(Y) · `imageClickResp`(Y) · `OriginBImage`(N, lockscreen icon) ·
`describeShort`(Y) · `coreInfoShort`(Y).

---

## 8. OriginIsland — `notification.superx.island` (+ `island.superx.*`)

### Island top-level
| key | type | values |
|-----|------|--------|
| `island.superx.leftTemplate` | int | 1 = image+text |
| `island.superx.leftInfo` | Bundle | §8.1 |
| `island.superx.rightTemplate` | int | 1 wave · 2 progress · 3 loading · 4 text+icon · 5 icon+text · 6 capsule |
| `island.superx.rightInfo` | Bundle | §8.2 |
| `island.superx.islandClick` | int | **0 = 出卡 (expand card)** · 1 = jump to `clickResp` · 2 = feedback only |
| `island.superx.clickResp` | PendingIntent | landing for island/card blank tap (only when islandClick=1) |
| `island.superx.template` | int | big-card template when island expands (1–9; absent ⇒ no card) |
| `island.superx.baseInfos` / `island.superx.infos` | Bundle | big-card data; absent ⇒ reuse core `baseInfos`/`infos` |
| `island.superx.showTime` | int | seconds on the island; max 180; default always |
| `island.superx.forceShow` | boolean | force-show in fullscreen |
| `island.superx.forceShowCard` | boolean | pop the card directly on click |
| `island.superx.callbackInfos` | — | broadcast back to CP when island/card dismissed |
| **`island.superx.priority`** | int | **[RE]** island priority |
| **`island.superx.islandType` / `islandShowType`** | int | **[RE]** island kind / show mode |
| **`island.superx.state`** | int | **[RE]** state (getState 0–5 observed) |
| **`island.superx.permanent`** | boolean | **[RE]** persistent island |
| **`island.superx.dismissCard`** | boolean | **[RE]** |
| **`island.superx.showBarWhenCard`** | boolean | **[RE]** keep the status-bar capsule while the card shows |
| **`island.superx.islandAfterSlideCard`** | int | **[RE]** |
| **`island.superx.forceShowCard.keyguard` / `forceShowCardInt`** | bool/int | **[RE]** |
| **`island.superx.landingInfo` / `landingPkg`** | String | **[RE]** landing target |
| **`island.superx.business`** | — | **[RE]** |
| **`island.superx.capsule.width`** | int | **[RE]** capsule width override |
| **`island.superx.clipToOutline`** | boolean | **[RE]** |
| **`island.superx.customTemplate`** | Parcelable | **[RE]** |
| **`island.superx.rightCommonIconValue`** | — | **[RE]** |

### 8.1 `island.superx.leftInfo`
`icon`(Y, Icon) · `content`(N, CharSequence — supports SpannableString color **and chronometer text**) ·
**`doublelineText`** (N, ArrayList\<CharSequence\>) **[RE]** — two text lines, no icon needed ·
**`generatingStatus`** (N, int) **[RE]**.

### 8.2 `island.superx.rightInfo`
| key | type | when |
|-----|------|------|
| `waveColor` | ArrayList\<int\>* | rightTemplate 1 (main, accent). *(superx_demo passes hex `ArrayList<String>`.)* |
| `waveState` | int | 1: 0 pause / 1 play (default) |
| `progressValue` | int | 2 |
| `progressColor` / `progressBgColor` | int | 2 |
| `progressState` | int | 2: -1 fail / 0 updating / 1 success |
| `progressContent` | String | 2 **[RE]** text in ring |
| `loadingColor` | int | 3 |
| `icon` | Icon | 4/5 (omit for text-only) |
| `content` | CharSequence | 4/5 (SpannableString color) |
| `capsuleContent` | CharSequence | 6 |
| `capsuleBgColor` | int | 6 |
| `clickResp` | PendingIntent | 4/5/6 (animations don't support click) |
| **`doublelineText`** | ArrayList\<CharSequence\> | **[RE]** two-line text |
| **`playPAG`** | boolean | **[RE]** play a PAG animation |
| **`template`** | int | **[RE]** per-side template override |
| **(bean) `rightButtonContent` + `rightButtonClickResp`** | CharSequence + PendingIntent | **[RE]** an inline right-island button |

---

## 9. `notification.superx.capsule` (status-bar capsule)
`state`(Y int: 0 hide / 1 show) · `icon`(Y) · `content`(N, default = coreInfo) · `contentColor`(N, default black) ·
`bgColor`(N, default white w/ border) · `newNode`(N) · `showTime`(N, seconds; default always) ·
`clickResp`(N, default = core clickResp) · **`landingAutoHide`**(N, boolean) **[RE]**.

---

## 10. AOD light effect — `notification.superx.lightEffectInfo` **[RE]**
Parsed into `EffectLightInfo(mode:int, mainColor:int, sosWarnAnim:int)`.
| key | type |
|-----|------|
| `notification.superx.lightEffect.isLight` | boolean — enable edge lighting |
| `notification.superx.lightEffectInfo` | Bundle |
| `notification.superx.lightEffectInfo.mainColor` | int — primary light color |
| `notification.superx.lightEffectInfo.mode` | int — effect mode |
| `notification.superx.lightEffectInfo.sosWarn` | int — SOS warning animation |
| `notification.superx.lightEffectInfo.sosWarnRepeatCount` | int |

---

## 11. Value tables

**Scene** (`scene`): documented — `NAVIGATION` `MOVIE` `HEALTH_REGISTER` `TAXI` `TAKEOUT` `DELIEVERY`
`CAR_STATE` `METTING` `TRAIN` `FLIGHT`; **[RE]** also handled — `INCALLING` / `VOIPCALL` (call-style,
pairs with the buttons template for Decline/Answer), `TIMER`, `RIDE_GUIDE`, `CRITICAL` (SOS/light),
plus provider variants `TAXI_BAIDU` `TAXI_DIDI` `TRAIN_NORMAL`. **Every scene used must be whitelisted
via `setSuperXInfosSceneList` first.**

**Card-click routing [RE]** (`SuperXTemplateBase.getParseSuperXCardClickResp`): if
`isUniqueIslandSuperXMessage` (requires `notification.superx.islandNotify=true` + island bundle) →
`island.superx.clickResp`; else if a v2 SuperX notification → core `notification.superx.clickResp`;
if overseas + a `ProgressStyle` Live Update → `notification.contentIntent`. **Gotcha:**
`ButtonsSuperXTemplate` (template 8) does **not** wire a whole-card click — only its buttons are
tappable. So a card that needs both a body-tap *and* buttons isn't expressible; use template 8 only
when ≥2 actions matter, and a chip on a tappable template (1/2/4) for a single action.

**`displays` bitmask:** `0x001` notification · `0x010` lockscreen · `0x100` status bar · `0x1000`
desktop widget · `0x10000` AOD · `0x100000` magic box · `0x1000000` 小v suggestion. Default: 4×2 card =
all; 4×1 card = notification & status bar. Example `0x111` = notification + lockscreen + status bar.

**Status / progressState colors [RE]** (`TEMPLATE_STATUS_TYPE_MAIN_COLOR`): 0 success `rgb(50,191,85)` ·
1 fail `rgb(237,77,71)` · 2 error `rgb(255,157,88)`.

**`islandClick`:** 0 expand card · 1 landing · 2 feedback. **`btnType`:** 1 filled(≤3) · 2 icon+text(2–5).

---

## 12. Reverse-engineering appendix

**Toolchain:** jadx 1.5.5 + cfr, in `C:\Users\Adam\re_tools\`. Decompiled source:
`re_tools/suikits_src/` (from `System UI kits_16.0.7.202.apk`). Quick key dump without decompiling:
`unzip -p APK 'classes*.dex' | grep -aoE '(notification|island)\.superx[A-Za-z0-9._]*' | sort -u`.

**Key classes:**
- `com.android.systemui.plugins.vivo.superx.constant.LiveSuperXConstants` — keys + template ints + inner `EffectLight`/`Notify`/`Path`/`Scene`.
- `com.vivo.notification.template.v2.SuperXTemplateUtils` — `template id → layout` map; status colors.
- `com.vivo.notification.template.v2.template.*` — per-template render (`ButtonsSuperXTemplate`, `DrivingNaviSuperXTemplate`, `MajorInfoSuperXTemplate`, `ProgressSuperXTemplate`, `HorizontalPairSuperXTemplate`, `NormalSuperXTemplate`, `NaviSuperXTemplate`, `CustomSuperXTemplate`).
- `com.vivo.island.bean.{LeftIslandBean,RightIslandBean,IslandDataBean,EffectLightInfo}` + `com.vivo.island.data.AtomIslandsBean` — island data model.
- `com.android.systemui.plugins.vivo.superx.backup.model.SuperXData` — backup/restore model (BaseInfo/CoreInfo/IslandInfo/ProgressInfo/SubInfo/BarCapsuleInfo).
