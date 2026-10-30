package com.fitplan.app

import android.app.Application
import com.fitplan.app.data.db.AllDaos
import com.fitplan.app.data.db.AppDatabase
import com.fitplan.app.data.db.SettingsEntity
import com.fitplan.app.data.db.UserEntity
import com.fitplan.app.session.WorkoutSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 全局依赖图：单 Activity + Room，无网络依赖。 */
object Graph {
    lateinit var daos: AllDaos
        private set
    lateinit var sessionManager: WorkoutSessionManager
        private set

    fun init(daos: AllDaos, sessionManager: WorkoutSessionManager) {
        this.daos = daos
        this.sessionManager = sessionManager
    }
}

class FitPlanApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.build(this)
        val sessionManager = WorkoutSessionManager(this, db, appScope)
        Graph.init(db, sessionManager)
        // 进程启动时接续当天会话（内部自行在其 scope 上协程化）
        sessionManager.onAppStart()
        appScope.launch {
            // 保证设置行始终存在
            if (db.settingsDao.get() == null) {
                db.settingsDao.upsert(SettingsEntity())
            }
            // 保证至少一名训练者（新装库 users 为空；迁移库已播种「我」id=1）
            if (db.userDao.getAll().isEmpty()) {
                db.userDao.upsert(UserEntity(name = "我", sort = 0))
            }
        }
    }
}
