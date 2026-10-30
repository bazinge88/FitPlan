# 10 — 加载态统一（T-F3）

Status: done
Owner role: frontend-dev
Blocked by: 07
Write scope: ui/plan/PlanScreen.kt, ui/combo/ComboListScreen.kt, ui/log/LogScreen.kt
Plan: docs/iteration-plan.md §5 T-F3

## What to build

三个页面在对应 StateFlow 为 null 时显示“加载中…”，非 null 后走既有空态/内容。

## Acceptance

- [ ] 冷启动不闪“未安排/暂无记录”
- [ ] 既有空态与交互不变
- [ ] 编译构建通过
