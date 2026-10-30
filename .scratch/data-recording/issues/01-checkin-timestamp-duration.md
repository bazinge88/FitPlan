# 01 — 打卡时刻与训练用时历史

**What to build:** 每组打卡自动记录时刻（用户无感）；训练用时 = 某天首组打卡到末组打卡的时间差，按「天 + 训练者」留存，周计划页每天可回看；中途结束的训练同样产生用时，无任何打卡则不产生；完成通知的用时提示行为不变；历史打卡数据（无时刻）不参与、不报错。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 打卡任一组后该组带打卡时刻；重复打卡/调整次数时时刻刷新为最新一次
- [x] 周计划页训练日显示当日用时；无打卡日不显示；部分完成（中途结束）也显示
- [x] 用时口径与完成通知一致（「H 小时 M 分钟」「已进行不到 1 分钟」同源）；单组约 0 分钟
- [x] 历史无时刻打卡不参与用时推导、不崩溃
- [x] 数据库非破坏迁移（以当时库版本号为准追加）；多训练者用时互不干扰
- [x] 用时推导为顶层纯函数并有边界测试（无打卡/单组/跨小时/中途结束/历史无时刻）
- [x] `./gradlew :app:testDebugUnitTest :app:assembleDebug` 全绿，零新增警告

## 实施备注（2026-10-02，补记）

- 迁移：v7→v8 非破坏迁移（`AppDatabase.kt` MIGRATION_7_8），SQL 为 `ALTER TABLE set_checkins ADD COLUMN checkedAt INTEGER`，仅加可空列，不动既有行。
- 字段语义：`SetCheckinEntity.checkedAt: Long? = null`（epoch millis），打卡/重复打卡/调整次数时刷新为最新一次；历史行 checkedAt 为 null，即「无时刻的历史打卡」口径。
- 用时推导为 `util/TrainingDuration.kt` 顶层纯函数四件套（buildTrainingDurations/durationsForUser/durationWords/durationText）：`buildTrainingDurations`（按 天+训练者 分组，取首末打卡时刻差即用时；无打卡不产生、单组约 0 分钟、无时刻的行 mapNotNull 排除故不参与也不报错；结果按 date、userId 升序）、`durationsForUser`（按训练者投影为 date→毫秒映射，供周计划页）、`durationText`（分钟向下取整：< 1 分钟「不到 1 分钟」，否则「X 分钟」/「H 小时 M 分钟」，与通知口径同源）。
- 写侧两条打卡路径都带时刻：主页 `WorkoutViewModel.checkIn` 新行写 `checkedAt = now`，通知 `SessionActionReceiver.quickCheckIn` 同样写 `checkedAt = now`；重复打卡（`updateRepsIfPresent`）与调整次数（`adjustReps`）的 DAO SQL 均同步刷新 checkedAt。
- 读侧使用点：周计划页 `PlanViewModel.durations` = `durationsForUser(buildTrainingDurations(checkins))`；进行中通知用时 `WorkoutSessionManager.formatElapsed` 走 `durationText`；完成通知的 `formatTotal` 沿用历史文案、刻意不走 durationText（其「不到 1 分钟」分支会改变既有文案）。
- 测试覆盖：`util/TrainingDurationTest.kt` 7 个用例（无打卡不产生 / 单组约 0 分钟 / 首末时刻差跨小时且中途结束成立 / 历史无时刻不参与不崩溃 / 多训练者互不干扰 / durationsForUser 投影 / durationText 与通知口径同源）。
- 本备注系收尾补记，落地在上轮完成。
