# FitPlan（健身计划）交接文档

- 更新日期：2026-10-02（第十轮迭代完成后更新，替代同日第九轮版本）
- 项目路径：`/Users/suyi/projects/exercise/FitPlan`
- 需求来源：`/Users/suyi/projects/exercise/requirements.rtf`（健身记录 App 需求文档）
- 状态：**十轮迭代完成**。第一、二轮已通过真机验收（PHB110，Android 16）；第三轮（训练中快捷调重量 + 组合选动作重设计 + 打卡逻辑优化）、第四轮（动作根源重量 + 一键同步，**第六轮已重构取代**）、第五轮（主页 Tab）、第六轮（**重量模型重构：当次重量 + 提醒式同步**）、第七轮（**训练会话常驻通知 + 训练用时**）、第八轮（**每组重量快照 + PR 纪录页**）、第九轮（**本地多训练者 + 每组重量独立编辑 + 组完成态修复**）已通过 60 个 JVM 单元测试与全量构建、第十轮（**数据记录扩展：打卡时刻与用时 + 逃组 + 体重记录 + 当日/动作备注 + 移除安排 + 导出扩展**）已通过 103 个 JVM 单元测试与全量构建，**第三至十轮均待用户真机验收**。历史交接文档与验收证据见 §7 归档地图。

## 1. 项目是什么

本地、离线使用的中文 Android 健身周计划与记录应用。核心链路：**主页（今日计划直出）** → 动作库 → 训练组合 → 周计划 → 训练详情页**清单式逐组打卡**（每组记录完成次数）→ 可选训练记录 → Markdown 导出。不做完成度统计/图表。

明确不做（与需求文档一致）：云同步、账号、Health Connect、Wear OS、训练提醒、组间休息计时器、社交、成就、排行榜、AI 生成计划、完成度/PR/图表统计、自动计算实际负重。

重量语义：正数=负重，0=自重，负数=辅助重量（如辅助引体向上 -20），允许小数；导出保留原始符号。

重量模型（第六轮重构，术语见仓库根 `../CONTEXT.md`、决策见 `../docs/adr/0001-combo-item-as-sync-root.md`）：
- **目标重量**：`combo_items.targetWeight`，各组合相互独立的根源，同一动作在不同组合允许不一致；修改入口只有组合编辑页行内编辑与提醒条确认同步，**不存在自动覆盖**。
- **当次重量**：`session_weights` 表（date+exerciseId 唯一，重复输入覆盖），训练页/主页重量行输入只记当天，**不写回组合条目**；显示优先级当次 > 目标，值下带「当次/目标」小标注。
- **提醒同步**：某动作历史当次最大重量（当天之前）> max(当前条目目标, `exercises.ackMaxWeight` 已处理重量) 时，动作卡展开区显示非阻断提醒条（上次实际用了 X，是否同步为目标重量）；「同步」默认只写当前条目，动作出现在多个组合时可勾选「同步到所有组合（N 处）」；「忽略」与同步都写入 ackMaxWeight，同重量不再提醒，更大重量出现时重新提醒。负数辅助重量按数值直接比较。
- **默认重量**（旧称根源重量）：`exercises.defaultWeight` 降级为组合编辑页添加动作时的预填值，不参与同步；动作编辑页一键同步按钮已移除。

训练会话（第七轮新增，术语见 `../CONTEXT.md`、决策见 `../docs/adr/0002-session-notification-without-foreground-service.md`）：主页「开始训练/结束训练」手动开关会话；常驻通知挂训练进度，[直接打卡] 按计划顺序记「第一个既未打卡也未逃的组」（免前台服务方案：常驻通知 + 进程内观察者 + 广播接收器）；[逃了] 把当前组标为逃组并结束会话（保留已打卡内容；第十轮起逃标细化为组级，原整日逃课卡片与反悔流已移除，见下）；用时口径：以**首组打卡时刻**为起点，进行中通知显示「已进行…」，全部完成提示「总用时…」，首组打卡前不计时。

本地多训练者（第九轮新增）：`users` 表 + `app_settings.activeUserId` 当前训练者（设置页「训练者」区块增删切换，至少保留一名）；打卡、当次重量按训练者归属，**PR 按「训练者 + 动作」维度独立统计**（`set_checkins`/`session_weights` 唯一键含 userId，历史数据迁移归入默认训练者「我」id=1）；所有状态构建（清单/通知/PR）按 activeUserId 过滤，不同训练者互不可见、互不覆盖；主页/通知直接打卡只推进当前训练者的进度。

数据记录扩展（第十轮新增，术语见 `../CONTEXT.md`）：每组打卡自动带时刻（训练用时 = 某天首末打卡时刻之差，顶层纯函数推导、不落表，历史无时刻打卡不参与）；逃标从天级细化为组级（`set_skips` 按组标记，打卡覆盖逃标，逃不算完成，今日清单常驻含未安排日）；体重可选记录（一天一条、重复输入覆盖，不催记、不标记漏记，kg 库内原值 lb 不换算）；备注双轨（当日备注天 + 训练者一条、动作备注天 + 动作一条，动作卡展开区回显当天之前最近一次动作备注）；另补「移除安排」删该天计划行回到未安排，六类记录均按 date 存储、删计划行不受损。

## 2. 技术栈

