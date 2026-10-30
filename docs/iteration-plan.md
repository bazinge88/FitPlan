# FitPlan 第十一轮迭代计划：逻辑健壮性收敛与前端体验优化

- 文档版本：v1.0（2026-10-18）
- 迭代代号：iteration11
- 工作区根：`/Users/suyi/projects/exercise`
- 项目代码：`/Users/suyi/projects/exercise/FitPlan`
- 主文档：本文件（`/Users/suyi/projects/exercise/docs/iteration-plan.md`）
- 需求来源：`/Users/suyi/projects/exercise/requirements.rtf`
- 交接基线：`/Users/suyi/projects/exercise/FitPlan/HANDOVER.md`（2026-10-03，第十轮完成 + 债务②收敛）
- 术语表：`/Users/suyi/projects/exercise/CONTEXT.md`
- 架构决策：`/Users/suyi/projects/exercise/docs/adr/0001-combo-item-as-sync-root.md`、`0002-session-notification-without-foreground-service.md`、`0003-per-set-weight-snapshot.md`
- 状态：**完成**（G0-G3 全通过；自动化验收 131 测试全绿，真机验收待用户执行；完成报告见 `/Users/suyi/projects/exercise/docs/iteration11-report.md`）

---

## 1. 现状认知摘要

### 1.1 产品是什么、边界在哪

FitPlan 是本地离线使用的中文 Android 健身周计划与记录应用。核心链路：**主页（今日计划直出）→ 动作库 → 训练组合 → 周计划 → 清单式逐组打卡 → 可选训练记录 → Markdown 导出**。不做云同步、账号、联网、Health Connect、Wear OS、训练提醒、组间计时、社交、成就、排行榜、AI 生成计划、完成度/统计图表（需求红线，本轮零触碰）。

第十轮后已具备的记录维度：逐组打卡（次数/每组重量快照/打卡时刻）、逃组、当次重量、本地多训练者、体重、当日备注、动作备注、训练用时、移除安排、Markdown 导出扩展。

### 1.2 技术栈与运行约束

| 项 | 现状 |
| --- | --- |
| 语言/UI | Kotlin 2.2.21 + Jetpack Compose（Material 3，动态取色） |
| 架构 | 单模块 MVVM，无 DI 框架，无网络权限 |
| 数据库 | Room 2.8.5，**DB v11**，10 条非破坏迁移（1→2 … 10→11） |
| SDK | minSdk = targetSdk = compileSdk = 36（Android 16） |
| 构建 | AGP 8.13.2 + Gradle wrapper 8.13 + JDK 21（源码目标 17） |
| 测试 | JUnit4 + kotlinx-coroutines-test，JVM 单测 **106 个基线全绿** |
| 构建基线 | `:app:assembleDebug` 通过；仅 2 条既有弃用警告（`fallbackToDestructiveMigration`、`menuAnchor`） |
| 版本控制 | **无 git 仓库**（工作区只有源码与构建产物） |
| 真机 | 当前 `adb devices` 无设备（PHB110 未连接），本轮验收以 JVM 测试 + 构建 + 静态审查为主 |

### 1.3 模块边界

- `data/db/`：Entities（15 张表）、Daos（13 个 DAO + `AllDaos` 聚合）、AppDatabase（v11 + 迁移）。
- `data/model/Schemes.kt`：三分化 / 练一休一内置排程。
- `session/`：WorkoutSessionManager（常驻通知，免前台服务）、SessionActionReceiver（通知按钮广播）。
- `export/MarkdownExporter.kt`：结构化 Markdown 导出（纯函数式拼接）。
- `util/`：TimeUtil、PrRecords、QuickCheckIn、TrainingDuration。
- `ui/`：AppNav（6 Tab + 独立路由 pr/workout/combo/exercise）、home/plan/combo/exercise/log/pr/settings/theme。
- `app/src/test/`：FakeDaos（内存实现全部 DAO）、TestSeeds、按领域拆分的 14 个测试类。

### 1.4 核心流程与数据一致性要点

1. **打卡主链路**：主页/训练详情页 `WorkoutViewModel.checkIn` 与通知 `SessionActionReceiver.quickCheckIn` 双入口，共享 `buildWorkoutUiState` 纯函数与 `workoutFlowBundle()` 流接线；打卡行存在即完成，重量快照来自 `session_weights`，打卡清除逃标，首组打卡写 `workout_session.startedAt`。
2. **重量模型**：组合条目目标重量是组合内独立根源；训练中只记当次重量；同步只能经提醒条确认（ADR-0001/0003）。`exercises.defaultWeight` 仅作组合编辑预填。
3. **会话/通知**：`workout_session` 行是权威状态；通知是视图；进程被杀期间通知不自更新，交互即自愈（ADR-0002）。
4. **多训练者**：打卡/逃组/当次重量/体重/两轨备注/用时/PR 全部按 `userId` 隔离；设置存 `activeUserId`。
5. **导出**：全部/指定周/日期范围三种范围；打卡段按天分组，体重/备注/逃组/用时并入；重量保留原始符号。

### 1.5 历史决策与不可回退项

- ADR-0001：训练中输入的是当次重量，**不得**写回组合目标；不要把它当回归修回旧行为。
- ADR-0002：通知采用免前台服务方案；会话行存在 = 进行中；残留通知按钮必须校验会话行存在。
- ADR-0003：每组重量快照式录入，PR@n 精确口径；取消重打 = 重快照；旧数据不回填。
- 第十轮术语：逃组（组级）、当日备注、动作备注、体重记录、移除安排、训练用时。
- 迁移铁律：已上真机的迁移不可改，只能新增迁移；本轮**不新增表/列**，DB 保持 v11。

### 1.6 本轮开始前的质量基线（已实测）

~~~bash
cd /Users/suyi/projects/exercise/FitPlan
GRADLE_USER_HOME=/Users/suyi/projects/exercise/.gradle-home ./gradlew --offline :app:testDebugUnitTest --console=plain
# BUILD SUCCESSFUL；test-results XML 汇总 tests=106
~~~

构建环境特别说明：本机沙箱不允许写 `~/.gradle`，已把 Gradle 用户目录克隆到工作区
`/Users/suyi/projects/exercise/.gradle-home`（APFS clone，3.5G 逻辑大小），所有构建必须带
`GRADLE_USER_HOME=/Users/suyi/projects/exercise/.gradle-home` 与 `--offline`。
并发构建通过 `/Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh` 串行化。

---

## 2. 逻辑问题清单（系统排查结果）

排查范围：HANDOVER/README/CONTEXT/ADR/需求/历史 issue/spec + 全部主源码（约 5.9k 行）+ 全部测试（约 4.5k 行）+ Gradle/Manifest/资源 + 第十轮遗留待办 + 数据流与异常路径。优先级口径：P0 = 必修（崩溃/数据丢失/主线不可用）；P1 = 高（数据一致性风险或高频错误行为）；P2 = 中（可感知缺陷/边界错误/体验明显受损）；P3 = 低（小瑕疵/性能/可维护性）。本轮选中项标记 `[本轮]`。

### L01 多段写操作非事务，进程中断留半成品 [本轮]
- **证据/位置**：`FitPlan/app/src/main/java/com/fitplan/app/ui/combo/ComboViewModels.kt:87-150`（组合保存：删条目→删打卡→逐条更新→批量插入）；`ui/log/LogViewModel.kt:55-89`（记录保存：upsert 条目→清组→插组）；`ui/settings/SettingsViewModel.kt:56-70,137-151`（删训练者/清空数据多表顺序删）；`ui/combo/ComboViewModels.kt:35-47`（删组合）；`ui/exercise/ExerciseViewModels.kt:57`（删动作）；`ui/plan/WorkoutViewModel.kt:242-268`（打卡→清逃标→写会话起点）；`session/SessionActionReceiver.kt:41-113`。HANDOVER §5 已承认组合保存非事务。
- **影响范围**：组合编辑、训练记录、清空数据、删训练者、删组合/动作、打卡与会话起点；数据一致性、导出正确性、后续统计。
- **复现/验证方式**：单元测试注入“第二步抛异常”的 Fake DAO，断言第一步写入被回滚；真机在保存瞬间强杀进程后重进抽查（本轮不做真机）。
- **优先级**：P1。
- **建议方案**：给 `AllDaos` 增加默认直通的事务边界 `suspend fun <T> runInTransaction(block)`；`AppDatabase` 用 `androidx.room.withTransaction` 实现；`FakeDaos` 用快照/恢复实现可回滚；把上述多段写操作包进事务。
- **预估工作量**：M（约 4-6 小时，含测试）。
- **风险**：Room 事务要求同一协程上下文；嵌套调用需避免；Fake 快照需覆盖全部 MutableStateFlow；误包只读逻辑会放大锁范围。

### L02 空组合训练态死锁与展示缺失 [本轮]
- **证据/位置**：`session/WorkoutSessionManager.kt:131`（`items.isNotEmpty() && ...`，空组合永远 allDone=false）；`ui/plan/WorkoutScreen.kt:117-123`（无 items 空态）；`ui/home/HomeScreen.kt:140-167`（空清单只剩按钮）；HANDOVER §5 “空组合边界”遗留备忘。
- **影响范围**：有组合无条目时，“开始训练”后常驻通知 0/0 永驻、训练页空白、主页无线索。
- **复现/验证方式**：新建空组合 → 排到某天 → 主页开始训练；观察通知与训练页；单元测试 `buildWorkoutUiState` 空组合 + `isWorkoutAllDone(emptyList())`。
- **优先级**：P2。
- **建议方案**：新增顶层纯函数 `isWorkoutAllDone(items)`；SessionManager 在 `items.isEmpty()` 时结束会话并撤通知；训练页/主页给空组合空态与返回/编辑引导。
- **预估工作量**：S（1-2 小时）。
- **风险**：把“无计划”（comboName=null）与“空组合”（comboName 非空且 items 空）区分错会误停会话。

