# FitPlan 第十二轮迭代完成报告：前端全面重构

- 日期：2026-10-18
- 迭代代号：iteration12
- 计划：`/Users/suyi/projects/exercise/docs/iteration12-plan.md`
- 独立验证：`/Users/suyi/projects/exercise/docs/iteration12-verification.md`（verifier 产出，待收口）
- 品牌/设计系统说明：`/Users/suyi/projects/exercise/docs/iteration12-brand.md`
- 交接文档：`/Users/suyi/projects/exercise/FitPlan/HANDOVER.md`
- 状态：**完成**（自动化 131/0/0、clean 构建、最终 APK 装机与冷启动、独立验证 GO；真机全量业务验收待用户执行）

## 1. TLDR

第十二轮把 FitPlan 前端从"默认 M3 模板感"重构为完整设计系统 + 全页面重做：新增 `ui/theme`（颜色/字阶/间距/圆角/动效 token）与 `ui/design`（19 个通用组件），9 个页面 + 6 Tab 导航 + 全部对话框/弹层重做；落地页面转场、Tab 切换、列表 stagger、打卡 spring 微动效、进度/数字动画、减少动态效果降级；完成"组环+对勾"品牌标记、自适应/monochrome/round 图标、Android 12+ 平台 Splash。业务语义、数据层与离线约束零变化：DB 仍 v11、无新依赖/权限、数据/导出/会话/VM 零改动；**131 个 JVM 测试全绿**；clean 构建通过、仅 1 条既有弃用警告（原基线 2 条，`Common.kt` 重写后 menuAnchor 警告消失）；真机 PHB110 覆盖安装 Success、最终 APK 冷启动 COLD 614-831ms、无 FATAL。独立验证（verifier）判定 **GO**：AC-01..AC-15 全部 PASS，无 P0/P1；P2 仅训练详情 hero 无用时（计划偏差、非回归），P3 为切 Tab gfxinfo 5.46% 边界与若干证据缺口。产出 `/Users/suyi/projects/exercise/FitPlan-iteration12-debug.apk`（63,482,439 字节，sha256 `b96b029f5e34449cfa3d89e7cd120dfcd41d3adf5a563f416065de7b8fb2d6fe`）。

## 2. 目标达成情况

| 目标 | 结果 | 证据 |
| --- | --- | --- |
| 设计 token 集中（颜色/字阶/间距/圆角/层级/动效） | 完成 | `ui/theme/Color.kt`、`Type.kt`、`Shape.kt`、`Motion.kt`；屏幕代码 grep 无硬编码颜色/圆角/字号 |
| 通用组件复用 | 完成 | `ui/design/` 19 个组件；屏幕均通过组件与 token 实现，无裸 Card/Scaffold/TopAppBar |
| 页面全量重构（主页/计划/训练/组合/动作/记录/PR/设置/导航/弹层） | 完成 | 代码 diff + 真机浅/深色截图（`FitPlan/docs/verify/iteration12/`） |
| 动效统一 + 减少动态降级 | 完成 | NavHost 转场、Tab AnimatedContent、fitEnter stagger、打卡 spring、FitRingProgress/AnimatedCount、FitDialog spring；ValueAnimator.areAnimatorsEnabled + ON_RESUME 刷新 |
| 图标与启动体验 | 完成 | adaptive icon + monochrome + round icon、平台 Splash 主题；真机图标/Splash 截图 |
| 测试/构建/真机 | 完成 | 131/0/0；clean BUILD SUCCESSFUL；最终 APK install Success、COLD 614-831ms、无 FATAL |
| 业务行为零回归 | 完成（自动化 + 真机只读抽查；用户全量验收待执行） | 数据层/VM/导出/会话零改动 diff；交互清单核对；131 测试 |
| 独立验证 | 完成（GO） | `docs/iteration12-verification.md`：AC-01..AC-15 PASS；干净构建/131 测试/最终 APK 装机/浅深色/大字体/减少动态/gfxinfo/无障碍/对比度证据 |
| 文档更新 | 完成 | 本报告、HANDOVER、README、plan、brand、verification |

## 3. 多 Agent 编排