| 项 | 版本 |
| --- | --- |
| Gradle wrapper | 8.13（本机 Gradle 9.7 与 AGP 8.x 不兼容，务必用 ./gradlew） |
| AGP | 8.13.2 |
| Kotlin | 2.2.21（Compose 编译器插件同版本） |
| KSP | 2.2.21-2.0.5 |
| Room | 2.8.5（数据库 **v11**，`MIGRATION_1_2`/`MIGRATION_2_3`/`MIGRATION_3_4`/`MIGRATION_4_5`/`MIGRATION_5_6`/`MIGRATION_6_7`/`MIGRATION_7_8`/`MIGRATION_8_9`/`MIGRATION_9_10`/`MIGRATION_10_11` 均非破坏迁移） |
| Compose BOM | 2026.06.01（2026.09 起需 AGP 9.1/SDK 37，已刻意不升） |
| Navigation Compose | 2.9.8（2.10 需 AGP 9.1，已刻意不升） |
| SDK | minSdk = targetSdk = compileSdk = 36（Android 16） |
| 测试 | JUnit 4.13.2 + kotlinx-coroutines-test 1.10.2（JVM 单元测试） |

单模块、MVVM、无 DI 框架、无网络权限（Manifest 未声明 INTERNET）。

## 3. 代码结构（app/src/main/java/com/fitplan/app/）

- FitPlanApp.kt：Application，Graph 单例持有全部 DAO；onCreate 创建 WorkoutSessionManager 并 `onAppStart()`（当天重挂/隔天残留通知清理）
- MainActivity.kt：单 Activity
- data/db/：Entities.kt、Daos.kt（SetCheckinDao 新增 observeAll() 全量打卡流，供 PR 统计，Fake 同步；第九轮起打卡/当次重量唯一键与写入 API 均含 userId，新增 updateRepsIfPresent / updateSetWeight / deleteForUser；第十轮新增 SetSkipDao（逃组，含 getAll 全量逃标流供导出）、BodyWeightDao、DayNoteDao、ExerciseNoteDao，`AllDaos` 收录全集）、AppDatabase.kt（Room v11）
- data/model/Schemes.kt：内置排程（三分化：周一/三/五槽位；练一休一：一/三/五/日训练、组合轮流循环）
- export/MarkdownExporter.kt：结构化 Markdown 导出（设置/动作库/训练组合/周计划/训练打卡/体重记录/训练记录）；周计划类型列两分支「休息日/训练日」+ 用时列（无数据「-」，第十轮）；训练打卡段按天分组（天标题 + 当日备注 + 用时 + 组表，动作格并入动作备注，逃组行完成次数格「逃」）
- session/WorkoutSessionManager.kt：训练会话常驻通知管理——start/stop/onAppStart（当天重挂/隔天残留清理，同撤常驻 1001 与完成 1002）/attachIfNeeded，渲染 InboxStyle 逐动作进度与「已进行…/总用时…」用时（POST_NOTIFICATIONS 守卫，点击回主页，[直接打卡]/[逃了] 两个 action）；与 VM 共用顶层 `workoutFlowBundle()` 接线（两级 Bundle、六条流）
- session/SessionActionReceiver.kt：通知按钮广播接收器（goAsync + IO 协程 + catch 兜底）——直接打卡=按计划顺序当组打卡并写会话起点，逃了=结束会话 + 当前组标逃（`set_skips`，保留已打卡内容）；全程校验会话行存在（残留通知不误写）；Manifest 已声明 `POST_NOTIFICATIONS` 权限与本 receiver（exported=false）
- util/TimeUtil.kt：周一起始周计算（BASE_MONDAY=4）、重量格式化（保留符号）
- util/PrRecords.kt：顶层纯函数 `buildPrEntries(checkins, items)`——排除 reps/weight 为 null 与无归属组的打卡，(exerciseId, reps) 分组取最大重量，首达=达成最大重量的最早 date；按 exerciseId、reps 双升序（PR 统计唯一路径；调用方先按当前训练者过滤打卡）
- ui/：AppNav.kt（底部 6 Tab：主页/计划/组合/动作/记录/设置 + 独立路由 "pr"）、home/、plan/、combo/、exercise/、log/、pr/、settings/、theme/
  - home/HomeScreen.kt：**主页**——自动定位今天：训练日直接内嵌今日计划的清单式打卡（复用 `WorkoutViewModel(date=今天)` 与共用列表 `WorkoutChecklist`，可逐组打卡、微调次数、记录当次重量）；休息日/未安排显示「今天休息」卡片（第十轮起卡片内含体重行与当日备注行）。外层不可加 verticalScroll（内含 LazyColumn，嵌套会高度无界崩溃），分支内容用 weight(1f) 占满。第七轮起训练日清单上方 TextButton「结束训练」（会话进行中）/「开始训练」（权限已授直接开始，未授走运行时申请，拒绝→Toast「未授予通知权限，无法挂出训练通知」且不开始）；第十轮起训练日分支顺序为：开始/结束训练 TextButton → 体重行 → 当日备注行 → 清单，按钮+清单包一层 Column(weight(1f))
  - ui/plan/WorkoutViewModel.kt：主页与训练页共用 VM；第七轮起 `checkIn` 补写会话起点 startedAt（主页路径首组打卡，与通知接收器幂等）；`buildWorkoutUiState` 顶层纯函数为 VM 与通知共用的唯一状态构建路径，现为 6 参（date/plans/combos/exercises/checkins + `WorkoutFlowBundle` 直传，2026-10-03 债务②收敛，两处调用点单行化），combine 经 `WorkoutCoreBundle`/`WorkoutFlowBundle` 两级载体接入（ViewModel 与通知路径共用顶层 `workoutFlowBundle()`）；`recordBodyWeight`/`recordDayNote`（空文本=清除）/`recordExerciseNote` 写入三类新记录（第十轮）
  - plan/WorkoutScreen.kt：训练详情页，**清单式打卡**——动作一行（行首聚合圈，全部组完成实心✓+文案置灰），展开后每组一行（行首打卡圈，点击打卡/取消；行尾 −/+ 微调次数并即时持久化，默认次数由 `defaultRepsFor` 解析：目标次数 → 前一组实际次数 → 只记完成）；展开区另含**同步提醒条**（非阻断：历史当次最大 > max(条目目标，已处理重量) 时出现，[同步] 默认写当前条目、多组合可勾选「同步到所有组合（N 处）」，[忽略] 记住该重量；`trackWeight=false` 不显示）与**重量行**（−/+ 步进 2.5，点数值弹窗精确输入，支持负数/小数，清空=取消当次记录；输入写 `session_weights` 当次重量、**不写回组合目标**，显示优先当次>目标并带「当次/目标」小标注；动作未启用 trackWeight 时隐藏）；展开区另有**动作备注输入**（预填当天备注、下方灰字回显「上次：X」，无历史不显示；只写/改无清空入口，第十轮）；清单列表抽为 `WorkoutChecklist` 与主页共用，2026-10-03 起回调收敛为 `WorkoutActions` 载体（顶层 `workoutActions(vm)` 工厂，主页/训练页调用点各一行）
  - exercise/ExerciseEditScreen.kt：动作编辑页，**默认重量**区块（trackWeight 启用时显示）——默认重量输入框（留空不设定；仅作组合编辑添加动作时的预填值，不参与同步；旧「根源重量 + 一键同步」已于第六轮移除）
  - combo/ExercisePickerDialog.kt：**选动作对话框**（组合编辑行内单选与「添加动作」批量多选共用）——搜索 + 分类 FilterChip + 已添加标记；多选确认按全部勾选返回（跨分类切换筛选不丢项）；**选中/批量添加的新行目标重量自动预填动作默认重量（行内已有值不覆盖）**
  - ui/pr/PrViewModel.kt 与 PrScreen.kt：**PR 纪录页**（独立路由 "pr"，入口=动作库页 TopAppBar「PR 记录」，ExerciseListScreen）——PrViewModel 四流 combine 喂顶层纯函数 `buildPrUiState`；页内 TopAppBar+返回，按动作分组、档位升序，行式「PR@8：52.5kg（首达 9/12）」，空态「暂无记录，打卡时记录重量后自动生成。」、加载态 EmptyHint「加载中…」；lb 显示库内原值+单位标签不换算（全 app 口径）
  - ui/settings/SettingsViewModel.kt：设置页 VM（导出/清空/训练者增删切换）——`clearAllData`/`deleteUser` 连带清理体重与两张备注表；`buildMarkdown` 接线体重/逃组/当日备注/动作备注四类新数据并按导出范围在内存过滤（第十轮）
