# 02 — 空组合训练态收口（T-L1）

Status: done
Owner role: logic-dev
Blocked by: None
Write scope: ui/plan/WorkoutViewModel.kt, session/WorkoutSessionManager.kt, FitPlan/app/src/test/java/com/fitplan/app/ui/plan/WorkoutUiStateTest.kt
Plan: docs/iteration-plan.md §5 T-L1

## What to build

新增顶层纯函数 `isWorkoutAllDone(items)`；SessionManager 在 comboName 非空且 items 为空时 stop() 并撤通知；allDone 改用纯函数。WorkoutUiState 不加字段，UI 自行判断空组合。

## Acceptance

- [ ] 空组合：comboName 非空、items 空、loading=false
- [ ] isWorkoutAllDone：空列表 false / 部分 false / 全部 true
- [ ] 全量测试绿
