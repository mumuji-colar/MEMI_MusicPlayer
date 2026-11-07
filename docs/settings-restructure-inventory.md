# 设置页结构盘点与重构映射

> 只读盘点结果，用于 1.12.1 / 1.12.2 / 1.12.3 三版重构。
> 盘点基准：`SettingsScreen.kt` 第 357 行起的 `mapOf<SettingsPage, LazyListScope.() -> Unit>`

---

## 一、现状概览

| 指标 | 值 |
| :--- | :--- |
| 一级入口 | 22 个（分 8 张分组卡） |
| SettingsPage 枚举值 | 25（含 3 个三级页：MusicServers / StorageCacheDetails / XiaomiSuperIsland） |
| 实际卡片总数 | 约 60 张 |
| 卡片封装写法 | **4 种并存**（见第五节） |
| 无分区标题的卡 | 10 张 |
| 违规使用裸 Material3 Card | 2 处（TrafficManagement） |

---

## 二、25 个页面盘点

「卡片数」为最终呈现的卡片数量，已穿透委托层级。

| # | 页面 | 渲染入口 | 卡片数 | 卡片标题（按顺序） | 备注 |
| :-- | :--- | :--- | :-- | :--- | :--- |
| 1 | General | `SettingsScreen.kt:358` 直接铺卡 | 1 | 无标题（含内嵌子分区「UI 缩放」） | 约 9 项；含 DPI 对话框 |
| 2 | Language | `:404` 直接铺卡 | 1 | 无标题 | **仅 2 项 → 合并候选** |
| 3 | AudioEffects | `:448` → `AudioEffectsSettingsPage` | 2~4（随页签） | 启用音效 / 分区开关 / 分区内容 | **8 个页签的动态编辑器**，卡片数不固定 |
| 4 | Theme | `:457` 4 次铺卡 | 4 | 深浅色模式 / 主题颜色 / 色彩风格 / 色彩空间 | 卡④仅 1 项 |
| 5 | Accounts | `:544` → `SettingsLoginExpandedContent` | 4 | 网易云 / B站 / YouTube / 管理服务器 | 无详情页头；**跳三级 → MusicServers** |
| 6 | Personalization | `:557` 2 张卡 | 2 | 启动与输入 / 首页栏目 | 卡①仅 2 项 |
| 7 | NowPlaying | `:598` 2 张卡 | 2 | 播放页信息 / 播放页控制 | 7 项 + 3 项 |
| 8 | Background | `:620` → `SettingsPersonalizationBackgroundCard` | 1 | 背景图 | 3 项 |
| 9 | Motion | `:665` 4 张卡（cardIndex 切片） | 4 | 动效总开关 / 高级模糊 / 播放页背景 / 歌词模糊 | 含性能模式三档 |
| 10 | Lyrics | `:715` 3 张卡 | 3 | 歌词与字体 / 来源偏好 / 歌词偏移 | `LyricsPageCards = [4,1,3]` |
| 11 | ExternalLyrics | `:768` 2 张卡 | 2 | 悬浮词幕 / 歌词服务 | `ExternalLyricsPageCards = [0,2]`；**卡内跳三级 → XiaomiSuperIsland** |
| 12 | XiaomiSuperIsland | `:934` → `XiaomiSuperIslandSettingsPage` | 2 | 无标题 + 子分区 | 约 15+ 项；需通知/Shizuku 权限 |
| 13 | Network | `:821` 直接铺卡 | 1 | 无标题 | **仅 1 项 → 合并候选** |
| 14 | Playback | `:847` 4 张卡 | 4 | 播放行为 / 音频输出 / 播放淡入淡出 / 下一首淡入淡出 | 卡③④各 1 开关 + 2 滑杆 |
| 15 | UsbExclusive | `:905` → `UsbExclusiveSettingsSection` | 4 | 无标题 / USB 音频设备 / 音质策略 / 处理链兼容性 | 卡④仅 2 条只读 |
| 16 | PlaybackSource | `:950` 直接铺卡 | 1 | 无标题 | 3 项 |
| 17 | MusicServers | `:944` → `MusicServerSettingsContent` | 1 + N | 新增卡 + 每服务器一张 | **0 设置项**，纯账号管理 |
| 18 | AudioQuality | `:1015` → `SettingsAudioQualitySection` | 1 | 音质设置 | 7 项 + 6 个对话框 |
| 19 | Storage | `:1072` → `settingsStoragePageItems` | 4（+迁移中 1） | 下载目录 / 文件名格式 / 缓存上限 / 缓存清理 | **卡①有入口 → StorageCacheDetails** |
| 20 | StorageCacheDetails | `:1112` → `settingsStorageCacheDetailsItem` | 1 + N | 汇总卡 + 分类卡 | **0 设置项**，纯只读 |
| 21 | TrafficManagement | `:1143` → `SettingsTrafficManagementSection` | 1 外层 + 1 内嵌 | 无标题 + 内嵌「流量统计」 | **大卡套小卡**；仅 1 个设置项；**内嵌卡用裸 Material3 Card** |
| 22 | Downloads | `:1134` → `settingsDownloadsPageItems` | 2 | 无标题 / 下载管理 | 卡②含跨模块跳转（下载管理器） |
| 23 | Backup | `:1149` → `settingsBackupPageItems` | 4（+1 条件） | 本地备份 / 播放历史 / GitHub 同步 / WebDAV 同步 / 同步提示 | 卡⑤条件显示 |
| 24 | ListenTogether | `:1170` → `SettingsListenTogetherSection` | 1 | 无标题 | 4 项全是弹窗入口 |
| 25 | About | `:1181` → `SettingsAboutPageContent` | 1 | 无标题 | 7 项平铺（含源码、开源许可） |

