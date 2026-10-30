# 01 — 跨 DAO 事务边界（T-L0）

Status: done
Owner role: logic-dev
Blocked by: None
Write scope: FitPlan/app/src/main/java/com/fitplan/app/data/db/, session/, ui/combo/ComboViewModels.kt, ui/log/LogViewModel.kt, ui/settings/SettingsViewModel.kt, ui/exercise/ExerciseViewModels.kt, ui/plan/PlanViewModel.kt, ui/plan/WorkoutViewModel.kt; FitPlan/app/src/test/java/com/fitplan/app/
Plan: docs/iteration-plan.md §5 T-L0

## What to build

AllDaos 新增默认直通 `runInTransaction`；AppDatabase 用 room withTransaction 实现；FakeDaos 快照/恢复实现回滚；把组合保存、删组合、删动作、记录保存、清空数据、删训练者、打卡、通知 action、复制上周包进事务。新增 TransactionRollbackTest 证明中途失败回滚。

## Acceptance

- [ ] `runInTransaction` 接口与 Room/Fake 两端实现完成
- [ ] 组合保存中途抛异常后旧条目与打卡原样恢复（新测试）
- [ ] 既有 106 测试全绿，编译通过
- [ ] 不在事务内执行 UI 回调