| 角色 | 职责 | 产出 |
| --- | --- | --- |
| Lead | 计划、备份、设计契约、任务板、阶段门、集成、最终构建/安装/截图、文档、发布 | `docs/iteration12-plan.md`、本报告、HANDOVER/README、APK |
| design-system-dev（teammate 1） | 设计系统、通用组件、导航壳、Splash/图标/品牌资产；只读质量扫描 | `ui/theme/**`、`ui/design/**`、`ui/Common.kt`、`ui/AppNav.kt`、`MainActivity.kt`、`res/**`、`docs/iteration12-brand.md` |
| screens-dev-a（teammate 2） | 主页/计划/训练详情 + 计划弹层 | 4 个页面文件 |
| screens-dev-b（teammate 3） | 组合/动作/记录/PR/设置 | 9 个页面文件 |
| verifier（teammate 4） | 独立源码审查、clean 构建、真机截图/无障碍/性能、AC 矩阵 | `docs/iteration12-verification.md`、`.scratch/iteration12/verify/**` |

编排要点：写作用域互斥（见计划 §10.2）；构建统一走 `.scratch/iteration11/with-gradle-lock.sh`；G1 真机冒烟后向 screens 回灌 2 轮 bugfix（360dp 组行换行、FAB 遮挡、单选/复选语义、重量值 contentDescription、group row graphicsLayer、FitListRow/FitCard role、新尺寸 token），全部修复后重新冻结再进入 verifier 阶段。

## 4. 改动摘要

### 4.1 设计系统（新增）

- 颜色：品牌 fallback 浅/深两套 + `FitExtraColors`（success/warning/track/hero/surfaceSunken/scrim）；动态取色保留（Android 12+），扩展色固定品牌值。对比度已算：正文 15.98:1 / 次文本 5.78:1 / 主色对白 6.30:1 / 深色主色对 bg 11.29:1。
- 字阶：CJK 友好 15 档，行高 ≥1.4，标题无负字距；`FitType.numeric` 使用 tabular numerals。
- 形状/间距/尺寸：`FitShapes`（card 24 / hero 28 / dialog 28 / sheet 28 / pill / field 16 / circle）、`FitSpace`（4dp 基准，gutter 20dp）、`FitSize`（rowMin 56、touchMin 48、setCircle 40、ring 96 等）。
- 动效：`FitMotion`（120/220/320/480ms；standard/emphasized 曲线；pop/gentle/sheet spring；enabled 降级）。
- 组件：FitScaffold、FitTopBar、FitScreenTitle、FitCard、FitSectionHeader、FitHairline、FitBottomBar、FitButton/FitIconButton/FitTextAction、FitDialog/FitBottomSheet、FitEmptyState、FitRingProgress/FitLinearProgress/AnimatedCount、FitListRow/FitSectionCard、FitMetaChip/FitStatusPill、Modifier.fitEnter、rememberFitHaptics、FitBrandMark、FitSnackbarHost。
- 兼容层：`ui/Common.kt` 保留 NumberField/DropdownSelector/ConfirmDialog/EmptyHint/setsSummary/parse* 旧签名与语义，旧屏幕零改动可编译（设计系统 P1 证据）。

### 4.2 页面重构（9 页 + 导航 + 弹层）

- 主页：大标题「今天」+ 日期 eyebrow；训练日进度 hero（环 + AnimatedCount）、主/次按钮（开始/结束训练）、今日记录卡（体重/备注）、清单；休息日品牌标记 + 说明 + 今日记录 + 「去计划页安排」主按钮；空组合引导；跨天刷新保留。
- 计划：大标题 + 周区间；复制上周/自动排程紧凑按钮；上一周/本周/下一周 + Pager；7 天日卡（今天高亮、状态、用时、编辑）；单日编辑 FitBottomSheet；自动排程/复制上周 FitDialog。
- 训练详情：FitTopBar + 组合名大标题 + 进度 hero；动作卡展开动画；组行单行 56dp（360dp 实测）；组圈 48dp 触达（视觉 40dp）+ spring pop + haptic；次数 stepper；标逃/重量 chip；同步提醒 warning 容器；当次重量行；动作备注；空/加载态。
- 组合：大标题 + 数量；卡片 + 更多菜单（编辑/复制/删除）；ExtendedFAB + 底部预留带（不遮挡末项）；选动作对话框（搜索/分类/多选/已添加）；编辑页行卡 + 校验。
- 动作：分类分组卡 + FlowRow 字段徽标 + 更多菜单 + PR 入口 + ExtendedFAB + 底部预留。
- 记录/PR/设置：日期切换与 EntryDialog；PR 分组卡 + 训练者 chip；设置分组列表（单选行 selectable 语义、危险操作红色、导出/清空确认、版本 BuildConfig）。
- 导航：FitBottomBar（选中/未选中图标、动画指示器、haptic、6 Tab 文案与路由不变）；Tab AnimatedContent；NavHost 详情页转场；边到边 + 系统栏明暗。
- 品牌资产：组环+对勾自适应图标（背景 #143D2A、前景 #F3F6EC）、monochrome、round icon、应用内 mark、平台 Splash 主题（windowSplashScreenBackground/AnimatedIcon/IconBackgroundColor/AnimationDuration），无新依赖。

