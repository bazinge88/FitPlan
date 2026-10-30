package com.fitplan.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/*
 * 圆角、间距、尺寸 token（docs/iteration12-plan.md §3.3/§3.4）。
 * 屏幕代码只能引用这里的 token，禁止裸写 dp 圆角。
 */

object FitShapes {
    /** 卡片（24dp）。 */
    val card: Shape = RoundedCornerShape(24.dp)

    /** 首页 hero / 大表面（28dp）。 */
    val hero: Shape = RoundedCornerShape(28.dp)

    /** 对话框（28dp）。 */
    val dialog: Shape = RoundedCornerShape(28.dp)

    /** 底部弹层（顶部 28dp）。 */
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** 小 chip（12dp）。 */
    val chip: Shape = RoundedCornerShape(12.dp)

    /** 胶囊（按钮 / 状态标签）。 */
    val pill: Shape = RoundedCornerShape(percent = 50)

    /** 正圆（组圈 / 聚合圈 / 圆形图标容器）。 */
    val circle: Shape = CircleShape

    /** 输入框（16dp）。 */
    val field: Shape = RoundedCornerShape(16.dp)

    /** 小控件（8dp）。 */
    val small: Shape = RoundedCornerShape(8.dp)

    /** 同步给 MaterialTheme 的形状表。 */
    val material: Shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
}

/** 4dp 基准间距。页面 gutter 20dp。 */
object FitSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp

    /** 页面水平 gutter / 卡片内边距。 */
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
}

/** 关键尺寸。触摸目标不小于 48dp，列表行不小于 56dp。 */
object FitSize {
    val gutter = 20.dp
    val rowMin = 56.dp
    val touchMin = 48.dp
    val icon = 24.dp
    val iconSmall = 18.dp
    val setCircle = 40.dp
    val fab = 56.dp

    /** 底部导航内容高度（不含系统导航栏 inset）。 */
    val bottomBar = 72.dp

    /** 顶栏内容高度（不含状态栏 inset）。 */
    val topBar = 64.dp

    /** 主按钮高度。 */
    val button = 52.dp

    /** 空态图标容器。 */
    val emptyIcon = 72.dp

    /** 动作聚合圈直径。 */
    val aggregateCircle = 32.dp

    /** 聚合圈内小对勾。 */
    val checkIcon = 18.dp

    /** 组圈内对勾。 */
    val checkIconLarge = 20.dp

    /** 描边线宽（组圈 / 聚合圈等）。 */
    val border = 2.dp

    /** 进度环直径。 */
    val ring = 96.dp

    /** 进度环线宽。 */
    val ringStroke = 10.dp
}
