# 02 — WorkoutChecklist 回调收敛（WorkoutActions 载体）

**What to build:** 主页与训练详情页共用清单 `WorkoutChecklist` 的 11 个回调收敛为单一 `WorkoutActions` 载体 + 顶层工厂（交接票面口径「12 回调」，实测 11 个回调 + state + modifier 共 13 参，以本票更正记账），消灭三处逐字重复：`WorkoutItemCard` 整份复制的回调签名、两个调用点各自重复的 11 行 `vm::` 方法引用列表。**纯重构、零行为变化。**

**Blocked by:** None — can start immediately（与 01 无文件交集；单工作树 + gradle 串行惯例，建议按编号串行执行）

**Status:** done

- [x] `WorkoutActions` 载体（11 个回调属性，名字与现回调一致）+ 顶层 `workoutActions(vm)` 工厂落地，风格沿用 `workoutFlowBundle()` 顶层工厂先例；不引入 sealed UiEvent/onEvent 新范式（仓库无此先例，主流风格为直接方法引用）
- [x] WorkoutChecklist / WorkoutItemCard / SetRow 改收载体，内部 11 个分发点行为逐字不变
- [x] 两个调用点（训练详情页与主页）各收敛为一行（remember 包装），重复的 11 行 vm:: 引用列表消失
- [x] 全量 JVM 测试绿 + 构建零新增警告；composable 接线无 JVM 测试面（项目无 Compose UI 测试），验收口径 = 构建 + 双轴评审 + 后续真机抽查，票尾如实记账

## Comments

- 2026-10-03 立票（主会话）：源自债务②两次评审建议（「传 WorkoutFlowBundle/节段结构体或抽 UI 事件类」），回调侧取「结构体载体」方案而非 UI 事件类——与既有 bundle 习惯一致、零新范式。回调实际数量以测绘为准更正为 11。

## 实施备注（2026-10-03）

- 票面「12 回调」更正：实测 11 个回调 + state + modifier 共 13 参，以 11 记账。
- 新增 `WorkoutActions` 载体（11 个回调属性，名字与原回调一致）+ 顶层工厂 `workoutActions(vm)`（仿 `workoutFlowBundle()` 先例），落 WorkoutScreen.kt；未引入 sealed UiEvent 新范式（仓库无先例，主流风格为直接方法引用）。
- `WorkoutChecklist` 13 参→3 参（state/actions/modifier）；`WorkoutItemCard`、`SetRow` 改收 `actions`，全部分发点实参表达式逐字不变（defaultRepsFor 包装、item.exerciseId 包装、标逃 if/else、SetWeightDialog onConfirm 均保留）。
- 两调用点各收敛一行 `actions = remember(vm) { workoutActions(vm) }`（WorkoutScreen.kt:119、HomeScreen.kt:165）；HomeScreen 补 `import com.fitplan.app.ui.plan.workoutActions`（remember 原有）。
- composable 接线无 JVM 测试面（项目无 Compose UI 测试），验收口径=构建+双轴评审+后续真机抽查；`EntryDialog.kt` 的同名私有 `SetRow` 为无关数据类，未触碰。
- JVM 测试 106 全绿；构建零新增警告。