- app/src/test/：FakeDaos.kt（全套真实内存实现，含 FakeExerciseDao upsert/delete/updateAckMaxWeight、FakeSessionWeightDao REPLACE/删行/清空 与 FakeWorkoutSessionDao；第十轮新增 FakeSetSkipDao（含 getAll）/FakeBodyWeightDao/FakeDayNoteDao/FakeExerciseNoteDao，FakeLogDao 部分真实化——清空数据与导出所需的空实现）、TestSeeds.kt（共享训练日 seed fixture，无 @Test）、util/QuickCheckInTest.kt（7 个，nextSet 递进/边界 + 直接打卡跳过逃组）、util/TrainingDurationTest.kt（7 个，用时推导边界：无打卡/单组/跨小时/中途结束/历史无时刻）、util/PrRecordsTest.kt（3 个，buildPrEntries 精确口径）、ui/plan/WorkoutViewModelTest.kt（37 个，当次重量落库/null 取消/负数比较/提醒条件/忽略记忆/同步单条目与全部条目/syncCount/组级标逃与取消/打卡覆盖逃标/完成判定/逃标用户隔离/越界逃标行/主页打卡写会话起点/打卡快照重量/打卡时刻刷新）、ui/plan/BodyWeightTest.kt（4 个，落库与状态带出/同天覆盖/多训练者隔离/休息日带出）、ui/plan/NotesTest.kt（10 个，双轨备注覆盖/回显/清理）、ui/plan/PlanViewModelTest.kt（3 个，移除安排不损历史）、ui/pr/PrUiStateTest.kt（3 个，训练者过滤）、ui/exercise/ExerciseEditViewModelTest.kt（3 个，默认重量保存且不改写组合条目/已处理重量不被保存覆盖）、ui/combo/ComboEditViewModelTest.kt（8 个，增量保存/孤儿清理）、export/MarkdownExporterTest.kt（16 个，打卡段落/按天分组/逃组标注/动作备注并入/负数重量符号/默认重量列/周计划两分支+用时/组次「次数×重量」）、ui/settings/SettingsExportTest.kt（2 个，体重段范围过滤/旧库导出不崩）、ui/plan/WorkoutUiStateTest.kt（3 个，buildWorkoutUiState 首批直测：休息日早退带出/训练日结构/逃组不算完成）