### 4.3 关键文件

新增：`FitPlan/app/src/main/java/com/fitplan/app/ui/theme/{Color,Type,Shape,Motion}.kt`、`ui/design/*.kt`（14 个）、`res/drawable/{ic_launcher_background,ic_launcher_monochrome,ic_fitplan_mark}.xml`、`res/mipmap-anydpi-v26/ic_launcher_round.xml`、`res/values-night/colors.xml`、`docs/iteration12-brand.md`、`docs/iteration12-plan.md`、本报告。
重写：`ui/theme/Theme.kt`、`ui/Common.kt`、`ui/AppNav.kt`、`MainActivity.kt`、`ui/home/HomeScreen.kt`、`ui/plan/{PlanScreen,PlanDialogs,WorkoutScreen}.kt`、`ui/combo/{ComboListScreen,ComboEditScreen,ExercisePickerDialog}.kt`、`ui/exercise/{ExerciseListScreen,ExerciseEditScreen}.kt`、`ui/log/{LogScreen,EntryDialog}.kt`、`ui/pr/PrScreen.kt`、`ui/settings/SettingsScreen.kt`、`res/values/{themes,colors}.xml`、`res/drawable/ic_launcher_foreground.xml`、`res/mipmap-anydpi-v26/ic_launcher.xml`、`AndroidManifest.xml`（仅 roundIcon）。

## 5. 验证结果

### 5.1 构建与测试（Lead 独立执行，clean）

```bash
ANDROID_USER_HOME=/Users/suyi/projects/exercise/.android-home \
  sh /Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh \
  clean :app:testDebugUnitTest :app:assembleDebug --console=plain
```

- 结果：**BUILD SUCCESSFUL in 17s，exit=0**（48 actionable tasks: 48 executed；最终构建在 ActionChip 48dp 触达修复后执行）。
- 独立复现（verifier fresh clean）：**BUILD SUCCESSFUL in 16s，exit=0**，131/0/0，APK sha256 与 Lead 产物一致（`b96b029f…`），见 `docs/iteration12-verification.md` §4.1。
- 测试：**tests=131 / failures=0 / errors=0 / skipped=0**（20 个测试类；与第十一轮基线一致，UI 重构零测试破坏）。
- 警告：**1 条**（`data/db/AppDatabase.kt:270 fallbackToDestructiveMigration` 既有弃用）；`Common.kt` 重写后 menuAnchor 警告消失，零新增。
- 原始日志：`/Users/suyi/projects/exercise/.scratch/iteration12/gates/lead-build-final.log`（首轮 `lead-build-g1.log`、修复轮 `lead-build-g2.log` 一并保留）。
- 产物：`/Users/suyi/projects/exercise/FitPlan/app/build/outputs/apk/debug/app-debug.apk` → `/Users/suyi/projects/exercise/FitPlan-iteration12-debug.apk`（63,482,439 字节；sha256 `b96b029f5e34449cfa3d89e7cd120dfcd41d3adf5a563f416065de7b8fb2d6fe`）。

### 5.2 约束核验

- DB 版本：`version = 11`（`AppDatabase.kt:29`）；Entities/DAO/AppDatabase 零改动；`data/`、`export/`、`session/`、`util/`、`*ViewModel.kt` 与备份 diff 为空。
- 依赖/构建脚本：`app/build.gradle.kts`、`settings.gradle.kts` 与备份完全一致（无新增依赖）。
- 权限：Manifest 仅既有 `POST_NOTIFICATIONS`；无 INTERNET/网络资源。
- 变更范围：diff 仅 UI/Kotlin 设计系统 + MainActivity + Manifest roundIcon + res；备份完整保留。