---

## 三、合并候选

**整页只有 1 张卡（9 个）**：General(9项)、Language(2项)、Network(1项)、PlaybackSource(3项)、AudioQuality(7项)、Background(3项)、TrafficManagement(1项)、ListenTogether(4项)、About(7项)

**整页设置项 < 3（5 个）**：Network(1)、Language(2)、TrafficManagement(1)、MusicServers(0)、StorageCacheDetails(0)

**单卡内容过少（页整体不小，但卡片该合并）**：
- Personalization「启动与输入」：2 项
- Theme「色彩空间」：1 项
- UsbExclusive「处理链兼容性」：2 条只读
- Playback「播放淡入淡出」「下一首淡入淡出」：各 1 开关 + 2 滑杆

---

## 四、三级页面边界

当前 `backTargetPage()` 声明的三级页：`MusicServers → Accounts`、`StorageCacheDetails → Storage`、`XiaomiSuperIsland → ExternalLyrics`

| 入口 | 位置 | 目标 |
| :--- | :--- | :--- |
| Accounts → 管理服务器 | `SettingsScreen.kt:548` | MusicServers |
| ExternalLyrics → 配置（超级岛） | `SettingsLyricsOffsetSection.kt:171` | XiaomiSuperIsland |
| Storage → 存储详情 | `SettingsScreen.kt:1080` | StorageCacheDetails |
| StorageCacheDetails → 清缓存 | `SettingsScreen.kt:1118` | 回 Storage + 弹窗 |
| Downloads → 下载管理 | `SettingsScreen.kt:1136` | 下载管理器（**非设置页体系**） |
| About → 开源许可 | `SettingsAboutSection.kt:113` | 对话框（非页面） |

---

## 五、卡片封装现状（重构时的统一点）

| 类型 | 实现 | 使用方 |
| :--- | :--- | :--- |
| **LazyListScope 扩展** | `miuixSettingsSectionCardItem` | General、Language、Theme×4、Network、PlaybackSource、AudioQuality、TrafficManagement、ListenTogether、About |
| **独立 composable** | `SettingsMotionSection` / `SettingsLyricsSection` / `SettingsPlaybackSection` / `UsbExclusiveSettingsSection` / `SettingsAudioQualitySection` / `SettingsTrafficManagementSection` / `SettingsListenTogetherSection` / `AudioEffectsSettingsPage` / `XiaomiSuperIslandSettingsPage` / `SettingsLoginExpandedContent` / `MusicServerSettingsContent` / `SettingsAboutPageContent` | 各自页面 |
| **小写 LazyListScope 函数** | `settingsStoragePageItems` / `settingsStorageCacheDetailsItem` / `settingsDownloadsPageItems` / `settingsBackupPageItems` | Storage、StorageCacheDetails、Downloads、Backup |
| **基础卡片壳** | `MiuixSettingsSectionCard`（唯一标准壳） | 上述全部最终都要经过它 |
| **各页自建壳** | `PersonalizationDetailCard` / `MotionDetailCard` / `LyricsDetailCard` / `DetailSectionCard` / `StorageDetailCard` / `BackupDetailCard` / `SettingsAccountCardSurface` | 各自页面内部 |

