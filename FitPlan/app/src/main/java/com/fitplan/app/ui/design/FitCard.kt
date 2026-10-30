package com.fitplan.app.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.ui.theme.FitType

/**
 * 卡片：L1 表面（surface + 极淡 hairline 描边，不用阴影）。
 * onClick 非空时整卡可点（涟漪被 shape 裁剪），语义为 Button。
 *
 * @param contentPadding 传 PaddingValues(0.dp) 可让卡片内容贴边（如分组卡内的列表行）。
 */
@Composable
fun FitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(FitSpace.xl),
    shape: Shape = FitShapes.card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        contentColor = contentColorFor(containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        val clickModifier = if (onClick != null) {
            Modifier.clickable(role = Role.Button, onClick = onClick)
        } else {
            Modifier
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickModifier)
                .padding(contentPadding),
            content = content,
        )
    }
}

/** 区块标题行：标题 + 可选副标题 + 可选尾部动作。上间距 24dp、下间距 12dp。 */
@Composable
fun FitSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = FitSpace.xxl, bottom = FitSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.padding(start = FitSpace.sm))
            trailing()
        }
    }
}

/** 1dp hairline 分隔线（outlineVariant，默认 0.6 alpha）。 */
@Composable
fun FitHairline(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

/** 页面大标题：eyebrow + headlineLarge 标题 + 可选副标题。 */
@Composable
fun FitScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (eyebrow != null) {
            Text(
                text = eyebrow,
                style = FitType.eyebrow,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(FitSpace.xs))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(FitSpace.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
