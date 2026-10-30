# iteration11 logic-dev 裁决记录（Ruling）

> 记录计划未覆盖或需自行裁决的取舍；每个 ruling 给理由与代价。日期：2026-10-18。

## R1 事务包装范围：按“多段写且可能留下引用不一致”界定，比计划枚举略扩/略缩
- **Ruling**：在计划列举入口（组合保存、删组合、记录保存、清空数据、删训练者、删动作、打卡、通知直接打卡、复制上周）之外，额外包装 `ComboListViewModel.copyCombo`（组合行 + 条目行两次写）。
  不包装 `WorkoutViewModel.syncToItem/syncToAll`（组合条目 + 动作已处理重量两次写）与 `SessionActionReceiver.skip`（会话删除经 `WorkoutSessionManager.stop()` 异步发起，无法纳入同一事务）。
- **理由**：copyCombo 中途失败会留下“有组合无条目”的空组合（正是 L02 的病态数据）；sync 两次写失败最多是提醒重现，不产生引用不一致；skip 的会话删除不在接收器协程内，硬包事务会引入跨组件事务耦合。
- **代价**：sync/ skip 仍非原子；若后续要求“零半成品”可再收敛（属 L09/L10 未选范围）。

## R2 所有 Flow 读取放在事务外，事务只包写入（checkIn / 删动作 / 清空数据）
- **Ruling**：`WorkoutViewModel.checkIn`（combo 条目→exerciseId、当次重量、会话行）、`ExerciseListViewModel.delete`（收集该动作的 comboItemIds）、`SettingsViewModel.clearAllData`（待删动作列表）的 Flow 读取全部移到 `runInTransaction` 之前；事务内只保留写入与 suspend 单查询（`getWithItems`/`getAll`/`getRange` 等）。
- **理由**：Room 的 Flow 查询在事务上下文内收集存在死锁/连接复用风险（事务持有写连接时 Flow 端另取查询连接）；事务只包写同样能保证“打卡行 + 清逃标 + 会话起点”等多表写同生共死。计划 T-L4 原文把 `observeAllWithItems().first()` 放在事务内，这里按生产安全优先调整。
- **代价**：读取在事务开始前完成，存在毫秒级读取-写入窗口（单机单用户应用可忽略；条目 id 集合若被并发改动，多删/少删的清理只影响孤儿行清理完备性，不损坏数据）；每次打卡多两次全表读（L11 已记账的既有性能特征，本轮不优化）。

## R3 事务回滚测试的调度器选择
- **Ruling**：`TransactionRollbackTest` 的组合保存回滚用例把 `Dispatchers.Main` 换成单线程真实 dispatcher（`Executors.newSingleThreadExecutor().asCoroutineDispatcher()`），轮询 `FakeDaos.rollbackCount` 确认失败路径完成；其余用例仍用 `UnconfinedTestDispatcher`。
- **理由**：注入真实 `IllegalStateException` 后，若 VM 协程跑在测试调度器上，未捕获异常会被 `runTest` 记为测试失败，无法在同一个测试里既触发异常又断言回滚。FakeDaos 增加 `rollbackCount`（测试基础设施）作为确定性完成信号。
- **代价**：该用例与其他用例的调度器约定不同（已注释说明）；FakeDaos 多一个仅测试用计数。

## R4 copyWeek 计数口径
- **Ruling**：`copied` 只统计“源周有安排、复制到目标周”的天数，源周为空而删除目标周旧安排的天不计入；`onDone` 在事务提交后回调。
- **理由**：与计划 §5 T-L2 明文一致（“onDone 计数仍为复制天数（不含删除日）”），且与既有 UI 文案口径（“已复制 N 天”）一致。
- **代价**：用户可能把“目标周被删了 5 天”理解为另一类变更；本轮不改文案（前端既有提示“已复制 N 天”）。

## R5 导出当日备注的单/多用户口径
- **Ruling**：`multiUser = userNames.size > 1` 时按天输出全部备注（按 userId 升序、`当日备注（用户名）：内容`）；否则保持旧行为——只输出 `activeUserId` 的备注、格式 `当日备注：内容`（字节级不变）。
- **理由**：既有 16 个导出测试与用户习惯要求单用户文本不变；多用户下不再静默丢另一人的备注（L06）。
- **代价**：单用户模式下若历史数据里存在 userId≠current 的备注仍不导出（旧行为保留，避免破坏既有断言）；真实导出 `userNames` 来自 users 表，多用户场景可覆盖。

## R6 空组合会话收口的位置
- **Ruling**：`WorkoutSessionManager.render` 中 `items.isEmpty() -> stop(); return` 放在 POST_NOTIFICATIONS 权限检查与 `comboName == null` 分支之后、`isWorkoutAllDone` 之前。
- **理由**：保持既有“无权限不渲染、无计划即结束”的顺序，改动面最小；UI 在无通知权限时不会 start 会话。
- **代价**：极端情况下（权限被中途撤销 + 空组合会话）不会自动收口，需等下次交互/冷启动清理（与 ADR-0002 的陈旧自愈语义一致）。

## R7 Fake 事务快照范围与注入钩子
- **Ruling**：FakeDaos 快照覆盖 12 个内存 fake 的 MutableStateFlow（plan/combo/exercise/checkin/skip/settings/sessionWeight/bodyWeight/dayNote/exerciseNote/workoutSession/user）；`FakeLogDao` 为无状态占位无需快照。`FakeComboDao.failOnNextInsertItems` 一次性消费，新增 `FakeDaos.rollbackCount` 供断言。
- **理由**：计划要求“快照覆盖全部 fake 的 MutableStateFlow”；FakeLogDao 所有写方法已是空实现/占位，无状态可回滚。
- **代价**：新增 13 张表时要记得同步扩快照（已在代码注释注明）。

## R8 清空数据补删会话的位置
- **Ruling**：`clearAllData` 在事务内清理完打卡/逃标/重量/体重/备注/计划后、删组合与动作前，调用 `workoutSessionDao.delete()`。
- **理由**：会话行是通知权威状态（ADR-0002），清空业务数据必须一并删行；顺序对结果不敏感，放在业务表组内便于阅读。
- **代价**：若通知仍挂着，依赖既有“计划为空 → render stop”自愈（验收清单已记）。

## R9 T-L3（InputRules）不越界
- **Ruling**：本轮 T-L3 按任务票 04 归 frontend-dev（含新增 `util/InputRules.kt` 与 `ui/Common.kt` 等 UI 文件）；logic-dev 未修改 `util/` 与任何 UI 界面文件。计划 §6.1 中逻辑 agent 的 `util/**` 作用域仅代表潜在范围，以任务票为准。
- **理由**：写作用域禁止清单明确 `util/InputRules.kt` 与 UI 文件禁止修改；避免双写冲突。
- **代价**：逻辑侧未独立复核 InputRules 实现，留给 verifier / Lead 抽查。

## R10 计划外补测 LoadingStateContractTest
- **Ruling**：新增 `FitPlan/app/src/test/java/com/fitplan/app/ui/LoadingStateContractTest.kt`（1 条），直接断言三个列表 VM 的 StateFlow 初始值为 null；计划 T-L6 只要求 PlanViewModelTest 覆盖。
- **理由**：F3 依赖三条契约，集中锁死初始值可防止后续异步接线改回 emptyList 导致加载态闪烁；同时补充测试计数（目标 ≥120）。
- **代价**：多一个测试文件（纯测试，不触碰生产代码）。
