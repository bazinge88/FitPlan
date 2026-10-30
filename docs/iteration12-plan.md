# FitPlan 第十二轮迭代计划：前端全面重构（设计系统 / 页面 / 动效 / 品牌）

- 文档版本：v1.0（2026-10-18）
- 迭代代号：iteration12
- 工作区根：`/Users/suyi/projects/exercise`
- 项目代码：`/Users/suyi/projects/exercise/FitPlan`
- 主文档：本文件（`/Users/suyi/projects/exercise/docs/iteration12-plan.md`）
- 交接基线：`/Users/suyi/projects/exercise/FitPlan/HANDOVER.md`（2026-10-18，第十一轮完成）
- 交接启动包：`/var/folders/dm/h4ym3rq90xsbrjppl92s8gxr0000gn/T/fitplan-handoff-2026-10-18-frontend-redesign.md`
- 术语表：`/Users/suyi/projects/exercise/CONTEXT.md`
- 架构决策：`/Users/suyi/projects/exercise/docs/adr/0001-combo-item-as-sync-root.md`、`0002-session-notification-without-foreground-service.md`、`0003-per-set-weight-snapshot.md`、`0004-cross-dao-transaction-boundary.md`
- 上一轮计划/报告：`/Users/suyi/projects/exercise/docs/iteration-plan.md`、`/Users/suyi/projects/exercise/docs/iteration11-report.md`
- 状态：**计划完成，G0 进行中**（开发中逐阶段更新）

---

## 1. 目标、非目标与硬约束

### 1.1 一句话目标

在不改变任何业务语义、数据层与离线约束的前提下，把 FitPlan 的前端从"默认 M3 模板感"重构为 **Apple 式排版留白 + Material 3 Expressive 动效与色彩 + FitPlan 自有品牌识别** 的旗舰级移动端体验：设计 token 集中、组件复用、页面全量重做、动效统一可降级、图标与启动体验精致。

### 1.2 目标（可验收）

1. 落地一套集中式设计系统：颜色、字阶、间距、圆角、层级/阴影、表面、图标风格、动效曲线与时长全部 token 化；屏幕代码不再硬编码颜色/圆角/字号。
2. 抽出可复用组件：Card / Section / ListRow / TopBar / LargeTitle / BottomBar / EmptyState / Dialog / BottomSheet / Snackbar / Button / Chip / Progress(Ring+Linear) / AnimatedCount / Haptics / Staggered entrance。
3. 全量重构页面：主页、计划、训练详情/清单打卡、组合列表/编辑、动作库/编辑、训练记录、PR、设置、底部导航与全部对话框/弹层。
4. 动效语言统一：页面推入/返回、Tab 切换、列表项入场、打卡成功微动效、进度与数字变化、弹层 spring；提供"减少动态效果"降级。
5. 品牌资产：自适应桌面图标（含 monochrome/themed icon、round icon）、Android 12+ SplashScreen 与启动体验、应用内品牌标记。
6. 验收：131 个 JVM 测试全绿、clean 构建零新增警告、真机安装与冷启动正常、浅色/深色/大字体/TalkBack/对比度/60fps 抽查通过、业务行为零回归；更新 HANDOVER/README 并产出 `docs/iteration12-report.md` 与 APK/截图。

### 1.3 非目标（本轮明确不做）

- 不改 DB：保持 v11，不新增表/列/迁移，不改 Entities/DAO/AppDatabase。
- 不改业务语义：重量模型（ADR-0001/0003）、逃组、会话通知（ADR-0002）、多训练者、备注双轨、体重、用时、PR、导出、移除安排的行为与文案口径。
- 不改导航信息架构：底部仍是 6 Tab（主页/计划/组合/动作/记录/设置），路由与参数不变。
- 不新增第三方依赖（离线缓存无 core-splashscreen 等新库）；不新增网络权限；不引入联网运行时资源。
- 不新增 Compose UI/仪器测试基础设施；关键 UI 用真机截图 + uiautomator dump 验收。
- 不做平板/横屏双栏新布局（只保证窄屏/横屏不崩、不截断；双栏列为后续候选）。
- 不做设置项中的"主题/动效开关"（跟随系统深色/动态取色与系统动画缩放；避免新增设置语义）。
- 不重构 ViewModel/数据流（除为 UI 需要而新增的纯展示型顶层函数，且必须补 JVM 测试）。

### 1.4 硬约束与不可回退项

1. 业务语义零变化；4 篇 ADR 的交互语义不得回退。视觉重构后必须逐项抽查打卡/逃组/重量/备注/体重/用时/PR/导出/移除安排。
2. DB 保持 v11；不改数据层。若确需改动必须停下走 ADR 并记录（本轮默认不触发）。
3. 离线：无 INTERNET、无账号；不新增网络权限；不引入需下载的字体/图标/图片。
4. 131 个 JVM 测试保持全绿；行为变化必须有测试；新增纯逻辑补 JVM 测试。
5. 真机 PHB110（adb id `5968de20`）在线：关键改动安装验证并截图（浅色+深色）。
6. 无 git：回滚基线 `/Users/suyi/projects/exercise/.scratch/iteration12/backup/src`（2026-10-18 快照，888K/101 文件），禁止删除备份。
7. 构建必须用工作区环境与锁脚本（见 §9.1），禁止直接 `./gradlew`。

---

## 2. 设计方向（brainstorming 结论）

> 用户已给出目标、审美方向与硬约束，本轮视为设计 brief 已批准；不再逐项询问，所有取舍记录在 §16。

### 2.1 品牌关键词

**克制（Calm）、节奏（Rhythm）、扎实（Grounded）、微光（Subtle Vitality）**。

- **克制**：少即是多。页面只保留一条主叙事；不堆叠卡片、不滥用阴影与渐变。
- **节奏**：用 4dp 基准的间距节奏、字阶对比与"组圈"符号建立视觉韵律；列表分组有呼吸。
- **扎实**：数据可信赖。数字排版清晰（tabular numerals），打卡与进度反馈明确，不花哨。
- **微光**：只在关键处给一点生命感（品牌绿、完成态的对勾与微动效、PR 暖琥珀色），其余安静。

### 2.2 设计原则（用于裁决每个页面的取舍）

1. **内容优先**：大标题、日期、训练名、进度数字是主角；装饰元素不超过一个（如组环）。
2. **一次一个主操作**：每屏一个主 CTA（开始/结束训练、保存、添加），次级操作降为文字按钮或图标。
3. **表面分层而非线条堆叠**：用背景/卡片/容器三层色调 + 少量 hairline 表达层级，不用重阴影。
4. **动效有因**：只对"状态变化/层级变化/空间关系"做动画；装饰性动画一律可降级。
5. **中文排版优先**：行高 ≥1.4，标题不用负字距；数字用等宽数字特性。
6. **可读与可触**：正文最小 13sp，触摸目标 ≥48dp，语义完整（TalkBack）。

### 2.3 差异化识别（不照搬 Apple/Google）

- **组环符号（Set Ring）**：一个圆环 + 对勾构成品牌标记，同时复用为打卡圈、聚合圈、进度环与桌面图标；形成"FitPlan = 一组一组完成"的识别。
- **深绿 + 米白纸感**：以深常青绿为品牌主色、暖米白为纸面，区别于 M3 默认紫/蓝模板感；深色模式用近黑墨绿而非纯黑。
- **暖琥珀 PR 色**：仅用于 PR/纪录与提醒，与主绿形成低饱和互补。
- **"大标题 + 分组卡"版式**：Apple 式的标题留白 + Google 式的分组表面与 expressive 圆角，但卡片圆角统一 24/28，不做 iOS 玻璃拟态。

### 2.4 视觉参考与对照

- 参考：Apple Fitness / Health 的排版留白、Material 3 Expressive 的 shape/motion/color、Linear/Things 的列表节奏。
- 不采用：玻璃拟态、大面积渐变、拟物纹理、复杂插画、任何需联网下载的字体或素材。
- 图标风格：Material Rounded/Outlined 线性图标（已有 material-icons-extended 1.7.8）+ 自绘品牌几何标记。

---

## 3. 设计 token 契约

> 所有 token 定义在 `ui/theme/` 与 `ui/design/`，屏幕代码只能引用 token / `MaterialTheme`，禁止硬编码。以下值为最终口径，实现时不得随意改动；如需调整必须在 §16 记录。

### 3.1 颜色（品牌 fallback；动态取色开启时以系统色板为主）

**浅色（Light）**