**问题**：最后一行那 7 个自建壳其实是同一件事的 7 份实现，重构时可以收敛。

---

## 六、Mochi 规范问题（1.12.2 必修）

| 问题 | 位置 | 处理 |
| :--- | :--- | :--- |
| **裸 Material3 `Card`** | `SettingsTrafficManagementSection.kt:139`（`TrafficStatsCard`）、`:210`（`TrafficEmptyCard`） | 换 `MiuixSettingsSectionCard` |
| 大卡套小卡 | TrafficManagement 外层卡内再套一张卡 | 拍平成两张平级卡 |
| 无分区标题的卡 10 张 | General、Language、Network、PlaybackSource、UsbExclusive 卡①、XiaomiSuperIsland×2、Downloads 卡①、ListenTogether、About | 补 `MiuixSettingsSectionIntro` 或明确「无需标题」并统一视觉 |
| 7 套自建卡片壳 | 见第五节 | 收敛为 `MiuixSettingsSectionCard` |

---

## 七、重构映射（10 个大分类）

「外观」若含动效+播放页会达到 11 张卡，过长，故拆为两个分类。

| 新分类 | 合并自 | 卡片构成 | 卡数 |
| :--- | :--- | :--- | :-- |
| **账号与音源** | Accounts + PlaybackSource | 网易云 / B站 / YouTube / 音源优先级 / 音乐服务器入口 | 5 |
| **播放** | Playback + AudioQuality | 播放行为 / 音频输出 / 淡入淡出（合并卡③④）/ 音质 | 4 |
| **音效**（三级） | AudioEffects | 8 页签编辑器保持独立页 | — |
| **外观** | Theme + Background | 深浅色 / 主题颜色 / 色彩风格 + 色彩空间（合并）/ 背景图 | 4 |
| **动效与性能** | Motion | 动效总开关 / 高级模糊 / 播放页背景 / 歌词模糊 | 4 |
| **界面布局** | NowPlaying + Personalization | 播放页信息 / 播放页控制 / 启动与输入 / 首页栏目 | 4 |
| **歌词** | Lyrics + ExternalLyrics | 歌词与字体 / 来源偏好 / 歌词偏移 / 悬浮词幕 / 歌词服务 | 5 |
| **存储与下载** | Storage + Downloads | 下载目录 / 文件名格式 / 缓存上限 + 清理（合并）/ 下载 / 下载管理 | 5 |
| **网络与流量** | Network + TrafficManagement | 代理与连通性 + 流量统计（拍平） | 2 |
| **备份与同步** | Backup + ListenTogether | 本地备份 / 播放历史 / GitHub / WebDAV / 一起听 | 5 |
| **通用** | General + Language | 通用开关 / 语言 | 2 |
| **关于** | About | 单卡平铺 | 1 |

### 三级页面（保留 5 处）

| 路径 | 原因 |
| :--- | :--- |
| 播放 → 音效 | 8 页签编辑器，塞进播放页会破坏可读性 |
| 播放 → USB 独占 | 硬件相关，独立页便于写风险提示 |
| 账号与音源 → 音乐服务器 | 服务器增删改，1+N 动态卡 |
| 存储与下载 → 存储详情 | 只读统计页，1+N 动态卡 |
| 歌词 → 小米超级岛 | 需 Root/Shizuku 权限，独立页便于提示 |

> 一级入口 22 → **11**（10 个大分类 + 关于），三级 3 → 5，但三级都是「内容确实放不下」的页面。

---

## 八、风险与实施顺序

| 风险 | 说明 | 缓解 |
| :--- | :--- | :--- |
| **卡片索引硬编码** | `LyricsPageCards = [4,1,3]`、`ExternalLyricsPageCards = [0,2]`，各页 `cardIndex` 切片 | 1.12.1 不动切片；1.12.2 统一改为集中声明 |
| **搜索锚点依赖卡片序号** | `SettingsSearchIndex.kt`（1089 行）假设「页头 index 0 + 卡片顺序」 | 改结构后必须重跑 `SettingsSearchAnchorIndexTest` |
| **两套切片机制并存** | `LyricsPageCards` 集中声明 vs `for (cardIndex in 0..N)` 散落 | 1.12.2 统一为一种 |
| **`AudioEffectsSettingsPage` 卡片数动态** | 随页签 2~4 张 | 保持独立页，不参与铺平 |
| 测试基线 | `SettingsPageTest` 35 项（含硬编码断言 `SettingsHomePageGroups.first { Playback in it }.contains(UsbExclusive)`） | 改结构时同步更新断言 |