### L03 “复制上周”不是精确镜像 [本轮]
- **证据/位置**：`ui/plan/PlanViewModel.kt:69-75`（只 upsert 源周存在的 7 天；目标周多出的旧安排保留）；对照 `copyDay` 已实现“源为空则删目标”。
- **影响范围**：周计划复制后出现“上周没安排、本周还留着旧组合”的幽灵安排，进而影响打卡入口与导出。
- **复现/验证方式**：目标周预置 7 天安排，源周仅 1 天；执行复制后目标周仍残留 6 天；单测断言目标周与源周逐日一致。
- **优先级**：P2。
- **建议方案**：按 7 天逐日处理：源无计划 → `deleteByDate(target)`；有 → `upsert(copy(date=target))`；`onDone` 计数仍为复制天数（不含删除日）。
- **预估工作量**：S（0.5-1 小时）。
- **风险**：把“删除未安排日”误解为“清除该天历史记录”（实际只删 `day_plans` 行，历史按 date 保留）。

### L04 目标组数/次数/时长可输入负数或 0 [本轮]
- **证据/位置**：`ui/Common.kt:44-48`（NumberField 统一允许负号）；`ui/combo/ComboEditScreen.kt:137-141`（sets/reps/duration 用 `parseIntOrNull`）；`ui/log/EntryDialog.kt:148-155`（同）；`ui/plan/WorkoutViewModel.kt:168-170`（`maxOf(targetSets, …)` 对负数只能兜底 1，但目标值本身仍脏）。
- **影响范围**：组合目标、训练记录；可能出现“已完成 0/-2 组”、PR@-1、导出负目标；数据进入库后无校验入口。
- **复现/验证方式**：组合编辑给组数输入 `-3` 并保存；检查库值与训练页/导出；单测覆盖输入规则与解析。
- **优先级**：P2。
- **建议方案**：新增 `util/InputRules.kt`：整数输入只允许数字；目标组数 1..50、次数 1..999、时长 1..1440；重量仍允许负数/0/小数。NumberField 增加 `allowDecimalNegative`（默认 false，仅重量字段 true）；组合编辑与训练记录的非法非空值显示错误并禁用保存。
- **预估工作量**：M（3-4 小时）。
- **风险**：历史脏数据加载时显示错误并阻止保存，需明确“清空该字段可保存”的交互；重量字段回归（负数/小数）必须测。

### L05 删除动作留下孤儿打卡/逃标 [本轮]
- **证据/位置**：`ui/exercise/ExerciseViewModels.kt:57`（直接 `exerciseDao.delete`）；`data/db/Daos.kt:265-307`（`set_checkins`/`set_skips` 无外键，仅提供按 `comboItemId` 清理）；对照 `ComboViewModels.kt:35-47` 删组合已清理。
- **影响范围**：删除被组合引用的动作 → 组合条目级联删除 → 打卡与逃标成孤儿；PR/导出虽按条目过滤而不可见，但库持续膨胀，且未来按 userId/date 的统计会读到脏行。
- **复现/验证方式**：动作加入组合→打卡→删除动作；查 `set_checkins`/`set_skips` 仍有行；单测在 FakeDaos 断言清理。
- **优先级**：P2。
- **建议方案**：删除前收集该动作在所有组合中的条目 id，先 `deleteForComboItems` 两张表，再删动作（包在 L01 事务内）。
- **预估工作量**：S（1-2 小时）。
- **风险**：`comboDao.observeAllWithItems().first()` 读取时机与级联顺序；不要误删其他动作同 id 的记录。

### L06 多训练者导出只导当前训练者的当日备注 [本轮]
- **证据/位置**：`export/MarkdownExporter.kt:203-210`（`dayNotes.find { it.date == day && it.userId == activeUserId }`）；同函数打卡行却遍历全部用户，且 `multiUser` 已追加“用户”列。
- **影响范围**：多训练者导出时另一人的当日备注静默丢失；导出作为离线备份/AI 分析数据不完整。
- **复现/验证方式**：两名训练者同一天各写当日备注并打卡 → 导出；只出现当前训练者备注；单测断言两条备注与用户标签。
- **优先级**：P2。
- **建议方案**：按天取全部 `dayNotes`；单训练者保持旧格式“当日备注：X”；多训练者逐条输出“当日备注（用户名）：X”，按 userId 排序。
- **预估工作量**：S（1-2 小时）。
- **风险**：单训练者导出文本必须字节级不变（既有测试与用户习惯）；多用户排序稳定。

### L07 清空数据不清训练会话；导出失败静默 [本轮]
- **证据/位置**：`ui/settings/SettingsViewModel.kt:137-151`（未删 `workout_session`）；`ui/settings/SettingsScreen.kt:62-74,222-226`（`openOutputStream` 失败/返回 null 时无任何反馈）；HANDOVER §5 “导出未做端到端核对”。
- **影响范围**：清空后若恰有进行中会话，通知/会话状态短暂悬空；SAF 写入失败用户无感知。
- **复现/验证方式**：单测预置会话行 + 清空数据断言会话删空；真机选择不可写 URI（本轮不做真机，代码审查 + 构建）。
- **优先级**：P2。
- **建议方案**：`clearAllData` 增删 `workout_session`；导出写入返回 Boolean 并 try/catch，成功/失败 Toast；清空成功也提示。
- **预估工作量**：S（1 小时，UI 侧由前端 agent 完成 Toast）。
- **风险**：删除会话后若通知仍挂着，靠既有“计划为空 → render stop”自愈；需在验收清单记录。

### L08 首页日期跨天不刷新、会话隔天残留依赖冷启动 [本轮]
- **证据/位置**：`ui/home/HomeScreen.kt:49`（`today` 只在重组时求值，无生命周期/定时刷新）；`MainActivity.kt`（无 `onResume` 钩子）；`session/WorkoutSessionManager.kt:62-76`（`onAppStart` 只在 `Application.onCreate` 调用）。
- **影响范围**：App 隔夜常驻前台/后台后回到主页仍是昨天；“开始训练”写到昨天；隔天残留通知要等杀进程重启才清。
- **复现/验证方式**：修改系统日期模拟隔天（或代码审查 + 新增 Lifecycle 调用）；手工验收项列入真机清单。
- **优先级**：P2。
- **建议方案**：HomeScreen 用 `LifecycleEventEffect(ON_RESUME)` 刷新 today 并作为 ViewModel key；MainActivity `onResume` 调 `Graph.sessionManager.onAppStart()`（既有幂等清理）。
- **预估工作量**：S（1 小时）。
- **风险**：频繁 attach/detach 会重发通知（`setOnlyAlertOnce` 已抑制声音）；不要把每次 onResume 都当作新会话。

### L09 通知连续快速点击“直接打卡”可能丢一次打卡 [未选]
- **证据/位置**：`session/SessionActionReceiver.kt:23-37`（每次广播新协程 goAsync，无串行化）；`QuickCheckIn.nextSet` 读-算-写之间无原子性；唯一键 REPLACE 会把两次点击压成同一行。
- **影响范围**：训练中连点通知按钮，进度少记一组；需要回 app 补打。
- **复现/验证方式**：同时发两次广播（`adb shell am broadcast` 两次）观察组数；或代码审查。
- **优先级**：P2。
- **建议方案**：Receiver companion 加 `Mutex` 串行化两个 action；`withLock` 内完成读-算-写；与 L01 事务叠加。
- **预估工作量**：S。
- **风险**：Mutex 生命周期随进程；广播处理时长仍受 goAsync 约束。

### L10 进行中通知的“已进行 X 分钟”不会自行增长 [未选]
- **证据/位置**：`session/WorkoutSessionManager.kt:165-170`（仅在数据流变化时 render；无定时器）；HANDOVER §4 第七轮验收项“进行中已进行…”。
- **影响范围**：训练中通知文案陈旧，分钟数停在最后一次交互。
- **复现/验证方式**：开始训练→静置 5 分钟→看通知仍显示旧分钟数。
- **优先级**：P2。
- **建议方案**：优先用 `setUsesChronometer(true)` + `setWhen(now-startedAt)` 让系统自绘计时；或 session 活跃期每 60s 触发一次 render。
- **预估工作量**：S-M。
- **风险**：改动通知文案口径，需同步更新真机验收项与 README。

### L11 打卡路径全表扫描 [未选]
- **证据/位置**：`ui/plan/WorkoutViewModel.kt:248-253`（每次新打卡读全量组合 + 全量当次重量）；`session/SessionActionReceiver.kt:64-68`（同）。
- **影响范围**：数据量增长后打卡延迟；当前小数据下可忽略。
- **复现/验证方式**：构造 1 万条当次重量后测打卡耗时（基准测试/手工）。
- **优先级**：P3。
- **建议方案**：新增 `ComboDao.exerciseIdForItem(id)` 与 `SessionWeightDao.get(date, exerciseId, userId)` 直查；两入口共用。
- **预估工作量**：S。
- **风险**：Fake 需同步实现；改动打卡主链路需回归既有 10+ 测试。

