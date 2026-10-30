# 健身计划 FitPlan

本地、离线使用的中文 Android 健身周计划与记录应用。核心是**安排整个训练计划**：动作库 → 训练组合 → 周计划（周一起始）→ 训练详情页/主页清单式逐组打卡 → 可选训练记录 → PR 纪录 → Markdown 导出。无云同步、无账号、无联网。第十二轮起使用 FitPlan 自有设计系统（深浅色 / 动态取色 / 统一动效 / 品牌图标与启动画面）。

## 功能一览

- **主页（今日）**：打开即见今天——训练日直接展示今日计划的清单式打卡（逐组打卡、次数微调、当次重量、每组重量、标逃），顶部有当日进度概览；休息日显示「今天休息」卡片，可记体重、当日备注，并一键跳到「计划」页安排；隔夜回到前台自动切到新的一天。
- **计划**：以周一为一周第一天；多周横向翻页；每天可设为训练日（绑定一个训练组合）、休息日或未安排；支持复制上周（逐日镜像）、从其他日期复制、复制前一天、移除安排、从内置方案自动排程。训练日显示当天训练用时（首末打卡时刻差）。
- **组合**：把若干动作固定成一组；命名、编辑、复制、删除、排序；每个动作可设目标组数/次数/重量/时长（留空=不设定；重量可负数/0/小数）。目标值有范围校验（组数/次数/时长必须为正整数），重量保留负数辅助语义。选动作对话框支持搜索、分类筛选、批量多选，新增行可用动作默认重量预填。
- **动作库**：创建/编辑/删除动作；可分类；按需启用组数/次数/重量/时长字段；可设默认重量（仅用于添加到组合时预填，不参与同步）；入口进入 PR 记录页。
- **训练打卡**：清单式逐组打卡（行首组圈，FitPlan 设计系统卡片与 spring 微动效）；行存在即完成；次数可 −/+ 微调（原子增减）；可给该组单独记重量；可标逃/取消逃标（逃不算完成，打卡覆盖逃标）；动作全部组完成自动聚合置灰。动作卡展开区提供当次重量行、同步提醒条与动作备注。
- **训练会话通知**：主页「开始训练」后挂常驻通知（免前台服务），显示每动作组数进度与「已进行…」用时，提供 [直接打卡] / [逃了] 两个快捷动作；进程重开自动接上，隔天残留自动清理。
- **重量模型（当前口径）**：
  - **目标重量**：组合条目自己的计划重量，各组合相互独立；
  - **当次重量**：某天某动作实际使用的重量，只记当天，不写回组合；
  - **每组重量**：打卡时快照当次重量，可逐组单独编辑；波动即各组各记各的；
  - **提醒式同步**：历史当次最大超过目标/已处理重量时，动作卡出现非阻断提醒，用户确认后才写入组合目标；
  - **PR@n**：按「动作 × 次数档」取每组真实重量最大值与首达日期，只统计当前训练者、只计记了重量的打卡组。
- **记录（可选）**：按日期记录动作 + 多组数据（组数/次数/重量/时长）+ 备注；字段由动作配置决定；负数重量表示辅助重量，0 表示自重；删除有二次确认。
- **更多记录**：体重（一天一条、重复覆盖、训练日/休息日可记、不催记）、当日备注、动作备注（回显上次）、逃组、训练用时、多训练者（打卡/PR/记录按训练者隔离）、移除安排（历史记录保留）。
- **Markdown 导出**：一键导出结构化 Markdown（设置/动作库/训练组合/周计划/训练打卡/体重记录/训练记录）；范围可选全部/指定周/日期范围；重量保留原始符号；多训练者时打卡与体重表追加「用户」列、当日备注按用户标注。导出成功/失败有 Toast 反馈。
- **设置**：重量单位 kg/lb（仅显示标签，不做数值换算）、默认记录字段、训练者增删切换、Markdown 导出、清空数据（二次确认，保留设置与训练者）、关于（版本取自构建）。

## 明确不做（与需求一致）

云同步、账号登录、Health Connect、Wear OS、训练提醒、组间休息计时器、社交、成就、排行榜、AI 生成计划、完成度/图表统计、自动计算实际负重。

## 技术栈

| 项 | 选择 |
| --- | --- |
| 语言 / UI | Kotlin 2.2 + Jetpack Compose（Material 3，动态取色） |
| 设计系统 | `ui/theme/` 颜色/字阶/间距/圆角/动效 token + `ui/design/` 19 个通用组件；浅/深色、动态取色、减少动态效果降级、组环品牌标记 |
| 品牌资源 | 自适应图标 + monochrome（Android 13+）+ round icon、Android 12+ 平台 Splash（无新增依赖） |
| 架构 | 单模块 MVVM；无 DI 框架；无网络权限（Manifest 未声明 INTERNET） |
| 构建 | AGP 8.13.2 / Gradle 8.13 / JDK 17+ |
| SDK | minSdk = targetSdk = compileSdk = 36（Android 16） |
| 存储 | Room 本地数据库 `fitplan.db`（当前 **DB v11**，10 条非破坏迁移，完全离线） |
| 测试 | JUnit4 + kotlinx-coroutines-test（JVM 单元测试，迭代 11 起 **131 个用例**全绿；迭代 12 保持 131/0/0） |
| 权限 | POST_NOTIFICATIONS（训练会话通知），无其他权限 |

## 构建与运行

### 环境要求

- JDK 17 或 21（本机验证 JDK 21）
- Android SDK Platform 36
- 命令行或 Android Studio 均可

