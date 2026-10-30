package com.fitplan.app.ui.design

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import com.fitplan.app.ui.theme.FitShapes
import com.fitplan.app.ui.theme.FitSize
import com.fitplan.app.ui.theme.FitSpace
import com.fitplan.app.ui.theme.fitMotion

/** 底部导航的一个 Tab：选中/未选中图标（filled/outlined）。 */
data class FitTabSpec(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * 底部导航（6 Tab）：选中项 filled 图标 + 胶囊指示器滑动动画 + 轻触反馈。
 * 自身处理系统导航栏 inset；内容高度 72dp。
 */
@Composable
fun FitBottomBar(
    tabs: List<FitTabSpec>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    val motion = MaterialTheme.fitMotion
    val colors = MaterialTheme.colorScheme
    val haptics = rememberFitHaptics()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        shadowElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FitSize.bottomBar)
                    .selectableGroup(),
            ) {
                val itemWidth = maxWidth / tabs.size
                val pillWidth = itemWidth * 0.72f
                val pillHeight = 32.dp
                val targetOffset = itemWidth * selectedIndex.toFloat() + (itemWidth - pillWidth) / 2f
                val indicatorOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = if (motion.enabled) {
                        tween(durationMillis = motion.medium, easing = motion.standard)
                    } else {
                        snap()
                    },
                    label = "fitBottomBarIndicator",
                )
                Box(
                    modifier = Modifier
                        .size(width = pillWidth, height = pillHeight)
                        // 指示器只改绘制层位移：切换 Tab 时不触发布局，也不引起整条导航栏重组
                        .graphicsLayer {
                            translationX = indicatorOffset.toPx()
                            translationY = FitSpace.sm.toPx()
                        }
                        .background(color = colors.primaryContainer, shape = FitShapes.pill),
                )
                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, tab ->
                        FitBottomBarItem(
                            tab = tab,
                            selected = index == selectedIndex,
                            onClick = {
                                if (index != selectedIndex) {
                                    haptics.tap()
                                    onSelect(index)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun FitBottomBarItem(
    tab: FitTabSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = MaterialTheme.fitMotion
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = FitSpace.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.Center) {
            Crossfade(
                targetState = selected,
                animationSpec = if (motion.enabled) tween(motion.short) else snap(),
                label = "fitBottomBarIcon",
            ) { isSelected ->
                Icon(
                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = null,
                    tint = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    modifier = Modifier.size(FitSize.icon),
                )
            }
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.primary else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}