| token | hex | 用途 |
| --- | --- | --- |
| background | #F6F7F2 | 页面底色（暖米白） |
| surface | #FFFFFF | 卡片 |
| surfaceContainerLowest | #FFFFFF | 最高层表面 |
| surfaceContainerLow | #F0F3EB | 页面内浅层/输入背景 |
| surfaceContainer | #E9EEE3 | 分组/次级表面 |
| surfaceContainerHigh | #E2E8DB | 强调表面 |
| surfaceContainerHighest | #DBE2D3 | 进度 track / 选中背景 |
| onSurface | #191C18 | 主文本（对 bg 15.98:1） |
| onSurfaceVariant | #5C6357 | 次文本（对 bg 5.78:1，对 surface 6.22:1） |
| outline | #8B9286 | 图标/边框 |
| outlineVariant | #D6DCCF | hairline |
| primary | #2E6B4F | 主色（对白 6.30:1） |
| onPrimary | #FFFFFF | 主色上的文字 |
| primaryContainer | #CFEBD9 | 主色容器/选中态 |
| onPrimaryContainer | #0B3320 | 主色容器文字（10.96:1） |
| secondary | #5C635A | 次色 |
| secondaryContainer | #E0E7DC | 次色容器 |
| tertiary | #8A5A2B | PR/纪录暖琥珀（对白 5.87:1） |
| tertiaryContainer | #F5E2C9 | PR 容器 |
| error | #B3261E | 危险（对白 6.54:1） |

**深色（Dark）**

| token | hex | 用途 |
| --- | --- | --- |
| background | #0F1210 | 页面底色（墨绿近黑） |
| surface | #171C17 | 卡片 |
| surfaceContainerLow | #1B211A | 浅层 |
| surfaceContainer | #20271F | 分组 |
| surfaceContainerHigh | #262E24 | 强调 |
| surfaceContainerHighest | #2C3529 | track/选中 |
| onSurface | #E2E7DD | 主文本（14.99:1） |
| onSurfaceVariant | #A9B2A4 | 次文本（8.61:1） |
| outline | #6F7A6C | 图标/边框 |
| outlineVariant | #3A4438 | hairline |
| primary | #8ED8AE | 主色（对 bg 11.29:1） |
| onPrimary | #05301C | 主色上的文字（8.70:1） |
| primaryContainer | #1F4D35 | 容器 |
| onPrimaryContainer | #C9EFDA | 容器文字（7.76:1） |
| secondary | #B7C2B2 | 次色 |
| secondaryContainer | #3A4438 | 次色容器 |
| tertiary | #F0BE8A | PR 暖琥珀（11.16:1） |
| tertiaryContainer | #5A3E21 | PR 容器 |
| error | #FFB4AB | 危险（11.10:1） |

**扩展色（FitExtraColors，品牌专用，不参与动态取色）**

| token | light | dark | 用途 |
| --- | --- | --- | --- |
| success | #2E6B4F | #8ED8AE | 完成态/全部完成 |
| successContainer | #CFEBD9 | #1F4D35 | 完成态容器 |
| warning | #8A5A2B | #F0BE8A | 同步提醒/注意 |
| track | #DBE2D3 | #2C3529 | 进度条轨道 |
| heroTop | #EAF4ED | #1B2A20 | 首页 hero 渐变起点（极淡） |
| heroBottom | #F6F7F2 | #0F1210 | 首页 hero 渐变终点（与背景融合） |
| surfaceSunken | #EEF1E8 | #121712 | 输入框/内嵌区 |
| scrim | #0B120D（alpha 0.32） | #000000（alpha 0.6） | 弹层遮罩 |

**动态取色策略**：Android 12+ 继续使用 `dynamicLightColorScheme/dynamicDarkColorScheme`；品牌识别由排版、形状、动效、品牌标记承载，动态色只替换 M3 色彩角色。FitExtraColors 始终使用品牌固定值（PR 暖色、完成态、track），保证识别稳定。浅/深色模式跟随系统；不做应用内主题开关（非目标）。

### 3.2 字阶（CJK 优先，system FontFamily.Default；数字启用 tabular numerals）

| token | size/lineHeight | weight | 用途 |
| --- | --- | --- | --- |
| displayLarge | 57/64 | Medium | 极少数英雄数字 |
| displayMedium | 45/52 | Medium | 首页 hero 数字（如完成组数） |
| displaySmall | 36/44 | Medium | 大号统计 |
| headlineLarge | 32/40 | Medium | 页面大标题（详情页） |
| headlineMedium | 28/36 | Medium | 大标题（Tab 页） |
| headlineSmall | 24/32 | Medium | 区块标题 |
| titleLarge | 22/28 | Medium | 卡片主标题 |
| titleMedium | 17/24 | Medium | 行/动作名 |
| titleSmall | 15/22 | Medium | 小标题 |
| bodyLarge | 16/24 | Normal | 正文 |
| bodyMedium | 14/21 | Normal | 默认正文 |
| bodySmall | 13/19 | Normal | 辅助说明 |
| labelLarge | 14/20 | Medium | 按钮/标签 |
| labelMedium | 12/16 | Medium | 徽标/Tab 标签 |
| labelSmall | 11/16 | Medium | 最小标签（Tab 文字不低于 11sp） |

- 中文标题不加负字距；`labelLarge/Medium` 的 Latin 可用 0.2-0.6sp 字距。
- 数字样式 `FitType.numeric`：`fontFeatureSettings = "tnum"`，用于进度、重量、PR、用时。
- 字重只用 Normal(400)/Medium(500)；大标题强调可用 Medium，hero 数字可用 SemiBold(600)，避免中文伪粗体过重。

### 3.3 间距（4dp 基准）

| token | dp | 用途 |
| --- | --- | --- |
| xxs | 2 | 微调 |
| xs | 4 | 图标与文字 |
| sm | 8 | 紧凑行内 |
| md | 12 | 行内分组 |
| lg | 16 | 卡片内次级间距 |
| xl | 20 | 页面 gutter / 卡片内边距 |
| xxl | 24 | 区块间距 |
| xxxl | 32 | 大区块间距 |
| huge | 40 | 页面顶部/底部留白 |

- 页面水平 gutter 统一 **20dp**；卡片内边距 **20dp**；列表项卡片间距 **12dp**；区块标题上间距 **28dp**、下间距 **12dp**。
- 触摸目标最小 **48dp**；列表行最小高度 **56dp**；组行 **56dp**；底部导航高度 **72dp + 系统 inset**。

### 3.4 圆角

| token | dp | 用途 |
| --- | --- | --- |
| shape.extraSmall | 8 | 小控件 |
| shape.small | 12 | 输入框内嵌/chip |
| shape.medium | 16 | 输入框/按钮容器 |
| shape.large | 24 | 卡片 |
| shape.extraLarge | 28 | hero / dialog / bottom sheet 顶部 |
| shape.pill | 50% | 主按钮/状态胶囊 |
| shape.hero | 28 | 首页 hero 卡 |
| shape.sheet | 28 top | 底部弹层 |

M3 `Shapes` 同步：extraSmall=8, small=12, medium=16, large=24, extraLarge=28。

### 3.5 层级/阴影/表面

- **L0 页面**：`background`。
- **L1 卡片**：`surface` + 1dp `outlineVariant.copy(alpha=0.6)` hairline；不用阴影（浅色）。
- **L2 悬浮**：FAB / Snackbar / BottomBar 用 `surfaceContainerHigh` + 轻阴影（elevation 3dp，ambient/spot 极淡）。
- **L3 弹层**：Dialog/BottomSheet 用 `surface` + 8dp 阴影 + 28dp 顶部圆角。
- 深色模式以 `surfaceContainer*` 色阶表达层级，不使用高亮描边；hairline 透明度 0.5。
- 禁止：大面积模糊、玻璃拟态、彩色阴影、多层嵌套卡片（最多两层：页面 → 卡片）。

### 3.6 动效 token

| token | 值 | 用途 |
| --- | --- | --- |
| duration.short | 120ms | 颜色/透明度微变 |
| duration.medium | 220ms | 页面转场、展开 |
| duration.long | 320ms | BottomSheet/复杂布局 |
| duration.extraLong | 480ms | hero/进度环首次呈现 |
| easing.standard | CubicBezier(0.2, 0.0, 0.0, 1.0) | 通用 |
| easing.emphasizedDecel | CubicBezier(0.05, 0.7, 0.1, 1.0) | 进入/展开 |
| easing.emphasizedAccel | CubicBezier(0.3, 0.0, 0.8, 0.15) | 退出/收起 |
| spring.pop | damping 0.55 / stiffness 700 | 打卡圆圈、对勾 |
| spring.gentle | damping 0.85 / stiffness 320 | 进度环/进度条/数字 |
| spring.sheet | damping 0.8 / stiffness 420 | 弹层进入 |
| stagger.step | 35ms，最多 8 项 | 列表项入场 |
| translate.enter | 12dp vertical | 列表项/页面内容进入 |
| scale.enter | 0.98 → 1.0 | 页面/弹层 |

