package moe.ouom.neriplayer.ui.screen.tab.settings.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This file is part of the MEMI輕音 fork and is distributed under GPL-3.0.
 */

/**
 * 设置页内重复出现的圆角常量。
 *
 * 1.20 之前每个文件各自写 `RoundedCornerShape(14.dp)` / `(24.dp)`，
 * 改一处视觉要翻好几个文件。这里只收敛**数值完全一致**的用法，
 * 语义不同的形状（标签页、预览舞台、滑块轨道）仍保留在各自文件里。
 */
internal val SettingsGlyphShape = RoundedCornerShape(14.dp)
internal val SettingsPanelShape = RoundedCornerShape(24.dp)