数据表：**users**（v7 新增训练者档案：id/name/sort，迁移播种默认训练者「我」id=1，新装由 FitPlanApp 兜底播种）、exercises（v3 起含 `defaultWeight` 默认重量，v4 起含 `ackMaxWeight` 已处理重量）、combos、combo_items、day_plans（主键=epochDay，v5 起含 `skipped` 逃课标记，**已弃用无读者**——逃标见 `set_skips`）、log_entries、log_sets、app_settings（单行，v7 起含 `activeUserId` 当前训练者）、**set_checkins**（逐组打卡：一行 = 某天某训练者某组合动作的某一组，行存在即该组已完成，`reps` 为该组完成次数（可 null=只记完成），v6 起含 `weight` 每组重量快照（可 null=未记重量；v7 起可经 `updateSetWeight` 单独编辑，动作级当次重量不覆盖已编辑值），v7 起含 `userId` 归属训练者，v8 起含 `checkedAt` 打卡时刻（可 null=历史行，不参与用时推导，重复打卡/调整次数刷新为最新），唯一索引 `(date, comboItemId, setIndex, userId)`）、**set_skips**（v9 逃组：一行 = 某天某训练者某组合条目的某一组被标逃，行存在即逃，唯一索引 `(date, comboItemId, setIndex, userId)`，打卡覆盖即删行）、**session_weights**（v4 新增当次重量：一行 = 某天某训练者某动作实际使用的重量，`weight` 非 null，v7 起唯一索引 `(date, exerciseId, userId)` 重复输入覆盖，外键级联删除动作）、**body_weights**（v10 体重记录：`date`+`userId` 唯一，重复输入覆盖）、**day_notes**（v11 当日备注：`date`+`userId` 唯一，重复覆盖）、**exercise_notes**（v11 动作备注：`date`+`exerciseId`+`userId` 唯一，重复覆盖，外键级联删除动作）、**workout_session**（v5 新增单行会话：date 主键，`startedAt` 首组打卡时刻可空）。

## 4. 已验证（真机 PHB110，Android 16 / API 36）

- 基础功能（原版冒烟，截图 `docs/verify/base/`）：构建安装、周计划初始页定位当前周、动作库/组合新建、自动排程三分化、记录添加、设置与导出 SAF 流程。
- 第一轮迭代（截图 `docs/verify/iteration1/`）：训练详情页展开卡片 + 逐组打卡（行尾圆圈版，已被第二轮取代）。
- 第二轮迭代（验收记录 `docs/verify/iteration2/README.md`，用户实测确认无问题）：
  - 清单式打卡主链路：行首圈打卡/取消、次数 −/+ 微调即时持久化、聚合置灰态；
  - 退出重进/杀进程重开，组数、次数、完成态完整恢复（Room 持久化）；
  - 全 App 淡色页底 + 白色 24dp 卡片 + 细分隔线；动态取色跟随壁纸；
  - 回归抽查：周计划排程/复制上周/自动排程、训练记录、动作/组合管理、设置与清空数据。
- 第三轮迭代（2026-09-30，**待真机验收**；截图建议归档 `docs/verify/iteration3/`）：
  - 训练中快捷调重量：动作卡展开区重量行（−/+ 步进 2.5、点数值精确输入、清空=清除），写回组合目标重量，下次训练沿用；
  - 组合编辑选动作重设计：下拉菜单 → 对话框（搜索 + 分类筛选 + 已添加标记），「添加动作」支持多选批量插入；
  - 打卡次数 −/+ 改为 DAO 原子增减（`UPDATE ... SET reps = MAX(1, reps + :delta)`，下限 1），消除快速连点丢增量竞态（§5 旧待办）；
  - 编辑组合改**增量保存**：按 exerciseId 顺序匹配旧条目沿用原 id（历史打卡保留、重排不丢），仅删除真正移除的条目并同步清理其全部打卡；删除组合同样清理打卡（§5 旧待办：孤儿行此前仅「清空数据」时清理）；
  - Markdown 导出新增「## 训练打卡」段落（日期/组合/动作/组/完成次数，按导出范围过滤；§5 旧待办）；
  - 单元测试 9 → 23 个全部通过；`./gradlew :app:testDebugUnitTest :app:assembleDebug` 全绿，零新增警告（仍仅既有 2 条弃用警告）。
- 第四轮迭代（2026-09-30，**待真机验收**；截图建议归档 `docs/verify/iteration4/`）：**动作根源重量 + 一键同步到所有组合**
  - 数据：`exercises` 新增 `defaultWeight`（根源重量），DB v2→v3 非破坏迁移（`ALTER TABLE exercises ADD COLUMN defaultWeight REAL`）；
  - 动作编辑页新增「根源重量」区块：默认重量输入（留空不设定）+「同步到所有组合（N 处）」按钮（仅已保存动作显示；二次确认后保存动作并把默认重量统一写入所有引用条目，`ComboDao.updateTargetWeightForExercise`）；
  - 训练页精确输入对话框：动作出现于多个组合时（`syncCount > 1`）显示「同步到所有组合（共 N 处）」勾选，勾选后一次更新全部条目，否则仅更新当前条目；−/+ 步进仍只改当前条目（快速微调不误伤其他组合）；
  - 组合编辑页：行内选动作 / 「添加动作」批量添加时，新行目标重量自动预填动作根源重量（行内已有值不覆盖）；
  - Markdown 导出「## 动作库」表新增「根源重量」列（未设定为 `-`，负数保留符号）；
  - 单元测试 23 → 29 个全部通过；`./gradlew :app:testDebugUnitTest :app:assembleDebug` 全绿，零新增警告（仍仅既有 2 条弃用警告）。