- **减少动态效果**：读取 `ValueAnimator.areAnimatorsEnabled()`（系统动画缩放为 0 时 false），并在 `ON_RESUME` 刷新；关闭时：时长归零、列表 stagger 取消、页面转场退化为 120ms 淡入淡出、打卡仅颜色瞬变、进度不做 spring 只跳变。CompositionLocal `LocalFitMotion.enabled` 贯穿所有组件。
- 动画只改 `alpha/translation/scale/color`，不改 `size` 触发重排（展开用 `AnimatedVisibility(expandVertically)` 限制在卡片内）。

---

## 4. 组件清单与接口契约

> 新增包：`com.fitplan.app.ui.theme`（token）与 `com.fitplan.app.ui.design`（组件）。下表签名为**最终契约**，屏幕 agent 按此调用；设计系统实现时不得改名/改参（如需扩展只能加带默认值的参数）。

### 4.1 Theme（`ui/theme/`）

```kotlin
// Color.kt
@Immutable data class FitExtraColors(
    val success: Color, val onSuccess: Color, val successContainer: Color, val onSuccessContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
    val track: Color, val heroTop: Color, val heroBottom: Color, val surfaceSunken: Color, val scrim: Color,
)
val LocalFitColors = staticCompositionLocalOf { lightFitExtraColors() }
val MaterialTheme.fitColors: FitExtraColors @Composable @ReadOnlyComposable get() = LocalFitColors.current

// Theme.kt
@Composable fun FitPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
)
// 内部：MaterialTheme(colorScheme, typography = FitTypography(), shapes = FitShapes.material, content)
// 同时提供 LocalFitColors、LocalFitMotion；处理系统栏图标明暗与边到边 inset。

// Type.kt
fun FitTypography(): Typography
object FitType { val numeric: TextStyle; val eyebrow: TextStyle; val heroNumber: TextStyle }

// Shape.kt
object FitShapes { val card: Shape; val hero: Shape; val dialog: Shape; val sheet: Shape; val chip: Shape; val pill: Shape; val field: Shape; val small: Shape; val material: Shapes }
object FitSpace { /* xxs..huge */ }
object FitSize { val gutter, rowMin, touchMin, icon, setCircle, fab, bottomBar }

// Motion.kt
@Immutable data class FitMotion(
    val enabled: Boolean,
    val short: Int, val medium: Int, val long: Int, val extraLong: Int,
    val standard: Easing, val emphasizedDecel: Easing, val emphasizedAccel: Easing,
    val pop: SpringSpec<Float>, val gentle: SpringSpec<Float>, val sheet: SpringSpec<Float>,
)
val LocalFitMotion = staticCompositionLocalOf { FitMotion(enabled = true) }
val MaterialTheme.fitMotion: FitMotion @Composable @ReadOnlyComposable get() = LocalFitMotion.current
```

### 4.2 通用组件（`ui/design/`）

```kotlin
// FitCard.kt
@Composable fun FitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(FitSpace.xl),
    shape: Shape = FitShapes.card,
    content: @Composable ColumnScope.() -> Unit,
)
@Composable fun FitSectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null)
@Composable fun FitHairline(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
@Composable fun FitScreenTitle(title: String, modifier: Modifier = Modifier, eyebrow: String? = null, subtitle: String? = null)

// FitScaffold.kt
@Composable fun FitScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = { FitSnackbarHost() },
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
)
@Composable fun FitTopBar(title: String, modifier: Modifier = Modifier, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {})
@Composable fun FitSnackbarHost(modifier: Modifier = Modifier)
// Snackbar 通道：屏幕通过 rememberFitSnackbarState() 获取 state 并在协程里 showMessage；FitScaffold 默认宿主。
// 设计系统需导出 rememberFitSnackbarState(): SnackbarHostState（内部用 LocalSnackbarHostState 共享）。

// FitBottomBar.kt
data class FitTabSpec(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)
@Composable fun FitBottomBar(tabs: List<FitTabSpec>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)

// FitButtons.kt
enum class FitButtonStyle { Primary, Tonal, Ghost, Destructive }
@Composable fun FitButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, style: FitButtonStyle = FitButtonStyle.Primary, leadingIcon: ImageVector? = null)
@Composable fun FitIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current)
@Composable fun FitTextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, destructive: Boolean = false)

// FitDialog.kt
@Composable fun FitDialog(
    title: String, onDismiss: () -> Unit,
    confirmLabel: String = "确定", onConfirm: (() -> Unit)? = null,
    dismissLabel: String = "取消", destructive: Boolean = false,
    text: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
)
@Composable fun FitBottomSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)

// FitEmptyState.kt
@Composable fun FitEmptyState(
    title: String, modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = Icons.Rounded.FitnessCenter,
    action: (@Composable () -> Unit)? = null,
)

// FitProgress.kt
@Composable fun FitRingProgress(progress: Float, modifier: Modifier = Modifier, size: Dp = 96.dp, strokeWidth: Dp = 10.dp, trackColor: Color = MaterialTheme.fitColors.track, color: Color = MaterialTheme.colorScheme.primary, content: @Composable BoxScope.() -> Unit = {})
@Composable fun FitLinearProgress(progress: Float, modifier: Modifier = Modifier, height: Dp = 6.dp, color: Color = MaterialTheme.colorScheme.primary, trackColor: Color = MaterialTheme.fitColors.track)
@Composable fun AnimatedCount(value: Int, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.titleLarge, color: Color = Color.Unspecified, suffix: String = "")

// FitList.kt
@Composable fun FitListRow(title: String, modifier: Modifier = Modifier, supporting: String? = null, leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null)
@Composable fun FitSectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) // 带行间 hairline 的分组卡

// FitChips.kt
enum class FitChipTone { Neutral, Primary, Tertiary, Success }
@Composable fun FitMetaChip(text: String, modifier: Modifier = Modifier, tone: FitChipTone = FitChipTone.Neutral, leadingIcon: ImageVector? = null)
@Composable fun FitStatusPill(text: String, modifier: Modifier = Modifier, tone: FitChipTone = FitChipTone.Neutral)

// FitEnter.kt
@Composable fun Modifier.fitEnter(index: Int = 0, maxStagger: Int = 8): Modifier

// Haptics.kt
@Composable fun rememberFitHaptics(): FitHaptics
class FitHaptics { fun tap(); fun toggleOn(); fun toggleOff(); fun success() }

// Brand.kt
@Composable fun FitBrandMark(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current)
```

### 4.3 旧通用组件兼容层（`ui/Common.kt` 保留签名）

```kotlin
fun parseDoubleOrNull(text: String): Double?
fun parseIntOrNull(text: String): Int?
@Composable fun NumberField(value, onValueChange, label, modifier, enabled, allowDecimalNegative, isError, supportingText) // 外观 token 化，语义不变
@Composable fun <T> DropdownSelector(options, label, selected, placeholder, onSelect, modifier, enabled)
@Composable fun ConfirmDialog(title, text, confirmLabel, onConfirm, onDismiss) // 委托 FitDialog
@Composable fun EmptyHint(text: String, modifier: Modifier = Modifier) // 委托 FitEmptyState
fun setsSummary(sets: Int?, reps: Int?, weight: Double?, durationMin: Int?): String // 逻辑不变
```

### 4.4 导航壳（`ui/AppNav.kt`）

- `AppNav()`、`MainScaffold(onEditCombo, onEditExercise, onOpenPr, onOpenWorkout)` 签名保持不变。
- 底部导航改为 `FitBottomBar`：选中项图标切换 filled + 胶囊指示器（动画），未选中 outlined；保留 6 Tab 文案。
- Tab 内容切换用 `AnimatedContent`（淡入 + 0.98→1.0 scale，220ms；减少动态时纯淡入）。
- NavHost 转场：详情路由（workout/combo/exercise/pr）用 `slideInHorizontally(1/5) + fadeIn` / 返回反向；主路由保持淡入。不自定义返回手势逻辑。
- 状态栏/导航栏图标明暗随主题；边到边布局，底部导航加系统 navigationBars inset。

---

## 5. 页面改造清单（逐页）

> 每页验收都包含：浅色/深色截图、大字体 1.3 抽查、TalkBack 关键控件、无业务行为回归。界面文案除视觉性新增（如空态标题、辅助说明）外必须沿用原文案与术语。

