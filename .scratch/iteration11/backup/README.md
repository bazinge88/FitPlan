# 健身计划 FitPlan

本地、离线使用的 Android 健身周计划与记录应用（中文界面）。核心是**安排整个训练计划**：动作库 → 训练组合 → 周计划（周一起始）→ 训练详情页逐组打卡 → 可选训练记录 → Markdown 导出。无云同步、无账号、无联网。

## 功能一览

- **主页**：打开即见今天——训练日直接展示今日计划（清单式逐组打卡、次数微调、重量调整一应俱全），休息日显示「今天休息」并引导到「计划」页安排。
- **动作库**：创建 / 编辑 / 删除动作，可分类（如推、拉、腿）也可自定义；每个动作可勾选需要记录的字段（组数 / 次数 / 重量 / 时长），未启用的字段在各处均不显示。每个动作可设**根源重量**（当前重量）：添加到组合时自动填入目标重量，改动后可一键同步到所有包含此动作的组合。
- **训练组合（动作组）**：把若干动作固定成一组；支持命名、编辑、复制、删除、排序；每个动作可设置目标组数 / 次数 / 重量 / 时长（可留空）。选动作用对话框完成：搜索、按分类筛选、批量多选添加，已选动作有「已添加」标记。
- **周计划**：以周一为一周第一天；多周横向翻页查看；每天可设为训练日（绑定一个训练组合）或休息日；支持复制上周计划、从任意日期复制到某天、复制前一天、清除安排。
- **内置自动排程**：
  - **三分化**：周一 / 周三 / 周五 三个槽位，各绑定一个训练组合，其余休息；
  - **练一休一**：隔天训练（一、三、五、日），勾选的组合按顺序轮流循环。
  - 自动生成后写入当周，可随意手动修改。
- **训练打卡**：训练详情页按清单逐组打卡（行首圆圈，Google M3 白卡风格），每次打卡记录该组完成次数（默认目标次数，可微调）并本地持久化；动作全部组完成自动聚合置灰。动作卡展开后可快捷调整重量（−/+ 步进 2.5 或点数值精确输入，支持负数与小数），修改写回组合目标重量、下次训练沿用；精确输入时若该动作出现在多个组合，可勾选「同步到所有组合」一次更新全部；编辑组合不会丢失已变动作的打卡历史。
- **训练记录（可选）**：按日期记录动作 + 多组数据（组数 / 次数 / 重量 / 时长，字段由动作配置决定）+ 备注；负数重量表示辅助重量（如辅助引体向上 `-20`），`0` 表示自重；不记录完成度、不做统计图表。
- **Markdown 导出**：一键导出为结构化 Markdown（设置 / 动作库 / 训练组合 / 周计划 / 训练打卡 / 训练记录），范围可选全部 / 指定周 / 日期范围；重量保留原始符号（负数原样导出），方便交给 AI 分析。
- **设置**：单位 kg / lb（默认 kg）、默认记录字段、导出、清空数据（二次确认）、关于。

## 明确不做（与需求一致）

云同步、账号登录、Health Connect、Wear OS、训练提醒、组间休息计时器、社交、成就、排行榜、AI 生成计划、完成度 / PR / 图表统计、自动计算实际负重。

## 技术栈

| 项 | 选择 |
| --- | --- |
| 语言 / UI | Kotlin 2.2 + Jetpack Compose（Material 3） |
| 架构 | MVVM + Room（无第三方网络、无 DI 框架） |
| 构建 | AGP 8.13.2 / Gradle 8.13 / JDK 17+ |
| SDK | minSdk = targetSdk = compileSdk = **36（Android 16）** |
| 存储 | Room 本地数据库（`fitplan.db`），完全离线 |
| 权限 | 无任何权限（Manifest 未声明 INTERNET） |

## 构建与运行

### 环境要求

- JDK 17 或 21
- Android SDK Platform 36（Build-Tools 35/36 均可）
- Android Studio Ladybug 及以上（可选，也可纯命令行）

### 命令行构建

```bash
cd FitPlan
./gradlew :app:assembleDebug          # Debug APK
./gradlew :app:assembleRelease        # Release APK（未配置签名，用于本地调试发布流程请自行配置 signingConfig）
./gradlew :app:testDebugUnitTest      # JVM 单元测试（WorkoutViewModel 等 9 例）
```

产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

### 安装到设备

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

或用 Android Studio 打开 `FitPlan` 目录，直接 Run。

> 注意：minSdk 为 36，只能安装在 Android 16 及以上的设备/模拟器上。

## 项目结构

```
FitPlan/
├── app/src/main/java/com/fitplan/app/
│   ├── FitPlanApp.kt            # Application + 全局依赖图
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── db/                  # Room：实体、DAO、数据库（v3，含 set_checkins 打卡表与动作根源重量）
│   │   └── model/Schemes.kt     # 内置排程方案（三分化 / 练一休一）
│   ├── export/MarkdownExporter.kt  # 结构化 Markdown 导出
│   ├── util/TimeUtil.kt         # 周一起始的周计算、重量格式化（保留符号）
│   └── ui/
│       ├── home/               # 主页（今日计划直出与打卡）
│       ├── plan/                # 周计划（周翻页 / 单日编辑 / 复制 / 自动排程）+ 训练详情页清单式打卡
│       ├── combo/               # 训练组合（列表 / 编辑器）
│       ├── exercise/            # 动作库（列表 / 编辑器）
│       ├── log/                 # 训练记录（按日查看 / 条目编辑）
│       ├── settings/            # 设置（单位 / 默认字段 / 导出 / 清空 / 关于）
│       └── theme/               # Material 3 主题（动态取色）
├── app/src/test/java/com/fitplan/app/   # JVM 单元测试（FakeDaos + WorkoutViewModelTest）
├── docs/                        # 归档：archive（历史交接）/ reference（设计参考）/ verify（验收截图）
└── app/src/main/res/            # 图标、主题、字符串
```

## 数据模型概要

- `exercises`：动作（名称、分类、启用字段）。
- `combos` / `combo_items`：训练组合及组内动作 + 各项目标（外键级联删除）。
- `day_plans`：按 epoch day 主键存每日安排（休息 / 训练 + 组合）。
- `log_entries` / `log_sets`：训练记录条目与每组数据（重量 Double，可负 / 可小数）。
- `set_checkins`：逐组打卡（`date + comboItemId + setIndex` 唯一；行存在即该组完成，`reps` = 完成次数，可 null）。
- `app_settings`：单行设置（单位、默认字段）。

## 后续发布到应用商店

- 已含自适应图标与正式版构建类型；发布前请：
  1. 配置正式签名（`signingConfigs`）；
  2. 视需要开启 `isMinifyEnabled` 并补充混淆规则；
  3. 替换应用名 / 包名（`applicationId = com.fitplan.app`）为自有品牌。
