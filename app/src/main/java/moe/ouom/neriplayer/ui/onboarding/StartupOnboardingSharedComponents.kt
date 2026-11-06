package moe.ouom.neriplayer.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.ui.haptic.HapticOutlinedButton
import moe.ouom.neriplayer.ui.theme.NeriMotion
import ui.feice.mochi.shape.MochiCapsule

@Composable
internal fun StepHeader(icon: ImageVector, title: String, description: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = OnboardingControlShape,
            color = colors.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun OnboardingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HapticOutlinedButton(
        onClick = onClick,
        modifier = modifier
            .widthIn(max = 104.dp)
            .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp),
        shape = OnboardingControlShape,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun StatusPill(label: String, connected: Boolean) {
    Surface(
        shape = MochiCapsule,
        color = statusPillBackground(connected)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = statusPillTextStyle(label.length),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            color = statusPillForeground(connected)
        )
    }
}

@Composable
private fun statusPillTextStyle(labelLength: Int): TextStyle {    if (labelLength >= 14) return MaterialTheme.typography.labelSmall
    return standardStatusPillTextStyle(labelLength)
}

@Composable
private fun standardStatusPillTextStyle(labelLength: Int): TextStyle {
    return if (labelLength >= 10) MaterialTheme.typography.labelMedium
    else MaterialTheme.typography.labelLarge
}

@Composable
private fun statusPillBackground(connected: Boolean): Color {
    val colors = MaterialTheme.colorScheme
    return if (connected) colors.primary.copy(alpha = 0.14f)
    else colors.outlineVariant.copy(alpha = 0.6f)
}

@Composable
private fun statusPillForeground(connected: Boolean): Color {
    val colors = MaterialTheme.colorScheme
    return if (connected) colors.primary else colors.onSurfaceVariant
}

@Composable
internal fun HintCard(body: String) {
    HintCardSurface { HintCardBody(body) }
}

@Composable
internal fun HintCard(title: String, body: String, content: @Composable ColumnScope.() -> Unit) {
    HintCardSurface {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        HintCardBody(body)
        content()
    }
}

@Composable
private fun HintCardBody(body: String) {
    Text(
        body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun HintCardSurface(content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    OnboardingGlassSurface(
        shape = OnboardingCardShape,
        color = colors.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

/**
 * Mochi 风格的单选圆点：细描边圈 + 选中后弹性鼓起的实心点。
 * 直接用 Material 的 RadioButton 会在初次设置里露出一块纯 Material 的圆形控件，
 * 和旁边 Mochi 的开关/滑块不是同一套手感，所以这里自绘一个。
 */
@Composable
internal fun MochiRadioDot(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val ringColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outline.copy(alpha = 0.6f),
        animationSpec = NeriMotion.smooth(),
        label = "onboarding_radio_ring"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = NeriMotion.bouncy(),
        label = "onboarding_radio_dot"
    )
    Box(
        modifier = modifier
            .size(22.dp)
            .border(width = 1.6.dp, color = ringColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (dotScale > 0.01f) {
            Box(
                Modifier
                    .size(11.dp)
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                        alpha = dotScale.coerceIn(0f, 1f)
                    }
                    .background(colors.primary, CircleShape)
            )
        }
    }
}

/** 顶部进度：Mochi 胶囊轨道 + 胶囊进度条，替代 Material 的线性进度条（带间隙和停靠点）。 */
@Composable
internal fun OnboardingProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val fraction = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(MochiCapsule)
            .background(colors.surfaceContainerHighest)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(MochiCapsule)
                .background(colors.primary)
        )
    }
}
