package com.fitplan.app.session

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fitplan.app.R
import com.fitplan.app.data.db.AllDaos
import com.fitplan.app.data.db.WorkoutSessionEntity
import com.fitplan.app.ui.plan.WorkoutUiState
import com.fitplan.app.ui.plan.buildWorkoutUiState
import com.fitplan.app.ui.plan.isWorkoutAllDone
import com.fitplan.app.ui.plan.workoutFlowBundle
import com.fitplan.app.util.TimeUtil
import com.fitplan.app.util.durationText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 训练会话：开始/结束/通知渲染。常驻通知 + 进程内观察者 + 广播接收器（见 ADR-0002，无前台服务）。
 * 会话行（workout_session）是权威来源：行存在 = 会话进行中；通知只是它的视图。
 */
class WorkoutSessionManager(
    private val context: Context,
    private val daos: AllDaos,
    private val scope: CoroutineScope,
) {
    private val _activeDate = MutableStateFlow<Long?>(null)

    /** 当前进行中的训练会话日期；null = 无会话。 */
    val activeDate: StateFlow<Long?> = _activeDate

    private var collectJob: Job? = null
    private var sessionObserveJob: Job? = null

    /** 会话行观察缓存的首组打卡时刻（epoch millis）；null = 尚未打卡或无会话。 */
    private val _startedAt = MutableStateFlow<Long?>(null)

    /** 开始训练：写会话标记并挂常驻通知、开始实时刷新。 */
    fun start(date: Long) {
        scope.launch { daos.workoutSessionDao.upsert(WorkoutSessionEntity(date = date)) }
        attach(date)
    }

    /** 结束会话（保留打卡数据）并撤下常驻通知。 */
    fun stop() {
        scope.launch { daos.workoutSessionDao.delete() }
        detach()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** 进程启动时接续：当天会话重新挂通知；隔天残留自动清理。 */
    fun onAppStart() {
        scope.launch {
            val session = daos.workoutSessionDao.get() ?: return@launch
            val today = TimeUtil.today().toEpochDay()
            if (session.date == today) {
                attach(today)
            } else {
                daos.workoutSessionDao.delete()
                // 隔天残留清理：常驻通知（1001）与完成提示（1002）一并撤下
                val notifications = NotificationManagerCompat.from(context)
                notifications.cancel(NOTIFICATION_ID)
                notifications.cancel(DONE_NOTIFICATION_ID)
            }
        }
    }

    /** 接上实时刷新（幂等）：通知按钮的广播在进程被拉起后调用。 */
    fun attachIfNeeded(date: Long) {
        if (_activeDate.value != date || collectJob?.isActive != true) {
            attach(date)
        }
    }

    private fun attach(date: Long) {
        detach()
        _activeDate.value = date
        // 并行观察会话行，缓存 startedAt 供通知文案读取（首条通知可能短暂缺用时，ADR-0002 陈旧自愈）
        sessionObserveJob = scope.launch {
            daos.workoutSessionDao.observe().collect { _startedAt.value = it?.startedAt }
        }
        collectJob = scope.launch {
            combine(
                daos.dayPlanDao.observeAll(),
                daos.comboDao.observeAllWithItems(),
                daos.exerciseDao.observeAll(),
                daos.setCheckinDao.observeForDate(date),
                // 设置、当次重量、逃标与体重先并一组，再并入两轨备注（与 ViewModel 共用 workoutFlowBundle）
                workoutFlowBundle(daos, date),
            ) { plans, combos, exercises, checkins, bundle ->
                buildWorkoutUiState(date, plans, combos, exercises, checkins, bundle)
            }.collect { state ->
                render(state)
            }
        }
    }

    private fun detach() {
        sessionObserveJob?.cancel()
        sessionObserveJob = null
        collectJob?.cancel()
        collectJob = null
        _activeDate.value = null
        _startedAt.value = null
    }

    private suspend fun render(state: WorkoutUiState) {
        if (
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (state.comboName == null) {
            // 计划被删除等极端情况：没有可跟踪的内容，直接结束会话
            stop()
            return
        }
        if (state.items.isEmpty()) {
            // 空组合（有组合但无条目）：0/0 通知无内容可跟踪，直接结束会话并撤通知（L02）
            stop()
            return
        }
        val manager = NotificationManagerCompat.from(context)
        ensureChannel()
        val allDone = isWorkoutAllDone(state.items)
        if (allDone) {
            // 今日完成：撤下常驻通知，留一条可滑掉的完成提示，会话自动结束
            manager.cancel(NOTIFICATION_ID)
            manager.notify(DONE_NOTIFICATION_ID, doneNotification(state, resolveStartedAt()))
            stop()
            return
        }
        manager.notify(NOTIFICATION_ID, activeNotification(state))
    }

    /**
     * 完成通知一次性发出、不会自愈：观察缓存尚未就绪（进程冷启动竞态）时，
     * 从会话行兜底读取首组打卡时刻，非空则顺手写回缓存。
     */
    private suspend fun resolveStartedAt(): Long? {
        _startedAt.value?.let { return it }
        val startedAt = daos.workoutSessionDao.get()?.startedAt
        if (startedAt != null) {
            _startedAt.value = startedAt
        }
        return startedAt
    }

    private fun activeNotification(state: WorkoutUiState): Notification {
        val totalDone = state.items.sumOf { it.doneCount }
        val totalRows = state.items.sumOf { it.sets.size }
        // 不用 kotlin.apply{}：androidx.core 1.17 起 Style.apply(...) 成为公开成员函数并抢先解析，
        // 会使 lambda 内失去 InboxStyle 接收者、表达式退化为 Unit
        val style = NotificationCompat.InboxStyle()
        state.items.forEach { item ->
            style.addLine("${item.exerciseName} ${item.doneCount}/${item.sets.size} 组")
        }
        style.setSummaryText("已完成 $totalDone/$totalRows 组")
        val pendingSync = state.items.any { it.syncPromptWeight != null }
        val baseText = if (pendingSync) "有重量待同步，点击进入" else "已完成 $totalDone/$totalRows 组"
        // 首组打卡后追加用时；尚未打卡（startedAt == null）不加计时文案
        val contentText = _startedAt.value
            ?.let { "$baseText · ${formatElapsed(System.currentTimeMillis() - it)}" }
            ?: baseText
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("训练进行中 · ${state.comboName}")
            .setContentText(contentText)
            .setStyle(style)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .addAction(0, "直接打卡", actionIntent(ACTION_QUICK_CHECKIN, REQUEST_CHECKIN))
            .addAction(0, "逃了", actionIntent(ACTION_SKIP, REQUEST_SKIP))
            .build()
    }

    private fun doneNotification(state: WorkoutUiState, startedAt: Long?): Notification {
        val baseText = "${state.comboName} · 全部组已打卡"
        // 从未打卡（startedAt == null）的会话不产生总用时文案
        val contentText = startedAt
            ?.let { "$baseText · ${formatTotal(System.currentTimeMillis() - it)}" }
            ?: baseText
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("今日训练完成 ✓")
            .setContentText(contentText)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
    }

    /** 进行中通知的用时文案（分钟向下取整；与周计划页用时同源，见 durationText）。 */
    private fun formatElapsed(millis: Long): String = durationText("已进行", millis)

    /** 完成通知的总用时文案（分钟向下取整）。文案沿用既有行为，不走 durationText（其「不到 1 分钟」分支会改变历史文案）。 */
    private fun formatTotal(millis: Long): String =
        when {
            millis < 3_600_000L -> "总用时 ${millis / 60_000L} 分钟"
            else -> "总用时 ${millis / 3_600_000L} 小时 ${(millis % 3_600_000L) / 60_000L} 分钟"
        }

    private fun contentIntent(): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_CONTENT,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun actionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, SessionActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "训练进行中", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "workout_session"
        const val NOTIFICATION_ID = 1001
        const val DONE_NOTIFICATION_ID = 1002
        const val ACTION_QUICK_CHECKIN = "com.fitplan.app.action.QUICK_CHECKIN"
        const val ACTION_SKIP = "com.fitplan.app.action.SKIP"
        private const val REQUEST_CONTENT = 1
        private const val REQUEST_CHECKIN = 2
        private const val REQUEST_SKIP = 3
    }
}
