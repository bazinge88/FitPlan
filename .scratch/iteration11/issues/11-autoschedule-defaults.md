# 11 — 自动排程默认值随数据到达（T-F4）

Status: done
Owner role: frontend-dev
Blocked by: None
Write scope: ui/plan/PlanDialogs.kt
Plan: docs/iteration-plan.md §5 T-F4

## What to build

LaunchedEffect(combos) 在槽位为 null 时填 combos[0..2]；cycleIds 为空时填 combos.take(2)；不覆盖用户选择；combos 空时生成禁用。

## Acceptance

- [ ] 冷启动立即打开自动排程后槽位最终有默认值
- [ ] 用户已改值不被覆盖