- 第五轮迭代（2026-09-30，**待真机验收**；截图建议归档 `docs/verify/iteration5/`）：**主页 Tab**
  - 底部导航新增「主页」为首 tab（共 6 Tab），App 打开即见今日状态；
  - 训练日：主页直接内嵌今日计划的清单式打卡（复用 WorkoutViewModel(今天) 与从训练页抽出的 `WorkoutChecklist` 共用列表），逐组打卡、次数微调、重量调整（含同步到所有组合）全部可用，与训练详情页数据实时一致；
  - 休息日/未安排：显示「今天休息」卡片与引导文案；
  - 单元测试仍 29 个全绿（无新增 VM 逻辑，纯 UI 复用）；构建零新增警告。
- 第六轮迭代（2026-09-30，**待真机验收**；截图建议归档 `docs/verify/iteration6/`）：**重量模型重构——当次重量 + 提醒式同步**（决策见仓库根 `../docs/adr/0001-combo-item-as-sync-root.md`）
  - 数据：新增 `session_weights` 当次重量表（date+exerciseId 唯一、重复输入覆盖、外键级联删除）+ `exercises.ackMaxWeight` 已处理重量列，DB v3→v4 非破坏迁移（`MIGRATION_3_4`）；
  - 重量行重定义：−/+ 步进与精确输入改为**记录当次重量**（只写 `session_weights`，不再写回 `combo_items.targetWeight`），显示优先当次>目标并带「当次/目标」小标注；清空并确定=取消当次记录；精确输入对话框移除「同步到所有组合」勾选；
  - 新增同步提醒条（动作卡展开区，非阻断）：该动作历史当次最大重量（当天之前）> max(当前条目目标，已处理重量) 时出现——[同步] 默认写当前条目目标重量，动作出现于多个组合时（`syncCount > 1`）可勾选「同步到所有组合（共 N 处）」；[忽略] 记住该重量不再提醒；两者都写入 `ackMaxWeight`，更大重量出现时重新提醒；负数辅助重量按数值比较；
  - 动作编辑页：「根源重量」改名「默认重量」，移除「同步到所有组合（N 处）」按钮与二次确认（`saveAndSync` 删除，默认重量仅作组合编辑预填）；`usageCount` 统计与 `ComboDao.observeCountForExercise` 一并移除；
  - Markdown 导出「## 动作库」列名「根源重量」→「默认重量」；设置「清空所有数据」补清 `session_weights`；
  - 单元测试 29 → 36 个全部通过（新增当次重量落库/提醒条件/忽略记忆/同步单条目与全部条目等，移除旧的写回与 saveAndSync 用例）；`./gradlew clean :app:testDebugUnitTest :app:assembleDebug` 全绿，零新增警告（仍仅既有 2 条弃用警告）。
- 第七轮迭代（2026-09-30，**待真机验收**；截图建议归档 `docs/verify/iteration7/`）：**训练会话常驻通知 + 训练用时**（决策见仓库根 `../docs/adr/0002-session-notification-without-foreground-service.md`）
  - 数据：`day_plans` 新增 `skipped` 逃课列 + 新表 `workout_session`（date 主键单行会话，`startedAt` 首组打卡时刻可空），DB v4→v5 非破坏迁移（`MIGRATION_4_5`，列名用驼峰 `startedAt` 与 Room 默认一致）+ `WorkoutSessionDao`（observe/get/upsert/delete）；
  - 通知：常驻通知 1001（InboxStyle 每动作「名称 n/m 组」+摘要「已完成 X/Y 组」、setOngoing、setOnlyAlertOnce、channel "workout_session" IMPORTANCE_LOW）+ 完成通知 1002（可滑掉）；POST_NOTIFICATIONS 守卫；点击回主页；两个 action（直接打卡/逃了）；修复 ensureChannel() 定义了却从未调用的 bug；用时渲染：进行中「已进行不到 1 分钟/X 分钟/H 小时 M 分钟」、完成「总用时…」，首组打卡前不显示，冷启动经 DB 直读兜底起点（resolveStartedAt）；
  - 接收器：新增 `SessionActionReceiver`（goAsync + IO 协程 + catch 兜底）——直接打卡=残留守卫→休息/无组合守卫→getWithItems→当组打卡（`QuickCheckIn.nextSet` 按计划顺序）→首组写 startedAt→写 SetCheckinEntity→attachIfNeeded（全完走 render 完成分支）；逃了=结束会话+标记 skipped（保留已打卡内容）；全程校验会话行存在（残留通知不误写）；
  - 接线：Manifest 声明 `POST_NOTIFICATIONS` 权限 + receiver（exported=false）；`Graph.init(daos, sessionManager)`；`FitPlanApp.onCreate` 创建 manager 并 `onAppStart()`（当天重挂/隔天残留清理，同撤 1001+1002）；
  - 主页：isSkipped → 「今天逃了」卡片（仿休息卡）+「反悔，继续训练」→ undoSkip（此时隐藏「今日计划」副标题）；否则清单上方 TextButton「结束训练」（会话进行中）/「开始训练」（权限已授直接开始，未授走运行时申请，拒绝→Toast「未授予通知权限，无法挂出训练通知」且不开始）；按钮+清单包 Column(weight(1f))，未加 verticalScroll；
  - VM：`checkIn` 补写 startedAt（主页路径首组打卡，与接收器幂等）；`undoSkip` 既有；切片 C 既有：顶层纯函数 `buildWorkoutUiState`（VM 与通知共用）、`WorkoutUiState.isSkipped`；
  - 导出：周计划类型列三分支「休息日/训练日（逃课）/训练日」；
  - 单元测试 36 → 45 个全部通过（新增 QuickCheckIn 4 个、逃课 VM 1 个、导出逃课标注 2 个、主页打卡写起点 2 个）；`./gradlew clean :app:testDebugUnitTest :app:assembleDebug` 全绿，零新增警告（仍仅既有 2 条弃用警告）。
