# 06 — 导出扩展

**What to build:** Markdown 导出并入本轮全部新数据：打卡段落动作行并入动作备注、当天段落并入当日备注与训练用时；新增「## 体重记录」段（按导出范围过滤）；周计划类型列回归「休息日/训练日」两分支（移除「训练日（逃课）」）；逃组在打卡段落按组标注（如「第 3 组：逃」）。

**Blocked by:** 01, 02, 03, 04

**Status:** done

- [x] 导出含「## 体重记录」段，按导出范围过滤，无数据时整段省略
- [x] 打卡段：逃组按组标注；动作备注并入动作行；当日备注与用时并入当天段落
- [x] 周计划表类型列两分支（休息日/训练日），不再出现逃课字样；用时列无数据为「-」
- [x] 无新数据的旧库导出不崩、合理缺省
- [x] 多训练者导出口径沿用现行规则（多训练者时含「用户」列）
- [x] 导出测试逐项覆盖（沿用既有导出测试范式）；全绿 + 构建零新增警告

## 实施备注（2026-10-02）

- 导出纯函数扩展：ExportData 补 bodyWeights/dayNotes/exerciseNotes/skips 四字段（均默认空，旧调用方零改动）；训练用时不新增字段，exporter 内部用票 01 顶层纯函数 `durationsForUser(buildTrainingDurations(checkins), settings.activeUserId)` 推导（checkins 已是范围过滤后列表；无时刻打卡不参与、单组约 0 分钟语义照旧，按 activeUserId 过滤）。
- 打卡段重构为按天分组段落（票面「当天段落」）：天集合 = 打卡 ∪ 当日备注 ∪ 逃组 日期（逃组独占天也渲染——测试编写时发现字面口径漏掉此类天，已补）；每天 = ### 天标题 + 当日备注行（按 activeUserId）+ 恒输出用时行（无数据「用时：-」）+ 组表（多训练者含「用户」列；原日期/星期列下沉到天标题）。
- 逃组「第 3 组：逃」的表格化表达：组格 = setIndex+1、完成次数格 =「逃」；打卡覆盖逃标（同 date+comboItemId+setIndex+userId 只保留打卡行）。动作备注按 (date, exerciseId, 行 userId) 并入动作格「动作名（备注：X）」（ItemRef 补 exerciseId）。
- 周计划表 5 列（+用时）：类型列回归「休息日/训练日」两分支，「训练日（逃课）」旧口径删除，day_plans.skipped 自此无生产读者（grep 复核：仅剩实体定义与历史迁移 SQL）；用时格无数据「-」。
- 新增「## 体重记录」段（打卡段与训练记录段之间）：范围过滤在 SettingsViewModel.buildMarkdown 内存过滤（照 checkins 范式，SetSkipDao 为此补 getAll 全量查询）；无数据整段省略；单位标签跟随 settings.unitIsLb、体重 formatWeight 原值不换算；多训练者「用户」列规则与打卡段一致（userNames.size>1）。
- 无新数据旧库导出：四字段默认空 → 打卡段占位、周计划两分支+用时「-」、体重段省略，全空 fake 的 VM 层测试钉死不崩。
- 测试：MarkdownExporterTest 9→16（改写打卡格式类 5 条与逃课条 1 条、新增 7 条）+ 新建 SettingsExportTest 2 条（范围过滤/旧库不崩）；FakeLogDao 的 getAllWithSets/getWithSetsForRange 由 TODO 补为空实现（继 clearAllData 之后第二个需要真实 FakeLogDao 的场景）。
- JVM 测试 103 个全绿（票 05 后 94 + 本票 9 条）、零新增警告（基线 2 条外）。