### L12 默认次数口径在清单与通知之间不完全一致 [未选]
- **证据/位置**：`ui/plan/WorkoutViewModel.kt:76-77`（`defaultRepsFor` 取“上一行”）；`util/QuickCheckIn.kt:30-32`（取“已打卡组里组序最大的行”）。乱序打卡时两者可能不同。
- **影响范围**：手动补打与通知直接打卡的默认次数差异；只影响预填，不损坏数据。
- **复现/验证方式**：仅打第 1、3 组后手动补第 2 组（清单默认取第 1 组次数），通知推进到第 2 组时取第 3 组次数；单测可复现。
- **优先级**：P3。
- **建议方案**：统一为“最近一个已打卡组”，抽纯函数共用。
- **预估工作量**：S。
- **风险**：改变既有默认次数行为，需用户可感知确认（本轮不做）。

### L13 fallbackToDestructiveMigration 静默清库风险 [未选]
- **证据/位置**：`data/db/AppDatabase.kt:266`；HANDOVER §4 既有弃用警告。
- **影响范围**：漏写迁移/降级时用户数据被静默清空，无提示；本地离线应用无云端备份。
- **复现/验证方式**：构造缺失迁移的版本路径（需改版本号）或代码审查。
- **优先级**：P2（低频高损）。
- **建议方案**：移除 destructive fallback，改为显式迁移失败时保留旧库并提示；或至少在升级路径上补迁移测试与 schema 导出。
- **预估工作量**：M-L（涉及 Room schema 导出/迁移测试依赖）。
- **风险**：移除 fallback 后未知版本直接崩溃，需先补齐全部迁移与错误兜底；本轮不做。

### L14 组合/动作编辑退出无未保存提示 [未选]
- **证据/位置**：`ui/combo/ComboEditScreen.kt:126-129`、`ui/exercise/ExerciseEditScreen.kt:108-112`（返回即丢弃草稿）。
- **影响范围**：误触返回丢失编辑内容。
- **复现/验证方式**：编辑多行后按返回；内容丢失。
- **优先级**：P3。
- **建议方案**：返回时若 dirty 弹确认丢弃对话框。
- **预估工作量**：S。
- **风险**：与系统返回手势/预测返回的交互需测试。

### L15 训练记录删除无二次确认 [本轮]
- **证据/位置**：`ui/log/LogScreen.kt:128-130`（删除图标直接 `vm.deleteEntry`）；同页新增/编辑有对话框但删除无确认。
- **影响范围**：误删记录不可撤销（无回收站/撤销）。
- **复现/验证方式**：点击记录卡删除图标，立即消失。
- **优先级**：P2。
- **建议方案**：复用 `ui/Common.kt` 的 `ConfirmDialog`，文案“删除这条训练记录？不可撤销”。
- **预估工作量**：S。
- **风险**：低。

### L16 自动排程对话框默认绑定值在组合异步加载后不更新 [本轮]
- **证据/位置**：`ui/plan/PlanDialogs.kt:191-195`（`remember { mutableStateOf(combos.getOrNull(0)?.id) }`，组合 flow 首次为空时永远为空，且无 `LaunchedEffect(combos)`）。
- **影响范围**：冷启动立即点“自动排程”时槽位为空，用户必须手动逐一选择；若未注意直接点“生成”，空槽位按休息处理，生成不完整计划。
- **复现/验证方式**：冷启动后立刻点自动排程；观察三个槽位；或 Compose 预览/手工。
- **优先级**：P2。
- **建议方案**：`LaunchedEffect(combos)` 在槽位为 null 时填充首个组合；练一休一勾选为空时填 `combos.take(2)`；用户已手动改过的不覆盖。
- **预估工作量**：S。
- **风险**：不要在每次组合变化时覆盖用户选择。

### L17 导出全量读表、内存拼接 [未选]
- **证据/位置**：`ui/settings/SettingsViewModel.kt:85-134`（`getAll`/全量 observeAll first）+ `MarkdownExporter` 字符串拼接。
- **影响范围**：长期使用后导出内存峰值与耗时；当前量级可接受。
- **复现/验证方式**：造 5 万行打卡后导出计时。
- **优先级**：P3。
- **建议方案**：按范围查询、流式写文件；短期仅记账。
- **预估工作量**：M。
- **风险**：SAF 流式写与错误处理联动，改动面大。

### L18 单位/日期/重量文案不一致 [本轮部分]
- **证据/位置**：`ui/pr/PrScreen.kt:99`（`52.5kg` 无空格）对照 `ui/home/HomeScreen.kt:191`（`52.5 kg`）；`ui/plan/PlanScreen.kt:188-192` 周标题无年份；`ui/settings/SettingsScreen.kt:152` 版本号硬编码 `v1.0.0`。
- **影响范围**：视觉一致性、设置关于页版本可能与实际构建不符。
- **复现/验证方式**：逐页目视 + 构建产物 versionName 对照。
- **优先级**：P3。
- **建议方案**：PR 行补空格；版本号改 `BuildConfig.VERSION_NAME`（必要时开 `buildFeatures.buildConfig`）；跨年周标题补年份。
- **预估工作量**：S。
- **风险**：改周标题格式可能影响截图/验收习惯，本轮只做 PR 空格与版本号。

### L19 “只记完成”组的快照重量不进导出（已知取舍） [未选]
- **证据/位置**：`export/MarkdownExporter.kt:250-254`（reps=null 时输出“只记完成”）；ADR-0003 Consequences 明示。
- **影响范围**：数据完整性与导出一致性；属已拍板取舍。
- **复现/验证方式**：记重量→打卡（不设次数）→导出。
- **优先级**：P3。
- **建议方案**：维持现状，若未来改需同时改 ADR 与导出测试。
- **预估工作量**：—
- **风险**：误改会破坏第八轮既定口径。

### L20 全部组标逃后会话不会自动完成 [未选]
- **证据/位置**：`session/WorkoutSessionManager.kt:131`（完成判定只看打卡）；用户手动把全部组标逃后无自动收口。
- **影响范围**：会话需手动“结束训练”；通知仍显示进行中。
- **复现/验证方式**：清单内把全部组标逃，观察通知与主页按钮。
- **优先级**：P3。
- **建议方案**：定义“无剩余可打卡组（全部打卡或逃）”也进入完成分支，文案区分“已完成/已结束”。
- **预估工作量**：S。
- **风险**：与“逃不算完成”的领域语义冲突，需产品口径确认（本轮按现有语义不动）。

### L21 写库失败无任何用户反馈 [未选]
- **证据/位置**：所有 `viewModelScope.launch { daos... }`（如 `WorkoutViewModel.checkIn`、`SettingsViewModel.updateSettings`）无 try/catch。
- **影响范围**：磁盘满/DB 异常时按钮无反应，用户以为没点到。
- **复现/验证方式**：注入抛异常 DAO（测试）或代码审查。
- **优先级**：P3。
- **建议方案**：统一 `viewModelScope.launch { runCatching { … }.onFailure { 提示 } }` 或在 VM 层加错误通道。
- **预估工作量**：M。
- **风险**：引入全局错误通道是架构变更，本轮不做。

### L22 Fake 测试替身与真库语义差异 [未选]
- **证据/位置**：`app/src/test/java/com/fitplan/app/FakeDaos.kt:156-160`（删除组合不级联条目，真库外键级联）；`FakeExerciseDao.delete` 不级联组合条目；HANDOVER §5 记 Fake 自增游标差异。
- **影响范围**：部分跨表场景测试覆盖不到真实行为。
- **复现/验证方式**：对照 Room 外键声明与 Fake 实现。
- **优先级**：P3。
- **建议方案**：Fake 与真库级联语义对齐（至少注释说明），或在关键测试中显式预置级联结果。
- **预估工作量**：S。
- **风险**：改 Fake 可能让既有测试失效；本轮只在新增测试里显式处理。

---

## 3. 前端展示优化清单

目标：在不改变产品红线与既有 M3 视觉语言（淡色页底 + 白色 24dp 卡片 + 细分隔线 + 动态取色）的前提下，提升信息密度、状态反馈、可访问性与响应式表现。每项给出优化目标、改动范围、验收标准、回归风险。

### F01 首页今日概览与休息日导航 [本轮]
- **优化目标**：打开主页即知“今天练了多少、还剩多少”；休息日能一步到计划页安排。
- **改动范围**：`ui/home/HomeScreen.kt`、`ui/AppNav.kt`；新增 `ui/plan/WorkoutProgress.kt`（纯函数 + 测试）。
- **验收标准**：训练日显示“已完成 X/Y 组”，全完成追加“今日完成”；空组合显示引导而非空白清单；休息日卡片含“去「计划」页安排”并能切到计划 Tab；单元测试覆盖 0/部分/全部完成与空列表。
- **回归风险**：主页高度增加可能挤压 LazyColumn；保持 `weight(1f)` 包裹，禁止外层 verticalScroll。

### F02 清单可用性与无障碍 [本轮]
- **优化目标**：TalkBack 能正确播报打卡状态；小点击目标达到 48dp；每个动作有可见进度；空组合有解释。
- **改动范围**：`ui/plan/WorkoutScreen.kt`。
- **验收标准**：组圈用 `toggleable(role=Checkbox)`，状态可被读屏识别；标逃/组重量芯片点击区域 ≥48dp；动作卡显示细进度条；空组合显示“这个组合还没有动作，请到「组合」页添加”。
- **回归风险**：`toggleable` 与既有 `clickable` 语义切换需保证点击仍触发打卡/取消；进度条在 sets 为空时不得除零。