### 5.1 主页 `ui/home/HomeScreen.kt`
- 顶部：eyebrow「10月18日 · 周六」+ 大标题「今天」+ 副标题今日组合名（或"今天没有安排训练"）；「开始训练/结束训练」从 TextButton 升级为主/次按钮（结束训练用 Tonal + 状态点）。
- Hero：训练日显示进度 hero 卡（FitRingProgress + 「已完成 X/Y 组」+ 组合名 + 今日完成状态）；完整保留 `workoutProgress` 口径与「今日完成」逻辑。
- 今日记录：体重行、当日备注行改为紧凑 `FitListRow` 样式（放在一张「今日记录」卡或 hero 卡内），保留"记录/修改"、空文本清除、lb 标签不换算。
- 清单：`WorkoutChecklist` 卡片化重构（见 5.3）。
- 休息日：品牌标记 + 「今天休息」+ 说明 + 今日记录 + 「去「计划」页安排」主按钮；不再使用整块白色空卡。
- 空组合：FitEmptyState 卡片 + 引导；保留隐藏开始训练。
- 列表项入场 `Modifier.fitEnter(index)`；进度数字用 `AnimatedCount`。
- 保留：ON_RESUME 跨天刷新、通知权限流程与 Toast 文案（可改 Snackbar 呈现但文案不变）、weight(1f) 高度约束（禁止外层 verticalScroll）。

### 5.2 计划页 `ui/plan/PlanScreen.kt` + `PlanDialogs.kt`
- 顶部：大标题「计划」+ 周区间副标题；右上「复制上周」「自动排程」改为图标按钮（保留可见文字或图标+文字紧凑按钮，确保可发现；TalkBack contentDescription）。
- 周切换：上一周/本周/下一周改为图标按钮 + 居中「本周」胶囊；保留 HorizontalPager 滑动。
- 7 天列表：每天一张紧凑卡：左侧星期/日期（不再固定 96dp 硬宽，改 weight 比例 + 大字体自适应），中间组合名/休息/未安排 + 用时，右侧状态胶囊 + 编辑图标；今天用 primaryContainer 底色/描边 + 「今天」标签；点击行为不变（训练日进训练页，其余开编辑面板）。
- 单日编辑 ModalBottomSheet：拖拽把手、日期大标题、休息/训练分段选择（M3 SegmentedButton 语义自绘或 toggleable 行）、组合下拉、保存/复制/移除三按钮重排（主次分明），保留「复制前一天」快捷。
- 自动排程 Dialog：方案单选行卡、槽位下拉、循环勾选；保留 LaunchedEffect 默认补齐与错误提示；生成按钮主样式。
- 复制上周确认与结果对话框改用 FitDialog，文案不变。

### 5.3 训练详情与清单 `ui/plan/WorkoutScreen.kt`
- 详情页：FitScaffold + FitTopBar（返回 + 标题「训练详情」）；内容大标题为组合名 + 日期 eyebrow；顶部进度 hero（环形进度 + 完成组数/总组数 + 用时若有）。
- 动作卡：FitCard；头部聚合圈（保留 toggleable 语义）→ 改为品牌组环风格；动作名/目标摘要/进度；展开箭头旋转动画；展开区用 `AnimatedVisibility`。
- 组行：56dp 高、打卡圈 40dp（保留 `toggleable + Role.Checkbox + contentDescription/stateDescription`）；次数 stepper 改为紧凑 pill（− 数字 +），保留 48dp 触达；「标逃/取消逃标」chip；每组重量 chip。
- 打卡微动效：圆圈 spring pop + 对勾 scale/fade + 颜色过渡 + haptic；全部完成时聚合圈 pop，进度条 spring。
- 同步提醒条：warning 容器 + 说明 + 「忽略/同步」按钮 + 多组合勾选，文案与逻辑不变。
- 重量行：− / 数值（当次/目标 小标注）/ +，数值点击弹 FitDialog；保留负数/小数/清空取消语义。
- 动作备注：输入 + 保存按钮 + 「上次：X」回显；保存按钮启用条件不变。
- 空态/加载态/无计划：FitEmptyState + 返回按钮，保留原文案。
- 主页与详情共用 `WorkoutChecklist` 的对外签名（state/actions/modifier）不变；`WorkoutActions` 与 `workoutActions(vm)` 不变。

### 5.4 组合列表 `ui/combo/ComboListScreen.kt`
- 大标题「训练组合」+ 数量副标题；FAB 改 ExtendedFAB「新建组合」或大号 FAB（保留 contentDescription）。
- 组合卡：名称 titleLarge、动作摘要 chips/两行摘要、右侧「更多」菜单（编辑/复制/删除）替代三个图标按钮；空组合摘要、超过 3 行「等 N 个动作」逻辑不变。
- 删除确认 FitDialog 文案不变；空态 FitEmptyState。
- 列表入场 stagger。

### 5.5 组合编辑 `ui/combo/ComboEditScreen.kt` + `ExercisePickerDialog.kt`
- FitTopBar（返回 + 「编辑组合/新建组合」+ 保存图标按钮，保存禁用条件不变）。
- 名称输入 token 化；动作行卡：动作选择行、目标字段（组数/次数/重量/时长）在 FlowRow/两列网格中排布，错误文案保留；上移/下移/移除改为紧凑图标组或更多菜单（行为不变）。
- 添加动作按钮改 Tonal 全宽按钮；选动作对话框：搜索框、分类 chips、列表行、已添加标记、多选计数按钮，行为不变（跨分类勾选不丢）。
- 页面底部留白 + 键盘 inset 处理。

### 5.6 动作库 `ui/exercise/ExerciseListScreen.kt`
- 大标题「动作库」+ 副标题；TopBar 右侧「PR 记录」入口改为带图标文字按钮。
- 分类分组：分类标题 + 分组卡；每行动作名 + 字段徽标（FlowRow，保留可换行不溢出）；删除入口放更多菜单或保留显式删除按钮（图标）并确认。
- FAB「新建动作」；空态 FitEmptyState。

### 5.7 动作编辑 `ui/exercise/ExerciseEditScreen.kt`
- FitTopBar + 保存；表单分组卡（名称/分类、启用字段 Switch 行、默认重量），说明文案保留（默认重量语义）；校验错误 token 化。

### 5.8 训练记录 `ui/log/LogScreen.kt` + `EntryDialog.kt`
- 大标题「训练记录」+ 日期切换（左右箭头 + 日期 + 今天按钮），保留日期状态逻辑。
- 记录卡片：条目名、组摘要「次数 × 重量」、备注；编辑/删除入口；删除确认保留。
- 新增/编辑对话框：选择条目、组数、次数、重量行，键盘类型与校验不变；FitDialog/自定义内容。

### 5.9 PR `ui/pr/PrScreen.kt`
- FitScaffold + 返回；大标题「PR 记录」+ 当前训练者 chip；按动作分组卡：动作名 + 档位行「PR@8：52.5 kg（首达 9/12）」；空态/加载态文案不变；重量单位保留空格。

### 5.10 设置 `ui/settings/SettingsScreen.kt`
- 大标题「设置」；分组列表（Apple 设置式）：训练者（当前训练者、切换、添加、删除）、单位与默认字段、数据（导出/清空）、关于（版本 BuildConfig）。
- 行样式 FitListRow + Switch/单选；危险操作红色；导出/清空流程与 Toast（可 Snackbar 呈现）文案不变；训练者增删逻辑不变。

### 5.11 导航/对话框/弹层
- 底部导航（§4.4）；所有 AlertDialog/ModalBottomSheet/DatePickerDialog 统一 FitDialog/FitBottomSheet 外观；保留原有标题、正文、按钮文案与顺序。
- Snackbar 宿主放在 FitScaffold（各 Tab 与详情页），用于导出/清空/保存反馈；保留原文案。

---

## 6. 动效清单

| 场景 | 实现 | 曲线/时长 | 减少动态降级 |
| --- | --- | --- | --- |
| 详情页推入/返回 | NavHost enter/exit transitions（slide 1/5 + fade） | emphasizedDecel 220ms / accel 160ms | 纯 fade 120ms |
| Tab 切换 | AnimatedContent fade-through + scale 0.98→1 | standard 220ms | 纯 fade 100ms |
| 列表项入场 | `Modifier.fitEnter(index)` alpha+translateY 12dp，stagger 35ms（≤8） | emphasizedDecel 220ms | 直接显示 |
| 卡片展开/收起 | AnimatedVisibility expandVertically + fade | spring gentle | 立即展开 |
| 打卡圆圈 | Animatable scale 1→1.15→1 + color + 对勾 scale/fade + haptic | spring.pop | 只颜色瞬变 |
| 聚合圈/全部完成 | scale pop + 颜色过渡 + 进度相关动画 | spring.pop / gentle | 颜色瞬变 |
| 进度环/进度条 | animateFloatAsState spring gentle | spring.gentle | 直接跳变 |
| 进度数字变化 | AnimatedCount vertical slide + fade | standard 220ms | 直接更新 |
| 弹层进入 | Dialog scale 0.96→1 + fade；BottomSheet 默认 spring | spring.sheet / 220ms | 淡入 |
| 删除/状态切换 | AnimatedVisibility + fade/expand | standard 180ms | 立即 |
| BottomBar 选中 | 胶囊指示器动画 + 图标 crossfade + haptic | spring.gentle | 立即切换 |
| 完成态页面（全部打卡） | 标题/进度颜色过渡 + 一次轻微 scale | gentle | 颜色瞬变 |