- 第八轮迭代（2026-10-01，**待真机验收**；截图建议归档 `docs/verify/iteration8/`）：**每组重量快照 + PR 纪录页**（决策见仓库根 `../docs/adr/0003-per-set-weight-snapshot.md`）
  - 数据：`set_checkins` 新增 `weight` 每组重量快照列（可 null=未记重量），DB v5→v6 非破坏迁移（`MIGRATION_5_6`：`ALTER TABLE set_checkins ADD COLUMN weight REAL`，v5 历史迁移未动）+ `SetCheckinDao.observeAll()` 与 Fake 对应方法；
  - 快照双路径：主页 `WorkoutViewModel.checkIn` 与通知 `SessionActionReceiver.quickCheckIn` 同源同值——打卡时把该动作当天当次重量（`session_weights`，date+exerciseId）快照进该组；没记过→只记次数（weight=null）；取消该组→调重量→重打=重快照；旧数据不回填；
  - PR 页：`util/PrRecords.kt` 顶层纯函数 `buildPrEntries`（精确口径：每组真实重量取最大，只计 reps/weight 都非空的组）+ 独立路由 "pr"（`ui/pr/`：PrViewModel 四流 combine + PrScreen），入口=动作库页 TopAppBar「PR 记录」；按动作分组、档位升序，行式「PR@8：52.5kg（首达 9/12）」，空态「暂无记录，打卡时记录重量后自动生成。」；lb=库内原值+单位标签不换算；
  - 导出：打卡表格组次列有重量组「12×52.5」（`TimeUtil.formatWeight` 整数去尾），无重量组字节级旧行为；已知取舍：reps=null 组仍显示「只记完成」、其快照重量不在导出体现；
  - 单元测试 45 → 52 个全部通过（新增 PR 快照 VM 2 个、导出「次数×重量」2 个、PrRecordsTest 3 个）；`./gradlew clean :app:testDebugUnitTest :app:assembleDebug` 全绿，零新增警告（仍仅既有 2 条弃用警告）。
- 第九轮迭代（2026-10-01，**待真机验收**；截图建议归档 `docs/verify/iteration9/`）：**本地多训练者 + 每组重量独立编辑 + 组完成态修复**
  - 数据：新增 `users` 训练者表（迁移播种「我」id=1，`FitPlanApp` 对新装库兜底播种）；`set_checkins`/`session_weights` 增加 `userId` 列（历史数据归入默认训练者，两张表按 Room 规则重建、数据原样迁移）；`app_settings` 增加 `activeUserId`；DB v6→v7 非破坏迁移（`MIGRATION_6_7`）；
  - 问题 1 修复（最后一项打卡无反馈）：根因是组行完成态误用 `reps != null` 判断——「只记完成」/ 无目标次数的动作（常为清单最后一项）整行永远显示未打卡。`WorkoutSetUi` 新增 `checked`（= 打卡行存在），组圆圈、行文案、聚合圈完成判定改按行存在；同时组圆圈与聚合圈改 `animateColorAsState` 颜色过渡，第一项/中间项/最后一项打卡的动画与完成态完全一致；
  - 问题 2 修复（PR 未按用户+动作分维度）：打卡与当次重量按训练者归属（`WorkoutViewModel`/`SessionActionReceiver` 读写均带 activeUserId），`buildWorkoutUiState`/`buildPrUiState` 按当前训练者过滤，`set_checkins` 唯一键含 userId 不同用户同组互不覆盖；PR 页标题下显示当前训练者；设置页新增「训练者」区块（单选切换 / 添加 / 删除——最后一名不可删，删除连带清其打卡与当次重量，删当前训练者自动切回第一名）；导出打卡表在多训练者时追加「用户」列（单训练者保持旧表结构）；
  - 问题 3 落地（每组每次数独立重量）：每组行尾新增「重量」芯片（已打卡且启用重量时显示，`52.5 kg` 或「记重量」），点击弹窗精确输入（负数/小数/清空=清除），`updateSetWeight` 只写该组 `set_checkins.weight`；打卡语义改为**行已存在仅覆盖次数、保留该组重量**（`updateRepsIfPresent`），动作级当次重量只在打卡产生新行时快照、不再覆盖已编辑的每组重量；同一动作不同次数档天然各自成 PR（PR 按每组真实重量聚合）；
  - 单元测试 52 → 60 个全部通过（新增：只记完成 checked、最后一项完成态一致性、重复打卡保留组重量、updateSetWeight 隔离、多训练者打卡隔离、PrUiState 训练者过滤 3 个）；`./gradlew clean :app:testDebugUnitTest :app:assembleDebug` 全绿，零新增警告（仍仅既有 2 条弃用警告）。