### F03 加载态统一，消除空态闪烁 [本轮]
- **优化目标**：冷启动时不再先显示“未安排/暂无记录”再跳成数据。
- **改动范围**：`ui/plan/PlanScreen.kt`、`ui/combo/ComboListScreen.kt`、`ui/log/LogScreen.kt`；VM 侧契约由逻辑 agent 提供（nullable StateFlow）。
- **验收标准**：三个页面首次进入显示“加载中…”；加载完成后空态/内容正确；单元测试覆盖 VM 初始 null → 数据。
- **回归风险**：nullable 改造影响既有测试与调用点；确保 `stateIn` 初始值语义一致。

### F04 自动排程默认绑定随数据到达 [本轮]
- **优化目标**：冷启动立即打开自动排程也有合理默认值。
- **改动范围**：`ui/plan/PlanDialogs.kt`。
- **验收标准**：组合异步加载后槽位/循环勾选自动补齐；用户已修改的值不被覆盖；组合为空时仍禁用生成。
- **回归风险**：LaunchedEffect 重入覆盖用户选择；需用“仅当 null/空时填充”的条件。

### F05 导出/删除反馈 [本轮]
- **优化目标**：导出成功/失败、清空完成有明确反馈；训练记录删除有二次确认。
- **改动范围**：`ui/settings/SettingsScreen.kt`、`ui/log/LogScreen.kt`、`ui/exercise/ExerciseListScreen.kt`（删除文案同步 L05）。
- **验收标准**：导出成功 Toast“已导出”；写入异常 Toast“导出失败，请重试”；取消 SAF 不提示；训练记录删除需确认；删除动作确认文案说明打卡/逃组会一并清理。
- **回归风险**：Toast 在 Compose 协程中的 context 生命周期；导出大文件时主线程不阻塞。

### F06 列表可读性与响应式 [本轮]
- **优化目标**：长组合名/多动作/四个字段徽标不撑破卡片；PR 文案单位一致；设置页版本取自构建。
- **改动范围**：`ui/combo/ComboListScreen.kt`、`ui/exercise/ExerciseListScreen.kt`、`ui/pr/PrScreen.kt`、`ui/settings/SettingsScreen.kt`、`FitPlan/app/build.gradle.kts`（仅当需要 buildConfig）。
- **验收标准**：组合卡片最多展示 3 行动作摘要 + “等 N 个动作”；长名称单行省略；动作库四个徽标可换行不溢出；PR 重量显示“52.5 kg”；关于页版本与 versionName 一致。
- **回归风险**：FlowRow 的实验 API 注解；BuildConfig 生成开关可能影响构建，需要全量构建验证。

### 未选前端项（记账，后续轮次）
- 周计划训练日完成度徽标/今日高亮（需新增聚合数据流）。
- 全局 Snackbar/撤销机制（需引入跨页宿主）。
- 编辑页未保存返回确认（L14）。
- 深色主题/对比度专项审计（依赖真机目视）。
- 横屏/平板双栏布局（当前手机单列是产品默认）。

---

## 4. 本轮迭代目标与非目标

### 4.1 目标（可验收）
1. 落地 7 项逻辑修复：事务边界、空组合收口、复制上周精确镜像、目标值校验、删动作孤儿清理、多用户导出备注、清空数据会话清理；并补加载态契约。
2. 落地 6 项前端优化：首页概览/休息日导航、清单无障碍与进度、加载态、自动排程默认值、导出/删除反馈、列表可读性。
3. 全量 JVM 测试从 106 增至 ≥120 且全绿；`:app:assembleDebug` 通过；零新增编译警告（基线 2 条弃用警告之外）。
4. 更新 HANDOVER、README、CONTEXT（如涉及术语）、iteration plan、verification 报告与 completion report。
5. 产出 `FitPlan-iteration11-debug.apk`（debug 变体，供后续真机验收）。
6. 无未解决 P0/P1 问题；所有未选问题带优先级与建议方案记录在案。

### 4.2 非目标
- 不新增数据库表/列，不改 DB 版本（保持 v11，零迁移风险）。
- 不新增依赖（除非测试必需且离线缓存可用；默认不加）。
- 不改需求红线，不新增统计/图表/云/账号/AI 功能。
- 不做真机验收（本机无设备）；只输出真机验收清单。
- 不做 L09/L10/L11/L13/L14/L17/L21/L22 等未选问题（保留在 §2）。
- 不重构导航架构、不引入 DI、不引入 Compose UI 测试框架。
- 不做深度视觉改版（仅现有 M3 语言内的信息与反馈优化）。

---
## 5. 任务拆分（可直接指导 agent 开发）

任务编号规则：`T-L*` 逻辑/数据；`T-F*` 前端；`T-V*` 验证；`T-D*` 文档。每个任务给出文件、接口契约、步骤、测试与验收。测试一律 JVM + FakeDaos，不引入新测试基础设施。

### T-L0 事务边界基础设施（P1，依赖：无）
**文件**
- 修改：`FitPlan/app/src/main/java/com/fitplan/app/data/db/Daos.kt`（`AllDaos`）
- 修改：`FitPlan/app/src/main/java/com/fitplan/app/data/db/AppDatabase.kt`
- 修改：`FitPlan/app/src/test/java/com/fitplan/app/FakeDaos.kt`
- 修改（包事务）：`ui/combo/ComboViewModels.kt`、`ui/log/LogViewModel.kt`、`ui/settings/SettingsViewModel.kt`、`ui/exercise/ExerciseViewModels.kt`、`ui/plan/PlanViewModel.kt`、`ui/plan/WorkoutViewModel.kt`、`session/SessionActionReceiver.kt`
- 新增测试：`FitPlan/app/src/test/java/com/fitplan/app/TransactionRollbackTest.kt`

**接口契约**
~~~kotlin
// Daos.kt, AllDaos 内
suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
// AppDatabase
override suspend fun <T> runInTransaction(block: suspend () -> T): T = withTransaction(block)
~~~
- 禁止嵌套调用 `runInTransaction`（各 VM 入口只包一层）。
- FakeDaos 覆盖该方法：进入前快照全部 MutableStateFlow 当前值，异常时恢复后重抛；正常返回不回滚。
- FakeComboDao 增加 `var failOnNextInsertItems: Boolean = false`，为回滚测试注入失败。

**步骤**
1. 写 `TransactionRollbackTest`：预置组合（2 条目）+ 该条目打卡；设置 `failOnNextInsertItems=true`；调用 `ComboEditViewModel.save`（草稿只留 1 个旧条目 + 1 个新动作）；断言抛异常后组合仍为原 2 条目、打卡仍在。
2. 跑测试确认失败（无事务时组合已被删条目）。
3. 实现 `runInTransaction` 与包装；跑测试转绿。
4. 逐个包装其余多段写入口，跑全量测试防回归。

**测试与验收**
- 新测试通过；既有 106 个全绿；`ComboEditViewModelTest` 8 个不受影响。
- 事务只包写操作，不在事务内做 UI 回调（`onDone` 放在事务后）。

### T-L1 空组合收口（P2，依赖：无）
**文件**
- 修改：`ui/plan/WorkoutViewModel.kt`（新增顶层 `isWorkoutAllDone`）
- 修改：`session/WorkoutSessionManager.kt`（空 items 结束会话）
- 测试：`app/src/test/java/com/fitplan/app/ui/plan/WorkoutUiStateTest.kt`

**接口契约**
~~~kotlin
fun isWorkoutAllDone(items: List<WorkoutItemUi>): Boolean =
    items.isNotEmpty() && items.all { it.doneCount >= it.sets.size }
~~~
- `render()`：`comboName == null` 仍走原分支；新增 `items.isEmpty()` → `stop(); return`；随后 `allDone = isWorkoutAllDone(items)`。
- 不改 `WorkoutUiState` 字段，UI 用 `comboName != null && items.isEmpty()` 判断空组合。

**测试**
- 空组合（combo 存在、无 items）：`comboName` 非空、`items` 为空、`loading=false`。
- `isWorkoutAllDone`：空列表 false；部分完成 false；全部完成 true。
**验收**：全量测试绿；Manager 不再可能出现 0/0 常驻通知（静态审查 + 纯函数测试）。

### T-L2 复制上周精确镜像（P2，依赖：T-L0）
**文件**：`ui/plan/PlanViewModel.kt`；测试 `ui/plan/PlanViewModelTest.kt`。
**实现**
~~~kotlin
fun copyWeek(sourceMonday: Long, targetMonday: Long, onDone: (Int) -> Unit) = viewModelScope.launch {
    val source = daos.dayPlanDao.getRange(sourceMonday, sourceMonday + 6).associateBy { it.date }
    var copied = 0
    for (offset in 0L..6L) {
        val targetDate = targetMonday + offset
        val src = source[sourceMonday + offset]
        if (src == null) daos.dayPlanDao.deleteByDate(targetDate)
        else { daos.dayPlanDao.upsert(src.copy(date = targetDate)); copied++ }
    }
    onDone(copied)
}
~~~
- 包在 `daos.runInTransaction`（T-L0 完成后）。
**测试**：目标周有 7 天旧安排、源周仅 2 天（1 训练 1 休息）→ 复制后目标周只剩 2 天且内容一致；返回计数=2；源周不变。

