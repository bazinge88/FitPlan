package com.fitplan.app.ui.design

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace

/** 按钮风格：主操作 / 次要（tonal）/ 文字（ghost）/ 危险操作。 */
enum class FitButtonStyle { Primary, Tonal, Ghost, Destructive }

/** 品牌按钮：胶囊形，高度 52dp，主操作用 Primary。 */
@Composable
fun FitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: FitButtonStyle = FitButtonStyle.Primary,
    leadingIcon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    val disabledContainer = colors.onSurface.copy(alpha = 0.12f)
    val disabledContent = colors.onSurface.copy(alpha = 0.38f)
    val buttonColors = when (style) {
        FitButtonStyle.Primary -> ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent,
        )
        FitButtonStyle.Tonal -> ButtonDefaults.buttonColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent,
        )
        FitButtonStyle.Ghost -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = colors.primary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContent,
        )
        FitButtonStyle.Destructive -> ButtonDefaults.buttonColors(
            containerColor = colors.error,
            contentColor = colors.onError,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent,
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = FitSize.button),
        enabled = enabled,
        shape = FitShapes.pill,
        colors = buttonColors,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = FitSpace.xxl, vertical = FitSpace.md),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(FitSize.iconSmall),
            )
            Spacer(modifier = Modifier.width(FitSpace.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 图标按钮：触摸目标 48dp；enabled=false 时 tint 降级且不响应点击（语义为 disabled）。 */
@Composable
fun FitIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(FitSize.touchMin),
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(FitSize.icon),
        )
    }
}

/** 文字按钮（次级操作）：48dp 触摸目标，destructive 时用 error 色。 */
@Composable
fun FitTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = FitSize.touchMin),
        enabled = enabled,
        shape = FitShapes.pill,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (destructive) colors.error else colors.primary,
            disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(horizontal = FitSpace.lg, vertical = FitSpace.sm),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
