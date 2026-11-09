# 歌词断行与中文分词

> 1.20.1 起内测、1.21 公开发布：歌词换行不再只按字宽算，会先问一句「这里是不是词与词之间」。

## 问题

改造前的断行有两个阶段：

1. `groupIntoWords` 按「音节是否以空格结尾」切词。中文没有空格，所以**一整句中文 = 一个超长单词**；
2. 断行算法在「允许断」的位置里挑一个让各行长度最接近的。

第 2 步对中文是「处处等权」的：字与字之间没有区别，于是它会把最整齐的位置当答案，哪怕那里正好是一个词的中间。典型症状：

```
我想要和你一
起去看海
```

「一起」被劈成两半。避头点/避尾点只能管标点，管不了词。

## 方案

用系统自带的 ICU 词典补上词边界（`android.icu.text.BreakIterator` + `Locale.CHINESE`），
再把「断在词内部」变成一件有代价的事。

关键约束：**不改 `wordId`**。`wordId` 同时驱动单词弹跳动画的缩放原点与逐字进度，
拆散它动效会跟着变形。所以分词结果只输出「字符偏移是不是词边界」，由断行成本消费。

### 代价模型

断行 DP 的目标是最小化 `Σ(剩余宽度² × 系数)`。拆词惩罚如果也做成系数，会被剩余宽度² 稀释——
行填得越满，惩罚越接近 0，而「填满」恰恰是最容易拆词的时候。所以拆词用一个**与剩余宽度无关的加项**：

```kotlin
badness = 剩余宽度² × breakScale(断点) + breakPenalty(断点)
```

| 项 | 取值 | 说明 |
| :--- | :--- | :--- |
| 词边界折扣 | ×1.0 | 不打折：词边界只是「不额外加价」，优势全交给下面的固定惩罚 |
| 拆词惩罚 | 5.0 × 平均字宽² | 约等于「宁可多空出 5 个字」也不拆词（用户实机调参后定稿） |

单位取平均字宽而不是可用宽度：宽度翻倍时惩罚翻四倍的话，大屏上算法会为了躲开拆词把行拉得极不均衡。

### 只作用于汉字

`isIntraHanWordBreak` 要求断点两侧都是汉字（`isCjk` 且不是假名/谚文，且是字母）：

- 拉丁单词：已由 `canBreakBetween` 整词禁断，不需要再罚；
- 中文标点：已由避头/避尾按 6 倍处理；
- 假名/谚文：各自有排版规则，不套中文词典的分词结果。

### 降级

ICU 取不到时（JVM 单元测试、极老系统）`CjkWordSegmentation.boundaries` 返回空集合，
断行退回改造前的纯均衡行为，不影响可用性。结果按整行文本缓存 128 条，避免滚动时反复分词。

## 顺带修的两处

| 问题 | 修法 |
| :--- | :--- |
| 「——」「……」被拆到两行，各留一半 | `isDoubledPunctuationPair`：相邻音节是同一个成对符号的两半时，禁止断开 |
| 左引号「“」留在行尾 | 补进 `lineEndForbidden`（避尾点） |

## 验证

`LyricsLineBreakingTest` 44 项，其中新增：

- 词边界断行不比词内更贵（拆词代价走 DP 固定加项，不挂在系数上）；
- 只有「汉字 + 汉字 + 词内」才算拆词（拉丁词内、汉字接标点都不算）；
- 16 字 / 每行 9 字 / 词边界在第 7 字后：加上惩罚后断在 7（7+9），不加惩罚断在 8（8+8）——对照组证明结论由惩罚驱动；
- 成对符号不拆、左引号避尾。

`accompanist-lyrics-ui` 全量 72 项（3 + 9 + 16 + 44）通过。

## 还能怎么调（改哪里、调多少、怎么验）

所有可调项都是 `const val`，集中在两个文件里。改完重新打包即可，没有开关、不需要重启应用。

### 拆词相关的两个（最常动）