- 第十轮迭代（2026-10-02，**待真机验收**；截图建议归档 `docs/verify/iteration10/`）：**数据记录扩展**（术语见仓库根 `../CONTEXT.md`，spec 见 `../.scratch/data-recording/spec.md`）
  - 打卡时刻与用时：`set_checkins` 新增 `checkedAt` 打卡时刻列（打卡/覆盖时写入，历史行 NULL 不参与、不报错），DB v7→v8 非破坏迁移；训练用时 = 某天首末打卡时刻之差，`util/TrainingDuration.kt` 顶层纯函数推导不落表，中途结束同样产生、无打卡不产生，周计划页每天可回看；完成通知用时行为不变；
  - 逃组：新表 `set_skips`（date+comboItemId+setIndex+userId 唯一，行存在即逃），DB v8→v9 非破坏迁移并回填历史天级逃课（每组至少 1 行、逐训练者排除已打卡组）；通知「逃了」= 当前组标逃并结束会话，清单组行可手动标逃/取消，打卡覆盖逃标，完成判定不变（全部组打卡）；主页整日逃课卡片与反悔流移除、今日清单常驻（含未安排日），`day_plans.skipped` 列保留但弃用；
  - 体重：新表 `body_weights`（date+userId 唯一，重复输入 REPLACE 覆盖），DB v9→v10 非破坏迁移；主页两入口（训练日清单上方与休息日卡片），训练日与休息日都可记；可选记录红线——不设称重提醒、不显示漏记提示；kg 库内原值、lb 只换标签不换算；「清空所有数据」与「删除训练者」连带清理；
  - 备注双轨：一张迁移建 `day_notes`（date+userId 唯一）与 `exercise_notes`（date+exerciseId+userId 唯一、外键级联删除动作），DB v10→v11；当日备注主页清单顶部入口（训练日与休息日皆可写，空文本确定=清除），动作备注动作卡展开区入口（预填当天备注、灰字回显「上次：X」=当天之前最近一次，无历史不显示；只写/改无清空入口）；与「记录」Tab 自由记录的条目备注两套数据、两套入口；
  - 移除安排：删该天计划行回到未安排——数据层 `DayPlanDao.deleteByDate` 能力此前已存在，如实记账「数据层能力已存在，本轮仅接线核对与补测试」（按钮文案对齐「移除安排」+ 补测试）；六类记录表（打卡/逃组/两备注/体重/当次重量）均无指向 day_plans 的外键、全部按 date 存储，删计划行不受损；重排该天后历史照常回看；
  - 导出扩展：新增「## 体重记录」段（按导出范围内存过滤，无数据整段省略）；训练打卡段重构为按天分组（天标题 + 当日备注 + 用时行 + 组表，动作备注并入动作格，逃组行完成次数格「逃」）；周计划类型列回归「休息日/训练日」两分支（删逃课口径）、用时格无数据「-」；
  - 单元测试 60 → 103 个全部通过（新增明细按各票备注）；`./gradlew clean :app:testDebugUnitTest :app:assembleDebug` 全绿，零新增警告（仍仅既有 2 条弃用警告）。
- 债务②收敛（2026-10-03，票见 `../.scratch/workout-param-convergence/issues/`）：`buildWorkoutUiState` 11→6 参（`WorkoutFlowBundle` 直传、两处调用点单行化）+ `WorkoutChecklist` 回调收敛为 `WorkoutActions` 载体（顶层 `workoutActions(vm)` 工厂；票面 12 回调实测 11 更正记账），纯重构零行为变化。单元测试 103→106（新增纯函数首批直测），构建零新增警告。遗留记账（评审建议、未展开）：VM/通知两处 combine 五流接线仍逐行重复（可仿 `workoutFlowBundle()` 提 `workoutUiStateFlow(daos, date)`）；unitIsLb 沿清单组件下传 4 层。
- 单元测试：`./gradlew :app:testDebugUnitTest` 全绿（tests=106, failures=0, errors=0）。
- 构建：`./gradlew :app:assembleDebug` 通过；全量重编译仅 2 条既有弃用警告（`data/db/AppDatabase.kt` 的 `fallbackToDestructiveMigration`、`ui/Common.kt` 的 `menuAnchor`），多轮改动零新增警告。
- 第七轮产物：`/Users/suyi/projects/exercise/FitPlan-iteration7-debug.apk`（debug 变体，62,889,181 字节 ≈ 60MB，2026-09-30 15:58 构建）。
- 第八轮产物：`/Users/suyi/projects/exercise/FitPlan-iteration8-debug.apk`（debug 变体，63,156,293 字节 ≈ 60MB，2026-10-01 构建）。
- 第九轮产物：已直接覆盖安装到真机 PHB110（2026-10-01，`adb install -r`，Success），升级链路当场验证见 §5 第九轮验收待办。
- 第十轮产物：本轮未单独出 APK（覆盖安装待办见 §5）。

## 5. 未验证 / 已知待办

