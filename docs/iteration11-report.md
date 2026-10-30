# FitPlan 第十一轮迭代完成报告

- 日期：2026-10-18
- 迭代代号：iteration11
- 计划：`/Users/suyi/projects/exercise/docs/iteration-plan.md`
- 独立验证：`/Users/suyi/projects/exercise/docs/iteration11-verification.md`
- 交接文档：`/Users/suyi/projects/exercise/FitPlan/HANDOVER.md`
- 状态：**完成**（自动化验收全绿；真机验收待用户执行）

## 1. TLDR

第十一轮迭代完成：7 项逻辑一致性修复 + 6 项前端体验优化全部落地。JVM 单元测试 **106 → 131（20 个类，failures=0 / errors=0）**；clean 测试与构建连续多轮全绿（Lead 最终独立执行 exit=0，BUILD SUCCESSFUL），编译警告仍为基线 2 条、零新增；DB 保持 **v11**、无新表/列/迁移、无新增依赖。产出 debug APK `FitPlan-iteration11-debug.apk`（63,249,629 字节，sha256 `c2185560dc179829d2e28e0025420bfc4bc179311033f87b8eb67023ca3976d9`）。唯一未闭环项是**真机验收**（本机 adb devices 无设备），已整理清单交用户。

## 2. 目标达成情况

| 目标 | 结果 | 证据 |
| --- | --- | --- |
| 事务边界（组合/记录/清空/删训练者/删动作/打卡/复制上周） | 完成 | ADR-0004；TransactionRollbackTest 2 条；Fake 全表快照/恢复 |
| 空组合收口 | 完成 | `isWorkoutAllDone`；SessionManager 空 items stop；主页/训练页空态；2 条新测试 |
| 复制上周逐日镜像 | 完成 | PlanViewModelTest 新增 1 条（幽灵安排删除/计数/源周不变） |
| 目标值校验（组数/次数/时长正整数，重量保留负/小数） | 完成 | InputRules 5 条测试；组合/记录 UI 错误文案与保存拦截 |
| 删除动作清理孤儿打卡/逃标 | 完成 | ExerciseListViewModelTest 2 条 |
| 多训练者导出当日备注 | 完成 | MarkdownExporterTest 新增 2 条；Iteration11AcceptanceTest 断言 |
| 清空数据清训练会话 + 加载态契约 | 完成 | SettingsViewModelTest 2 条、LoadingStateContractTest 1 条、PlanViewModelTest 1 条 |
| 前端 6 项（首页概览/无障碍/加载态/自动排程/反馈/可读性） | 完成 | 代码审查 + 纯函数测试；真机待验收 |
| 测试 ≥120 且全绿、构建零新增警告 | 完成 | 131/0/0；警告 2 条与基线一致 |
| 文档与交接更新、完成报告 | 完成 | 本文件、HANDOVER、README、ADR-0004、verification |
| debug APK | 完成 | `/Users/suyi/projects/exercise/FitPlan-iteration11-debug.apk` |

## 3. 多 Agent 编排与阶段门

### 3.1 角色与产出

| 角色 | 职责 | 产出 |
| --- | --- | --- |
| Lead（主 agent） | handoff 阅读、逻辑/前端审计、迭代计划、任务拆分、冲突裁决、集成、最终验证、文档 | `docs/iteration-plan.md`、本报告、HANDOVER/README/ADR 更新、最终 APK |
| logic-dev（teammate） | T-L0..T-L6 逻辑实现与测试 | 11 个生产文件 + 8 个测试文件；裁决记录 `.scratch/iteration11/gates/logic-rulings.md` |
| frontend-dev（teammate） | T-F1..T-F6 + T-L3 前端实现与测试 | 17 个生产文件 + 2 个新测试文件；裁决记录 `.scratch/iteration11/gates/frontend-rulings.md` |
| verifier（teammate） | T-V1 独立验证与验收测试 | `Iteration11AcceptanceTest.kt`（4 条）、`docs/iteration11-verification.md`、`.scratch/iteration11/gates/verifier-G2.md` |