约束：动画不得引起布局抖动；列表项动画只在首次组合时触发，滚动复用时不得重复触发；不做无限循环装饰动画（省电、避免干扰）。

---

## 7. 品牌图标、Splash 与启动体验

### 7.1 品牌标记（组环 + 对勾）

- 语义：圆环 = 一组（set），对勾 = 完成；整体像"进度环"，与清单打卡的视觉符号同源。
- 造型：108×108 viewport 安全区内居中；圆环外径约 46dp、环宽约 7dp；对勾为圆角折线；右下可加一个小圆点（下一组）但保持极简——最终以 design-system 实现稿为准，保持单色可识别、缩小到 24dp 仍清晰。
- 应用内：`drawable/ic_fitplan_mark.xml`（白色/可 tint），用于空态、Splash、关于页。

### 7.2 自适应图标

- `res/mipmap-anydpi-v26/ic_launcher.xml` 与 `ic_launcher_round.xml`：`<adaptive-icon>` background=品牌深绿（#143D2A，可加极淡同色渐变/纹理），foreground=`@drawable/ic_launcher_foreground`（米白 #F3F6EC 组环+对勾），monochrome=`@drawable/ic_launcher_monochrome`（Android 13+ themed icon，单色路径）。
- Manifest 增加 `android:roundIcon="@mipmap/ic_launcher_round"`；保留 `android:icon="@mipmap/ic_launcher"`。
- 验收：真机桌面图标截图（普通壁纸 + 主题图标模式若可切换）、深色壁纸下清晰；`aapt2 dump badging` 或 `adb shell cmd package resolve-activity` 检查图标资源存在。

### 7.3 SplashScreen 与启动体验（不新增依赖）

- 使用平台 API 36 内建 SplashScreen 主题属性（`android:windowSplashScreenBackground`、`windowSplashScreenAnimatedIcon`、`windowSplashScreenIconBackgroundColor`、`windowSplashScreenAnimationDuration`），不引入 core-splashscreen。
- `values/themes.xml` 的 `Theme.FitPlan` 增加 splash 属性；`android:windowBackground` 设为品牌底色避免白闪；深色模式可同用品牌绿（保持识别）或 `values-night` 覆盖为墨绿。
- 启动后首帧：背景色与 splash 同色系（FitPlanTheme background），避免跳变。
- 验收：冷启动 `am start -W` 输出 + 启动瞬间截图/录屏抽帧；无白屏/黑屏闪烁。

### 7.4 触觉反馈

- 打卡/取消打卡：轻触 confirm 反馈；标逃/取消：toggle 反馈；全部完成：一次 success 轻震；BottomBar 切换：轻触。使用 Compose `LocalHapticFeedback`（`HapticFeedbackType.Confirm/ToggleOn/ToggleOff/LongPress` 中可用者）；不新增 VIBRATE 权限（Compose haptic 走 View feedback，无需权限）。
- 不提供设置开关（跟随系统触感设置）。

---

## 8. 无障碍、响应式与性能

### 8.1 无障碍（验收项）

- 保留并扩展：组圈 `toggleable + Role.Checkbox + stateDescription`；聚合圈 contentDescription；重量/标逃 chip ≥48dp；所有图标按钮有 contentDescription；纯装饰图标 `contentDescription = null`。
- 大字体：所有布局用 `wrapContent`/weight/FlowRow，不写死行高与宽度；计划页去掉 96dp 固定日期列；BottomBar 标签允许 2 行内不截断（labelSmall 11sp）。
- 对比度：正文/次要文本/按钮文字 ≥4.5:1（§3.1 已计算）；大字号 ≥3:1。
- TalkBack 路径：主页（进度、开始训练、体重、备注、清单打卡）→ 计划（周切换、日卡）→ 训练（展开、组打卡、次数、标逃、重量）→ 设置（导出/清空/训练者）。用 uiautomator dump 检查 content-desc 与 clickable bounds。

### 8.2 响应式

- 窄屏 320dp：页面 gutter 20dp；标题不截断；按钮全宽；计划日卡两行布局。
- 大字体 1.3/1.5：关键页面截图；文本可换行；不出现横向溢出。
- 横屏：允许纵向滚动；不做双栏；BottomBar 与内容不重叠；Splash 正常。
- 系统栏 inset：edge-to-edge 下所有页面内容不被状态栏/导航栏遮挡。

### 8.3 性能（60fps）

- 动画只用 alpha/transform/color；避免每帧读 DB/分配大对象；列表 key 稳定；图标为 vector（无位图解码）。
- 列表 stagger 限制前 8 项；页面转场 220ms；不做 blur/大面积阴影。
- 验收：`adb shell dumpsys gfxinfo com.fitplan.app reset` → 滚动/打卡/切 Tab → `dumpsys gfxinfo` 读取 Janky frames 百分比；目标 <5%（记录原始数字与设备）。若设备不支持/统计噪声大，记录并说明。

---

## 9. 环境、构建与真机纪律

### 9.1 构建（强制）

```bash
# 全量验证（Lead/verifier；clean + tests + assemble）
ANDROID_USER_HOME=/Users/suyi/projects/exercise/.android-home \
  sh /Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh \
  clean :app:testDebugUnitTest :app:assembleDebug --console=plain
```

- 所有 Gradle 调用必须走锁脚本（内部已设置 `GRADLE_USER_HOME=/Users/suyi/projects/exercise/.gradle-home` 与 `--offline`）；禁止直接 `./gradlew`。
- 开发期快速反馈可用 `:app:compileDebugKotlin`（同样走锁脚本），但每个里程碑必须至少一次 assemble。
- 测试计数：
```bash
grep -h 'tests=' /Users/suyi/projects/exercise/FitPlan/app/build/test-results/testDebugUnitTest/*.xml \
  | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}'
```
- 基线：131 tests / 0 failures / 0 errors；警告 2 条（AppDatabase fallbackToDestructiveMigration、Common.menuAnchor——若 Common 重写后 menuAnchor 警告消失，基线降为 1 条，允许，不得新增其他警告）。

### 9.2 真机

```bash
adb devices -l                                  # 期望 5968de20 device
adb install -r /Users/suyi/projects/exercise/FitPlan-iteration12-debug.apk
adb shell am start -W -n com.fitplan.app/.MainActivity
adb exec-out screencap -p > /Users/suyi/projects/exercise/FitPlan/docs/verify/iteration12/<name>.png
adb shell cmd uimode night yes|no               # 深色/浅色
adb shell settings put system font_scale 1.3    # 大字体；验后恢复 1.0
adb shell settings put global animator_duration_scale 0   # 减少动态效果；验后恢复 1.0
```

- 截图目录：`/Users/suyi/projects/exercise/FitPlan/docs/verify/iteration12/`（浅色/深色成对命名，如 `01-home-light.png` / `01-home-dark.png`）。
- 每轮关键改动必须安装验证；最终 APK 冷启动 ok、无 FATAL EXCEPTION（`adb logcat -d | grep -i "FATAL EXCEPTION"` 为空）。

---

## 10. 文件结构与写作用域

### 10.1 新增/重写文件

```text
FitPlan/app/src/main/java/com/fitplan/app/
  ui/theme/
    Theme.kt          （重写：主题、动态色、系统栏、LocalFitColors/LocalFitMotion）
    Color.kt          （新增：色板 + FitExtraColors + LocalFitColors）
    Type.kt           （新增：FitTypography + FitType）
    Shape.kt          （新增：FitShapes + FitSpace + FitSize）
    Motion.kt         （新增：FitMotion + LocalFitMotion + 减少动态检测）
  ui/design/
    FitCard.kt, FitScaffold.kt, FitTopBar.kt, FitBottomBar.kt, FitButtons.kt,
    FitDialog.kt, FitEmptyState.kt, FitProgress.kt, FitList.kt, FitChips.kt,
    FitEnter.kt, Haptics.kt, Brand.kt, FitSnackbar.kt
  ui/Common.kt        （重写：兼容层 + NumberField/DropdownSelector 样式）
  ui/AppNav.kt        （重写：底部导航、Tab 动效、NavHost 转场）
  MainActivity.kt     （边到边 + 系统栏）
FitPlan/app/src/main/res/
  values/themes.xml, values/colors.xml（splash/品牌色）
  drawable/ic_launcher_foreground.xml, ic_launcher_monochrome.xml, ic_launcher_background.xml, ic_fitplan_mark.xml
  mipmap-anydpi-v26/ic_launcher.xml, ic_launcher_round.xml
FitPlan/app/src/main/AndroidManifest.xml（roundIcon）
```