### T-L3 目标数值校验（P2，依赖：无）
**文件**
- 新增：`FitPlan/app/src/main/java/com/fitplan/app/util/InputRules.kt`
- 修改：`ui/Common.kt`（NumberField）、`ui/combo/ComboEditScreen.kt`、`ui/log/EntryDialog.kt`、`ui/exercise/ExerciseEditScreen.kt`（重量字段传新参数）
- 新增测试：`app/src/test/java/com/fitplan/app/util/InputRulesTest.kt`

**接口契约**
~~~kotlin
object InputRules {
    const val MAX_SETS = 50
    const val MAX_REPS = 999
    const val MAX_DURATION_MIN = 1440
    fun isIntInputAllowed(text: String): Boolean
    fun isWeightInputAllowed(text: String): Boolean
    fun parseIntInRange(text: String, min: Int, max: Int): Int?
    fun parsePositiveInt(text: String, max: Int): Int?
}
~~~
- `NumberField` 参数：`allowDecimalNegative: Boolean = false`；true 时用 `isWeightInputAllowed`，否则 `isIntInputAllowed`。
- 重量字段（组合目标重量、动作默认重量、记录重量）传 `true`。
- 组合编辑：sets/reps/duration 用 `parseIntInRange`（空=null，非法显示错误并禁用保存）；记录编辑同。
- 既有脏数据：加载出负数时显示“需为 1 以上”，清空或改值后可保存。

**测试**：整数输入拒绝负号与小数点；重量输入接受 `-20.5`、`0`；`parsePositiveInt("0",50)==null`；`parsePositiveInt("51",50)==null`；`parseIntInRange("",1,50)==null`；`parsePositiveInt("8",999)==8`。
**验收**：编译通过；构建；上述测试绿；组合/记录 UI 不出现负数目标。

### T-L4 删除动作孤儿清理（P2，依赖：T-L0）
**文件**：`ui/exercise/ExerciseViewModels.kt`；新增测试 `app/src/test/java/com/fitplan/app/ui/exercise/ExerciseListViewModelTest.kt`。
**实现**：`runInTransaction` 内：`comboDao.observeAllWithItems().first()` → 过滤 `exerciseId` 收集 item ids → `setCheckinDao.deleteForComboItems(ids)` + `setSkipDao.deleteForComboItems(ids)` → `exerciseDao.delete(exercise)`。
**测试**：两个动作、组合各含 1 条目；删除动作 A 后 A 的打卡/逃标清空、动作 A 消失；动作 B 的打卡保留。
**验收**：新测试绿；全量绿；`ExerciseListScreen` 文案同步（前端 F05）。

### T-L5 多训练者导出当日备注（P2，依赖：无）
**文件**：`export/MarkdownExporter.kt`；测试 `app/src/test/java/com/fitplan/app/export/MarkdownExporterTest.kt`。
**实现**：`appendCheckins` 按天取全部 notes；`multiUser` 时逐条 `当日备注（用户名）：内容`（按 userId 升序）；单用户保持 `当日备注：内容`（不变）。
**测试**：两用户同一天打卡 + 备注 → 输出含两条带用户名的备注；单用户输出仍为旧格式且不出现“（用户名）”。
**验收**：既有导出测试全绿（更新受影响的断言需说明）；新增测试绿。

### T-L6 清空数据会话清理 + 加载态契约（P2，依赖：T-L0）
**文件**
- `ui/settings/SettingsViewModel.kt`：`clearAllData` 内补 `daos.workoutSessionDao.delete()`，整体包事务。
- 加载态 nullable 契约：
  - `ui/plan/PlanViewModel.kt`：`val dayPlans: StateFlow<Map<Long, DayPlanEntity>?>`，初始 `null`。
  - `ui/log/LogViewModel.kt`：`val entries: StateFlow<List<LogEntryWithSets>?>`，初始 `null`。
  - `ui/combo/ComboViewModels.kt`：`ComboListViewModel.combos: StateFlow<List<ComboWithItems>?>`，初始 `null`。
- 测试：新增 `app/src/test/java/com/fitplan/app/ui/settings/SettingsViewModelTest.kt`（清空数据清会话、保留设置与训练者）；`PlanViewModelTest` 适配 nullable + 增加“初始 null → 数据到达非 null”1 条。
**接口契约（前端依赖）**：上述三个 StateFlow 初始值必须为 `null`，数据到达后为非 null；UI 以 null 显示“加载中…”。

### T-F1 首页概览/休息日导航/跨天刷新（P2，依赖：无）
**文件**：`ui/home/HomeScreen.kt`、`ui/AppNav.kt`、`MainActivity.kt`；新增 `ui/plan/WorkoutProgress.kt` + 测试 `app/src/test/java/com/fitplan/app/ui/plan/WorkoutProgressTest.kt`。
**接口契约**
~~~kotlin
data class WorkoutProgress(val doneSets: Int, val totalSets: Int) {
    val allDone: Boolean
    val summaryText: String // “已完成 3/12 组” / “今日完成 · 12/12 组”
}
fun workoutProgress(items: List<WorkoutItemUi>): WorkoutProgress
~~~
- `AppNav.MainScaffold`：tab 0 传 `onOpenPlan = { tab = 1 }`；HomeScreen 签名加 `onOpenPlan: () -> Unit = {}`。
- HomeScreen：休息日卡片加“去「计划」页安排”TextButton；训练日清单上方加进度行；`state.items.isEmpty()`（组合存在）时显示空组合提示且隐藏“开始训练”。
- 跨天：`var today by remember { mutableStateOf(TimeUtil.today().toEpochDay()) }` + `LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { today = TimeUtil.today().toEpochDay() }`；ViewModel key 仍为 `home-$today`。
- `MainActivity.onResume()` 调 `Graph.sessionManager.onAppStart()`（Application 已初始化）。
**测试**：`workoutProgress` 空列表、部分、全部完成 3 条。
**验收**：编译通过；构建；测试绿；空组合/休息日/进度行交互代码审查。

### T-F2 清单可用性与无障碍（P2，可并行）
**文件**：`ui/plan/WorkoutScreen.kt`。
**要点**
- 空组合分支：`state.comboName != null && state.items.isEmpty()` → 居中提示 + 返回按钮；修复 `EmptyHint` 在 `Column` 中 `fillMaxSize` 挤掉按钮的问题（用 `Box(Modifier.weight(1f))` 或 `Column(verticalArrangement=Center)` + `TextButton`）。
- `SetCircle`：`Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { if (checked) onCancel() else onCheckIn() })`；`semantics { contentDescription = ... }` 保留；补 `stateDescription = if (checked) "已打卡" else "未打卡"`。
- “标逃/取消逃标”文字点击区：`heightIn(min = 48.dp)`、`padding(horizontal = 12.dp)`；组重量芯片同样处理。
- 动作卡进度条：`LinearProgressIndicator(progress = { doneCount / sets.size.toFloat() }, ...)`，sets 为空不渲染。
- `AggregateCircle` 加 `contentDescription = if (allDone) "动作已完成" else "动作未完成"`。
- 动作备注“保存”按钮：仅在 `text.trim() != (todayNote ?: "")` 且非空时启用。
**验收**：编译 + 构建；读屏语义静态审查；不得改变既有打卡/标逃/重量编辑行为。

### T-F3 加载态统一（P2，依赖：T-L6 契约）
**文件**：`ui/plan/PlanScreen.kt`、`ui/combo/ComboListScreen.kt`、`ui/log/LogScreen.kt`。
**要点**：`dayPlans` 为 null → 页面主体显示 `EmptyHint("加载中…")`（TopAppBar 保留）；`combos`、`entries` 同理；非 null 后走既有空态/内容。
**验收**：编译；冷启动不出现“未安排/暂无记录/还没有训练组合”闪烁（代码审查）；既有交互不变。

### T-F4 自动排程默认值（P2，依赖：无）
**文件**：`ui/plan/PlanDialogs.kt`。
**要点**：`LaunchedEffect(combos)`：`slot1/slot2/slot3` 为 null 时取 `combos.getOrNull(0..2)?.id`；`cycleIds` 为空时取 `combos.take(2).map { it.id }`；用户已选值不覆盖；combos 为空时生成按钮仍禁用。
**验收**：编译；构建；代码审查覆盖“加载后补齐/不覆盖用户选择”两条路径。

### T-F5 导出/删除反馈（P2，依赖：无）
**文件**：`ui/settings/SettingsScreen.kt`、`ui/log/LogScreen.kt`、`ui/exercise/ExerciseListScreen.kt`。
**要点**
- `writeMarkdown` 返回 Boolean（`openOutputStream` 为 null 或抛异常 → false）；SAF 回调 `scope.launch { val ok = writeMarkdown(...); Toast(...) }`；成功“已导出 Markdown”，失败“导出失败，请重试”。
- 清空数据成功后 Toast“已清空所有数据”。
- 训练记录删除：`var pendingDelete by remember { mutableStateOf<LogEntryWithSets?>(null) }` + `ConfirmDialog`；确认后删除。
- 动作删除确认文案补“该动作的打卡与逃组记录会一并清理”。
**验收**：编译；构建；导出取消不提示、失败提示可读；删除必须确认。

