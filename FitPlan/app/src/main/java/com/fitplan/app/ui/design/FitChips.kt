package com.fitplan.app.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.ui.theme.fitColors

/** 徽标色调：中性 / 主色 / PR 暖琥珀 / 完成态。 */
enum class FitChipTone { Neutral, Primary, Tertiary, Success }

@Composable
private fun chipColors(tone: FitChipTone): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    val extra = MaterialTheme.fitColors
    return when (tone) {
        FitChipTone.Neutral -> colors.surfaceContainerHighest to colors.onSurfaceVariant
        FitChipTone.Primary -> colors.primaryContainer to colors.onPrimaryContainer
        FitChipTone.Tertiary -> colors.tertiaryContainer to colors.onTertiaryContainer
        FitChipTone.Success -> extra.successContainer to extra.onSuccessContainer
    }
}

/** 元信息徽标（动作摘要、字段等）：胶囊底色 + labelMedium。 */
@Composable
fun FitMetaChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: FitChipTone = FitChipTone.Neutral,
    leadingIcon: ImageVector? = null,
) {
    val (container, content) = chipColors(tone)
    Row(
        modifier = modifier
            .clip(FitShapes.pill)
            .background(container)
            .padding(horizontal = FitSpace.sm + FitSpace.xxs, vertical = FitSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(FitSpace.xs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 状态胶囊（今天/休息日/已完成等）：带圆点前缀，用于列表右侧状态。 */
@Composable
fun FitStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    tone: FitChipTone = FitChipTone.Neutral,
) {
    val (container, content) = chipColors(tone)
    Row(
        modifier = modifier
            .clip(FitShapes.pill)
            .background(container)
            .padding(horizontal = FitSpace.sm + FitSpace.xxs, vertical = FitSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(content),
        )
        Spacer(modifier = Modifier.width(FitSpace.xs + FitSpace.xxs))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
