# 0004 — 跨 DAO 多段写入统一事务边界，读 Flow 留在事务外

FitPlan 的组合保存、删除组合/动作、训练记录保存、清空数据、删除训练者、打卡与会话起点等多段写入分散在多个 DAO（`combo_items`、`set_checkins`、`set_skips`、`day_plans`、`workout_session` 等），此前逐条 suspend 调用、无事务边界：进程在两步之间死亡会留下“组合条目已删但打卡未清”“清空数据半途”等半成品。我们决定引入统一事务边界：

- `AllDaos` 增加 `suspend fun <T> runInTransaction(block: suspend () -> T): T`，默认直通（测试 Fake 可覆盖）；
- 生产实现 `AppDatabase` 用 `androidx.room.withTransaction` 覆盖；
- 各 ViewModel/接收器的多段写入口各包一层事务，`onDone` 等 UI 回调放在事务外；
- **Flow 读取一律留在事务外**：事务内只做写入与 suspend 单查询，避免在 Room 事务上下文收集 invalidation 驱动的 Flow 造成死锁风险（打卡的快照重量/会话起点、删动作的条目 id、清空数据的动作列表均先读后写）。

被否掉的方案：①给每个写入口单独写 SQL 事务（重复且难复用于 Fake）；②把事务逻辑塞进 DAO 层（跨 DAO 场景无法覆盖）；③用全局数据库锁串行所有写入（过度限制且影响读并发）。

## Consequences

- JVM 测试用 `FakeDaos` 的全表快照/恢复等价模拟回滚，并用注入失败验证组合保存中途回滚；**真实 Room 事务行为仍需真机抽验**（保存瞬间强杀后重进无半成品）。
- 禁止嵌套 `runInTransaction`（各入口只包一层）；新增多段写入口应复用同一边界。
- 仍有刻意保留的非事务两段写：`WorkoutViewModel.syncToItem/syncToAll`（失败只导致提醒重现）与 `SessionActionReceiver.skip`（会话删除经 Manager 异步，无法与标逃同事务）——影响可控，已记账。
- 新增内存 Fake 表时需同步扩 `FakeDaos` 的事务快照范围。