### T-F6 列表可读性/响应式/文案（P3，依赖：无）
**文件**：`ui/combo/ComboListScreen.kt`、`ui/exercise/ExerciseListScreen.kt`、`ui/pr/PrScreen.kt`、`ui/settings/SettingsScreen.kt`、`FitPlan/app/build.gradle.kts`（仅当需要 BuildConfig）。
**要点**
- 组合卡：动作列表最多 3 行，超出显示“等 N 个动作”；组合名 `maxLines = 1` + `TextOverflow.Ellipsis`。
- 动作库：四个 `FieldBadge` 放进 `FlowRow`（`@OptIn(ExperimentalLayoutApi::class)`），长名称 `maxLines = 1`。
- PR 行重量：`TimeUtil.formatWeight(it) + " " + unit`（补空格）。
- 设置关于：`BuildConfig.VERSION_NAME`；若构建报未生成 BuildConfig，则加 `buildFeatures { buildConfig = true }`。
- 周计划周标题暂不改（避免验收口径变化）。
**验收**：全量构建+测试绿；长文本/四徽标不溢出（代码审查）；关于页显示 1.0.0。

### T-V1 独立验证与验收测试（P2，依赖：T-L0..L6、T-F1..F6 完成）
**文件**：新增 `FitPlan/app/src/test/java/com/fitplan/app/Iteration11AcceptanceTest.kt`；新增 `/Users/suyi/projects/exercise/docs/iteration11-verification.md`。
**职责**
1. 独立复核：逐条对照本计划验收标准；检查无 DB 版本/迁移改动、无新增依赖、写作用域无越界。
2. 新增跨切面验收测试（不与开发测试重复）：
   - 多训练者完整隔离：A 打卡 + 当日备注 + 体重 + 当次重量；B 看不到 A 的进度/PR 数据，导出含两人备注。
   - 空组合：`buildWorkoutUiState` 空 items + `isWorkoutAllDone` false。
   - 复制上周 + 移除安排 + 历史保留组合场景。
   - 删动作后孤儿清理 + 其他动作保留。
3. 运行 `sh .scratch/iteration11/with-gradle-lock.sh clean :app:testDebugUnitTest :app:assembleDebug --console=plain`，记录 tests/failures/errors 与警告；把原始输出摘要写入 verification 文档。
4. 输出验收矩阵（计划项 → 证据 → PASS/FAIL）与残余风险；FAIL 项回报 Lead，不自行修改他人文件。

### T-D1 文档与交接（Lead，依赖：T-V1）
- 更新 `FitPlan/HANDOVER.md`：状态行、§3 结构（InputRules/WorkoutProgress/事务边界）、§4 第十一轮块、§5 剩余待办（未选问题 + 真机验收清单）、§7 归档地图。
- 重写 `FitPlan/README.md` 至现状（六 Tab、当前重量语义、记录扩展、构建命令）。
- 更新 `CONTEXT.md`：仅在出现新领域术语时追加（本轮预计无新术语；事务/阶段门属工程实践，不进术语表）。
- 新增 `/Users/suyi/projects/exercise/docs/iteration11-report.md`：改动摘要、验证结果、遗留问题、后续建议。
- 阶段门评审记录追加至本文件 §11 与 `.scratch/iteration11/gates/`。

---

## 6. Agent 角色与编排方式

### 6.1 角色

| 角色 | 目标/职责 | 写作用域（workspace 相对） | 交付物 |
| --- | --- | --- | --- |
| Lead（主 agent） | 调研、计划、任务下发、冲突裁决、集成、构建、文档、阶段门 | `docs/**`、`FitPlan/HANDOVER.md`、`FitPlan/README.md`、`CONTEXT.md`、`.scratch/iteration11/**`（除各 agent 专属） | 迭代文档、完成报告、最终 APK |
| logic-dev（逻辑审查/方案设计 + 实现） | T-L0..T-L6；先复核计划中的逻辑问题证据，发现新增问题回报 Lead；实现与测试 | `FitPlan/app/src/main/java/com/fitplan/app/data/**`、`export/**`、`session/**`、`util/**`、`ui/plan/WorkoutViewModel.kt`、`ui/plan/PlanViewModel.kt`、`ui/settings/SettingsViewModel.kt`、`ui/combo/ComboViewModels.kt`、`ui/log/LogViewModel.kt`、`ui/exercise/ExerciseViewModels.kt`、`FitPlan/app/src/test/java/com/fitplan/app/**`（新增/修改对应测试） | 代码 + 测试 + 变更报告 |
| frontend-dev（前端实现） | T-F1..T-F6；在既有 M3 规范内实现；新增纯函数测试 | `ui/AppNav.kt`、`ui/Common.kt`、`ui/home/**`、`ui/plan/WorkoutScreen.kt`、`ui/plan/PlanScreen.kt`、`ui/plan/PlanDialogs.kt`、`ui/combo/ComboEditScreen.kt`、`ui/combo/ComboListScreen.kt`、`ui/combo/ExercisePickerDialog.kt`、`ui/exercise/ExerciseListScreen.kt`、`ui/exercise/ExerciseEditScreen.kt`、`ui/log/LogScreen.kt`、`ui/log/EntryDialog.kt`、`ui/settings/SettingsScreen.kt`、`ui/pr/PrScreen.kt`、`MainActivity.kt`、`ui/plan/WorkoutProgress.kt`（新增）、`FitPlan/app/src/test/java/com/fitplan/app/ui/plan/WorkoutProgressTest.kt`（新增）、`FitPlan/app/build.gradle.kts`（仅 BuildConfig 开关）、`util/InputRules.kt`（新增）与 `app/src/test/java/com/fitplan/app/util/InputRulesTest.kt`（新增） | 代码 + 测试 + 变更报告 |
| verifier（测试验证） | T-V1：独立验收、跨切面测试、全量构建证据、验收矩阵 | `FitPlan/app/src/test/java/com/fitplan/app/Iteration11AcceptanceTest.kt`（新增）、`docs/iteration11-verification.md`（新增）、`.scratch/iteration11/gates/verifier-*.md` | 验收测试 + 验证报告 |

### 6.2 编排机制

- 使用 DSH Agent Teams：Lead 通过 `spawn_teammate` 创建 3 名 teammate（fresh context），共享同一工作目录。
- 共享任务板：Lead 为三条工作流创建任务（含写作用域、依赖关系），teammate 通过 `team_task_get/update` 认领并完成；verifier 任务 `blocked_by` 前两者。
- 文件系统没有 git：**任何回滚以 `.scratch/iteration11/backup/src` 为基线**（APFS clone，2026-10-18 快照）。禁止执行 git 命令。
- 并发写：严格按 6.1 写作用域；跨域需求（如前端需要 VM 新字段）通过计划中的接口契约传递，必要时发消息给 Lead 裁决，不得直接修改他人文件。
- Gradle 串行：所有构建一律经 `sh /Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh <args>`；禁止直接 `./gradlew`（会绕过锁与 GRADLE_USER_HOME）。允许开发期只跑 `:app:compileDebugKotlin` 快速反馈；全量构建由 Lead/verifier 在阶段门执行。
- 每个任务完成标准：代码改完 + 对应测试写好并跑过（开发 agent 可跑单测类/编译）+ 在共享任务写“完成备注”（改动文件、测试命令与结果、风险）。
- 汇报格式：TLDR 结论；改动文件绝对路径；验证命令与原样输出摘要；未决问题/风险；下一步。
- 消息频率：不刷屏；每完成一个任务组或遇到阻塞时发一条；verifier 在 G2 前不打扰开发 agent。

### 6.3 阶段门（AskMatt 等价协议）

用户要求“每个关键阶段调用 AskMatt 技能判断当前开发阶段”。经全工作区与技能目录检索（`~/.dsh/skills`、DSH 应用目录），**当前环境不存在名为 AskMatt 的技能**（会话技能目录仅有 brainstorming/code-review/…/verification-before-completion 等）。为不阻塞任务，按以下等价协议执行，并记录为假设 A1：

**阶段门评审（Stage Gate Review，缩写 SGR）**：每个门输出四段——
1. **阶段结论**：当前处于计划/开发/测试前/发布前中的哪一阶段，是否可进入下一阶段（GO / NO-GO / CONDITIONAL）。
2. **风险判断**：本阶段新增/遗留风险，按 P0-P3，含证据。
3. **下一步行动**：最多 5 条，含负责人与验证命令。
4. **证据**：命令输出、测试计数、文件清单、评审人（Lead 或 verifier）。

四个门：
- **G0 计划完成门**：本文件评审通过、基线测试 106 绿、备份完成、任务板就绪。
- **G1 开发中门**（各 agent 首个任务组完成后）：抽查 diff、编译通过、无越界写入、接口契约一致。
- **G2 测试前门**：dev 全部任务完成、verifier 开始前；检查验收标准可测、无 P0/P1 未决、测试计划明确。
- **G3 发布前门**：全量 clean 构建 + 测试全绿、文档更新、验收矩阵 PASS、残余风险可接受。

记录位置：本文件 §11 + `.scratch/iteration11/gates/G0.md` 等。

---

## 7. 依赖关系与执行顺序

~~~text
T-L0 事务基础设施 ─┬─> T-L2 复制上周
                   ├─> T-L4 删动作清理
                   └─> T-L6 清空会话 + 加载态契约 ──> T-F3 加载态 UI