### 5.3 真机（PHB110 / Android 16 / adb 5968de20）

- 安装：`adb install -r` **Success**（覆盖第十一轮 APK，既有库数据完好）。
- 冷启动：`am start -W` **Status ok / LaunchState COLD**；最终 APK Lead 测得 **831ms**、verifier 独立复测 **614ms（复测 572ms）**；logcat 无 `com.fitplan.app` FATAL EXCEPTION。
- Splash：启动帧为品牌深绿底 + 米白组环/对勾（截图 `FitPlan/docs/verify/iteration12/final-splash.png`），无白闪。
- 桌面图标：应用详情页显示品牌自适应图标（`final-appinfo-icon.png`）；资源含 monochrome（Android 13+ themed icon）。
- 页面截图：浅色 `final-01..14`、深色 `final-30..37`、大字体 1.3 `final-40..44`、减少动态 `final-50/51`、展开态/列表底部/对话框等。
- 组行：uiautomator 实测组圈中心间距 **228px @ 640dpi = 57dp（含 1dp divider）→ 单行 56dp**；组圈点击 bounds **192×192px = 48dp**。
- 列表底部：组合/动作/记录三页滚到底后最后一项完全露出，ExtendedFAB 悬浮在预留带上（`final-06-combo-bottom-light.png` / `final-10-exercise-bottom-light.png`）。
- 大字体 1.3：uiautomator 无越界节点（bounds 全在屏宽内）；组行按设计退化为 FlowRow 换行（无横向截断）。
- 减少动态：`animator_duration_scale=0` 冷启动/切 Tab/展开正常，动画退化不崩。
- 性能（scroll-only）：Lead **437 帧 / Janky 3 = 0.69%**（90th 10ms）；verifier 在最终 APK 独立复测 **449 帧 / Janky 5 = 1.11%**（90th 9ms），PASS（<5%）。Tab 切换段独立测量 293 帧 / 16 janky = **5.46%**（95th 29ms、99th 101ms），P3 边界、非滚动主链路。
- 无障碍：组圈 `toggleable + Role.Checkbox + stateDescription`；图标按钮 contentDescription 齐全；重量 chip/stepper 点击 bounds 48dp 高；日期/设置单选行等语义已修（详见 verifier 报告与 AC 矩阵）。

### 5.4 独立验证（verifier）

- 报告：`/Users/suyi/projects/exercise/docs/iteration12-verification.md`（282 行，AC-01..AC-15 矩阵 + 证据索引）。
- 结论 **GO**：无 P0/P1；源码/约束/构建/131 测试/业务语义/无障碍触达/对比度/性能 PASS；P2 仅训练详情 hero 无用时（计划偏差、非回归）。
- verifier fresh clean 构建 16s / 131/0/0 / 1 warning；APK sha256 与 Lead 产物一致；最终 APK 装机 lastUpdateTime 19:41:55、独立冷启动 COLD 614ms。
- 截图 23 张（verify-01..14、verify-20..26）：浅色主页/计划/单日 sheet/自动排程/复制上周/训练详情折叠+展开+重量弹窗/标逃 chip/组合列表+菜单；最终 APK 首页、深色、大字体、减少动态。uiautomator 实测组圈 48×48dp、组行节距 57dp、底栏 60×72dp。
- ENV-LIMITED：组合编辑/动作库/记录/PR/设置页独立截图、Splash 独立抓帧、TalkBack 实播受设备被用户占用影响；以 Lead 冒烟产物 + 代码审查补证（verification §5.6）。

### 5.5 对比度（WCAG，python 计算）

浅色：onSurface/bg 15.98、onSurfaceVariant/bg 5.78、primary/white 6.30、onPrimaryContainer/primaryContainer 10.96、tertiary/white 5.87、error/white 6.54。深色：onSurface/bg 14.99、onSurfaceVariant/bg 8.61、primary/bg 11.29、onPrimary/primary 8.70。均 ≥4.5:1（大字号 ≥3:1）。

## 6. 阶段门（SGR）