### 命令行构建

~~~bash
cd FitPlan
./gradlew :app:assembleDebug          # Debug APK
./gradlew :app:testDebugUnitTest      # JVM 单元测试
./gradlew :app:assembleRelease        # Release（未配置签名，仅用于本地调试发布流程）
~~~

产物位于 `FitPlan/app/build/outputs/apk/debug/app-debug.apk`。

> 本工作区沙箱不允许写用户目录下的 Gradle 缓存与 Android home，构建时需把 `GRADLE_USER_HOME` / `ANDROID_USER_HOME` 指到工作区克隆目录并离线执行：
> ~~~bash
> cd /Users/suyi/projects/exercise/FitPlan
> ANDROID_USER_HOME=/Users/suyi/projects/exercise/.android-home \
>   GRADLE_USER_HOME=/Users/suyi/projects/exercise/.gradle-home \
>   ./gradlew --offline :app:testDebugUnitTest :app:assembleDebug
> ~~~
> 多 agent 并发构建请使用 `.scratch/iteration11/with-gradle-lock.sh` 串行化。

### 安装到设备

~~~bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
~~~

> 注意：minSdk 为 36，只能安装在 Android 16 及以上的设备/模拟器上。

## 项目结构

~~~text
FitPlan/
├── app/src/main/java/com/fitplan/app/
│   ├── FitPlanApp.kt            # Application + 全局依赖图（Graph）
│   ├── MainActivity.kt           # 单 Activity + 前台恢复钩子
│   ├── data/
│   │   ├── db/                   # Room：实体、DAO、数据库（v11）与 10 条迁移
│   │   └── model/Schemes.kt      # 内置排程（三分化 / 练一休一）
│   ├── export/MarkdownExporter.kt# 结构化 Markdown 导出
│   ├── session/                  # 训练会话常驻通知 + 通知按钮广播接收器
│   ├── util/                     # TimeUtil、PrRecords、QuickCheckIn、TrainingDuration、InputRules
│   └── ui/
│       ├── AppNav.kt             # 底部 6 Tab + 独立路由（workout/combo/exercise/pr）+ 转场
│       ├── Common.kt             # 通用输入框/对话框/空态（设计系统兼容层）
│       ├── theme/                # 设计 token（Color/Type/Shape/Motion/Theme）
│       ├── design/               # 通用组件（Card/TopBar/BottomBar/Dialog/Progress/...）
│       ├── home/                 # 主页（今日概览与打卡）
│       ├── plan/                 # 周计划、训练详情、自动排程、训练状态与进度
│       ├── combo/                # 训练组合列表/编辑/选动作
│       ├── exercise/             # 动作库列表/编辑
│       ├── log/                  # 训练记录（按日）
│       ├── pr/                   # PR 记录页
│       └── settings/             # 设置（单位/默认字段/训练者/导出/清空/关于）
├── app/src/test/java/com/fitplan/app/   # JVM 单元测试（FakeDaos + 领域测试）
├── docs/                         # 归档（archive/reference/verify）
└── HANDOVER.md                   # 最新交接文档（以此为准）
~~~

## 数据模型概要（DB v11）

- `exercises`：动作（名称、分类、启用字段、默认重量、已处理重量）。
- `combos` / `combo_items`：训练组合及组内动作 + 各项目标（外键级联删除）。
- `day_plans`：按 epoch day 主键存每日安排（休息 / 训练 + 组合）；`skipped` 列已弃用（组级逃标见 `set_skips`）。
- `log_entries` / `log_sets`：训练记录条目与每组数据（重量 Double，可负/可小数）。
- `set_checkins`：逐组打卡（`date + comboItemId + setIndex + userId` 唯一；行存在即完成，`reps` 可 null=只记完成，`weight` 每组快照，`checkedAt` 打卡时刻）。
- `set_skips`：逃组（同唯一键，行存在即逃；打卡覆盖即删行）。
- `session_weights`：当次重量（`date + exerciseId + userId` 唯一）。
- `body_weights`：体重（`date + userId` 唯一，重复覆盖）。
- `day_notes` / `exercise_notes`：当日备注 / 动作备注（重复覆盖，动作备注外键级联）。
- `users`：本地训练者；`app_settings.activeUserId` 当前训练者；`app_settings` 还有单位与默认字段。
- `workout_session`：训练会话（date 主键，`startedAt` 首组打卡时刻可空）。

## 文档

- 交接文档：`/Users/suyi/projects/exercise/FitPlan/HANDOVER.md`（状态、验证历史、待办、归档地图）
- 领域术语：`/Users/suyi/projects/exercise/CONTEXT.md`
- 架构决策：`/Users/suyi/projects/exercise/docs/adr/`（组合条目为同步根源 / 免前台服务通知 / 每组重量快照）
- 第十一轮迭代：`/Users/suyi/projects/exercise/docs/iteration-plan.md`、`docs/iteration11-report.md`
- 第十二轮迭代（前端全面重构）：`/Users/suyi/projects/exercise/docs/iteration12-plan.md`、`docs/iteration12-report.md`、`docs/iteration12-verification.md`、`docs/iteration12-brand.md`

## 后续发布到应用商店

1. 配置正式签名（`signingConfigs`）；
2. 视需要开启 `isMinifyEnabled` 并补充混淆规则；
3. 替换应用名/包名（`applicationId = com.fitplan.app`）；图标已为 FitPlan 品牌资产（adaptive + monochrome + round），上架前可按商店规范再精修；
4. 补隐私政策页（本地应用仍需声明权限与数据使用）。
