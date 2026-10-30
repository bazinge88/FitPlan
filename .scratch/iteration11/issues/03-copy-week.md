# 03 — 复制上周精确镜像（T-L2）

Status: done
Owner role: logic-dev
Blocked by: 01
Write scope: ui/plan/PlanViewModel.kt, FitPlan/app/src/test/java/com/fitplan/app/ui/plan/PlanViewModelTest.kt
Plan: docs/iteration-plan.md §5 T-L2

## What to build

copyWeek 按 7 天逐日处理：源无计划则删目标日；有计划则 upsert copy(date=target)；计数只算复制天数；包事务。

## Acceptance

- [ ] 目标周 7 天旧安排 + 源周 2 天 → 复制后目标周只剩 2 天且内容一致
- [ ] onDone 计数=2；源周不变
- [ ] 全量测试绿