- **G0 计划完成门**：GO。计划 824 行；基线 131 测试；备份 888K/101 文件；任务板 task-1..4。证据 `.scratch/iteration12/gates/G0.md`。
- **G1 开发中门**：GO（含 2 轮 bugfix）。首轮全量 clean 构建 131/0/0 通过；真机冒烟发现 360dp 组行换行、列表 FAB 遮挡、单选/复选语义、重量值读屏等 5 类问题，回灌给 screens/DS 修复；修复后再次 clean 构建通过。
- **G2 测试前门**：GO。全部页面代码完成并冻结，131/0/0，assemble 成功；无 P0/P1 未决；verifier 进入独立验证。
- **G3 发布前门**：**GO**。最终 APK（b96b…）clean 构建可复现、装机 Success、独立冷启动 COLD 614ms、FATAL 0；AC-01..AC-15 PASS；文档收口。剩余 P2/P3 见 §7。证据 `.scratch/iteration12/gates/G3.md` 与 `docs/iteration12-verification.md`。

## 7. 遗留问题与已知边界

1. **共享元素转场未实现**：Compose 1.11.4 支持 SharedTransitionLayout，但跨 NavHost/列表作用域 plumbing 风险较高，列为 P2 stretch 未做（计划 A6），不影响现有转场质量。
2. **Snackbar 通道已备未用**：`FitSnackbarHost`/rememberFitSnackbarState 已提供，但设置导出/清空、主页权限提示仍用 Toast（计划 A7 允许，文案与触发条件不变）。
3. **训练详情 hero 不显示用时**：`WorkoutUiState` 无 duration 字段且本轮不改 VM；计划页用时正常（计划 §5.3 "用时若有" 的边界）。
4. **组行极端宽度**：360dp + 3 位整数重量（如 112.5 kg）+ 2 位次数时 FlowRow 会换行（无横向溢出）；常规数据单行 56dp。
5. **动态取色主导色彩**：Android 12+ 动态取色时主色来自壁纸，品牌识别由排版/形状/动效/图标/扩展色承载（计划 A8）；如需强品牌绿可在后续轮次评估应用内主题开关。
6. **DatePicker 仍为 M3 原生 Dialog**：日期选择/导出范围未包 FitDialog（避免大尺寸 DatePicker 在自定义对话框中溢出），交互稳定优先。
7. **真机全量业务验收待用户执行**：本轮完成自动化 + 只读交互抽查；导出写文件、清空、删除、打卡等写操作未在用户库上执行（计划 §13 清单）。
8. **P3 清单**：切 Tab gfxinfo 5.46%（边界，99th 101ms）、2 处裸 dp、列表卡内「更多」按钮 45.8dp、Snackbar 通道未用、同步提醒条未真机触发、短文本「标逃」chip 未独立 bounds 复测（已随最终 APK 装机+代码验证）。

## 8. 发布门与交付物

- 独立验证：`docs/iteration12-verification.md` **GO**；AC-01..AC-15 PASS，无 P0/P1；源码/约束/构建/测试/业务语义/无障碍触达/对比度/性能均通过；P2 仅训练详情 hero 未显示用时（计划偏差、非回归）。
- 验证环境异常（如实记录）：19:36 前后设备被真实用户拿起（微信/Steam 前台），verifier 发生过 1 次坐标点击落在非 FitPlan 前台（未触及 FitPlan 数据、未改状态），随即停止输入；不受影响项均已独立验证，受限项在 verification §5.6 标注 ENV-LIMITED 并以 Lead 冒烟产物补证。

- 自动化：131 测试全绿、clean 构建通过、APK 产出并安装成功。
- 文档：本报告、`docs/iteration12-plan.md`、`docs/iteration12-brand.md`、`docs/iteration12-verification.md`、`HANDOVER.md`、`README.md`。
- 截图：`/Users/suyi/projects/exercise/FitPlan/docs/verify/iteration12/`（浅/深/大字体/减少动态/Splash/图标）。
- 脚本：`/Users/suyi/projects/exercise/.scratch/iteration12/verify/uitools.py`。
- 回滚基线：`/Users/suyi/projects/exercise/.scratch/iteration12/backup/src`。

## 9. 后续建议

1. 用户按 HANDOVER §5 清单做全量真机验收（尤其写操作路径与训练会话通知）。
2. 引入 Compose UI/仪器测试补齐动效/无障碍回归（评估依赖与体积）。
3. 评估共享元素转场、应用内主题/动效开关、横屏双栏等下一轮候选。
4. 处理第十一轮遗留 P2：L13 迁移安全、L09 通知串行化、L10 通知计时。