### 三版对应

- **1.12.1**：只做路由合并 + 主页 11 入口，卡片内部顺序与索引不动
- **1.12.2**：页内打磨（补标题、收敛 7 套壳、修 Material3 Card、合并单卡）+ 卡片索引重排
- **1.12.3**：滚动搜索栏
- **1.20**：合并收口

---

## 九、1.12.1 实施记录（已完成）

### 改动清单

| 文件 | 改动 |
| :--- | :--- |
| `SettingsPage.kt` | 新增 10 个一级分类枚举（`AccountSources` / `PlaybackHub` / `Appearance` / `MotionPerformance` / `LayoutHub` / `LyricsHub` / `StorageDownloads` / `NetworkTraffic` / `BackupSync` / `GeneralHub`）；`SettingsHomePageGroups` 重写为 4 组 11 入口；新增 `SettingsTopLevelPages` 与 `SettingsHubComposition`；`backTargetPage` 改为四级/三级页回到对应分类 |
| `SettingsScreen.kt` | `mapOf` 后追加 `.toMutableMap().apply { ... }`，用 `SettingsHubComposition` 把原子页 lambda 按顺序聚合；**25 个原子页分支与卡片内部顺序一字未动** |
| `SettingsSearchIndex.kt` | `settingsPageForSection` / `settingsPage()` 指向新分类；`settingsSearchScrollAnchor` 改为「原子页起始卡 + 原子页内偏移」；新增 `SettingsAtomicCardCount`、`settingsHubAtomFor`、`settingsTargetBelongsTo`；页面级搜索只生成一级入口；`resolveSettingsSearchHighlightTarget` 的主题判断改为 `Appearance`；新增 10 个分类的搜索别名 |
| `SettingsPageGlyphs.kt` | 补 10 个分类的 clean 风格图标 |
| `strings_settings_hubs.xml`（三语） | 新增 20 个字符串（10 标题 + 10 描述） |
| 测试 | `SettingsPageTest` 35 项、`SettingsSearchAnchorIndexTest` 4 项、`SettingsSearchLocaleTest` 7 项全部通过 |

### 最终结构

```
设置
├─ 账号与音源  = 账号(4卡) + 音源优先级(1卡)          → 音乐服务器为三级
├─ 播放        = 播放行为(4卡) + 音质(1卡)            → USB 独占、音效为三级
├─ 歌词        = 歌词(3卡) + 外部歌词(2卡)            → 小米超级岛为三级
├─ 外观        = 主题(4卡) + 背景(1卡)
├─ 动效与性能  = 动效(4卡)
├─ 界面布局    = 播放页(2卡) + 首页与启动(2卡)
├─ 存储与下载  = 存储(4卡) + 下载(2卡)                → 存储详情为三级
├─ 网络与流量  = 网络(1卡) + 流量(1卡)
├─ 备份与同步  = 备份(4卡) + 一起听(1卡)
├─ 通用        = 通用(1卡) + 语言(1卡)
└─ 关于
```

一级入口 22 → **11**。

### 遗留到 1.12.2 的问题

| 问题 | 位置 |
| :--- | :--- |
| 裸 Material3 `Card` | `SettingsTrafficManagementSection.kt:141`、`:211` |
| 大卡套小卡 | 同上（网络/流量合并后会更明显） |
| 硬编码 `RoundedCornerShape` | `AudioEffectsControls.kt:222`、`AudioEffectsSettingsPage.kt:206-207`、`SettingsAccountCards.kt:168/207`、`SettingsCookieAuthDialogs.kt:349`、`SettingsMusicServerCard.kt:70`、`SettingsConfigProtectionDialogs.kt:88` |
| 10 张卡无分区标题 | 见第二节备注列 |
| 两套切片机制 | `LyricsPageCards` vs `for (cardIndex in 0..N)` |

### 已知取舍

分类页的搜索锚点精确到「原子页内原锚点」，而非重排后的真实卡片序号 —— 因为 1.12.2 会重排卡片，现在精确计算会白费。当前精度足以命中目标卡片。

### 崩溃修复记录（1.12.1 首发包）

**现象**：真机（Samsung SM-N9860 / Android 13）打开设置页立即崩溃。

