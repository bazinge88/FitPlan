package com.fitplan.app.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fitplan.app.Graph
import com.fitplan.app.data.db.SetCheckinEntity
import com.fitplan.app.data.db.SetSkipEntity
import com.fitplan.app.ui.plan.DEFAULT_USER_ID
import com.fitplan.app.util.QuickCheckIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 常驻通知按钮的广播接收器：直接打卡 / 逃了（见 ADR-0002，无前台服务）。
 * 广播可能在进程被拉起后到达，用 goAsync + IO 协程完成数据库读写后再 finish。
 */
class SessionActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    WorkoutSessionManager.ACTION_QUICK_CHECKIN -> quickCheckIn()
                    WorkoutSessionManager.ACTION_SKIP -> skip()
                }
            } catch (e: Exception) {
                // 兜底：广播处理失败只记日志，不炸进程（通知仍在，可重试）
                Log.e("SessionActionReceiver", "session action failed", e)
            } finally {
                result.finish()
            }
        }
    }

    /** 直接打卡：写入当前训练者的下一个应打卡的组，并接上实时刷新让通知重渲染（首组打卡顺带记用时起点）。 */
    private suspend fun quickCheckIn() {
        val session = Graph.daos.workoutSessionDao.get() ?: return
        val plan = Graph.daos.dayPlanDao.getByDate(session.date) ?: return
        if (plan.isRest || plan.comboId == null) return
        val combo = Graph.daos.comboDao.getWithItems(plan.comboId) ?: return
        val userId = Graph.daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID
        // 本地多训练者：直接打卡只推进当前训练者的进度（逃标同样按训练者隔离）
        val checkins = Graph.daos.setCheckinDao.observeForDate(session.date).first()
            .filter { it.userId == userId }
        val skips = Graph.daos.setSkipDao.observeForDate(session.date).first()
            .filter { it.userId == userId }
        val next = QuickCheckIn.nextSet(combo.items.sortedBy { it.sort }, checkins, skips)
        if (next == null) {
            // 已全部练完：接上观察者，让 render 走完成分支
            Graph.sessionManager.attachIfNeeded(session.date)
            return
        }
        // 首组打卡写用时起点（与该组打卡时刻同源同值）
        val now = System.currentTimeMillis()
        if (session.startedAt == null) {
            Graph.daos.workoutSessionDao.upsert(session.copy(startedAt = now))
        }
        // 快照式重量：与主页打卡同口径——该训练者该动作当天的当次重量快照进该组；没记过则 weight=null（只记次数）
        val exerciseId = combo.items.find { it.id == next.comboItemId }?.exerciseId
        val weight = exerciseId?.let { id ->
            Graph.daos.sessionWeightDao.observeAll().first()
                .find { it.date == session.date && it.exerciseId == id && it.userId == userId }?.weight
        }
        Graph.daos.setCheckinDao.upsert(
            SetCheckinEntity(
                date = session.date,
                comboItemId = next.comboItemId,
                setIndex = next.setIndex,
                reps = next.reps,
                weight = weight,
                userId = userId,
                checkedAt = now,
            ),
        )
        Graph.sessionManager.attachIfNeeded(session.date)
    }

    /** 逃了：当前组（直接打卡指针指向的组）标为逃组并结束会话，已打卡内容保留；无会话（残留通知）时只撤通知。 */
    private suspend fun skip() {
        val session = Graph.daos.workoutSessionDao.get()
        if (session == null) {
            Graph.sessionManager.stop()
            return
        }
        val plan = Graph.daos.dayPlanDao.getByDate(session.date)
        val comboId = plan?.takeIf { !it.isRest }?.comboId
        val combo = comboId?.let { Graph.daos.comboDao.getWithItems(it) }
        if (combo != null) {
            val userId = Graph.daos.settingsDao.get()?.activeUserId ?: DEFAULT_USER_ID
            // 与直接打卡同一指针：当前组 = 第一个既未打卡也未逃的组，只逃这一组，其余组保持未练
            val checkins = Graph.daos.setCheckinDao.observeForDate(session.date).first()
                .filter { it.userId == userId }
            val skips = Graph.daos.setSkipDao.observeForDate(session.date).first()
                .filter { it.userId == userId }
            val next = QuickCheckIn.nextSet(combo.items.sortedBy { it.sort }, checkins, skips)
            if (next != null) {
                Graph.daos.setSkipDao.upsert(
                    SetSkipEntity(
                        date = session.date,
                        comboItemId = next.comboItemId,
                        setIndex = next.setIndex,
                        userId = userId,
                    ),
                )
            }
        }
        Graph.sessionManager.stop()
    }
}
