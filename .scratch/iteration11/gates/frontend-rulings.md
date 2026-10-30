# frontend-dev 裁决与偏差记录（iteration11）

工作区：/Users/suyi/projects/exercise
角色：frontend-dev，任务 task-2（T-F1..T-F6 + T-L3）
记录时间：2026-10-18

## 结论先行

- T-F1 / T-F2 / T-F3 / T-F4 / T-F5 / T-F6 / T-L3 全部实现完成；:app:compileDebugKotlin 为 BUILD SUCCESSFUL，仅 2 条既有弃用警告（AppDatabase.fallbackToDestructiveMigration、Common.menuAnchor），零新增警告。
- 新增纯函数测试 8 个（WorkoutProgressTest 3 + InputRulesTest 5）在 nullable 契约落地前已跑绿（testsuite 断言 0 failures/0 errors）。契约落地后测试源码编译被 logic-dev 的 PlanViewModelTest.kt 适配中错误阻塞（非本 agent 文件，不在写作用域），待其修复后由全量测试复验。
- T-F3 最终实现方式：三个页面用显式可空局部变量（val plansOrNull: Map<Long, DayPlanEntity>? = dayPlans 等）承接 VM 的 nullable StateFlow。契约落地前后都能零警告编译：落地前永远走非空分支，落地后 null 显示「加载中…」。logic-dev 的 T-L6 契约 18:18 落地，随即完成适配。

## 裁决（自主决定，不扩 scope）

1. **PR 重量空格在展示层归一**：计划 T-F6 只授权 ui/pr/PrScreen.kt，而 PrRowUi.weightText 由 ui/pr/PrViewModel.kt 的纯函数拼成（不在前端写作用域）。裁决：在 PrScreen 增加私有 spacedWeightText() 对「数字+kg/lb」补空格，实现 AC「PR 重量显示 52.5 kg」，不动 VM/纯函数契约。后续如需彻底归一，可由 Lead 授权改 PrViewModel（一行 "${TimeUtil.formatWeight(...)} $unit"）。
2. **主页空组合同时隐藏「开始训练」与清单**：仅显示引导卡 + 体重/备注行。理由：计划 T-F1 要求隐藏开始训练；空组合会话由 T-L1（items 为空即结束会话）兜底，不会出现无法结束的会话。
3. **训练页空组合与无计划合并分支**：两者共用「weight(1f) 包 EmptyHint + 返回按钮」，修复原 EmptyHint 的 fillMaxSize 在 Column 中把返回按钮挤出屏幕的问题；文案按 comboName 是否为 null 区分（空组合：「这个组合还没有动作，请到「组合」页添加」）。
4. **NumberField 扩展**：allowDecimalNegative: Boolean = false（契约要求）+ isError/supportingText（显示错误用）；键盘类型随 allowDecimalNegative 切 Number/Decimal。既有调用点全部向后兼容。
5. **InputRules 增加契约超集辅助函数**：isInvalidPositiveInt(text, max)、isInvalidWeight(text)、parseWeightOrNull(text)，供组合/记录两处 UI 统一「非空才报错」判断；契约中的 5 个成员签名不变。
6. **错误文案使用具体范围**：如「组数需为 1-50」「次数需为 1-999」「时长需为 1-1440 分钟」「重量需为数字（可负数/小数）」，覆盖计划中「加载出历史脏数据时显示错误并禁用保存、清空或改值后可保存」的场景（空串 = 不设定，不报错）。计划文字「需为 1 以上」为示例口径，此处按可核验性细化。
7. **组合/记录保存解析改为范围解析**：sets/reps/duration 用 parsePositiveInt，越界/非法一律不落库（保存按钮已被禁用，双保险）；重量仍用 parseDoubleOrNull（负数/0/小数合法）。
8. **动作编辑页默认重量**只传 allowDecimalNegative = true，不新增错误提示（重量非目标数值，与计划 T-L3 文件清单一致）。
9. **跨天刷新**用 remember + LifecycleEventEffect(Lifecycle.Event.ON_RESUME)（lifecycle-runtime-compose 2.9.4 实测可用，无需 DisposableEffect fallback），ViewModel key 仍为 home-$today。
10. **MainActivity.onResume** 调 Graph.sessionManager.onAppStart()（幂等，会话行权威）。
11. **主页进度行**仅在 items 非空时显示；空清单不显示「0/0」（空组合走引导卡）；全部完成时用 primary 色与「今日完成 · X/Y 组」。
12. **自动排程默认值**用 LaunchedEffect(combos)，仅槽位为 null / cycleIds 为空时填充；combos 为空直接 return，生成按钮仍按 combos 非空 + 方案条件禁用。
13. **T-F6 版本号**：BuildConfig.VERSION_NAME + buildFeatures { buildConfig = true }（AGP 8 默认不生成，Lead 预检确认）。
14. **组合卡摘要**最多 3 行动作 + 「等 N 个动作」（N=总动作数）；组合名/动作行均单行省略。动作库四徽标改 FlowRow（@OptIn(ExperimentalLayoutApi)），实编译零新增警告。
15. **训练记录删除确认**复用 ui/Common.kt ConfirmDialog，文案「确定删除这条训练记录？删除后不可恢复。」；动作删除文案补「该动作的打卡与逃组记录也会一并清理」。
16. **导出反馈**：writeMarkdown 返回 Boolean（openOutputStream 为 null 或抛异常 → false）；SAF 取消不提示；成功「已导出 Markdown」/失败「导出失败，请重试」；清空成功 Toast「已清空所有数据」。
17. **无障碍**：组圈 toggleable(role = Role.Checkbox) + contentDescription + stateDescription（已打卡/未打卡）；标逃与组重量芯片改 Box(heightIn(min = 48.dp), contentAlignment = Center) 保证 48dp 触摸区且文字居中；聚合圈加「动作已完成/未完成」语义；动作卡加细进度条（sets 为空不渲染）；动作备注保存仅在非空且与当天备注不同时启用。
18. **T-F3 顺序与写法**：按 Lead 指令放在最后；为避免契约未落地时对非空 StateFlow 判空产生 "condition is always false" 警告，UI 统一用显式可空局部变量承接（见结论文）。

