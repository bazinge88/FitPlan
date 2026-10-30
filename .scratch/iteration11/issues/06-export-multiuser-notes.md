# 06 — 多训练者导出当日备注（T-L5）

Status: done
Owner role: logic-dev
Blocked by: None
Write scope: export/MarkdownExporter.kt, FitPlan/app/src/test/java/com/fitplan/app/export/MarkdownExporterTest.kt
Plan: docs/iteration-plan.md §5 T-L5

## What to build

appendCheckins 按天输出全部训练者当日备注：单用户保持“当日备注：X”；多用户逐条“当日备注（用户名）：X”，按 userId 排序。

## Acceptance

- [ ] 两用户同日备注都出现且带用户名
- [ ] 单用户输出格式不变（既有测试通过）