- 第十轮真机验收待做：**v9→v10 与 v10→v11 两个迁移覆盖安装（启动不崩、既有数据完好）**、体重录入/同天覆盖/休息日入口/多训练者隔离、当日备注写改清（空文本确定=清除）、动作备注展开区写改与「上次：X」回显（无历史不显示）、移除安排后该天回到未安排且六类记录原样、重排后历史回看、导出（体重段按范围过滤、逃组标注、动作备注并入动作行、当日备注与用时并入当天段落、周计划两分支无逃课字样、多训练者「用户」列）。
- 第九轮真机验收待做：设置页「训练者」增删切换（最后一名不可删、删除连带清理）、多训练者打卡互不覆盖（切用户后同组显示未打卡、各打各的组各得一行）、PR 页按训练者区分（标题显示当前训练者、切换后数据变化）、**v6→v7 升级后旧打卡/当次重量归入「我」且数据完好（覆盖安装启动不崩）**、无目标次数动作打卡后组圈实心+聚合圈实心（尤其清单最后一项）、每组「重量」芯片（设置/编辑/清除、动作级当次重量不覆盖已编辑组重量）、打卡圆圈颜色过渡动画、多训练者导出打卡表含「用户」列。
- 第八轮真机验收待做：记重量→打卡→该组带重量、不记重量→只记次数、三组波动各自快照、取消重打快照新值、通知直接打卡快照、旧打卡数据升级后照常显示且不计 PR、PR 页（入口/分组/档位升序/首达日期/空态）、导出「次数×重量」、v5→v6 升级后数据完好（已被第九轮 v6→v7 验收覆盖升级链路，仍建议抽查）。
- 第七轮真机验收待做：开始训练（权限授予）与拒绝权限→Toast「未授予通知权限，无法挂出训练通知」且不开始、通知挂出与打卡实时刷新（组数/跨动作递进）、误触纠错回主页圆圈取消、[逃了]→「今天逃了」卡片→[反悔，继续训练] 恢复、主页开始/结束按钮切换、进行中「已进行…」（主页首组打卡也计时）、完成通知「总用时…」可滑掉、隔天启动清理（1001+1002 都撤）、**v4→v5 升级后数据完好**、重量待同步文案、通知点击回主页、当天结束再开始。
- 第六轮真机验收待做：重量行输入当次重量（步进/精确输入/清空取消、显示「当次/目标」标注、不写回组合目标、跨天回落）、同步提醒条（超过历史最大时出现、同步当前条目/勾选全部、忽略后不再提醒、更大重量重现、负数比较）、动作编辑页「默认重量」（无同步按钮）、旧数据升级到 DB v4 后数据完好。
- 第五轮真机验收待做：主页 tab 定位今天（训练日内嵌清单与训练详情页数据一致、打卡即时持久化、跨天切换）、休息日卡片展示。
- 第四轮真机验收待做：~~动作编辑页根源重量同步按钮、训练页「同步到所有组合」勾选、根源重量导出列~~（已被第六轮重构取代，验收以第六轮为准）；组合编辑添加动作预填默认重量、旧数据升级 DB 项并入第六轮（按 v4 验证）。
- 第三轮真机验收待做：选动作对话框（搜索/分类/多选/已添加）、连点次数 stepper、编辑组合后当日打卡保留、导出含打卡段。
- 编辑组合的增量保存为多段 suspend 顺序调用（删条目 → 删打卡 → 逐条更新 → 批量插入），未包 Room 事务，中途进程死亡会留半成品（旧实现同样非事务；如需严格一致可包 `@Transaction` / `withTransaction`）。
- 训练页重量行「未设定」时按 − 直接得 -2.5（辅助重量语义合法，方向略意外，可再斟酌交互）。
- `set_checkins` 无外键；孤儿行现已在「编辑组合移除条目 / 删除组合 / 清空所有数据」三个入口同步清理。
- ~~根源同步为一次性覆盖写~~：第六轮已移除自动覆盖——同步只能经提醒条确认（默认当前条目、可勾选全量），各组合目标重量相互独立。
- 训练页重量行「未设定」时按 − 直接得 -2.5 当次重量（辅助重量语义合法，方向略意外，可再斟酌交互）。
- 导出的 .md 未做端到端字节级核对（负数重量符号已由 MarkdownExporterTest 覆盖导出逻辑，建议真机导出后人工抽查一次）。
- Fake（测试内存实现）的 insertItems 用自增游标，不模拟 SQLite AUTOINCREMENT 高水位（删除条目后新 id 可复用），仅影响测试数据安排，不影响真机。
- 空组合边界（有组合无条目）：完成判定永假、常驻通知无行可渲染，需后续轮次处理（第七轮审查遗留备忘）。
- README.md 内容滞后（仍第三轮语义），待按现状重写（第七轮遗留备忘）。
- 上架前：配置 release 签名、评估 minify、替换 applicationId/应用名/图标、补隐私政策页。

## 6. 构建与运行

```bash
cd /Users/suyi/projects/exercise/FitPlan
./gradlew :app:assembleDebug          # Debug APK
./gradlew :app:testDebugUnitTest      # JVM 单元测试
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

设备要求 Android 16+。功能说明见同目录 README.md。

## 7. 文档与截图归档地图

| 路径 | 内容 |
| --- | --- |
| `HANDOVER.md` | 本文档（最新交接，以此为准） |
| `docs/archive/HANDOVER-CHECKIN-UI.md` | 第二轮迭代交接文档（已加完成戳归档，含 §8 完成记录） |
| `docs/reference/` | 第二轮设计参考：图1 清单式打卡、图2 Google M3 卡片风格 |
| `docs/verify/base/` | 原版基础功能真机冒烟截图（原 `.verify/`） |
| `docs/verify/iteration1/` | 第一轮迭代真机验收截图（原 `.verify-new/`） |
| `docs/verify/iteration2/` | 第二轮迭代真机验收记录（用户实测通过；未截图存档） |
| `docs/verify/iteration7/` | 第七轮迭代真机验收截图（建议归档，待真机验收后补充；目录尚不存在） |
| `docs/verify/iteration8/` | 第八轮迭代真机验收截图（建议归档，待真机验收后补充；目录尚不存在） |
| `docs/verify/iteration9/` | 第九轮迭代真机验收截图（建议归档，待真机验收后补充；目录尚不存在） |
| `docs/verify/iteration10/` | 第十轮迭代真机验收截图（建议归档，待真机验收后补充；目录尚不存在） |
