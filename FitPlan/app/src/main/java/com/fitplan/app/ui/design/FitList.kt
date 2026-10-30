package com.fitplan.app.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace

/** 分组卡内行序号（用于自动行间 hairline：第 0 行不画分隔线）。 */
private class FitSectionRowCounter {
    var next = 0
}

private val LocalFitSectionRowCounter = staticCompositionLocalOf<FitSectionRowCounter?> { null }

/**
 * 分组卡：卡片表面 + 行间 hairline（行由 [FitListRow] 或直接内容构成）。
 * 行直接放在 FitSectionCard 内时自动省略第一行上方的分隔线。
 */
@Composable
fun FitSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val counter = remember { FitSectionRowCounter() }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FitShapes.card,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        CompositionLocalProvider(LocalFitSectionRowCounter provides counter) {
            Column(modifier = Modifier.fillMaxWidth()) {
                counter.next = 0
                content()
            }
        }
    }
}

/**
 * 分组卡内的一行：标题 + 可选说明 + 可选前导/尾部内容。
 * 行高最小 56dp；onClick 非空时整行可点（读屏角色默认 Button，可由 [role] 覆盖为 RadioButton 等；
 * 例如单选列表把 role 设为 Role.RadioButton，并把 leading RadioButton 的 onClick 传 null 避免双目标）。
 * [role] 仅在 onClick 非空时生效。
 */
@Composable
fun FitListRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    role: Role? = Role.Button,
) {
    val counter = LocalFitSectionRowCounter.current
    val sectionIndex = counter?.next
    if (counter != null) {
        counter.next = (sectionIndex ?: 0) + 1
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (sectionIndex != null && sectionIndex > 0) {
            FitHairline(modifier = Modifier.padding(start = FitSpace.lg))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FitSize.rowMin)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = role, onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = FitSpace.lg, vertical = FitSpace.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(FitSpace.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supporting != null) {
                    Spacer(modifier = Modifier.height(FitSpace.xxs))
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(FitSpace.md))
                trailing()
            }
        }
    }
}
