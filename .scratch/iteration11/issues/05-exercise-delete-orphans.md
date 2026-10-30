# 05 — 删除动作清理孤儿打卡/逃标（T-L4）

Status: done
Owner role: logic-dev
Blocked by: 01
Write scope: ui/exercise/ExerciseViewModels.kt, FitPlan/app/src/test/java/com/fitplan/app/ui/exercise/ExerciseListViewModelTest.kt
Plan: docs/iteration-plan.md §5 T-L4

## What to build

删除动作前收集该动作在所有组合中的条目 id，事务内先清 set_checkins/set_skips，再删动作。

## Acceptance

- [ ] 删除动作 A 后 A 的打卡/逃标清空、动作消失；动作 B 数据保留
- [ ] 全量测试绿