共享任务板：task-1（logic-dev）、task-2（frontend-dev）均 completed；task-3（verifier）completed。写作用域 39 项变更全部在计划 §6.1 范围内（无越界）。

### 3.2 阶段门（AskMatt 等价 SGR）

本环境不存在名为 AskMatt 的技能（已检索 `~/.dsh/skills` 与 DSH 技能目录，记录为假设 A1）。按等价“阶段门评审（SGR）”执行，每个门输出阶段结论/风险判断/下一步/证据：

- **G0 计划完成门**：GO。计划 754 行；基线 106 测试全绿；备份与构建锁脚本完成。记录 `.scratch/iteration11/gates/G0.md`。
- **G1 开发中门**：GO。两 dev 工作流完成；127 测试全绿；compile 成功；DB v11 未变；无越界。记录 `.scratch/iteration11/gates/G1.md`。
- **G2 测试前门**：首轮 NO-GO（TransactionRollbackTest 顺序依赖 flaky，clean 全量 131/1/0），logic-dev 仅改测试后连续 3 次全绿；verifier 带环境前置连续 2 次 clean 全绿 → **GO**。记录 `.scratch/iteration11/gates/verifier-G2.md`。
- **G3 发布前门**：见 §8（本文档完成后判定）。

## 4. 改动摘要（按领域）

### 4.1 逻辑与数据一致性（logic-dev + Lead 复核）

- **事务边界**：`AllDaos.runInTransaction` 默认直通；`AppDatabase` 用 Room `withTransaction`；`FakeDaos` 全表快照/恢复 + 失败注入 + `rollbackCount`；包装组合保存/删组合/复制组合、记录保存、删训练者、清空数据、删动作、打卡、通知直接打卡、复制上周；Flow 读取留在事务外（ADR-0004）。
- **空组合**：`isWorkoutAllDone` 顶层纯函数；SessionManager 空 items 结束会话并撤通知；主页/训练页空态引导。
- **复制上周**：7 天逐日镜像，源周无安排则删目标周旧安排；计数只算复制天数。
- **删动作孤儿清理**：事务内先清该动作所有组合条目的 `set_checkins`/`set_skips`，再删动作。
- **多用户导出**：多训练者当日备注逐条「当日备注（用户名）：内容」按 userId 升序；单用户旧格式不变。
- **清空数据**：补删 `workout_session`；整体事务。
- **加载态契约**：`PlanViewModel.dayPlans` / `LogViewModel.entries` / `ComboListViewModel.combos` 初始 null，数据到达非 null。
- 关键文件：`FitPlan/app/src/main/java/com/fitplan/app/data/db/Daos.kt`、`FitPlan/app/src/main/java/com/fitplan/app/data/db/AppDatabase.kt`、`FitPlan/app/src/main/java/com/fitplan/app/session/WorkoutSessionManager.kt`、`FitPlan/app/src/main/java/com/fitplan/app/session/SessionActionReceiver.kt`、`FitPlan/app/src/main/java/com/fitplan/app/export/MarkdownExporter.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/combo/ComboViewModels.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/log/LogViewModel.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/settings/SettingsViewModel.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/exercise/ExerciseViewModels.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/plan/PlanViewModel.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/plan/WorkoutViewModel.kt`。

### 4.2 前端展示优化（frontend-dev + Lead 复核）

