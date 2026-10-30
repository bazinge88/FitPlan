# 07 — 清空数据会话清理与加载态契约（T-L6）

Status: done
Owner role: logic-dev
Blocked by: 01
Write scope: ui/settings/SettingsViewModel.kt, ui/plan/PlanViewModel.kt, ui/log/LogViewModel.kt, ui/combo/ComboViewModels.kt; FitPlan/app/src/test/java/com/fitplan/app/ui/settings/SettingsViewModelTest.kt（新增）, ui/plan/PlanViewModelTest.kt
Plan: docs/iteration-plan.md §5 T-L6

## What to build

clearAllData 增删 workout_session 并包事务；PlanViewModel.dayPlans / LogViewModel.entries / ComboListViewModel.combos 改为初始 null 的 nullable StateFlow（前端加载态契约）。

## Acceptance

- [ ] 清空后会话行消失，设置与训练者保留（新测试）
- [ ] 三个 StateFlow 初始 null，数据到达非 null（Plan 测试覆盖）
- [ ] 全量测试绿
