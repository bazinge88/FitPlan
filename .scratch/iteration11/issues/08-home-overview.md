# 08 — 首页概览/休息日导航/跨天刷新（T-F1）

Status: done
Owner role: frontend-dev
Blocked by: None
Write scope: ui/home/HomeScreen.kt, ui/AppNav.kt, MainActivity.kt, ui/plan/WorkoutProgress.kt（新增）; FitPlan/app/src/test/java/com/fitplan/app/ui/plan/WorkoutProgressTest.kt（新增）
Plan: docs/iteration-plan.md §5 T-F1

## What to build

WorkoutProgress 纯函数 + 测试；主页训练日进度行；休息日“去计划页安排”并切 Tab；空组合提示且隐藏开始训练；ON_RESUME 刷新 today；MainActivity.onResume 调 sessionManager.onAppStart()。

## Acceptance

- [ ] WorkoutProgressTest 3 条绿
- [ ] 休息日 CTA、空组合提示、进度行代码审查通过
- [ ] 编译构建通过
