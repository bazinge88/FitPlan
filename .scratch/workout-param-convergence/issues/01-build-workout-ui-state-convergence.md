# 01 — buildWorkoutUiState 参数收敛（WorkoutFlowBundle 直传）

**What to build:** 唯一状态构建路径 `buildWorkoutUiState` 的签名从 11 个位置参数收敛为「5 个基础入参 + 1 个既有 `WorkoutFlowBundle` 载体」，ViewModel 与通知路径两处调用点的逐字位置摊平消失（含 settings 插第 7 位全靠位置对齐的错序隐患）；同时补上该纯函数的首批直接单元测试，钉住休息日早退分支与正常分支的既有行为。**纯重构、零行为变化。**

**Blocked by:** None — can start immediately（与 02 无文件交集；单工作树 + gradle 串行惯例，建议按编号串行执行）

**Status:** done

- [x] 签名收敛为 6 参：date/plans/combos/exercises/checkins + flow（WorkoutFlowBundle）；函数体既有判定与产出逐字不变（以解构或等价方式接入 bundle 字段，不重排任何逻辑）
- [x] WorkoutViewModel 与 WorkoutSessionManager 两处调用点改为直传 bundle，逐字重复的位置摊平消失
- [x] 新增纯函数直接测试（首批）：① 无计划日早退分支带出当日体重/当日备注与单位；② 正常训练日 comboName/条目数/组行结构；③ 逃组行不算完成——按既有测试范式（FakeDaos + TestSeeds），不引入新 fixture 框架
- [x] 全量 JVM 测试绿（103 存量 + 新增直测），构建零新增警告（基线 2 条弃用警告之外）
- [x] grep 复核全仓库无旧 11 参签名调用残留

## Comments

- 2026-10-03 立票（主会话）：源自票 03 /code-review「Data Clumps」记账与票 04 扩参挂账（当时 9 参，现 11 参），两次评审建议收敛；本次采用「复用既有 WorkoutFlowBundle 直传」方案（评审给出的方案一），不新建Inputs大载体。WorkoutChecklist 回调侧独立成票 02。

## 实施备注（2026-10-03）

- 签名 11→6 参：`date/plans/combos/exercises/checkins + flow: WorkoutFlowBundle`；函数体首行解构 `val (settings, sessionWeights, skips, bodyWeights, dayNotes, exerciseNotes) = flow`（与载体声明顺序逐一对应），正文零改动，KDoc 同步。
- 两处调用点单行化 `buildWorkoutUiState(date, plans, combos, exercises, checkins, bundle)`（WorkoutViewModel.kt:236、WorkoutSessionManager.kt:101），逐字摊平消失，settings 不再靠位置对齐；grep 复核无旧 11 参调用残留。
- 新增 `WorkoutUiStateTest.kt` 3 条直测（该纯函数首批直测）：①休息日早退分支带出体重/当日备注/单位；②正常训练日 comboName/单条目/组行结构（TestSeeds 不设 targetSets → 断言「无目标至少一行」既有行为）；③逃组行 checked=false + skipped=true、doneCount=0。
- 节奏适配记账：纯重构零行为变化，TDD 红绿改为「直测先按旧签名写并验证绿 → 收敛签名 → 新签名复绿」，断言全程零改动（钉行为在线）；51 个 VM 级存量测试为行为网。
- JVM 测试 103→106 全绿；构建零新增警告。

- 2026-10-03 /code-review 记账：combine 五流接线+调用在 VM（约 :228-236）与 SessionManager（约 :93-101）仍逐行重复，可仿 `workoutFlowBundle()` 先例提取 `workoutUiStateFlow(daos, date): Flow<WorkoutUiState>`——超出本票验收范围，按「记账不展开」惯例遗留；unitIsLb 沿清单组件下传 4 层为轻微 Data Clumps，同记不展开。评审采纳项：直测文件的 WorkoutFlowBundle 字面量重复已用默认参 helper `workoutFlowBundleOf` 收敛。