```
IllegalArgumentException: Key "GeneralHub:content" was already used.
```

**根因**：原子页的卡片 key 原先写成 `"${selectedPage.name}:content"`，其中 `selectedPage` 是 `settingsDetailContent` 的**运行时入参**。聚合渲染后该参数变成分类页（GeneralHub），导致 General 与 Language 两个原子页的 key 都算成 `"GeneralHub:content"`，LazyColumn 内 key 重复。

**影响面**：10 个分类页里 9 个（只要含 ≥2 张卡）。`MotionPerformance` 只含 1 个原子页故未触发；`StorageDownloads` / `BackupSync` 因外部函数用硬编码枚举名（`"${SettingsPage.Storage.name}:card:$cardIndex"`）未受影响。

**修复**：把 26 处 key 前缀从运行时 `selectedPage.name` 改为**各分支自己的固定页面名**（脚本按 `SettingsPage.X to {` 分支区间批量替换）。

**验证**：脚本比对各分类页内所有 `item(key=)` / `miuixSettingsSectionCardItem` 的 key，10 个分类页均无重复；编译通过。

**教训**：1.12.1 的测试全部是纯函数（搜索锚点、页面映射），**没有覆盖 Compose 渲染**，因此 key 冲突一路漏到真机。1.12.2 应补一个逐页渲染的 Robolectric 冒烟测试。

---

## 十、1.12.2 实施记录（已完成）

### 改动清单

| 类别 | 内容 |
| :--- | :--- |
| **Mochi 规范** | `SettingsTrafficManagementSection` 的两张原生 Material3 `Card`（`TrafficStatsCard` / `TrafficEmptyCard`）改为**卡内分区**（去掉卡片背景），同时消除「大卡套小卡」 |
| **标题补齐** | `UsbExclusive` 第一张卡补 `SettingsSectionTitle`（「USB 独占播放」），与同页其他三张卡一致 |
| **卡片合并** | 外观页「色彩空间」原独占一张卡（仅 1 项），并入「色彩风格」卡；主题原子页 4 卡 → 3 卡 |
| **回归测试** | 新增 `SettingsCardKeyUniquenessTest`（3 项）：① 卡片 key 不得依赖 `selectedPage.name`；② 每个分类页内 key 唯一；③ 每个原子页都有渲染分支 |

### 同步改动

- `SettingsAtomicCardCount[Theme]` 4 → 3
- `atomicSettingsAnchor` 中 `manual:theme_color_spec` 锚点 4 → 3
- 外观页背景卡起始位置 5 → 4
- 相关测试期望同步（`SettingsPageTest` 35、`SettingsSearchAnchorIndexTest` 4、`SettingsSearchLocaleTest` 3、`SettingsCardKeyUniquenessTest` 4 = **46 项全绿**）

### 补充修复：三级页入口缺失（真机反馈）

**现象**：真机上「音效」「USB 独占」在设置里找不到，只能靠搜索跳进去。

**根因**：`SettingsHubComposition` 只拼接原子页；`AudioEffects`、`UsbExclusive` 是独立成页的模块，不参与拼接。1.12.1 把它们从一级入口下沉为三级页，却**没有在父页里放入口行**。其余三个三级页（音乐服务器、存储概览、小米超级岛）原本就由各自的原子页组件提供跳转，所以只有这两个漏了。

**修复**：

| 文件 | 内容 |
| :--- | :--- |
| `component/SettingsSubPageEntry.kt`（新增） | `SettingsSubPageEntryRow`（ListItem + `settingsItemClickable` + 右箭头）与 `SettingsSubPageEntrySection`（卡内标题 + 若干入口行） |
| `SettingsScreen.kt` | 播放分类页在音质卡之后追加 `AudioQuality:entries` 卡，含「音效」「USB 独占」两行，点击切换 `navigation.activePage` |
| `SettingsSearchIndex.kt` | `SettingsAtomicCardCount[AudioQuality]` 1 → 2 |
| 三语文案 | `settings_hub_advanced_audio` / `_desc` |
| `SettingsCardKeyUniquenessTest` | 新增第 4 项 `everyNestedPageHasANavigationEntry`：所有 `backTargetPage() != null` 的页面，源码里必须存在 `activePage = SettingsPage.X` 或 `openSettingsPage(SettingsPage.X)` |

这条静态断言正是防同类回归的：以后再把某页下沉成三级页却忘了加入口，测试会直接失败。

