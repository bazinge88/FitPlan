# 05 — 移除安排

**What to build:** 周计划页可清掉某天已排的组合，该天回到未安排；该天已有的打卡、逃组、备注、当次重量、体重全部保留——排程调整不损历史。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 数据层补齐天计划删除能力（此前只有 upsert，无删除）
- [x] 周计划页单日提供「移除安排」操作，移除后该天显示未安排
- [x] 移除后该天已有打卡、逃组、当日备注、动作备注、体重、当次重量全部原样保留
- [x] 重新排程该天后历史记录仍可回看
- [x] 单元测试覆盖「移除不删记录」；全绿 + 构建零新增警告

## 实施备注（2026-10-02）

- 数据层能力已存在：`DayPlanDao.deleteByDate`（Daos.kt:122）与调用链（DayEditSheet 按钮 → PlanScreen onClear → PlanViewModel.clearDay → deleteByDate，FakeDayPlanDao 已实现）在本轮开工前即已端到端可用——如实记账「数据层能力已存在，本轮仅接线核对与补测试」，未重复造方法；spec Further Notes「DayPlanDao 无删除能力」一句已过时。
- 本轮实际改动：①按钮文案「清除安排」→「移除安排」（PlanDialogs.kt:139）+ clearDay/DayEditSheet KDoc 对齐 CONTEXT.md「移除安排」词条；②PlanViewModelTest 新增两条测试（buildVm helper 同步补订阅 dayPlans）。
- 「移除不损历史」的机制证据：六张记录表（set_checkins/set_skips/day_notes/exercise_notes/body_weights/session_weights）均无指向 day_plans 的外键（reviewer 逐表审计），全部按 date 存储与计划行无关；测试一以六表整行 data class 相等断言钉死（含他天计划不受影响的双向断言）。
- 重排回看证据：测试二 clearDay → setDay 重排 → WorkoutViewModel state.items 的 doneCount==1——打卡按 date 关联，计划行删除重建不影响回看；通知路径 SessionActionReceiver 对 plan==null 安全早退，移除当天安排后通知打卡不崩。
- 移除后该天主页走 combo==null 分支（未安排卡），体重与当日备注入口仍在——与 spec「今日清单常驻（含未安排日）」一致。
- 红绿节奏说明：功能预存在，测试一次通过（无红阶段）；JVM 测试 94 个全绿（票 04 后 92 + 本票 2 条）、零新增警告（基线 2 条外）。
