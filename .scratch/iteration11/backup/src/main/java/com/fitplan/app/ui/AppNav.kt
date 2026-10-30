package com.fitplan.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitplan.app.ui.combo.ComboEditScreen
import com.fitplan.app.ui.combo.ComboListScreen
import com.fitplan.app.ui.exercise.ExerciseEditScreen
import com.fitplan.app.ui.exercise.ExerciseListScreen
import com.fitplan.app.ui.home.HomeScreen
import com.fitplan.app.ui.log.LogScreen
import com.fitplan.app.ui.plan.PlanScreen
import com.fitplan.app.ui.plan.WorkoutScreen
import com.fitplan.app.ui.pr.PrScreen
import com.fitplan.app.ui.settings.SettingsScreen
import com.fitplan.app.util.TimeUtil

private data class Tab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("主页", Icons.Filled.Home),
    Tab("计划", Icons.Filled.EditCalendar),
    Tab("组合", Icons.AutoMirrored.Filled.List),
    Tab("动作", Icons.Filled.FitnessCenter),
    Tab("记录", Icons.Filled.History),
    Tab("设置", Icons.Filled.Settings),
)

@Composable
fun AppNav() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
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
        ) { entry ->
            val exerciseId = entry.arguments?.getLong("exerciseId") ?: -1L
            ExerciseEditScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(route = "pr") {
            PrScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun MainScaffold(
    onEditCombo: (Long) -> Unit,
    onEditExercise: (Long) -> Unit,
    onOpenPr: () -> Unit = {},
    onOpenWorkout: (Long) -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            0 -> HomeScreen(modifier)
            1 -> PlanScreen(modifier, onOpenWorkout = onOpenWorkout)
            2 -> ComboListScreen(modifier, onEditCombo)
            3 -> ExerciseListScreen(modifier, onEditExercise, onOpenPr)
            4 -> LogScreen(modifier)
            else -> SettingsScreen(modifier)
        }
    }
}
