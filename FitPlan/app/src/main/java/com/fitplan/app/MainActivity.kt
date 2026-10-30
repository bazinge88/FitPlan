package com.fitplan.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fitplan.app.ui.AppNav
import com.fitplan.app.ui.theme.FitPlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 边到边：内容延伸到状态栏/导航栏下方，系统栏透明；
        // 图标明暗在 FitPlanTheme 内随深色/浅色切换（enableEdgeToEdge 的 auto 也做一次兜底）。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            FitPlanTheme {
                AppNav()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 回到前台时幂等接续当天会话 / 清理隔天残留通知（会话行是权威状态，重复调用安全）
        Graph.sessionManager.onAppStart()
    }
}