- **主页**：今日进度行（已完成 X/Y 组 / 今日完成）、休息日「去「计划」页安排」、空组合引导并隐藏开始训练、ON_RESUME 跨天刷新、MainActivity.onResume 接续/清理会话。
- **清单**：空组合提示；组圈 toggleable + Role.Checkbox + stateDescription；标逃/组重量芯片 48dp 触达；动作级进度条（sets 空不渲染）；聚合圈语义；动作备注仅在脏值时可保存。
- **加载态**：周计划/组合/记录三页 null 时「加载中…」，消除冷启动空态闪烁。
- **自动排程**：LaunchedEffect(combos) 仅补 null 槽位与空循环勾选，不覆盖用户选择。
- **反馈**：导出成功/失败 Toast（IO 写盘）、清空成功 Toast、训练记录删除二次确认、动作删除文案补打卡/逃组清理。
- **可读性/一致性**：组合卡最多 3 行摘要 + 等 N 个动作；动作库 FlowRow 徽标；PR 重量「52.5 kg」；设置页版本取 BuildConfig.VERSION_NAME（新增 buildConfig=true）。
- 关键文件：`FitPlan/app/src/main/java/com/fitplan/app/ui/home/HomeScreen.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/plan/WorkoutScreen.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/plan/WorkoutProgress.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/plan/PlanScreen.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/plan/PlanDialogs.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/Common.kt`、`FitPlan/app/src/main/java/com/fitplan/app/util/InputRules.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/combo/ComboEditScreen.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/log/LogScreen.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/settings/SettingsScreen.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/exercise/ExerciseListScreen.kt`、`FitPlan/app/src/main/java/com/fitplan/app/ui/pr/PrScreen.kt`、`FitPlan/app/src/main/java/com/fitplan/app/MainActivity.kt`、`FitPlan/app/build.gradle.kts`。

### 4.3 测试与文档

- 新增/扩展测试：TransactionRollbackTest 2、ExerciseListViewModelTest 2、SettingsViewModelTest 2、LoadingStateContractTest 1、InputRulesTest 5、WorkoutProgressTest 3、Iteration11AcceptanceTest 4；WorkoutUiStateTest +2、PlanViewModelTest +2、MarkdownExporterTest +2。
- 文档：本报告、HANDOVER、README、ADR-0004、iteration-plan、iteration11-verification、G0/G1/verifier-G2 记录、两份 rulings。


## 5. 验证结果（Lead 最终独立执行）

### 5.1 命令与输出摘要

~~~bash
ANDROID_USER_HOME=/Users/suyi/projects/exercise/.android-home \
  sh /Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh \
  clean :app:testDebugUnitTest :app:assembleDebug --console=plain
~~~

- 结果：**BUILD SUCCESSFUL in 15s，exit=0**（48 actionable tasks: 47 executed, 1 up-to-date）。
- 测试：**131 tests / 0 failures / 0 errors**（20 个测试类）。
- 警告：**2 条**，与基线完全一致（`AppDatabase.kt` 的 `fallbackToDestructiveMigration`、`Common.kt` 的 `menuAnchor`），零新增。
- 环境：Kotlin daemon 因沙箱不可写用户目录自动 fallback（日志 e: 噪声，非诊断/编译错误）；未影响结果。
- 原始日志：`/Users/suyi/projects/exercise/.scratch/iteration11/gates/lead-final-build.log`。
- 产物：`/Users/suyi/projects/exercise/FitPlan/app/build/outputs/apk/debug/app-debug.apk` → 复制为 `/Users/suyi/projects/exercise/FitPlan-iteration11-debug.apk`（63,249,629 字节；sha256 `c2185560dc179829d2e28e0025420bfc4bc179311033f87b8eb67023ca3976d9`）。

### 5.2 验收标准

AC-01..AC-13、AC-15 全部 PASS；AC-14 随本文档与 HANDOVER/README/plan 更新完成。逐项证据见 `docs/iteration11-verification.md` §1 矩阵。要点：
- AC-01 事务回滚：TransactionRollbackTest（直接事务复刻 save 写序列，中途失败整体回滚）。
- AC-02 空组合：WorkoutUiStateTest 2 条 + Iteration11AcceptanceTest。
- AC-03 复制上周：PlanViewModelTest + acceptance test（六类历史保留）。
- AC-04 目标校验：InputRulesTest 5 条 + UI 代码审查。
- AC-05 删动作清理：ExerciseListViewModelTest 2 条 + acceptance test。
- AC-06 多用户导出备注：MarkdownExporterTest + acceptance test。
- AC-07 清空数据：SettingsViewModelTest。
- AC-08/09/11/12 前端：纯函数测试 + 静态审查（真机待验）。
- AC-10 加载态：LoadingStateContractTest + 三页 null 分支代码审查。
- AC-13 构建与测试：上述 clean 全量证据。
- AC-15 约束：DB version=11、10 条迁移、Entities 未改；build.gradle 仅 +buildConfig=true；无新增依赖；无越界写入。


