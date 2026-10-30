# 14 — 独立验证与验收测试（T-V1）

Status: done
Owner role: verifier
Blocked by: 01,02,03,04,05,06,07,08,09,10,11,12,13
Write scope: FitPlan/app/src/test/java/com/fitplan/app/Iteration11AcceptanceTest.kt（新增）, docs/iteration11-verification.md（新增）, .scratch/iteration11/gates/verifier-*.md
Plan: docs/iteration-plan.md §5 T-V1

## What to build

独立验收：跨切面测试（多用户隔离/空组合/复制+移除/删动作清理）；全量 clean 测试 + 构建；验收矩阵与残余风险；FAIL 回报 Lead。

## Acceptance

- [ ] Iteration11AcceptanceTest 通过
- [ ] clean 全量构建与测试证据写入 verification 文档
- [ ] 验收矩阵逐项 PASS/FAIL