**产物**：`dist/MEMI-1.12.2-2611072-fix1.apk`（versionCode 不变，可直接覆盖安装）。

### 仍留到 1.12.3 / 1.20

| 项目 | 原因 |
| :--- | :--- |
| 9 处硬编码 `RoundedCornerShape` | 视觉差异极小（8dp→Mochi 8dp 肉眼难辨），改动风险大于收益 |
| Playback 淡入淡出两卡合并、Personalization 启动与输入并卡、UsbExclusive 卡④ | 都要重排卡片索引，与 1.12.3 的滚动搜索栏一起做更划算 |
| Downloads 卡① 的标题 | 其组件内部结构较独立，单改收益低 |
| 7 套自建卡片壳收敛 | 影响面大，放 1.20 合并收口 |
| 两套切片机制统一 | 同上 |

## 十一、1.12.3 实施记录（滚动搜索栏）

### 目标

主页入口已经收敛到 11 个，但搜索框一直固定占着列表第一行，页面顶部永远有一块输入框。1.12.3 把它挪进顶栏，按滚动状态出现：

| 状态 | 表现 |
| :--- | :--- |
| 刚进设置页 | 顶栏只有「设置」标题，无搜索框 |
| 向下滚动、大标题折叠到底 | 搜索框从标题下方滑出 |
| 向上滚回顶部 | 搜索框收起 |
| 搜索词非空 | 强制保留输入框（否则结果还在却改不了关键词） |
| 切换到二级页 | 不出现（搜索栏只挂在一级主页） |

### 实现

| 文件 | 改动 |
| :--- | :--- |
| `page/MiuixSettingsScaffold.kt` | `MiuixSettingsHomeScaffold` 新增 `searchBar: (@Composable () -> Unit)?` 与 `forceSearchBarVisible: Boolean`；顶栏改为 `Column { 大标题栏 + AnimatedVisibility(搜索栏) }`，进出场用 `mochiSpring`；判定逻辑抽成纯函数 `shouldShowSettingsHomeSearchBar(collapsedFraction, forceVisible, laidOut, canScrollForward)` |
| `navigation/SettingsNavigationSearch.kt` | 删除列表首行的 `settings_search_field` item；新增 `SettingsHomeSearchBar` 供顶栏使用 |
| `SettingsScreen.kt` | 传入 `searchBar = { SettingsHomeSearchBar(navigation) }`、`forceSearchBarVisible = 查询非空`；查询变为非空时 `animateScrollToItem(0)`，否则结果卡片在视野外 |

### 显示判定的四个条件

```
forceVisible || collapsedFraction >= 0.85 || (laidOut && !canScrollForward)
```

- `collapsedFraction >= 0.85`：大标题基本收完才插入一行，避免标题还在收缩时顶栏跳一下
- `forceVisible`：搜索结果还在时不能把输入框收走
- `laidOut && !canScrollForward`：内容不足一屏、或已经滑到底时标题根本收不起来，必须给入口，否则永远搜不到；`laidOut` 用来挡首帧（未布局时 `totalItemsCount == 0`），避免搜索栏闪现

### 顺带修掉的一个遗留问题

`settingsHomeSelectedPage` 原先只处理「三级页 → 父页」。1.12.1 之后搜索可以直接落到原子页（如 `Storage`、`Playback`），这类页面不在主页列表里，结果是**主页什么都高亮不了**。现在补上「原子页 → 所属分类」的上溯：

```kotlin
page.backTargetPage()?.let { return it }
return SettingsHubComposition.entries.firstOrNull { page in it.value }?.key ?: page
```

同时修正了 `SettingsNavigationPolicyTest` 里一条 1.12.1 遗留的过期断言（`StorageCacheDetails` 的父页已从 `Storage` 变为 `StorageDownloads`），该断言此前一直是失败状态。

### 测试

`SettingsPageTest` 35 + `SettingsCardKeyUniquenessTest` 4 + `SettingsSearchAnchorIndexTest` 4 + `SettingsSearchLocaleTest` 3 + `SettingsNavigationPolicyTest` 14 = **60 项全绿**。

其中 `SettingsNavigationPolicyTest` 新增 `home search bar only shows after the large title collapses`，覆盖六个分支：初始不显示、半途不显示、折叠后显示、首帧不显示、内容不足时显示、搜索词非空时显示。

### 产物

`dist/MEMI-1.12.3-2611081.apk`