## 6. 风险与回滚

- 本轮未改 DB schema，旧库覆盖安装无迁移风险；仍建议按真机清单抽查。
- 真实 Room 事务只有编译级验证，JVM 用 Fake 快照模拟；需真机“保存瞬间强杀后重进”抽查。
- 无真机：UI 交互、读屏语义、Toast、跨天刷新仅编译 + 代码审查 + 纯函数测试。
- 回滚基线：`/Users/suyi/projects/exercise/.scratch/iteration11/backup/src`（迭代前源码快照，APFS clone）。回滚命令见迭代计划 §10。
- 构建环境补丁：`.gradle-home`、`.android-home` 为工作区内的构建环境产物，不参与源码交付；如清理需重新克隆/复制 keystore 后按 §5.1 命令重建。

## 7. 遗留问题

### 7.1 真机验收（用户执行）

见 `FitPlan/HANDOVER.md` §5「第十一轮真机验收待做」与迭代计划 §9.3，覆盖：旧库覆盖安装、主页跨天/隔天通知清理、空组合、目标校验、删动作残留、多用户导出备注、记录删除确认、导出 Toast、加载态、自动排程默认值、TalkBack/触达、事务强杀抽查。

### 7.2 未选问题（已记账，后续轮次）

L09 通知连续点击可能丢打卡（P2）；L10 进行中通知分钟数不自增长（P2）；L11 打卡路径全表扫描（P3）；L13 fallbackToDestructiveMigration 静默清库风险（P2）；L14 编辑页返回无未保存提示（P3）；L17 导出全量读表（P3）；L21 写库失败无用户反馈（P3）；L22 Fake 与真库级联语义差异（P3）。完整证据/方案/工作量见迭代计划 §2。

### 7.3 已知边界

- 单用户导出保持旧格式（activeUserId 过滤），多用户才输出全部备注；与历史兼容的刻意取舍。
- syncToItem/syncToAll 与 SessionActionReceiver.skip 仍为非事务两段写（影响可控，已记账）。
- 空组合通知收口、真实事务行为无 JVM 通知/DB 测试，依赖静态审查 + 真机抽查。
- 无 git 仓库，本轮靠备份 + 写作用域隔离 + diff 复核。

## 8. G3 发布前门

- **阶段结论**：GO（自动化部分）。131 测试全绿、clean 构建与 APK 产出、文档更新、AC 矩阵 PASS；唯一 CONDITIONAL 为真机验收（用户执行）。
- **风险判断**：无真机为残余风险；Room 真实事务与通知行为待真机抽查；未选问题均为 P2/P3 且已记账。
- **下一步行动**：交付 APK 与真机清单；用户验收后若发现回归，按计划 §10 回滚/修复；未选问题进入下一轮候选。
- **证据**：§5 构建日志与哈希、`docs/iteration11-verification.md`、`.scratch/iteration11/gates/`；独立复核 `.scratch/iteration11/gates/verifier-G3.md`（APK 哈希一致、源码冻结、文档一致性、无新增依赖、DB v11 均 PASS）。

## 9. 后续建议（下一轮候选）

1. 真机验收第十一、十轮待办，优先验证事务强杀、跨天刷新、多用户导出与 TalkBack。
2. 下一轮优先处理 P2：L13 迁移安全（schema 导出 + 迁移测试）、L09 通知串行化、L10 通知计时。
3. 引入 Compose UI/仪器测试补齐前端行为验证（需评估 AGP/SDK 与依赖体积）。
4. 评估 workoutUiStateFlow(daos, date) 收敛 VM/通知重复接线（债务②遗留）。
5. 发布前工程化：release 签名、minify/R8、applicationId/图标/隐私政策。

