# 04 — 目标数值校验（T-L3）

Status: done
Owner role: frontend-dev
Blocked by: None
Write scope: FitPlan/app/src/main/java/com/fitplan/app/util/InputRules.kt（新增）, ui/Common.kt, ui/combo/ComboEditScreen.kt, ui/log/EntryDialog.kt, ui/exercise/ExerciseEditScreen.kt; FitPlan/app/src/test/java/com/fitplan/app/util/InputRulesTest.kt（新增）
Plan: docs/iteration-plan.md §5 T-L3

## What to build

InputRules：整数输入过滤、重量输入过滤、范围解析；NumberField 增加 allowDecimalNegative（默认 false）；组合目标组数/次数/时长与记录字段使用 1..上限；重量仍允许负数/0/小数；非法非空值显示错误并禁用保存。

## Acceptance

- [ ] InputRulesTest 覆盖整数/重量/边界
- [ ] 组合与记录 UI 无法输入负数组数/次数/时长；重量可 -20.5
- [ ] 编译与构建通过