T-L1 空组合收口 ──(纯函数接口)──> T-F2 清单空态（可并行，UI 自行判空）
T-L3 目标校验（前端全权）── 独立
T-L5 导出备注 ── 独立
T-F1/F4/F5/F6 ── 独立（F3 依赖 T-L6）
T-L0..L6 + T-F1..F6 ──> T-V1 独立验证 ──> T-D1 文档与发布门
~~~

执行波次：
- **Wave 0（Lead）**：计划、备份、基线、任务板、G0。
- **Wave 1（并行）**：logic-dev 做 T-L0→T-L1→T-L2→T-L4→T-L5→T-L6；frontend-dev 做 T-F1→T-F2→T-F4→T-F5→T-F6→T-L3→T-F3（F3 等 T-L6 契约，可最后做）。两者无文件交集。
- **Wave 2（Lead 集成）**：G1 抽查 → 编译修复 → 通知 verifier。
- **Wave 3（verifier）**：T-V1 独立测试 + 全量 clean 构建，G2。
- **Wave 4（Lead）**：文档 T-D1、APK、G3、完成报告。

---

## 8. 里程碑与时间盒

| 里程碑 | 内容 | 时间盒（工作量预估） | 出口条件 |
| --- | --- | --- | --- |
| M0 计划与基线 | 本文件、备份、基线测试、任务板 | 已完成 | G0 GO |
| M1 逻辑实现 | T-L0..T-L6 | 约 6-8 个 agent 工作小时 | 代码+测试就绪、编译通过 |
| M2 前端实现 | T-F1..T-F6 + T-L3 | 约 5-7 个 agent 工作小时 | 代码+测试就绪、编译通过 |
| M3 集成 | 冲突裁决、编译修复、回归 | 1-2 小时 | 全量测试绿、构建绿 |
| M4 独立验证 | T-V1 | 1-2 小时 | 验收矩阵 PASS、G2 GO |
| M5 文档与发布 | T-D1、APK、G3 | 1-2 小时 | 文档更新、报告产出、G3 GO |

时间盒是自主执行预算，不向用户索要排期；每个门不合格则回到 Wave 修复，不跳过。

---

## 9. 测试与验收标准

### 9.1 自动化验证命令
~~~bash
# 全量 JVM 测试（要求 tests >= 120, failures=0, errors=0）
sh /Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh \
  clean :app:testDebugUnitTest --console=plain

# 全量构建（要求 BUILD SUCCESSFUL，零新增警告）
sh /Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh \
  :app:assembleDebug --console=plain