## 未扩 scope 的观察（仅记账，不处理）

- 主页 Column 的 horizontal 16dp 与 WorkoutChecklist 内部 horizontal 16dp 叠加为双重缩进（既有问题，未改）。
- 训练页重量行未设定时按 − 直接得 -2.5（HANDOVER 已记，未改）。
- 设置页导出范围「指定周」文案为「X 起」而非「X 所在周」，属既有文案，未改。
- 训练页组圈仍为 40dp（计划只要求语义，不要求放大到 48dp）。
- 设置页「关于」版本号依赖 BuildConfig；若最终构建关闭 buildConfig 需回退硬编码。
## 验证证据（2026-10-18 18:21 采集）

- 主源码编译：sh .scratch/iteration11/with-gradle-lock.sh :app:compileDebugKotlin --console=plain → BUILD SUCCESSFUL；全量重编译仅 2 条既有弃用警告（AppDatabase.fallbackToDestructiveMigration、Common.menuAnchor），本轮新增文件零警告。（Kotlin daemon 因沙箱禁止写 ~/Library 会回退 no-daemon 编译，属环境噪声，不影响结果。）
- 自有单测：--tests WorkoutProgressTest --tests InputRulesTest → BUILD SUCCESSFUL；testsuite: WorkoutProgressTest tests=3 failures=0 errors=0；InputRulesTest tests=5 failures=0 errors=0。
- 全量单测（契约与 logic-dev 测试适配完成后）：:app:testDebugUnitTest → BUILD SUCCESSFUL；test-results 汇总 19 个 suite、tests=127、failures=0、errors=0。
- 写作用域自检：与 .scratch/iteration11/backup/src 逐文件 cmp，本 agent 仅修改 frontend 授权文件 15 个 + 新增 2 个主源码文件 + 2 个测试文件 + build.gradle.kts 的 buildConfig 开关；未触碰 VM/数据层/docs/其他测试。