### 10.2 写作用域（互斥，越界即回滚）

| 角色 | 写作用域（workspace 相对） |
| --- | --- |
| Lead | `docs/**`、`FitPlan/HANDOVER.md`、`FitPlan/README.md`、`CONTEXT.md`、`.scratch/iteration12/**`、APK 根目录产物 |
| design-system-dev | `FitPlan/app/src/main/java/com/fitplan/app/ui/theme/**`、`ui/design/**`、`ui/Common.kt`、`ui/AppNav.kt`、`FitPlan/app/src/main/java/com/fitplan/app/MainActivity.kt`、`FitPlan/app/src/main/res/**`、`FitPlan/app/src/main/AndroidManifest.xml`、`docs/iteration12-brand.md` |
| screens-dev-a（页面 A） | `ui/home/**`、`ui/plan/WorkoutScreen.kt`、`ui/plan/PlanScreen.kt`、`ui/plan/PlanDialogs.kt` |
| screens-dev-b（页面 B） | `ui/combo/ComboListScreen.kt`、`ui/combo/ComboEditScreen.kt`、`ui/combo/ExercisePickerDialog.kt`、`ui/exercise/ExerciseListScreen.kt`、`ui/exercise/ExerciseEditScreen.kt`、`ui/log/LogScreen.kt`、`ui/log/EntryDialog.kt`、`ui/pr/PrScreen.kt`、`ui/settings/SettingsScreen.kt` |
| verifier | `docs/iteration12-verification.md`、`.scratch/iteration12/verify/**`、`FitPlan/app/src/test/java/com/fitplan/app/Iteration12AcceptanceTest.kt`（仅新增测试） |

禁止：任何 agent 修改 `data/**`、`export/**`、`session/**`、`util/**`、`*ViewModel.kt`、`FitPlan/app/build.gradle.kts`、其他 agent 的作用域文件。

---

## 11. 任务拆分

> 任务编号：T-01 设计系统；T-02 页面 A；T-03 页面 B；T-04 独立验证；T-05 Lead 集成/文档/发布。每个任务给出文件、契约、步骤、验收与测试要求。

### T-01 设计系统 + 导航壳 + 品牌资产（owner: design-system-dev）

**文件**
- 新增/重写：`ui/theme/Theme.kt`、`Color.kt`、`Type.kt`、`Shape.kt`、`Motion.kt`；`ui/design/*.kt`（§10.1 全部）；`ui/Common.kt`；`ui/AppNav.kt`；`MainActivity.kt`。
- 资源与 Manifest：§10.1 的 `res/**` 与 `AndroidManifest.xml`。
- 文档：`docs/iteration12-brand.md`（图标/启动/动效原则与验收证据）。

**步骤（两个里程碑）**
- **P1（关键路径，先交付）**：token（Color/Type/Shape/Motion）+ 核心组件（FitCard/SectionHeader/Hairline/ScreenTitle/FitScaffold/FitTopBar/FitBottomBar/FitButton/FitEmptyState/FitDialog/FitBottomSheet/FitProgress/AnimatedCount/FitListRow/FitChips/fitEnter/Haptics/FitSnackbar）+ 重写 Common.kt（兼容层）+ AppNav 底部导航与转场 + MainActivity 边到边。完成标准：`sh ... :app:compileDebugKotlin` 成功（旧屏幕代码不改也能编译），并给 Lead 发消息“DS P1 ready”。
- **P2**：Splash 主题、自适应/单色/圆形图标、品牌标记、docs/iteration12-brand.md、动效细节打磨与减少动态验证。

**接口契约**：§3/§4 全部签名；屏幕 agent 只按契约调用。
**测试**：若新增纯逻辑（如减少动态的时长映射、stagger 延迟计算、颜色对比度工具），必须补 JVM 测试；否则以编译 + 真机截图验收。
**验收**：compile 成功；res 资源可被 aapt 打包；图标/Splash 真机截图；屏幕代码 grep 无硬编码颜色/圆角/字号（由 verifier 复核）。

### T-02 页面 A：主页 / 计划 / 训练详情（owner: screens-dev-a）

**文件**：`ui/home/HomeScreen.kt`、`ui/plan/PlanScreen.kt`、`ui/plan/PlanDialogs.kt`、`ui/plan/WorkoutScreen.kt`。
**约束**：不改 `WorkoutViewModel`、`PlanViewModel`、`WorkoutProgress`；保留 `WorkoutChecklist`/`WorkoutActions`/`workoutActions` 对外签名与行为；不改文案口径（新增纯视觉辅助文案需与 Lead 确认，术语按 CONTEXT.md）。
**步骤**：
1. 读现状 4 文件 + 设计契约；列出交互清单（打卡、取消、次数±、标逃、重量步进/弹窗、备注、同步、开始/结束、复制上周、自动排程、单日编辑、复制前一天、移除安排、周切换、跨天刷新）。
2. 重构页面（按 §5.1-5.3）；每页先完成布局与组件替换，再加动效（fitEnter/AnimatedVisibility/AnimatedCount）。
3. 编译（DS P1 完成后）：`... :app:compileDebugKotlin`；修复错误；不得改他人文件。
4. 自查：每个交互清单逐项点检（代码路径存在、回调签名不变、TalkBack 语义保留、48dp）。
5. 在共享任务写完成备注（改动文件、编译命令/结果、交互清单核对、风险）。
**验收**：编译通过；交互清单 100% 保留；页面截图（浅/深）与大字体检查；无业务逻辑改动。

### T-03 页面 B：组合 / 动作 / 记录 / PR / 设置（owner: screens-dev-b）

**文件**：§10.2 第二行 9 个文件。
**约束**：不改任何 ViewModel；保留所有对话框文案与逻辑；`ExercisePickerDialog` 的搜索/筛选/多选语义不变。
**步骤**：同 T-02（读现状 → 重构 → 编译 → 自查 → 完成备注）；顺序建议：组合列表/编辑 → 动作列表/编辑 → 记录/EntryDialog → PR → 设置。
**验收**：编译通过；交互清单保留；截图/大字体；无业务逻辑改动。

### T-04 独立验证（owner: verifier，blocked_by T-01/T-02/T-03）

**产出**：`docs/iteration12-verification.md`、`.scratch/iteration12/verify/**`（原始日志/截图索引/uiautomator dump）、必要时 `Iteration12AcceptanceTest.kt`（仅新增测试）。
**步骤**：
1. 源码审查：DB v11 未变、无新增依赖/权限、写作用域无越界（对照 backup diff）、无业务文件改动（data/export/session/util/VM）。
2. 全量 clean 构建 + 测试（§9.1 命令），记录 tests/failures/errors、警告数、exit code。
3. 真机：安装、冷启动、浅/深色每页截图、大字体、减少动态、TalkBack/uiautomator、gfxinfo、对比度计算。
4. 逐条 AC（§12）输出 PASS/FAIL 矩阵与证据；FAIL 回报 Lead，不自行改生产文件。
5. G2/G3 证据文件写入 `.scratch/iteration12/gates/`。

### T-05 Lead：集成、文档、发布（owner: Lead）

- 备份（已完成）、任务板、G0-G3、冲突裁决、编译修复协调、最终 clean 构建、APK 复制与 sha256、真机安装与截图、HANDOVER/README/CONTEXT 更新、`docs/iteration12-report.md`、最终答复。
- 发布前 code review（Standards + Spec 双轴，可用 code-review 技能或自审）。

---

## 12. 验收标准（AC 矩阵）