文件：`np-submodule/accompanist-lyrics-ui/src/src/commonMain/kotlin/com/mocharealm/accompanist/lyrics/ui/composable/lyrics/LyricsLayoutCalculator.kt`
（在文件里搜常量名即可定位，行号会随改动漂移，这里不写死）

| 常量 | 默认值 | 所在行（约） | 作用 | 往大调 | 往小调 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `CJK_WORD_SPLIT_PENALTY_SYLLABLES` | `5.0f` | 316 | 拆一个词的代价，单位是「平均字宽²」 | 更不容易拆词，但行更参差（建议 ≤ 6） | 更容易拆词，行更整齐（建议 ≥ 1.2；低于 1 基本等于关掉） |
| `CJK_WORD_BOUNDARY_BREAK_SCALE` | `1.0f` | 301 | 断在词与词之间时的折扣 | 现在已是 1.0（不打折，优势全在固定惩罚） | 想额外偏向词边界可降到 0.9（别低于 0.7，会和均衡目标打架） |

### 标点与均衡的四个（一般不用动）

文件：同目录下 `LyricsLineBreaking.kt`

| 行号 | 常量 | 默认值 | 作用 |
| :--- | :--- | :--- | :--- |
| 50 | `FORBIDDEN_AT_LINE_START_SCALE` | `6f` | 标点被顶到行首的代价（避头点），调大更严格 |
| 53 | `FORBIDDEN_AT_LINE_END_SCALE` | `6f` | 开引号 / 开括号留在行尾的代价（避尾点） |
| 56 | `PUNCTUATION_BREAK_SCALE` | `0.75f` | 句读标点之后断行的折扣，调小更爱在逗号句号后换行 |
| 59 | `INTRA_WORD_BREAK_SCALE` | `1.15f` | 同一 `wordId` 内断行的额外成本（拉丁词另有硬禁断，主要影响中文） |

### 症状对照

| 看到的现象 | 改哪个 | 方向 |
| :--- | :--- | :--- |
| 词保住了，但两行长短差得明显 | `CJK_WORD_SPLIT_PENALTY_SYLLABLES` | 5.0 → 3.5 或 3 |
| 还是会把「一起」「知道」这类词拆开 | 同上 | 5.0 → 6（再往上行会明显参差） |
| 明明有逗号，却偏要在句中换行 | `PUNCTUATION_BREAK_SCALE` | 0.75 → 0.6 |
| 行首还是冒出逗号 / 句号 | `FORBIDDEN_AT_LINE_START_SCALE` | 6 → 10 |
| 某些人名、生造词被词典切错 | 改不了 | ICU 词典的边界，属已知限制；不建议为此塞自定义词典 |

### 改完怎么验

**1. 跑单元测试**（约 1 分钟，会连带跑对照组）：

```bash
cd "C:/Users/dongy/WorkBuddy/2026-11-02-20-41-10/NeriPlayer/NeriPlayer-NeriPlayer-51f0100c.10312030"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.12"
"C:/Users/dongy/.gradle/tools/gradle-9.6.1/bin/gradle" \
  :accompanist-lyrics-ui:testDebugUnitTest --offline -PallowUnsignedRelease=true
```

> 对照组断言的是「断在第 7 个字」。把惩罚调到 1.0 以下会让它失败，这属于预期——说明惩罚已经弱到不起作用了。

**2. 打包**：

```bash
export JAVA_HOME="C:/Users/dongy/.jdks/jdk-17.0.20.1+1"
"C:/Users/dongy/.gradle/tools/gradle-9.6.1/bin/gradle" \
  :app:assembleRelease --offline -PallowUnsignedRelease=true
```

产物在 `app/build/outputs/apk/release/NeriPlayer-<版本号>.apk`；签名用 build-tools 的 `zipalign` + `apksigner`，命令与我每次打包时一致。

**3. 只想看观感**：不必走完整打包，`adb install -r` 手头那个包改不了的——常量是编译期值，必须重编。图快可以只装 debug：`:app:installDebug`。

