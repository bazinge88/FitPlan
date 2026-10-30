package com.fitplan.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitplan.app.ui.combo.ComboEditScreen
import com.fitplan.app.ui.combo.ComboListScreen
import com.fitplan.app.ui.design.FitBottomBar
import com.fitplan.app.ui.design.FitScaffold
import com.fitplan.app.ui.design.FitTabSpec
import com.fitplan.app.ui.exercise.ExerciseEditScreen
import com.fitplan.app.ui.exercise.ExerciseListScreen
import com.fitplan.app.ui.home.HomeScreen
import com.fitplan.app.ui.log.LogScreen
import com.fitplan.app.ui.plan.PlanScreen
import com.fitplan.app.ui.plan.WorkoutScreen
import com.fitplan.app.ui.pr.PrScreen
import com.fitplan.app.ui.settings.SettingsScreen
import com.fitplan.app.ui.theme.fitMotion
import com.fitplan.app.util.TimeUtil

/** 底部 6 Tab（选中 filled / 未选中 outlined，文案与顺序不变）。 */
private val TABS = listOf(
    FitTabSpec(label = "主页", selectedIcon = Icons.Filled.Home, unselectedIcon = Icons.Outlined.Home),
    FitTabSpec(label = "计划", selectedIcon = Icons.Filled.EditCalendar, unselectedIcon = Icons.Outlined.EditCalendar),
    FitTabSpec(
        label = "组合",
        selectedIcon = Icons.AutoMirrored.Filled.List,
        unselectedIcon = Icons.AutoMirrored.Outlined.List,
    ),
    FitTabSpec(
        label = "动作",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter,
    ),
    FitTabSpec(label = "记录", selectedIcon = Icons.Filled.History, unselectedIcon = Icons.Outlined.History),
    FitTabSpec(label = "设置", selectedIcon = Icons.Filled.Settings, unselectedIcon = Icons.Outlined.Settings),
)

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val motion = MaterialTheme.fitMotion

    // 主路由只做淡入淡出（减少动态时统一 120ms 纯淡入）。
    val pageFadeIn: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = if (motion.enabled) motion.medium else 120,
            easing = motion.standard,
        ),
    )
    val pageFadeOut: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = if (motion.enabled) motion.short else 120,
            easing = motion.standard,
        ),
    )
    // 详情路由：推入 slideIn(1/5) + fade；返回 slideOut(1/5) + fade。
    val detailEnter: EnterTransition = if (motion.enabled) {
        slideInHorizontally(
            animationSpec = tween(durationMillis = motion.medium, easing = motion.emphasizedDecel),
        ) { fullWidth -> fullWidth / 5 } + fadeIn(
            animationSpec = tween(durationMillis = motion.medium, easing = motion.standard),
        )
    } else {
        pageFadeIn
    }
    val detailPopExit: ExitTransition = if (motion.enabled) {
        slideOutHorizontally(
            animationSpec = tween(durationMillis = motion.medium, easing = motion.emphasizedAccel),
        ) { fullWidth -> fullWidth / 5 } + fadeOut(
            animationSpec = tween(durationMillis = motion.short),
        )
    } else {
        pageFadeOut
    }

    NavHost(
        navController = navController,
        startDestination = "main",
        enterTransition = { pageFadeIn },
        exitTransition = { pageFadeOut },
        popEnterTransition = { pageFadeIn },
        popExitTransition = { pageFadeOut },
    ) {
        composable(
            route = "main",
            enterTransition = { pageFadeIn },
            exitTransition = { pageFadeOut },
            popEnterTransition = { pageFadeIn },
            popExitTransition = { pageFadeOut },
        ) {
            MainScaffold(
                onEditCombo = { id -> navController.navigate("combo/$id") },
                onEditExercise = { id -> navController.navigate("exercise/$id") },
                onOpenPr = { navController.navigate("pr") },
                onOpenWorkout = { date -> navController.navigate("workout/$date") },
            )
        }
        composable(
            route = "workout/{date}",
            arguments = listOf(navArgument("date") { type = NavType.LongType }),
            enterTransition = { detailEnter },
            popExitTransition = { detailPopExit },
        ) { entry ->
            val date = entry.arguments?.getLong("date") ?: TimeUtil.today().toEpochDay()
            WorkoutScreen(
                date = date,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "combo/{comboId}",
            arguments = listOf(navArgument("comboId") { type = NavType.LongType }),
            enterTransition = { detailEnter },
            popExitTransition = { detailPopExit },
        ) { entry ->
            val comboId = entry.arguments?.getLong("comboId") ?: -1L
            ComboEditScreen(
                comboId = comboId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "exercise/{exerciseId}",
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
            enterTransition = { detailEnter },
            popExitTransition = { detailPopExit },
        ) { entry ->
            val exerciseId = entry.arguments?.getLong("exerciseId") ?: -1L
            ExerciseEditScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "pr",
            enterTransition = { detailEnter },
            popExitTransition = { detailPopExit },
        ) {
            PrScreen(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * 主壳（签名保持不变）：FitBottomBar + Tab 切换 AnimatedContent + 统一 inset。
 *
 * 系统栏 inset 由本壳处理：每个 Tab 页拿到的 modifier 已包含 top/horizontal inset 与
 * bottomBar 高度，并通过 consumeWindowInsets 向下传递已消费状态，页面无需再补状态栏 padding。
 */
@Composable
fun MainScaffold(
    onEditCombo: (Long) -> Unit,
    onEditExercise: (Long) -> Unit,
    onOpenPr: () -> Unit = {},
    onOpenWorkout: (Long) -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val motion = MaterialTheme.fitMotion
    FitScaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FitBottomBar(
                tabs = TABS,
                selectedIndex = tab,
                onSelect = { index -> tab = index },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (!motion.enabled) {
                    fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                } else {
                    (
                        fadeIn(tween(motion.medium, easing = motion.standard)) +
                            scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(motion.medium, easing = motion.standard),
                            )
                        ) togetherWith fadeOut(tween(motion.short))
                }
            },
            label = "mainTabContent",
        ) { currentTab ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Tab 页 modifier 已含系统栏 top/horizontal inset 与底部导航高度；
                // 同时消费已用掉的 inset，页面内不要再自建 Scaffold 补状态栏 padding。
                val screenModifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                when (currentTab) {
                    // 主页休息日 CTA：一键切到「计划」Tab 安排当天训练
                    0 -> HomeScreen(screenModifier, onOpenPlan = { tab = 1 })
                    1 -> PlanScreen(screenModifier, onOpenWorkout = onOpenWorkout)
                    2 -> ComboListScreen(screenModifier, onEditCombo)
                    3 -> ExerciseListScreen(screenModifier, onEditExercise, onOpenPr)
                    4 -> LogScreen(screenModifier)
                    else -> SettingsScreen(screenModifier)
                }
            }
        }
    }
}
