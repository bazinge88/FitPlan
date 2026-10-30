# FitPlan 第十二轮独立验证报告（前端全面重构）

- 报告版本：v1.1（2026-10-18，含最终 APK 装机后补测与 APK 哈希收口）
- 验证角色：verifier（独立于开发；不修改生产代码）
- 验证对象：第十二轮迭代（设计系统 / 页面 / 动效 / 品牌），计划见 /Users/suyi/projects/exercise/docs/iteration12-plan.md
- 工作区：/Users/suyi/projects/exercise，项目：/Users/suyi/projects/exercise/FitPlan
- 结论：**GO（通过）**。源码/约束/构建/测试/业务语义/无障碍触达/对比度/性能均 PASS；无 P0/P1 产品缺陷，P2 为一项计划偏差（训练详情 hero 未显示用时）与一项证据缺口（部分独立截图/TalkBack 实播，见 §5.6）。最终 APK b96b… 已装机并通过 verifier 独立冷启动（COLD 614ms、FATAL 0）；scroll-only gfxinfo 1.11% PASS，切 Tab 5.46% 记 P3 边界。设备曾被真实用户占用（详见 §11），未独立覆盖项已如实标注 ENV-LIMITED。

## 1. 结论摘要（AC 矩阵）

| ID | 验收项 | 结论 | 关键证据 |
| --- | --- | --- | --- |
| AC-01 | 设计 token 集中，屏幕无硬编码 | **PASS**（2 处裸 dp，P3） | theme/Color/Type/Shape/Motion 落地；屏幕 grep 无 Color(0x / RoundedCornerShape( / CircleShape / .sp / fontSize |
| AC-02 | 组件复用 | **PASS**（Snackbar 通道未使用，P3） | §4 组件全部实现；使用计数见 §3.5；FitSnackbarHost 仅作 FitScaffold 默认宿主 |
| AC-03 | 页面全覆盖重构 | **PASS（主链路+关键弹层）+ 部分页 ENV-LIMITED** | verifier 独立截图 23 张：浅色主页/计划/训练详情（折叠/展开/标逃 chip）/组合+更多菜单、深色主页/计划、大字体主页/计划、减少动态主页/训练详情、最终 APK 首页；动作/记录/PR/设置页独立截图未取得（设备占用），Lead final-09/10/35/36/37 补充；代码审查 9 页面全部重构且 VM 调用集合未变 |
| AC-04 | 动效与减少动态降级 | **PASS** | motion.enabled 24 处降级点；Motion.kt 读 ValueAnimator.areAnimatorsEnabled + ON_RESUME；verifier 独立 animator=0 冷启动+训练详情展开不崩（verify-25/26），Lead final-50/51 补充 |
| AC-05 | 图标与启动 | **PASS** | APK 含 adaptive/round/monochrome/splash 主题；应用详情图标截图正常；final-splash.png 深绿底+米白组环对勾 |
| AC-06 | 浅色/深色/动态取色 | **PASS** | verifier 独立：verify-21-home-dark / verify-22-plan-dark（最终 APK，dynamic color 生效下深色正常）；Lead final-30..37 补其余 6 页；动态取色开启时主色为系统青色（符合计划 §3.1 策略） |
| AC-07 | 大字体 1.3 不截断 | **PASS** | verifier 独立：verify-23-home-font13 / verify-24-plan-font13（最终 APK，目视无截断/溢出）；Lead final-40..44 补设置/组合/训练详情页 |
| AC-08 | 无障碍语义与 48dp 触达 | **PASS** | uiautomator dump：组圈 48×48dp、标逃/取消逃标 chip 57.5×48dp、组重量 chip 48×48dp、重量 ±/数值 48dp、返回 48dp、底栏 60×72dp、组行节距 57dp；Role.Checkbox+stateDescription 保留；final ActionChip widthIn(min=48dp) 已随最终 APK 装机（构建+代码验证）；短文本「标逃」chip 独立 bounds 复测 ENV-LIMITED（现有数据无「未打卡未逃」组，避免写数据） |
| AC-09 | 对比度 ≥4.5:1 | **PASS** | 计算脚本 .scratch/iteration12/verify/contrast.out：计划 §3.1 全部文本色对与计划值精确一致且 ≥4.5；outline（非文本）3.20/4.20 ≥3:1 |
| AC-10 | 60fps janky <5% | **PASS（滚动）+ P3（切 Tab 边界）** | verifier 独立 scroll-only 最终 APK：449 帧 / 5 janky = 1.11%，90th 9ms、95th 9ms；Lead G2 437 帧/3=0.69%。切 Tab 段 293 帧/16=5.46%（95th 29ms、99th 101ms），略高于 5% 目标，记 P3 |
| AC-11 | 业务零回归 | **PASS** | data/export/session/util/VM/测试零改动；131/0/0；WorkoutActions/WorkoutChecklist/workoutActions 契约 diff 为空；9 个页面的 VM 调用集合 diff 为空；关键文案保留；真机既有数据完好 |
| AC-12 | clean 构建零新增警告 | **PASS** | fresh clean 构建 exit 0，131 tests/0 failures/0 errors，仅 1 条既有 warning（AppDatabase fallbackToDestructiveMigration，计划允许） |
| AC-13 | 真机安装与冷启动 | **PASS** | 最终 APK b96b… 已装机（Lead install -r Success，lastUpdateTime 19:41:55）；verifier 独立 force-stop + am start -W → COLD ok TotalTime 614ms（复测 572ms），Lead 831ms；logcat FATAL 0 条；pre-final 阶段 crash buffer 无 com.fitplan.app（2 条 FATAL 为 uiautomator 进程，§5.3） |
| AC-14 | 文档 | **PASS** | iteration12-plan/iteration12-brand/iteration12-report/HANDOVER（11 处「第十二轮」）/README 均已更新（mtime 19:22–19:38）；本报告已产出 |
| AC-15 | 约束（DB/依赖/权限/备份/越界写） | **PASS** | DB v11、10 个迁移未改；Entities/Daos/AppDatabase 零 diff；build.gradle 零 diff；Manifest 仅 +roundIcon；权限仅 POST_NOTIFICATIONS；backup 101 文件完好；diff 清单全部在写作用域内 |

## 2. 验证环境与产物

- 构建环境：ANDROID_USER_HOME=/Users/suyi/projects/exercise/.android-home，锁脚本 /Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh（内部 GRADLE_USER_HOME=.gradle-home + --offline）；未直接调用 ./gradlew。
- 真机：PHB110，adb id 5968de20，Android 16 / API 36；物理 1440×3216，density override 640（4 px/dp，即 360×804dp）；a11y 服务为 hello.litiaotiao（第三方，未改动）。
- 最终代码（含 screens-dev-a 最后一次 ActionChip widthIn(min=48dp) 修复）构建产物：
  - APK：/Users/suyi/projects/exercise/FitPlan/app/build/outputs/apk/debug/app-debug.apk，63,482,439 字节，sha256 `b96b029f5e34449cfa3d89e7cd120dfcd41d3adf5a563f416065de7b8fb2d6fe`
  - 与 Lead 发布产物 /Users/suyi/projects/exercise/FitPlan-iteration12-debug.apk 哈希完全一致。
- verifier 原始证据目录：/Users/suyi/projects/exercise/.scratch/iteration12/verify/
  - verify-build.log（19:27，最终改动前的构建）、verify-build-final.log（19:38，最终代码构建）、verify-build-final-start.txt
  - contrast.py / contrast.out（对比度计算）
  - device/（baseline-settings.txt、install.txt、am-start-cold.txt、dumps/*.xml、bounds-summary.txt、touch-targets.txt、current.png）

## 3. 源码与约束审查（T-04 步骤 1）

### 3.1 与备份基线的 diff（越界写检查）

基线：/Users/suyi/projects/exercise/.scratch/iteration12/backup/src（101 文件），命令 `diff -rq backup/src FitPlan/app/src`。全部差异（32 行）：

- 重写（22）：AndroidManifest.xml、MainActivity.kt、ui/AppNav.kt、ui/Common.kt、ui/theme/Theme.kt、home/HomeScreen.kt、plan/PlanScreen.kt、plan/PlanDialogs.kt、plan/WorkoutScreen.kt、combo/ComboListScreen.kt、combo/ComboEditScreen.kt、combo/ExercisePickerDialog.kt、exercise/ExerciseListScreen.kt、exercise/ExerciseEditScreen.kt、log/LogScreen.kt、log/EntryDialog.kt、pr/PrScreen.kt、settings/SettingsScreen.kt、res/values/themes.xml、res/values/colors.xml、res/mipmap-anydpi-v26/ic_launcher.xml、res/drawable/ic_launcher_foreground.xml
- 新增：ui/theme/{Color,Type,Shape,Motion}.kt；ui/design/（14 个组件文件）；res/drawable/{ic_launcher_background,ic_launcher_monochrome,ic_fitplan_mark}.xml；res/mipmap-anydpi-v26/ic_launcher_round.xml；res/values-night/colors.xml
- **零改动**：data/、export/、session/、util/、全部 *ViewModel.kt、app/src/test/（`diff -rq` 全部 exit 0）
- **零改动**：FitPlan/app/build.gradle.kts（与 backup/app-build.gradle.kts diff 为空）、settings.gradle.kts / build.gradle.kts / gradle.properties（mtime 2026-09-28 16:15，未改）
- AndroidManifest 唯一差异：`+ android:roundIcon="@mipmap/ic_launcher_round"`（无新权限）
- 结论：无越界写，全部改动落在 §10.2 写作用域内。

### 3.2 数据层与 DB

- AppDatabase.kt：`version = 11`，MIGRATION_1_2 … MIGRATION_10_11 共 10 个迁移全部存在且未改；Entities.kt / Daos.kt / AppDatabase.kt 与备份 diff 为空。
- 无新表/新列/新迁移；无 schema 变化。

### 3.3 依赖与权限（APK 实测）

- `aapt2 dump permissions`：仅 android.permission.POST_NOTIFICATIONS（另有 androidx debug 自动注入的 com.fitplan.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION）；**无 INTERNET**。
- `aapt2 dump badging`：minSdk/targetSdk/compileSdk=36；无新增第三方依赖（app/build.gradle.kts 未改、settings.gradle.kts 未改、无 core-splashscreen）。
- APK 资源实测：mipmap/ic_launcher、mipmap/ic_launcher_round、drawable/{ic_launcher_foreground,ic_launcher_background,ic_launcher_monochrome,ic_fitplan_mark}、color/{fitplan_splash_background,fitplan_window_background} 均在；Theme.FitPlan 含 windowSplashScreenBackground/AnimatedIcon/IconBackgroundColor/AnimationDuration=200 与 windowBackground；merged manifest roundIcon=@0x7f080001。

### 3.4 token 与契约核对

- Color.kt / Type.kt / Shape.kt / Motion.kt 与计划 §3 数值一致（浅/深两套 M3 角色、FitExtraColors 13 项、字阶 14 档、FitShapes 8/12/16/24/28、FitSpace 2..40、FitSize 20/56/48/24/40/56/72/64/52/96/10、动效 120/220/320/480 + 3 条 easing + 3 组 spring）。
- 契约 diff 全空：WorkoutActions（data class）、`workoutActions(vm)`、WorkoutChecklist 签名；AppNav()/MainScaffold(...) 签名；路由 main/pr/workout/{date}/combo/{comboId}/exercise/{exerciseId} 未变。
- 兼容层 Common.kt：parseDoubleOrNull/intOrNull/NumberField/DropdownSelector/ConfirmDialog（仍先 onDismiss 再 onConfirm）/EmptyHint/setsSummary 签名与语义不变，仅外观 token 化与 menuAnchor 新 API。
- 9 个页面文件对 vm/actions/state 的调用集合与备份 diff 为空；空态/说明文案逐条保留（如「今天没有安排训练，可到「计划」页查看或安排」「把若干动作固定成一组，再安排到周计划里。」「点击右下角新建，如卧推、深蹲、辅助引体向上。」「训练记录是可选的，用于留档和导出数据。」「打卡时记录重量后自动生成。」）。
- 无障碍语义：组圈 `toggleable + Role.Checkbox + contentDescription + stateDescription(已打卡/未打卡)` 保留；标逃/组重量/重量±/返回/周切换/更多操作均有 contentDescription；底栏 selectableGroup + Role.Tab。

### 3.5 组件使用计数（ui 屏幕代码，排除 design/theme 定义）

FitCard 25、FitSectionHeader 17、FitHairline 19、FitScreenTitle 16、FitScaffold 10、FitTopBar 8、FitBottomBar 3、FitButton 20、FitIconButton 24、FitTextAction 12、FitDialog 32、FitBottomSheet 2、FitEmptyState 22、FitRingProgress 2、FitLinearProgress 2、AnimatedCount 3、FitListRow 5、FitSectionCard 2、FitMetaChip 9、FitStatusPill 2、fitEnter 21、rememberFitHaptics 4、FitBrandMark 2。
token 使用：FitSpace 185、FitSize 60、FitShapes 25、fitColors 8、fitMotion 7、MaterialTheme.colorScheme 97、typography 71。
未使用：FitSnackbarHost / rememberFitSnackbarState（仅 FitScaffold 默认宿主；Toast 保留，计划 A7 允许）。

### 3.6 硬编码扫描（AC-01）

在 ui/（排除 theme/design）grep：`Color(0x` = 0；`Color.White/Black/Red/Green/Gray/Blue/Yellow/Transparent` = 0；`RoundedCornerShape(` = 0；`CircleShape` = 0；`fontSize =` = 0；`数字.sp` = 0；裸 dp 仅 2 处：WorkoutScreen.kt:336 `PaddingValues(0.dp)`、PlanDialogs.kt:173 DatePicker 标题 `padding(16.dp)`（P3，可后补 token）。

### 3.7 其他审查结论

- 共享元素转场：全仓 grep sharedElement/SharedTransition = 0（计划 A6 列为 P2 stretch，未实现，非 AC 要求）。
- Toast→Snackbar 未迁移：HomeScreen（通知权限拒绝）、SettingsScreen（导出/清空）仍用 Toast，文案不变；计划 A7 允许。
- 动效降级点 24 处（AppNav/FitEnter/FitBottomBar/FitProgress/WorkoutScreen 等），SetCircle 在 motion 关闭时不播 spring、颜色动画 tween(0)。

## 4. 构建与测试（T-04 步骤 2，fresh 执行）

### 4.1 最终代码 clean 全量构建（verifier 独立执行）

命令（2026-10-18 19:38:42 开始，19:38:59 结束）：

```bash
ANDROID_USER_HOME=/Users/suyi/projects/exercise/.android-home \
  sh /Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh \
  clean :app:testDebugUnitTest :app:assembleDebug --console=plain
```

- 日志：/Users/suyi/projects/exercise/.scratch/iteration12/verify/verify-build-final.log
- 首任务 `:app:clean`（第 1 行）→ 证明非增量；结果 `BUILD SUCCESSFUL in 16s`、`EXIT=0`（日志第 237/239 行）、48 actionable tasks。
- 测试：131 tests / 0 failures / 0 errors（20 个 test-results XML；逐类：Iteration11Acceptance 4、TransactionRollback 2、MarkdownExporter 18、LoadingStateContract 1、ComboEditViewModel 8、ExerciseEditViewModel 3、ExerciseListViewModel 2、BodyWeight 4、Notes 10、PlanViewModel 5、WorkoutProgress 3、WorkoutUiState 5、WorkoutViewModel 37、PrUiState 3、SettingsExport 2、SettingsViewModel 2、InputRules 5、PrRecords 3、QuickCheckIn 7、TrainingDuration 7）。
- 编译 warning：**1 条**（AppDatabase.kt:270 `fallbackToDestructiveMigration` 弃用），与计划 §9.1「Common 重写后允许基线降为 1 条」一致，无其他 warning。
- APK：63,482,439 字节，sha256 `b96b029f5e34449cfa3d89e7cd120dfcd41d3adf5a563f416065de7b8fb2d6fe`（与 Lead 发布 APK 一致）。
- 环境噪声（非代码问题）：日志 46/148 行出现 `java.nio.file.FileSystemException … kotlin-daemon-client…tmp: Operation not permitted` → Kotlin 增量守护进程不可用，自动 fallback 为进程内编译，构建仍成功；属沙箱环境限制，不计入 warning。

### 4.2 最终改动前构建（供对照）

- 日志：/Users/suyi/projects/exercise/.scratch/iteration12/verify/verify-build.log（19:27:23–19:27:40）
- 同样 clean + 131/0/0 + exit 0 + 1 条既有 warning；APK sha256 `9927ae13ca9877ff7be6f65077dc84440608689cec630d12f1290f82d1e5012a`（此版本即真机实际安装版本；final 版本较其只多 ActionChip 一行 widthIn(min=48dp)）。

## 5. 真机验证（T-04 步骤 3）

### 5.1 安装与冷启动

- 基线设置（/Users/suyi/projects/exercise/.scratch/iteration12/verify/device/baseline-settings.txt）：font_scale=1.0、animator/transition/window scale=1、深色=no、heads-up=0（Lead 预置）、无障碍服务=hello.litiaotiao（未改动）。
- `adb -s 5968de20 install -r app-debug.apk` → `Performing Streamed Install / Success`（19:31，日志 device/install.txt）。
- 冷启动 `am start -W`：`Status: ok / LaunchState: COLD / TotalTime: 700ms`（device/am-start-cold.txt）；复测 654ms。
- 最终 APK（b96b…）装机与冷启动（19:41:55 lastUpdateTime）：Lead `install -r` Success；verifier 独立 `am force-stop` + `am start -W` → `Status: ok / LaunchState: COLD / TotalTime: 614ms`（复测 572ms），Lead 测得 831ms；`FATAL EXCEPTION` 0 条（device/final-patch.txt）。
- logcat crash buffer 无 `com.fitplan.app` 崩溃；应用进程存活（pid 362）。

### 5.2 既有数据完好（升级不丢数据）

冷启动后只读观察：主页 10月18日·周日休息日 + 今日记录（体重「今天未记录」、备注「未写」）；计划页 10/12–10/18 本周、「周二 10/13 垂直推拉+抗旋钮 训练日 用时 1 小时 20 分钟」、「今天」高亮在周日；训练详情 16/19 组已完成、5 个动作、最后一动作 3 组均为「已逃」（取消逃标 chip）；组合页 4 个组合与摘要；均为用户既有数据，未写入。

### 5.3 FATAL EXCEPTION 排查结论

logcat 中 2 条 FATAL（19:31:22 PID 31462、19:31:35 PID 31684）均为 **uiautomator dump 命令行进程**：`com.android.commands.uiautomator.DumpCommand.run` + `IllegalStateException: UiAutomationService … already registered`，成因是 Lead 截图脚本与 verifier dump 并发导致 UiAutomation 重复注册；crash buffer 中无 fitplan 进程，应用全程未崩。device/dumps 与 logcat 原始输出可复核。

### 5.4 浅色页面与对话框（verifier 独立截图，FitPlan/docs/verify/iteration12/）

| 截图 | 页面/弹层 | 核对结果 |
| --- | --- | --- |
| verify-01-home-light.png | 主页（休息日） | eyebrow 日期、大标题「今天」、休息卡+品牌标记、今日记录体重/备注、去「计划」页安排主按钮、6 Tab 底栏 |
| verify-02-plan-light.png | 计划页 | 大标题「计划」+周区间、复制上周/自动排程、上一周/本周/下一周、7 天卡、今天 primaryContainer 高亮+「今天」标签、用时显示 |
| verify-03-plan-daysheet-light.png | 单日编辑 BottomSheet | 2026-10-18（周日）、休息日/训练日分段、保存、从其他日期复制、移除安排、复制前一天（10/17）；打开后系统返回关闭，未保存 |
| verify-04-plan-autoschedule-light.png | 自动排程 Dialog | 三分化/练一休一、3 个槽位默认已绑定组合、覆盖提示、取消/生成；取消关闭 |
| verify-05-plan-copyweek-light.png | 复制上周确认 | 文案含上一周/当前周区间、取消/复制；取消关闭 |
| verify-06-workout-collapsed-light.png | 训练详情 | TopBar 返回+「训练详情」、日期 eyebrow、组合名大标题、环形进度 16/19、动作卡折叠态、动作完成聚合圈 |
| verify-07-workout-expanded-light.png | 训练详情展开 | 重量行（-33 kg 当次、±/点数值）、5 个组圈（48dp）、次数 stepper、组重量 chip（-19/-12/mg）、全部完成动作聚合圈 ✓ |
| verify-08-workout-weight-dialog-light.png | 当次重量弹窗 | 「当次重量（kg）」、正数负重·0 自重·负数辅助说明、只记录今天不写组合目标说明、取消/确定；取消关闭 |
| verify-09-workout-setweight-dialog-light.png | 组重量弹窗 | 「第 2 组重量（kg）」、只作用于这一组说明；取消关闭 |
| verify-10/10b/11-workout-*.png | 底部动作展开/标逃 chip | 「绳索pallof拉」0/3、重量行 15 kg（目标）、组行 48dp 圈、「取消逃标」chip（既有逃组数据） |
| verify-12/12b-combo-list-light.png | 组合列表 | 大标题「训练组合」+「共 4 个组合」、组合卡摘要（前 3 行+等 N 个动作）、更多菜单、ExtendedFAB「新建组合」 |
| verify-14-combo-menu-light.png | 组合更多菜单 | 编辑/复制/删除三项（删除确认未点，避免写数据） |

说明：verify-13 原为误命名（点击组合卡未进入编辑页，实际仍是列表，因组合列表本就没有整卡点击、编辑入口在更多菜单，与旧版一致），已重命名为 verify-12b-combo-list-light.png，不进入 AC 证据。

### 5.5 设备空闲后补测（最终 APK，verifier 独立，19:43–19:44）

设备恢复空闲后，用 Lead 装好的最终 APK（lastUpdateTime 19:41:55）完成：

| 截图/证据 | 内容 | 结果 |
| --- | --- | --- |
| verify-20-home-finalapk-light.png | 最终 APK 冷启动首页（休息日） | 正常；动态取色生效（系统青色主色） |
| verify-21-home-dark.png / verify-22-plan-dark.png | 深色模式主页/计划页 | 正常，无白块/双重 padding |
| verify-23-home-font13.png / verify-24-plan-font13.png | font_scale=1.3 主页/计划页 | 无截断/横向溢出，今日卡与底栏正常 |
| verify-25-home-reducedmotion.png / verify-26-workout-reducedmotion.png | animator_duration_scale=0 冷启动 + 训练详情展开 | 不崩、动画退化（无 spring 过渡残留） |
| device/gfxinfo-scroll.txt | 组合列表 scroll-only 6 次 | 449 帧 / 5 janky = **1.11%**，90th 9ms、95th 9ms、99th 15ms |
| device/gfxinfo-tabswitch.txt | Tab 依次切换 6 次 | 293 帧 / 16 janky = **5.46%**（legacy 10.92%），95th 29ms、99th 101ms |
| device/final-patch.txt | 装机时间、冷启动、gfxinfo、FATAL 汇总 | FATAL 0；设置已恢复（font 1.0 / animator 1 / night no） |

### 5.6 仍未独立覆盖/受限项（ENV-LIMITED）

19:36 前后设备被真实用户拿起使用（前台切到微信，另出现第三方 App/OPLUS 截图窗口），当即停止输入与截图；19:43 设备恢复空闲后仅补测了上表项目。以下项目未获得 verifier 独立真机证据，改用 Lead 冒烟产物 + 代码审查补证：

- 组合编辑/选动作对话框、动作库/编辑、记录/EntryDialog、PR、设置/导出/清空/训练者对话框的独立截图（Lead final-09/10/35/36/37/20/21 有部分覆盖；代码审查 9 文件已重构且 VM 调用集合未变）
- 桌面图标深色壁纸/主题图标三态（Lead final-launcher-home/drawer、final-appinfo-icon；verifier 已目视 final-appinfo-icon 品牌图标正常；主题图标模式未切换）
- Splash 启动瞬间抓帧（Lead final-splash.png：深绿底+米白组环对勾；verifier 未独立抓帧）
- TalkBack 实播（设备启用的是第三方 hello.litiaotiao 服务，未改全局无障碍设置；以 uiautomator 语义+content-desc 作替代证据）
- 同步提醒条 UI（现有数据未满足触发条件，避免写数据未构造；代码路径 SyncPromptRow 文案/逻辑保留、ViewModel 零改动）
- 短文本「标逃」chip 的 48dp 独立 bounds（现有数据该动作 3 组均「已逃」，无「未打卡未逃」组；不写数据无法构造；已随最终 APK 装机 + 代码/构建验证 widthIn(min=48dp)）
- 打卡写路径/导出/清空/训练者增删等会写用户数据的操作（按纪律只读验收，仅打开后取消）

## 6. 无障碍触达（uiautomator dump 实测，density 640 = 4px/dp）

原始：.scratch/iteration12/verify/device/dumps/*.xml、touch-targets.txt、bounds-summary.txt。

| 控件 | 实测 px | 换算 dp | 结论 |
| --- | --- | --- | --- |
| 组打卡圈（toggleable/Checkbox） | 192×192 | 48.0×48.0 | PASS（视觉 40dp，触达扩到 48dp） |
| 标逃/取消逃标 chip | 230×192 | 57.5×48.0 | PASS |
| 每组重量 chip | 192×192 | 48.0×48.0 | PASS |
| 重量 − / + 按钮 | 192×192 | 48.0×48.0 | PASS |
| 重量数值（点开弹窗） | 296×192 | 74.0×48.0 | PASS |
| 返回按钮 | 192×192 | 48.0×48.0 | PASS |
| 上一周/下一周 | 192×192 | 48.0×48.0 | PASS |
| 底栏 Tab | 240×288 | 60.0×72.0 | PASS |
| 组行节距 | 228 纵向 | 57.0 | ≥56dp 行高 |
| 更多操作（列表卡内 2 处） | 192×183 | 48.0×45.8 | P3 轻微不足（图标按钮本体 48dp，行内被裁 2px） |

语义：组圈 contentDescription「完成第 N 组打卡/取消第 N 组打卡」+ stateDescription「已打卡/未打卡」+ Role.Checkbox；标逃 chip「取消第 N 组逃标」；组重量「编辑第 N 组重量」；重量「编辑当次重量，当前 X kg」；周切换「上一周/下一周」；返回「返回」。与计划 §8.1 一致。

## 7. 性能（gfxinfo）

verifier 在最终 APK 上独立测量（设备 19:43 空闲后，设置已恢复默认动画）：

| 场景 | 帧数 | Janky | 90th | 95th | 99th | 判定 |
| --- | --- | --- | --- | --- | --- | --- |
| 组合列表 scroll-only ×6 | 449 | 5（**1.11%**） | 9ms | 9ms | 15ms | PASS（<5%） |
| Tab 依次切换 ×6 | 293 | 16（**5.46%**，legacy 10.92%） | 9ms | 29ms | 101ms | P3 边界（略高于 5%） |

- 原始输出：.scratch/iteration12/verify/device/gfxinfo-scroll.txt、gfxinfo-tabswitch.txt。
- Lead G2 另一次 scroll-only：437 帧 / 3 janky = 0.69%，90th 10ms（.scratch/iteration12/gates/G2.md）。
- Lead「853 帧 janky 6.80%」采集含冷启动与截图操作，为噪声测量，不采信。
- Tab 切换片段 99th 101ms 提示 AnimatedContent 重建有少量长帧；属视觉动效场景，非滚动/打卡主链路，记 P3 待后续优化（可考虑减少 Tab 内容重建或缩短 scale 过渡）。
- 代码侧低风险证据：动画仅改 alpha/translation/scale/color；fitEnter stagger 限 8 项且 rememberSaveable 不重复；指示器只改 graphicsLayer；列表 key 稳定；无位图解码/无 blur。

## 8. 对比度（AC-09）

脚本：/Users/suyi/projects/exercise/.scratch/iteration12/verify/contrast.py，原始输出 contrast.out。计划 §3.1 文本色对全部与计划给定值精确一致（误差 <0.01）且 ≥4.5:1：

| 模式 | 色对 | 计算值 | 计划值 | 阈值 |
| --- | --- | --- | --- | --- |
| light | onSurface/bg | 15.98 | 15.98 | PASS |
| light | onSurfaceVariant/bg | 5.78 | 5.78 | PASS |
| light | onSurfaceVariant/surface | 6.22 | 6.22 | PASS |
| light | primary/white | 6.30 | 6.30 | PASS |
| light | onPrimaryContainer/primaryContainer | 10.96 | 10.96 | PASS |
| light | tertiary/white | 5.87 | 5.87 | PASS |
| light | error/white | 6.54 | 6.54 | PASS |
| dark | onSurface/bg | 14.99 | 14.99 | PASS |
| dark | onSurfaceVariant/bg | 8.61 | 8.61 | PASS |
| dark | primary/bg | 11.29 | 11.29 | PASS |
| dark | onPrimary/primary | 8.70 | 8.70 | PASS |
| dark | onPrimaryContainer/primaryContainer | 7.76 | 7.76 | PASS |
| dark | tertiary/bg | 11.16 | 11.16 | PASS |
| dark | error/bg | 11.10 | 11.10 | PASS |

补充：outline 对白 3.20:1、深色对 bg 4.20:1。outline 用于图标/边框（非文本），按 WCAG 1.4.11 非文本对比度 3:1 判定 PASS。

## 9. 动效与减少动态（AC-04）

- 页面转场：NavHost 主路由 fade 220/120ms；详情路由 slide 1/5+fade，返回反向；减少动态时纯淡入。
- Tab 切换：AnimatedContent fade + scale 0.98→1（220ms；关闭时 100ms fade）。
- 列表入场：Modifier.fitEnter alpha+12dp translate、35ms stagger ≤8、rememberSaveable 防重播。
- 打卡：SetCircle spring pop 1→1.15→1 + 颜色过渡 + haptic；全部完成聚合圈 pop；进度环/条 spring gentle；AnimatedCount。
- 弹层：FitDialog/FitBottomSheet + FitShapes.dialog/sheet；底栏胶囊指示器只改绘制层。
- 减少动态检测：Motion.kt `ValueAnimator.areAnimatorsEnabled()` + LocalLifecycleOwner ON_RESUME 刷新；`LocalFitMotion.enabled` 贯穿 AppNav/FitEnter/FitBottomBar/FitProgress/WorkoutScreen 等 24 处；关闭时时长为 0/snap/直接显示。
- 真机独立实测：animator_duration_scale=0 下冷启动 + 训练详情展开正常（verify-25/26），无 spring 残留；Lead final-50/51 补充主页/训练页截图。

## 10. 残余风险与未完成项

| 级别 | 项 | 说明 | 建议 |
| --- | --- | --- | --- |
| P2（计划偏差） | 训练详情 hero 未显示「用时」 | 计划 §5.3 写「进度 hero（环形进度 + 完成组数/总组数 + 用时若有）」，实现只有组数进度；G2 已记账。非回归（旧版详情页也无用时，VM 未改无法直接取用时） | 与用户确认是否需要；若要，需在 UI 层由打卡时刻推导（不改 VM） |
| P3（性能） | 切 Tab gfxinfo 5.46% | verifier 独立测量 293 帧/16 janky（scroll-only 1.11% PASS）；99th 101ms，AnimatedContent 重建有少量长帧 | 可接受；若用户感知卡顿，再收紧 Tab 过渡或复用页面状态 |
| P3（证据） | 部分页面独立截图缺失 | 组合编辑/选动作、动作库/编辑、记录、PR、设置页未获 verifier 独立截图（设备被用户占用）；Lead final-09/10/35/36/37 部分覆盖 | 可由用户真机验收时逐页确认（业务语义已由代码/VM/131 测试保障） |
| P3（证据） | Splash 启动瞬间未独立抓帧 | 只有 Lead final-splash.png | 用户验收时可目视启动过程 |
| P3（证据） | TalkBack 未实播 | 设备第三方无障碍服务占用 | 用户可临时开启 TalkBack 抽验关键路径 |
| P3 | 2 处裸 dp | WorkoutScreen 0.dp contentPadding、PlanDialogs DatePicker 标题 16.dp | 改用 FitSpace token（可选） |
| P3 | 更多操作按钮 45.8dp | 列表卡内 192×183px | 与设计确认是否需补 min 触达 |
| P3 | FitSnackbar 通道未使用 | Toast 保留（A7 允许），组件已实现 | 下一轮统一或删除未用通道 |
| P3 | 共享元素未做 | 计划 A6 明确为 stretch | 记录为未完成，不影响 AC |
| P3 | 同步提醒条未真机触发 | 现有数据不满足触发条件，避免写数据 | 用户数据变化后可抽查 |
| P3 | TalkBack 未实播 | 设备第三方无障碍服务占用，未改全局设置 | 以 uiautomator 语义替代；用户可自行开启 TalkBack 抽验 |
| P3 | 动态取色主导视觉 | 设备开启动态取色，主色为系统青色而非品牌绿（计划 §3.1 策略允许） | 若需品牌绿截图，可临时关闭动态取色 |
| P3（证据） | 短文本「标逃」chip 未独立 bounds 复测 | 现有数据该动作 3 组均已逃，无「未打卡未逃」组；不写数据无法构造 | 已随最终 APK 装机 + 代码/构建验证 widthIn(min=48dp)；用户后续打卡时可复核 |

## 11. 验证环境异常记录（如实记录）

- 2026-10-18 19:36 前后，真机 PHB110 前台从 com.fitplan.app 被切换到 com.tencent.mm（微信），最近任务出现 com.pwrd.steam.esports 与 OPLUS 长截屏窗口，判断为真实用户拿起手机使用。
- 影响：verifier 脚本在切换瞬间执行了一次坐标点击 (1024,896)（原目标为组合菜单「编辑」项），落在非 FitPlan 前台，未触及 FitPlan 数据、未修改任何 FitPlan 状态；随后立即停止一切输入/截图，只做只读 dumpsys 轮询，未再采集用户私人内容。
- 处置：交付物只包含前台为 com.fitplan.app 时采集的截图与 dump；受影响的深色/大字体/减少动态/gfxinfo/部分页面独立复测标记 ENV-LIMITED。
- 并发记录：19:31 期间 Lead 的 final-* 截图脚本与 verifier dump 并发，导致 2 次 uiautomator 进程 FATAL（非 App 崩溃），已排除。

## 12. 证据索引

- 构建：.scratch/iteration12/verify/verify-build-final.log、verify-build.log、verify-build-final-start.txt
- 对比度：.scratch/iteration12/verify/contrast.py、contrast.out
- 真机：.scratch/iteration12/verify/device/{baseline-settings.txt, install.txt, am-start-cold.txt, final-patch.txt, gfxinfo-scroll.txt, gfxinfo-tabswitch.txt, dumps/, bounds-summary.txt, touch-targets.txt}
- 截图（verifier 独立，浅色 + 深色/大字体/减少动态/最终 APK）：FitPlan/docs/verify/iteration12/verify-01…verify-14（16 张，含 12b）、verify-20…verify-26（7 张）
- 截图（Lead 冒烟产物，用于 ENV-LIMITED 补证）：FitPlan/docs/verify/iteration12/final-01/02/03/04/05/06/09/10（浅色）、final-20/21（对话框）、final-30…37（深色）、final-40…44（大字体 1.3）、final-50/51（减少动态）、final-60/61/62（最终 APK 首页/训练详情/桌面）、final-launcher-home/drawer、final-appinfo-icon、final-splash
- Lead 最终构建日志：.scratch/iteration12/gates/lead-build-final.log；G2 证据：.scratch/iteration12/gates/G2.md
- 计划/品牌文档：docs/iteration12-plan.md、docs/iteration12-brand.md

## 13. 结论

- 无 P0/P1 产品缺陷；P2 仅一项计划偏差（训练详情 hero 未显示用时，G2 已记账；非回归）。源码、约束、构建、测试、业务语义、无障碍触达、对比度、性能均通过；最终 APK 在 clean 构建下可复现（sha256 b96b029f… 与 Lead 产物一致），且已装机、独立冷启动 COLD 614ms、FATAL 0。
- 残余 P3：切 Tab gfxinfo 5.46%（边界）、2 处裸 dp、更多操作按钮 45.8dp、Snackbar 通道未用、共享元素未做、同步提醒条未真机触发、短文本「标逃」chip 未独立 bounds 复测；另有部分页面独立截图/TalkBack 实播/Splash 抓帧受设备占用影响（§5.6）。均可由用户真机验收时目视/操作确认，不阻塞发布。
- 判定：**GO**。建议 Lead 将本报告与 §5.5/§5.6 证据并入 G3，并在最终答复中提示用户「第十二轮前端重构待真机验收」及本次验证环境异常（§11）。