| ID | 验收项 | 证据 |
| --- | --- | --- |
| AC-01 | 设计 token 集中：颜色/字阶/间距/圆角/动效均在 theme/design 定义 | 代码审查 + grep 屏幕文件无 `Color(0x`/裸 `RoundedCornerShape(`/裸 `sp` 字号 |
| AC-02 | 组件复用：§4 组件均实现并在 ≥2 处使用（或页面必需处） | grep 使用点 + 组件清单 |
| AC-03 | 页面全覆盖重构：主页/计划/训练/组合列表+编辑/动作列表+编辑/记录/PR/设置/导航/对话框 | 浅/深色截图逐页 + 代码审查 |
| AC-04 | 动效：页面转场、Tab 切换、列表入场、打卡微动效、进度/数字、弹层 spring、减少动态降级 | 截图/录屏 + `animator_duration_scale=0` 对比截图 + 代码审查 |
| AC-05 | 图标与启动：adaptive + monochrome + round icon + splash 主题 | 真机桌面/启动截图；资源文件与 Manifest 审查 |
| AC-06 | 浅色/深色/动态取色 | 两套截图；动态色设备截图 |
| AC-07 | 大字体 1.3 关键页面不截断/不溢出 | 截图 + uiautomator bounds 检查 |
| AC-08 | 无障碍：TalkBack 语义、48dp 触达、组圈 toggleable 保留 | uiautomator dump（content-desc/clickable bounds）+ 代码审查 |
| AC-09 | 对比度：正文/次要/按钮 ≥4.5:1 | 计算脚本输出（§3.1 已给） |
| AC-10 | 60fps：关键滚动/打卡场景 janky <5% | gfxinfo 原始输出 |
| AC-11 | 业务零回归：打卡/逃组/重量/备注/体重/用时/PR/导出/移除安排/多训练者/通知行为不变 | 131 测试全绿 + 代码 diff 审查 + 真机抽查 |
| AC-12 | 构建：clean 构建成功、零新增警告 | 构建日志（tests=131+、exit=0、warnings≤基线） |
| AC-13 | 真机安装与冷启动 | `adb install -r` Success、`am start -W` COLD ok、logcat 无 FATAL |
| AC-14 | 文档：plan/report/HANDOVER/README 更新 | 文件审查 |
| AC-15 | 约束：DB v11、无新依赖/权限、备份保留、无越界写 | grep/diff 证据 |

---

## 13. 真机验收清单（第十二轮）

- [ ] 覆盖安装第十一轮 APK → 启动不崩、既有数据完好（计划/打卡/体重/备注）。
- [ ] 冷启动 Splash 无白闪；桌面图标（普通/深色/主题图标）清晰。
- [ ] 主页训练日：进度 hero 数字正确、开始/结束训练、体重/备注录入、清单打卡/取消/次数±/标逃/组重量/当次重量/同步提醒/动作备注全部可用；打卡动效与 haptic。
- [ ] 主页休息日：休息卡 + 体重/备注 + 去计划页；空组合提示且不能开始训练。
- [ ] 计划页：周切换（按钮/Pager）、复制上周（确认/结果）、自动排程（默认值/生成）、单日编辑（休息/训练/复制/移除）、今天高亮、用时显示。
- [ ] 训练详情页：返回、展开/收起、全部交互同上、空态/加载态。
- [ ] 组合：新建/编辑/保存/非法输入拦截/上移下移/移除/添加动作（搜索/分类/多选/已添加）/复制/删除确认。
- [ ] 动作：新建/编辑/字段开关/默认重量/删除确认/PR 入口。
- [ ] 记录：日期切换、新增/编辑/删除确认、组次与重量。
- [ ] PR：训练者 chip、分组/档位/首达日期、空态。
- [ ] 设置：训练者增删切换、单位、默认字段、导出（成功/失败/取消）、清空确认、版本。
- [ ] 深色模式逐页；大字体 1.3 逐页；减少动态逐页（动画退化）。
- [ ] TalkBack 关键路径；触摸目标 ≥48dp；对比度目视。
- [ ] 从计划页进训练页再返回、Tab 间切换、系统返回手势：无状态丢失/崩溃。
- [ ] 训练会话通知仍正常（开始训练→通知挂出→直接打卡/逃了→完成），文案未变。
- [ ] 导出 Markdown 内容与旧版一致（结构/字段/符号）。

---

## 14. 风险与回滚

| 风险 | 概率/影响 | 缓解 | 回滚 |
| --- | --- | --- | --- |
| 设计系统 API 变更导致屏幕编译大面积失败 | 中/高 | 契约先行冻结签名；DS P1 先编译；G1 抽查 | 屏幕回退到 backup 对应文件；DS 修复向后兼容 |
| 视觉重构引入交互回归（点击/展开/输入/保存） | 中/高 | 每页交互清单 + 真机逐项 + 保留原有回调签名/semantics | 单文件回滚；重装上一版 APK 对比 |
| 动画导致卡顿/掉帧 | 中/中 | 只动 alpha/transform；stagger 限 8；gfxinfo 验证 | 关闭对应动效（reduce motion 或删减） |
| NavHost 转场与返回手势/深链冲突 | 低/中 | 用官方 transitions API；真机返回测试 | 退回无转场（仅 fade） |
| 动态取色破坏品牌识别 | 中/低 | 品牌识别靠排版/形状/动效/标记；扩展色固定 | 关闭动态色 fallback 品牌色（改一行） |
| 减少动态检测不生效 | 低/中 | ValueAnimator.areAnimatorsEnabled + ON_RESUME 刷新；真机 scale=0 验证 | 手动 token 降级 |
| 大字体/窄屏截断 | 中/中 | 去固定宽度、FlowRow、maxLines 审慎；1.3/1.5 截图 | 调整布局 |
| 工作区无限并发行写冲突 | 中/高 | 写作用域互斥 + 构建锁 + Lead diff 抽查 | backup 恢复越界文件 |
| 无 git 误改不可追溯 | 中/高 | backup + 阶段 diff + 禁止删除备份 | `cp -cR backup/src/. FitPlan/app/src/` |
| 范围膨胀做不完 | 中/高 | 非目标清单；共享元素列为 stretch；G2 可 CONDITIONAL | 砍 P2 打磨项，保 AC 核心 |

回滚命令（工作区根）：
```bash
# 单文件
cp -c /Users/suyi/projects/exercise/.scratch/iteration12/backup/src/main/java/com/fitplan/app/ui/home/HomeScreen.kt \
      /Users/suyi/projects/exercise/FitPlan/app/src/main/java/com/fitplan/app/ui/home/HomeScreen.kt
# 全量（谨慎，先确认目标）
cp -cR /Users/suyi/projects/exercise/.scratch/iteration12/backup/src/. /Users/suyi/projects/exercise/FitPlan/app/src/
```

---

## 15. 阶段门评审（SGR，AskMatt 等价协议）

每个门四段：阶段结论（GO/NO-GO/CONDITIONAL）、风险判断（P0-P3 + 证据）、下一步行动（≤5 条）、证据（命令输出/测试计数/文件/评审人）。记录在本文件 §17 与 `.scratch/iteration12/gates/`。

- **G0 计划完成门**：本文件完成；基线 131 测试（up-to-date 证据）；备份完成；任务板就绪；4 名 teammate 分工明确。
- **G1 开发中门**：DS P1 编译通过；屏幕代码进行中/完成；无越界写；接口契约一致；开发期 compile 通过。
- **G2 测试前门**：全部页面代码完成；131 测试全绿；至少一次 assemble 成功；无 P0/P1 未决；reduced-motion/深色/大字体自测完成。
- **G3 发布前门**：clean 全量构建 + 131+ 测试全绿；真机安装截图归档；AC 矩阵 PASS；文档更新；APK sha256；残余风险可接受。

### 已执行的门记录

- **G0（2026-10-18 19:0x）**：GO。计划完成；基线 131 测试；备份 888K/101 文件；任务板 task-1..4。证据 `.scratch/iteration12/gates/G0.md`。
- **G1（2026-10-18 19:18-19:25）**：GO（带 CONDITIONAL，已闭环）。首轮 clean 构建 131/0/0；真机冒烟发现 360dp 组行换行、列表 FAB 遮挡、单选/复选语义、重量读屏、scale 重组等问题；回灌 A/B/DS 修复后重新 clean 构建通过。证据 `.scratch/iteration12/gates/G1.md`。
- **G2（2026-10-18 19:25-19:30）**：GO。全部页面冻结；131/0/0；APK 安装/冷启动正常；浅/深/大字体/减少动态/性能/无障碍抽测完成；无 P0/P1。证据 `.scratch/iteration12/gates/G2.md`。
- **G3（2026-10-18 19:46）**：**GO**。最终 APK 双端 clean 构建同哈希（`b96b029f…`）、装机 Success、独立冷启动 COLD 614ms、FATAL 0；AC-01..AC-15 PASS；文档收口。证据 `.scratch/iteration12/gates/G3.md`、`docs/iteration12-verification.md`。

---

## 16. 假设与决策

