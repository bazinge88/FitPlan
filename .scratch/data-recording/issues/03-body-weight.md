# 03 — 体重记录

**What to build:** 主页可录入当天体重：训练日清单上方与休息日卡片都提供入口；天 + 训练者一条，重复输入覆盖；训练日与休息日都可记；可选记录——不设称重提醒、不显示漏记提示（零负担）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 新增体重表（天 + 训练者唯一，重复输入 REPLACE 覆盖），非破坏迁移（以当时库版本号为准追加）
- [x] 训练日主页可录入/修改当天体重；休息日卡片同样可录
- [x] 同一天再次输入覆盖旧值，不留多条
- [x] 切换训练者后各记各的、互不可见
- [x] 全 app 不出现催记、漏记标记类提示
- [x] 「清空所有数据」连带清理体重表（沿用既有清空范式）
- [x] 单元测试覆盖覆盖语义与多训练者隔离；全绿 + 构建零新增警告

## 实施备注（2026-10-02）

- 数据库 v9→v10：新增 `body_weights`（唯一键 date+userId），`MIGRATION_9_10` 建表（含 `userId INTEGER NOT NULL DEFAULT 1` 与 date 普通索引、date+userId 唯一索引，SQL 与 Room 生成规则一致）。迁移正确性按 spec Testing Decisions 由**真机覆盖安装验收**覆盖（JVM 不测迁移），迁移已实现、全量构建通过，真机验收沿用迭代惯例待执行。
- 单位口径核对结论：`TimeUtil.formatWeight`（util/TimeUtil.kt:51）只做去尾零格式化、不做 kg/lb 换算，单位后缀由调用方按 `unitIsLb` 传（WeightRow 同款）——体重显示直接沿用该口径，与 spec「kg 库内原值、lb 不换算——沿用 PR 页口径」一致，未发明任何换算。
- 入口仅主页两处（spec 明文）：训练日清单上方（开始/结束训练按钮与清单之间）与休息日卡片内；对话框复用当次重量范式（数值 regex、Decimal 键盘、`TimeUtil.formatWeight` 预填）。体重无负数语义，输入 regex 不接受负号（与 ui/Common.kt `NumberField` 的有意差别，负号留给辅助重量）。
- 状态构建走唯一路径：`buildWorkoutUiState` 加 `bodyWeights` 参数并带出 `WorkoutUiState.bodyWeight`；休息日/无计划早退分支同样带出体重与 `unitIsLb`（此前该分支不带单位，体重入口需要）。combine 六条流经新增 `WorkoutFlowBundle` 载体（ViewModel 与 WorkoutSessionManager 通知路径共用）。
- `recordBodyWeight` 无删除语义（票面只要求录入/修改，覆盖即可）；「清空所有数据」与「删除训练者」两处均连带清理体重（后者超出票面第 6 条字面，系沿用 deleteUser 既有 `deleteForUser` 三连范式防孤儿行，/code-review 两轴均建议保留）。
- 验收 5 核对口径：体重行空态文案「今天未记录」是入口自身的空态描述（同 `WeightRow` 的「未设定」），不属催记/漏记标记；全 app 无称重提醒、无漏记标记。
- `buildWorkoutUiState` 现为 9 个位置参数（bundle 在调用点摊平回传，四类列表类型互异故编译器可防错序）——/code-review 指出的 Data Clumps 债记账不展开，票 04（当日备注）同区域实施时可一并收拢。
- JVM 测试 82 个全绿、零新增警告（基线 78 + BodyWeightTest 4 条：落库与状态带出 / 同天覆盖 / 多训练者隔离 / 休息日带出体重与单位）。
