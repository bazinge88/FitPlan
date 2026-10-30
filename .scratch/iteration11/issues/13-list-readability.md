# 13 — 列表可读性/响应式/文案（T-F6）

Status: done
Owner role: frontend-dev
Blocked by: None
Write scope: ui/combo/ComboListScreen.kt, ui/exercise/ExerciseListScreen.kt, ui/pr/PrScreen.kt, ui/settings/SettingsScreen.kt, FitPlan/app/build.gradle.kts
Plan: docs/iteration-plan.md §5 T-F6

## What to build

组合卡最多 3 行动作摘要 + 等 N 个动作；长名称省略；动作库徽标 FlowRow；PR 重量补空格；关于页版本 BuildConfig.VERSION_NAME（必要时开 buildConfig）。

## Acceptance

- [ ] 长文本/四徽标不溢出（代码审查）
- [ ] 全量构建+测试绿