# 测试计数
grep -h 'tests=' /Users/suyi/projects/exercise/FitPlan/app/build/test-results/testDebugUnitTest/*.xml \
  | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}'
~~~

### 9.2 验收标准（逐条可核验）
1. **AC-01 事务回滚**：`TransactionRollbackTest` 证明组合保存中途失败后条目与打卡回到旧状态。
2. **AC-02 空组合**：`WorkoutUiStateTest` 空组合分支 + `isWorkoutAllDone(emptyList())=false`；训练页/主页出现空态引导（代码审查）。
3. **AC-03 复制上周**：`PlanViewModelTest` 目标周与源周逐日一致，源周无安排日目标周被删除。
4. **AC-04 目标校验**：`InputRulesTest` 全绿；组合/记录 UI 不能输入负数组数/次数/时长，重量仍可负数。
5. **AC-05 删动作清理**：`ExerciseListViewModelTest` 孤儿打卡/逃标清空且其他动作数据保留。
6. **AC-06 导出备注**：`MarkdownExporterTest` 多用户两条备注带用户标签；单用户格式不变。
7. **AC-07 清空数据**：`SettingsViewModelTest` 会话行清空、设置/训练者保留。
8. **AC-08 首页概览**：`WorkoutProgressTest` 全绿；休息日 CTA 与空组合提示（代码审查）。
9. **AC-09 无障碍**：组圈 `toggleable` + `Role.Checkbox`；标逃/重量芯片 `heightIn(min=48.dp)`（grep/代码审查）。
10. **AC-10 加载态**：三个页面 null 时显示“加载中…”（代码审查 + VM 测试）。
11. **AC-11 自动排程默认值**：LaunchedEffect 补齐且不覆盖用户选择（代码审查）。
12. **AC-12 反馈**：导出成功/失败 Toast、清空成功 Toast、训练记录删除确认（代码审查 + 构建）。
13. **AC-13 构建与测试**：tests ≥ 120、failures=0、errors=0；`:app:assembleDebug` 成功；警告不超过基线 2 条。
14. **AC-14 文档**：HANDOVER/README/iteration11-report/verification 更新完毕；本文件 §11 四个门记录齐全。
15. **AC-15 约束**：DB 仍为 v11（`grep "version = 11"`），无新增依赖（比对 `app/build.gradle.kts`），无越界文件改动（对 backup diff）。

### 9.3 真机验收清单（本轮不执行，交付给用户）
- v10→v11 旧库覆盖安装启动不崩、六类记录完好（沿用第十轮待办）。
- 首页隔夜跨天：日期/清单/开始训练指向新一天；隔天残留通知被清理。
- 空组合：主页/训练页提示、开始训练不挂 0/0 通知。
- 组合目标输入负数被阻止；重量负数/小数仍可输入。
- 删除动作后 PR/导出无残留、库不膨胀（需 adb/DB 抽查）。
- 多用户导出：两名训练者当日备注都出现且带用户标签。
- 训练记录删除确认；导出成功/失败 Toast。
- 冷启动加载态不闪“未安排/暂无记录”。
- 自动排程冷启动立即打开时槽位有默认值。
- 无障碍：TalkBack 播报组完成状态；标逃/重量点击区域可点。

---

## 10. 风险与回滚方案

| 风险 | 概率/影响 | 缓解 | 回滚 |
| --- | --- | --- | --- |
| 事务改造引入编译错误/行为回归 | 中/高 | 先加接口与 Fake 回滚测试，再逐入口包装；每步跑测试 | 从 `.scratch/iteration11/backup/src` 恢复对应文件；或仅回退 `runInTransaction` 包装保留接口 |
| 前端 nullable 契约与逻辑 agent 不同步 | 中/中 | 计划中固定签名；frontend 不自行改 VM；编译期暴露 | 恢复 UI 文件；VM 契约以逻辑 agent 为准 |
| 两个 agent 并发写/构建冲突 | 中/中 | 写作用域清单 + Gradle 锁脚本 + Lead 抽查 diff | 以 backup 为基线人工合并；重跑全量 |
| FakeDaos 事务快照漏字段导致测试误判 | 中/中 | 快照覆盖全部 13 个 fake 的 MutableStateFlow；回滚测试断言多表 | 修复 Fake（测试基础设施，不动生产） |
| 无真机导致 UI 行为未实测 | 高/中 | 代码审查 + 纯函数测试 + 构建；交付真机清单 | 记录为残余风险，用户真机验收后修复 |
| 无 git 导致误改不可追溯 | 中/高 | 备份 + 每阶段 `diff -ru`；禁止破坏性命令 | `cp -cR backup/src FitPlan/app/` 恢复后重建 |
| 范围膨胀导致本轮做不完 | 中/中 | 非目标清单；未选项只记账；阶段门 GO/NO-GO | 砍 T-F6/L05 等 P3/P2 低优先项，保留 AC 核心 |
| 构建环境（沙箱/离线）失败 | 低/高 | 已验证 `.gradle-home` + `--offline` + 锁脚本 | 重新克隆 `~/.gradle`；必要时向用户申请一次性沙箱升级（仅构建） |

回滚操作示例（工作区根执行）：
~~~bash
# 恢复单个文件
cp -c /Users/suyi/projects/exercise/.scratch/iteration11/backup/src/main/java/com/fitplan/app/ui/home/HomeScreen.kt \
      /Users/suyi/projects/exercise/FitPlan/app/src/main/java/com/fitplan/app/ui/home/HomeScreen.kt
# 全量恢复（谨慎，先确认目标路径）
cp -cR /Users/suyi/projects/exercise/.scratch/iteration11/backup/src/. \
       /Users/suyi/projects/exercise/FitPlan/app/src/
~~~

---

## 11. 阶段门评审记录（AskMatt 等价 SGR）

### G0 计划完成门（2026-10-18）
- **阶段结论**：GO。计划文档完成；基线测试 106 全绿；备份完成；任务板待创建。
- **风险判断**：P1 事务改造是最大不确定性；P2 前端 nullable 契约需严格按接口执行；无真机为已知残余风险。
- **下一步行动**：创建 DSH 共享任务；spawn logic-dev/frontend-dev/verifier；开始 Wave 1。
- **证据**：`/Users/suyi/projects/exercise/docs/iteration-plan.md`；基线命令输出 BUILD SUCCESSFUL / tests=106；备份目录 `.scratch/iteration11/backup` 852K。

### G1 开发中门（2026-10-18）
- **阶段结论**：GO（Wave 1 开发完成，进入集成/验证前）。
- **风险判断**：Room 真实事务只有编译级验证（Fake 快照模拟），真机需抽查强杀回滚；空组合通知收口仅纯函数测试 + 静态审查；syncToItem/syncToAll/receiver.skip 仍非事务（P3 记账）；无真机为残余风险。
- **下一步行动**：verifier 执行 Phase 2（独立验收测试 + clean 全量构建）；Lead 更新 HANDOVER/README/report 并准备 APK。
- **证据**：task-1/task-2 completed；tests=127 / failures=0 / errors=0（19 suites）；compileDebugKotlin 成功、零新增警告；DB v11 未变、无新依赖；写作用域无越界（对 backup diff）；裁决记录 logic-rulings.md / frontend-rulings.md。

### G2 测试前门（2026-10-18）
- **阶段结论**：首轮 NO-GO → 修复后 **GO**。首轮 clean 全量 131 tests / 1 failure（TransactionRollbackTest 顺序依赖 flaky，非生产失败）；logic-dev 仅改测试（直接事务复刻 save 写序列 + 同步捕获）后连续 3 次全量 131/0/0；verifier 带 `ANDROID_USER_HOME` 前置连续 2 次 clean :app:testDebugUnitTest :app:assembleDebug 全绿（BUILD SUCCESSFUL 18s/17s）。
- **风险判断**：无生产 P0/P1；测试 flaky 已关闭；真实 Room 事务/通知行为仍待真机抽查；无真机为残余风险。
- **下一步行动**：T-D1 文档、APK 交付、G3。
- **证据**：`docs/iteration11-verification.md` v1.0；`.scratch/iteration11/gates/verifier-G2.md`；APK 63,249,629 bytes；警告 2 条与基线一致。

### G3 发布前门（2026-10-18）
- **阶段结论**：**GO（自动化部分）**。131 tests / 0 failures / 0 errors；Lead 最终独立 clean 构建 exit=0、BUILD SUCCESSFUL；APK 产出并复制到工作区根；HANDOVER/README/iteration11-report/ADR-0004 更新完成；AC-01..AC-15 全部 PASS（AC-14 随文档收口）。唯一 CONDITIONAL：真机验收需用户执行（本机无设备）。
- **风险判断**：残余风险见报告 §6/§7；未选问题均为 P2/P3 且带方案记账。
- **下一步行动**：交付 APK + 真机验收清单；按需下一轮修复真机发现项；未选问题进入候选。
- **证据**：`.scratch/iteration11/gates/lead-final-build.log`（exit=0、131/0/0、警告 2）；`FitPlan-iteration11-debug.apk`（63,249,629 bytes；sha256 c2185560dc179829d2e28e0025420bfc4bc179311033f87b8eb67023ca3976d9）；`docs/iteration11-report.md`；verifier 独立复核 `.scratch/iteration11/gates/verifier-G3.md`（APK 哈希一致、源码冻结、文档一致性、无新增依赖、DB v11 均 PASS）。

---

## 12. 沟通规则

1. 中文回复，保留专业术语（MCP、Skill、Agent、token、Room、Compose 等），不用 emoji。
2. TLDR 先说结论；再列证据与文件，文件给绝对路径或“工作区相对”明确路径；多文件逐条列。
3. 不做无必要询问：能从文档/代码/测试/合理假设得到的自行决定，并记录到本文件 §13。
4. 遇到无法继续的阻塞（例如构建环境彻底不可用、接口契约互相矛盾无法裁决）才向用户提问；提问需给出选项与推荐。
5. Agent 之间通过共享任务板 + 消息沟通；完成/阻塞/需裁决各发一条，不刷屏。
6. 禁止：git 命令、删除/移动备份目录、直接 `./gradlew`（绕过锁）、修改他人写作用域文件、改 DB schema/版本、引入新依赖、触碰需求红线。
7. 所有验证结论必须附命令与原样输出摘要；禁止“应该通过”“看起来没问题”。

---

## 13. 假设、决策与影响记录

| 编号 | 假设/决策 | 依据 | 影响 | 若假设错误的代价 |
| --- | --- | --- | --- | --- |
| A1 | 环境不存在 AskMatt 技能，采用等价“阶段门评审 SGR”协议 | 全盘检索 `~/.dsh/skills`、技能目录、会话缓存，仅有本用户请求提及 AskMatt | 四个门仍有结论/风险/下一步/证据四段输出；在报告中如实说明 | 用户若本意是另一个具体技能，需补充执行；不影响代码交付 |
| A2 | 本轮不做真机验收，以 JVM 测试 + 构建 + 静态审查为自动化证据 | `adb devices` 无设备；前几轮也需用户真机验收 | 交付真机清单与残余风险 | UI 真机行为可能仍有偏差，需下一轮修复 |
| A3 | 无 git，使用 `.scratch/iteration11/backup/src` 作为回滚基线 | 工作区无 .git | 回滚靠文件复制 + diff | 大范围改动时合并成本高；已用写作用域隔离 |
| A4 | DB 保持 v11，不新增表/列/迁移 | 降低迁移风险；本轮问题均可应用层修复 | 无迁移验收项；旧库直接升级安全 | 若发现必须 schema 改动的问题，转为后续迭代 |
| A5 | 本轮 13 个任务 + 3 个 agent 是合理规模；未选问题只记账 | 106 测试基线、单人本地应用、历史每轮 5-8 项 | 迭代可在数小时内完成 | 若时间不足，按非目标清单砍 P3 项 |
| A6 | 用户要求“完整阅读 handoff 文档”，以 HANDOVER/README/CONTEXT/ADR/需求/spec/issue 为主，不逐字节阅读截图与 flomo 逆向材料 | 截图与 flomo 为外部参考，非本项目 handoff | 调研覆盖产品/架构/历史决策/遗留待办 | 若遗漏某条历史约束，评审时可能返工 |
| A7 | 构建使用工作区克隆的 `.gradle-home` + `--offline` + 锁脚本 | 沙箱禁止写 `~/.gradle`；已验证可构建 | 所有 agent 统一构建方式 | 若缓存损坏，重新 clone `~/.gradle` |
| A8 | 计划文件放 `docs/iteration-plan.md`（用户示例路径）；任务票放 `.scratch/iteration11/` | 用户明确示例 + AGENTS.md issue tracker 约定 | 文档路径稳定；符合仓库习惯 | 无 |
| A9 | 前端优化限定在既有 M3 视觉语言内，不做改版 | 需求与历史验收强调白卡/淡底/动态取色 | 改动可逆、回归面小 | 用户若期待更大改版，需下一轮 |
| A10 | 用户要求的“多个 agent”落地为 3 名 teammate + Lead，覆盖逻辑、前端、测试验证 | 用户明确至少覆盖三类职责 | 写作用域清晰、可并行 | 若需更多角色，可追加 reviewer/文档 agent |

---

## 14. 附录：文件与命令速查

- 计划：`/Users/suyi/projects/exercise/docs/iteration-plan.md`
- 备份：`/Users/suyi/projects/exercise/.scratch/iteration11/backup/`
- 构建锁：`/Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh`
- 任务票：`/Users/suyi/projects/exercise/.scratch/iteration11/issues/`
- 门记录：`/Users/suyi/projects/exercise/.scratch/iteration11/gates/`
- 验证报告：`/Users/suyi/projects/exercise/docs/iteration11-verification.md`（verifier 产出）
- 完成报告：`/Users/suyi/projects/exercise/docs/iteration11-report.md`（Lead 产出）
- 基线测试：`cd /Users/suyi/projects/exercise/FitPlan && GRADLE_USER_HOME=/Users/suyi/projects/exercise/.gradle-home ./gradlew --offline :app:testDebugUnitTest`
- 基线 APK 路径：`/Users/suyi/projects/exercise/FitPlan/app/build/outputs/apk/debug/app-debug.apk`

---

## 15. 执行日志（Lead 维护）

| 时间 | 事件 | 证据/结果 |
| --- | --- | --- |
| 2026-10-18 18:0x | 完成 handoff 全量阅读（HANDOVER/README/CONTEXT/ADR/需求/spec/issues/源码/测试/配置） | 主源码 10,366 行、106 测试基线确认 |
| 2026-10-18 18:1x | 基线构建与测试验证 | BUILD SUCCESSFUL；tests=106；仅 2 条既有弃用警告 |
| 2026-10-18 18:2x | 创建回滚备份与构建锁脚本 | .scratch/iteration11/backup（852K）；with-gradle-lock.sh 冒烟通过 |
| 2026-10-18 18:3x | 完成 docs/iteration-plan.md v1.0 | 754 行；含 22 条逻辑问题、6 项前端优化、13 个任务、4 个阶段门 |
| 2026-10-18 18:4x | 创建 shared tasks 与 3 名 teammate | task-1 logic-dev、task-2 frontend-dev、task-3 verifier（blocked_by 前两者） |
| 2026-10-18 18:5x | G0 通过，Wave 1 启动 | logic-dev/frontend-dev/verifier 均 running |

| 2026-10-18 18:2x | G1 开发中门通过（GO） | tests=127/0/0；compile 成功；DB v11 未变；写作用域无越界 |
| 2026-10-18 18:2x | 通知 verifier 进入 Phase 2 | task-1/task-2 completed，工作区冻结，verifier 运行 clean 全量验证 |

| 2026-10-18 18:3x | G2 首轮 NO-GO（TransactionRollbackTest flaky） | verifier clean 全量 131/1/0；仅测试文件修复 |
| 2026-10-18 18:3x | G2 GO | 修复后连续 3 次全量 131/0/0；verifier 连续 2 次 clean 全绿；APK 63,249,629 bytes |
| 2026-10-18 18:4x | G3 GO（自动化部分） | Lead clean 构建 exit=0；131/0/0；APK 复制 + sha256；文档收口 |

| 2026-10-18 18:55 | 第十一轮 APK 真机安装 | PHB110 adb install -r Success；冷启动 801ms；截图 FitPlan/docs/verify/iteration11/01-home-after-install.png |

