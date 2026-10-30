# FitPlan 第十二轮品牌资产与动效说明

- 文档版本：v1.0（2026-10-18）
- owner：design-system-dev（task-1）
- 关联计划：/Users/suyi/projects/exercise/docs/iteration12-plan.md §3/§4/§6/§7
- 关联文件：ui/theme/**、ui/design/**、ui/AppNav.kt、ui/Common.kt、MainActivity.kt、app/src/main/res/**、AndroidManifest.xml
- 状态：组件与资源已落地并通过 :app:processDebugResources + :app:compileDebugKotlin；真机截图待 verifier/Lead 归档

## 1. 品牌标记：组环 + 对勾

语义：圆环 = 一组（set），对勾 = 完成；与清单打卡的视觉符号同源，同时复用为桌面图标、Splash 图标、空态标记。
造型参数（108×108 viewport，居中于 66dp 自适应安全区）：

| 元素 | 几何 |
| --- | --- |
| 圆心 | (54, 54) |
| 组环外径 | 46（半径 23） |
| 组环线宽 | 7，圆头端点 |
| 对勾 | (43,54) → (51,62) → (66,45)，线宽 8，圆头圆角连接 |
| 单色可识别 | 最小 24dp 清晰；只依赖形状不依赖颜色 |

资源：
- /Users/suyi/projects/exercise/FitPlan/app/src/main/res/drawable/ic_launcher_foreground.xml（米白 #F3F6EC，自适应图标前景）
- /Users/suyi/projects/exercise/FitPlan/app/src/main/res/drawable/ic_launcher_monochrome.xml（白色单色，Android 13+ 主题图标）
- /Users/suyi/projects/exercise/FitPlan/app/src/main/res/drawable/ic_fitplan_mark.xml（品牌绿 #2E6B4F，应用内可 tint）
- 组件：/Users/suyi/projects/exercise/FitPlan/app/src/main/java/com/fitplan/app/ui/design/Brand.kt 的 FitBrandMark（Canvas 绘制，tint 跟随 LocalContentColor）

## 2. 色板

实现位置：/Users/suyi/projects/exercise/FitPlan/app/src/main/java/com/fitplan/app/ui/theme/Color.kt。

- 浅色：暖米白纸面 #F6F7F2 + 深常青绿主色 #2E6B4F + 暖琥珀 PR 色 #8A5A2B。
- 深色：墨绿近黑 #0F1210 + 亮绿主色 #8ED8AE。
- 动态取色：Android 12+ 默认开启 dynamicLight/DarkColorScheme，只替换 M3 色彩角色；品牌识别由排版、形状、动效与标记承载。
- 品牌扩展色 FitExtraColors（固定值，不参与动态取色）：success/onSuccess/successContainer/onSuccessContainer、warning 四件套、track、heroTop、heroBottom、surfaceSunken、scrim；入口 MaterialTheme.fitColors。

对比度（计划 §3.1 已核算，实现值一致）：浅色 onSurface 15.98:1、onSurfaceVariant 5.78:1、primary 对白 6.30:1；深色 onSurface 14.99:1、onSurfaceVariant 8.61:1、primary 对背景 11.29:1。

## 3. 字阶与排版

实现位置：/Users/suyi/projects/exercise/FitPlan/app/src/main/java/com/fitplan/app/ui/theme/Type.kt。

- 系统字体 FontFamily.Default（CJK 优先，无字体资产下载）。
- 字重只用 Normal(400)/Medium(500)；FitType.heroNumber 用 SemiBold(600) 仅限英雄数字。
- displayLarge 57/64、displayMedium 45/52、displaySmall 36/44、headlineLarge 32/40、headlineMedium 28/36、headlineSmall 24/32、titleLarge 22/28、titleMedium 17/24、titleSmall 15/22、bodyLarge 16/24、bodyMedium 14/21、bodySmall 13/19、labelLarge 14/20、labelMedium 12/16、labelSmall 11/16。
- 中文标题不加负字距；label 系列 0.2/0.4/0.3sp 正字距（Latin 友好）。
- 数字：FitType.numeric（fontFeatureSettings = "tnum"），用于进度、重量、PR、用时；配合 typography.merge 使用。

## 4. 圆角、间距与尺寸

实现位置：/Users/suyi/projects/exercise/FitPlan/app/src/main/java/com/fitplan/app/ui/theme/Shape.kt。

- FitShapes：card 24、hero 28、dialog 28、sheet（顶部 28）、chip 12、pill 50%、field 16、small 8；material 同步 M3 Shapes。
- FitSpace：xxs 2、xs 4、sm 8、md 12、lg 16、xl 20（页面 gutter/卡片内边距）、xxl 24、xxxl 32、huge 40。
- FitSize：gutter 20、rowMin 56、touchMin 48、icon 24、iconSmall 18、setCircle 40、fab 56、bottomBar 72、topBar 64、button 52、emptyIcon 72、aggregateCircle 32、checkIcon 18、checkIconLarge 20、border 2、ring 96、ringStroke 10。
- FitShapes 另提供 circle（CircleShape 正圆，用于组圈/聚合圈/圆形图标容器）；屏幕禁止裸用 CircleShape 尺寸字面量。
- 层级：L0 background；L1 卡片 surface + 1dp outlineVariant 0.6 hairline、无阴影；L2 悬浮（底部栏/Snackbar）surfaceContainerHigh + 3dp 阴影；L3 弹层 surface + 28dp 圆角。

## 5. 图标（自适应 / monochrome / round）

- /Users/suyi/projects/exercise/FitPlan/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml：background = @drawable/ic_launcher_background（品牌深绿 #143D2A 极淡同色渐变）、foreground = @drawable/ic_launcher_foreground（米白组环+对勾）、monochrome = @drawable/ic_launcher_monochrome。
- /Users/suyi/projects/exercise/FitPlan/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml：同构，供 roundIcon。
- /Users/suyi/projects/exercise/FitPlan/app/src/main/AndroidManifest.xml：android:icon = @mipmap/ic_launcher、android:roundIcon = @mipmap/ic_launcher_round。
- 前景位于 66dp 自适应安全区中心，缩放到 24dp 仍可识别；无位图资产、无下载字体。

## 6. Splash 与启动体验（不新增依赖）

- 主题：/Users/suyi/projects/exercise/FitPlan/app/src/main/res/values/themes.xml 的 Theme.FitPlan 使用平台 API 36 内建 SplashScreen 属性：
  - android:windowSplashScreenBackground = @color/fitplan_splash_background（品牌深绿 #143D2A，浅/深色一致保持识别）
  - android:windowSplashScreenAnimatedIcon = @drawable/ic_launcher_foreground（米白组环+对勾）
  - android:windowSplashScreenIconBackgroundColor = @color/ic_launcher_background
  - android:windowSplashScreenAnimationDuration = 200
  - android:windowBackground = @color/fitplan_window_background（浅色 #F6F7F2 / values-night #0F1210，避免白闪与首帧跳变）
  - android:statusBarColor / navigationBarColor = transparent；windowLightStatusBar = false（Splash 阶段深绿底配浅色图标）
- 颜色定义：/Users/suyi/projects/exercise/FitPlan/app/src/main/res/values/colors.xml 与 /Users/suyi/projects/exercise/FitPlan/app/src/main/res/values-night/colors.xml。
- 首帧衔接：MainActivity 调用 enableEdgeToEdge（系统栏透明 + 图标明暗兜底），FitPlanTheme 在 SideEffect 中按深色/浅色再次同步 isAppearanceLightStatusBars / isAppearanceLightNavigationBars；Compose 背景 = FitPlanTheme background，与 windowBackground 同值。
- 未引入 core-splashscreen 依赖，未新增权限。

## 7. 动效 token 与减少动态降级

实现位置：/Users/suyi/projects/exercise/FitPlan/app/src/main/java/com/fitplan/app/ui/theme/Motion.kt（FitMotion / LocalFitMotion / MaterialTheme.fitMotion / rememberSystemAnimationsEnabled）。

| token | 值 |
| --- | --- |
| short / medium / long / extraLong | 120 / 220 / 320 / 480 ms |
| standard | CubicBezier(0.2, 0, 0, 1) |
| emphasizedDecel | CubicBezier(0.05, 0.7, 0.1, 1) |
| emphasizedAccel | CubicBezier(0.3, 0, 0.8, 0.15) |
| pop / gentle / sheet | spring(0.55/700) / spring(0.85/320) / spring(0.8/420) |

减少动态检测：ValueAnimator.areAnimatorsEnabled() 初始化读取，LocalLifecycleOwner 监听 ON_RESUME 刷新；关闭时 FitMotion.enabled = false。

降级矩阵（已实现）：

| 场景 | 实现 | 减少动态 |
| --- | --- | --- |
| 详情页推入/返回 | NavHost slideIn/Out 1/5 + fade | 120ms 纯 fade |
| Tab 切换 | AnimatedContent fade + scale 0.98→1 | 100ms 纯 fade |
| 列表项入场 | Modifier.fitEnter（alpha + 12dp translate，35ms stagger，≤8 项，rememberSaveable 不重复） | 直接显示 |
| 进度环/进度条 | spring gentle | snap 跳变 |
| 数字变化 | AnimatedCount 垂直滑动 + 淡入淡出 | 直接更新 |
| 底部导航指示器 | 胶囊 graphicsLayer.translationX 滑动 220ms（只改绘制层，不触发布局）+ 图标 crossfade | snap 立即切换 |
| 弹层 | ModalBottomSheet 默认 spring；对话框 28dp 圆角 | 由系统动画缩放统一降级 |

约束：动画只改 alpha/translation/scale/color；stagger 限制前 8 项；无无限循环装饰动画；列表用 rememberSaveable 避免滚动复用重播。

## 8. 导航壳

实现位置：/Users/suyi/projects/exercise/FitPlan/app/src/main/java/com/fitplan/app/ui/AppNav.kt。

- 6 Tab 文案与顺序不变（主页/计划/组合/动作/记录/设置）；FitTabSpec 携带 filled/outlined 图标。
- FitBottomBar：72dp 内容高 + 系统导航栏 inset；选中项胶囊指示器 animateDpAsState 滑动 + 图标 Crossfade；selectableGroup + Role.Tab 语义；切换轻触 haptic。
- Tab 内容 AnimatedContent（淡入 + 0.98→1.0 scale）。
- NavHost：main 路由纯淡入淡出；workout/combo/exercise/pr 详情路由 slideIn 1/5 + fade，返回反向；路由、参数与回退行为不变。
- inset 语义：MainScaffold 统一处理 top/horizontal 系统栏 inset 与底部栏高度，Tab 页拿到的 modifier 已含 padding，并通过 consumeWindowInsets 传递已消费状态；Tab 页不再自建 Scaffold 补状态栏 padding（避免双重 padding）。
- AppNav() 与 MainScaffold(...) 签名未变。

## 9. 验收记录

### 9.1 已执行命令与原始输出摘要（2026-10-18）

~~~bash
ANDROID_USER_HOME=/Users/suyi/projects/exercise/.android-home \
  sh /Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh \
  :app:processDebugResources :app:compileDebugKotlin --console=plain
~~~

结果：BUILD SUCCESSFUL in 9s，EXIT=0；资源打包（adaptive icon / monochrome / Splash 主题属性 / values-night）与 Kotlin 编译均通过。
警告：仅 1 条既有弃用（data/db/AppDatabase.kt fallbackToDestructiveMigration）；Common.kt 重写后旧的 menuAnchor 弃用警告消失（计划 §9.1 允许基线降为 1 条），无新增警告。

### 9.2 待 verifier/Lead 真机确认

- 桌面图标：普通壁纸、深色壁纸、主题图标（monochrome）三态截图。
- 冷启动：am start -W 冷启动 ok；启动瞬间无白屏，Splash 深绿底 + 米白标记，首帧背景无跳变。
- 浅色/深色逐页截图确认无双重 padding（状态栏/导航栏不重叠内容）。
- 减少动态：animator_duration_scale = 0 时动画退化为淡入/跳变。
- 60fps 抽查（gfxinfo）与 TalkBack 语义（第 7/8 节实现对应的组件级语义）。

## 10. 约束与回滚

- 未新增依赖、未新增权限、未引入联网资源/字体/位图。
- 写作用域内文件：ui/theme/**、ui/design/**、ui/Common.kt、ui/AppNav.kt、MainActivity.kt、app/src/main/res/**、AndroidManifest.xml、本文件。
- 回滚：备份基线 /Users/suyi/projects/exercise/.scratch/iteration12/backup/src（禁止删除）。
- 已知取舍：Splash 背景在浅/深色均用品牌深绿（识别优先），首帧窗口底色分浅/深色；如用户要求浅色 Splash 用米白底，只需把 values/colors.xml 的 fitplan_splash_background 改为 #F6F7F2 并补浅色标记 drawable。