| 编号 | 假设/决策 | 依据 | 影响 | 若假设错误 |
| --- | --- | --- | --- | --- |
| A1 | 环境无 AskMatt 技能，采用 SGR 四段协议 | 第十一轮已确认；本环境技能目录无 AskMatt | 阶段门仍有结论/风险/下一步/证据 | 用户补充技能后补跑 |
| A2 | 用户已给出设计方向与硬约束，视为设计 brief 已批准；不再逐项提问 | 用户明确"不要询问非阻塞问题"+ 详细目标 | 节省往返；决策记录于本表 | 用户可随时纠正，按 A3 回滚 |
| A3 | 品牌识别采用"组环+对勾"单色几何标记、深绿/米白纸感；不引入字体资产 | 离线、无新依赖、CJK 系统字体；组圈是产品核心符号 | 图标/Splash/空态统一 | 若用户不喜欢，改标记为纯几何（工作量 S） |
| A4 | 底部 6 Tab 与路由不变，仅重做视觉与动效 | 业务语义零变化；避免信息架构变更风险 | 导航改造可控 | 若需合并 Tab，属下一轮 IA 变更 |
| A5 | 不新增依赖：Splash 用平台主题属性；共享元素用 Compose 1.11.4 内置 | 离线缓存无 core-splashscreen；BOM 2026.06.01 → Compose 1.11.4 | 无下载风险 | 若 API 不可用，降级为静态启动图 |
| A6 | 共享元素为 P2 stretch：若实现，用于计划页训练日卡 → 训练详情标题；否则记录为未完成 | 跨 NavHost plumbing 风险较高，收益有限 | 不影响 AC 核心 | 报告中明确说明 |
| A7 | 反馈呈现可由 Toast 升级为 Snackbar，文案与触发条件不变 | 视觉/一致性优化；不改变业务语义 | 导出/清空/保存反馈更统一 | 若真机行为异常，回退 Toast |
| A8 | 深色/动态取色跟随系统，不新增主题设置项 | DB 不改、设置语义不扩张 | 无新增存储/设置 | 若用户要求开关，用 SharedPreferences（另评估） |
| A9 | 采用 4 名 teammate：design-system-dev、screens-dev-a、screens-dev-b、verifier；品牌资产归 design-system-dev | 用户要求 3-4 名；画面工作量大需两名页面 agent；品牌与设计系统同域避免冲突 | 并行度高；DS 为关键路径 | 可追加 verifier/文档 agent |
| A10 | 屏幕代码不新增业务逻辑；若提取纯函数必须补 JVM 测试 | 业务语义零变化 + 测试要求 | 131 基线不降 | 若确实提取逻辑，按 TDD 补测 |
| A11 | 动效验收以截图/录屏 + reduce-motion 对比为主，不做逐帧数值断言 | 无 Compose UI 测试基础设施 | 验收可执行 | 后续引入 UI 测试时升级 |
| A12 | 构建环境沿用第十一轮（.gradle-home/.android-home/锁脚本） | 已验证可用 | 直接开工 | 缓存损坏则重新克隆 |
| A13 | 计划文件路径用 `docs/iteration12-plan.md`（用户指定），不用 writing-plans 默认路径 | 用户明确要求 | 文档路径固定 | 无 |
| A14 | 大字体验收以 font_scale 1.3 为主、1.5 抽查关键页 | Android 无障碍常见档位；1.5 极端场景抽查 | 平衡工作量 | 1.5 问题记录为后续 |
| A15 | 动态取色截图依赖设备当前壁纸；若壁纸无色/不可控，记录并仅验证浅深色 | 设备无壁纸切换权限保障 | 证据可能受限 | 报告中说明 |

---

## 17. 任务板、执行波次与执行日志

### 17.1 执行波次

- **Wave 0（Lead）**：计划、备份、基线、任务板、G0。
- **Wave 1（并行）**：design-system-dev 做 T-01 P1（关键路径）；screens-dev-a/b 同时读现状并开始按契约写页面（先不依赖编译）；Lead 更新文档/准备验收脚本。
- **Wave 1.5（Lead 集成）**：DS P1 ready 后，Lead 跑 compile，通知屏幕 agent 编译修复。
- **Wave 2（并行）**：screens-dev-a/b 完成页面；design-system-dev 做 P2（图标/Splash/打磨）。
- **Wave 3（Lead）**：G1/G2、集成编译、冲突裁决。
- **Wave 4（verifier）**：T-04 独立验证（clean 构建、真机截图、AC 矩阵）。
- **Wave 5（Lead）**：文档、APK、G3、最终答复。

### 17.2 执行日志（Lead 维护）

| 时间 | 事件 | 证据/结果 |
| --- | --- | --- |
| 2026-10-18 19:0x | 读取交接文档、HANDOVER、CONTEXT、4 ADR、iteration-plan、iteration11-report、全部 UI 源码；确认基线 | 38 个 Kotlin 文件、UI 约 5.5k 行、131 测试（20 类） |
| 2026-10-18 19:1x | 基线测试 | `testDebugUnitTest` BUILD SUCCESSFUL（up-to-date；test-results 记录 131） |
| 2026-10-18 19:2x | 备份 | `.scratch/iteration12/backup/src`（888K/101 文件），备份 manifest/build.gradle |
| 2026-10-18 19:3x | 写计划 v1.0 | 本文件 |
| 2026-10-18 19:4x | 创建任务板 / G0 | 见 §17.3 / `.scratch/iteration12/gates/G0.md` |
| 2026-10-18 19:05 | spawn 4 名 teammate（design-system-dev / screens-dev-a / screens-dev-b / verifier）并认领 task-1..4 | `list_agents` 四个 running；任务板 task-1..4 |
| 2026-10-18 19:13 | DS P1 ready（token + 组件 + Common + AppNav + MainActivity），compile 通过 | DS 简报；屏幕文件零改动可编译 |
| 2026-10-18 19:18 | G1 首轮 full clean 构建 + 安装 + 真机冒烟 | BUILD SUCCESSFUL 21s；131/0/0；COLD 824ms；截图 final-01.. |
| 2026-10-18 19:20-19:25 | 真机发现 5 类问题并回灌修复（360dp 组行、FAB 遮挡、单选/复选语义、读屏值、scale 重组、role/token） | A/B/DS 简报；重新 compile 0 error |
| 2026-10-18 19:25 | G1 修复后 final clean：BUILD SUCCESSFUL 19s；131/0/0；1 warning；APK 63,482,439 bytes | `.scratch/iteration12/gates/lead-build-g2.log` |
| 2026-10-18 19:25-19:30 | 真机验收：Splash/图标/组行 56dp/FAB 底部/深色/大字体/减少动态/gfxinfo 0.69% | `FitPlan/docs/verify/iteration12/`、`.scratch/iteration12/verify/gfxinfo-scroll.txt` |
| 2026-10-18 19:30 | 冻结源码，spawn verifier 执行 T-04；G2 GO | task-4 in_progress；`.scratch/iteration12/gates/G2.md` |
| 2026-10-18 19:32-19:37 | 真机验证期间用户拿起手机（前台切到微信/Steam），verifier 暂停输入；Lead 修 ActionChip 48dp 触达并做最终 clean 构建 | BUILD SUCCESSFUL 17s；131/0/0；APK sha256 b96b029f...；`.scratch/iteration12/gates/lead-build-final.log` |
| 2026-10-18 19:37 | 最终代码冻结，verifier 复核最终构建 + 设备空闲后补测 | 最终 APK b96b…；install Success；COLD 831ms（Lead）/614ms（verifier） |
| 2026-10-18 19:38-19:44 | verifier fresh clean 构建 + 最终 APK 装机/冷启动/深色/大字体/减少动态/gfxinfo/无障碍/对比度 | 16s / 131/0/0 / 1 warning；同哈希；scroll 1.11%；AC-01..15 PASS；`docs/iteration12-verification.md` |
| 2026-10-18 19:46 | G3 GO；文档收口（report/HANDOVER/README/plan/brand/verification/截图索引） | `.scratch/iteration12/gates/G3.md`；最终 APK sha256 b96b029f… |
| （后续） | verifier 独立验证 + 文档 + G3 | 每个门更新 |

### 17.3 任务板（DSH shared tasks）

| task | 标题 | owner | blocked_by | 写作用域 |
| --- | --- | --- | --- | --- |
| task-1 | T-01 设计系统 + 导航壳 + 品牌资产 | design-system-dev | — | §10.2 |
| task-2 | T-02 页面 A：主页/计划/训练 | screens-dev-a | task-1（P1 接口） | §10.2 |
| task-3 | T-03 页面 B：组合/动作/记录/PR/设置 | screens-dev-b | task-1（P1 接口） | §10.2 |
| task-4 | T-04 独立验证 | verifier | task-1/2/3 | §10.2 |

> 任务板 detail 以 DSH shared tasks 实际记录为准；本表为人类可读摘要。

---

## 18. 沟通与协作纪律

1. 中文回复，保留专业术语；不用 emoji。
2. 先结论后证据；文件给绝对路径或工作区相对路径，多文件逐条列出。
3. 每个 agent 完成一个任务组或遇到阻塞/需裁决时发一条消息，不刷屏。
4. 完成备注必须含：改动文件、验证命令与原始输出摘要、未决风险、下一步。
5. 禁止：git 命令；删除/移动备份；直接 `./gradlew`；修改他人写作用域；改 DB/数据层/业务逻辑；新增依赖/权限。
6. 任何"通过/完成"结论必须附 fresh 命令输出或真机证据；禁止"应该/看起来"。
