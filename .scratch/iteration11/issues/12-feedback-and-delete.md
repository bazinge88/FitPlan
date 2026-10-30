# 12 — 导出/清空反馈与训练记录删除确认（T-F5）

Status: done
Owner role: frontend-dev
Blocked by: None
Write scope: ui/settings/SettingsScreen.kt, ui/log/LogScreen.kt, ui/exercise/ExerciseListScreen.kt
Plan: docs/iteration-plan.md §5 T-F5

## What to build

writeMarkdown 返回 Boolean + try/catch，成功/失败 Toast；清空成功 Toast；训练记录删除 ConfirmDialog；动作删除文案补打卡/逃组清理说明。

## Acceptance

- [ ] 导出取消无提示，失败有提示
- [ ] 删除训练记录需确认
- [ ] 编译构建通过
