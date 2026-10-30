# FitPlan（健身计划）

本地、离线使用的中文 Android 健身周计划与记录应用。核心链路是**安排并执行整个训练计划**：

> 动作库 → 训练组合 → 周计划（周一起始）→ 今日清单式逐组打卡 → 可选训练记录 → PR 纪录 → Markdown 导出

无账号、无云同步、无联网（Manifest 未声明 INTERNET 权限）。第十二轮起采用 FitPlan 自有设计系统（浅/深色、动态取色、统一动效、品牌图标与启动画面）。

- 技术栈：Kotlin 2.2 + Jetpack Compose（Material 3），单模块 MVVM，Room 本地库（DB v11），AGP 8.13 / Gradle 8.13 / compileSdk 36（Android 16，minSdk = 36）
- 测试：JVM 单元测试 131 个用例全绿（JUnit4 + kotlinx-coroutines-test）
- 权限：仅 POST_NOTIFICATIONS（训练会话常驻通知）

完整功能说明、重量模型（目标 / 当次 / 每组 / PR@n）、数据模型、构建命令与项目结构，见 **[`FitPlan/README.md`](FitPlan/README.md)**。

## 快速开始

```bash
cd FitPlan
./gradlew :app:assembleDebug        # 构建 Debug APK
./gradlew :app:testDebugUnitTest    # 运行 JVM 单元测试
```

产物：`FitPlan/app/build/outputs/apk/debug/app-debug.apk`。要求 JDK 17+、Android SDK Platform 36。
安装设备需 Android 16+（minSdk = 36）。

## 仓库结构

```text
.
├── FitPlan/            # Android 应用工程（源码、测试、HANDOVER.md、详细 README）
├── docs/               # 迭代计划 / 报告 / 验证记录、ADR、领域与流程文档
│   └── adr/            # 架构决策记录
├── .scratch/           # 本地 issue tracker（各 feature 下的 issues / spec）
├── CONTEXT.md          # 领域术语（ubiquitous language）
├── AGENTS.md           # 工作区约定（协作、issue、triage）
└── requirements.rtf    # 需求文档
```

## 文档索引

| 文档 | 路径 | 说明 |
| --- | --- | --- |
| 详细 README | [`FitPlan/README.md`](FitPlan/README.md) | 功能全集、重量模型、数据模型、构建与结构 |
| 交接文档 | [`FitPlan/HANDOVER.md`](FitPlan/HANDOVER.md) | 最新状态、验证历史、待办、归档地图（以此为准） |
| 领域术语 | [`CONTEXT.md`](CONTEXT.md) | 打卡、重量、会话等术语定义 |
| 架构决策 | [`docs/adr/`](docs/adr/) | 组合条目为同步根源 / 免前台服务通知 / 每组重量快照 / 跨 DAO 事务边界 |
| 迭代记录 | [`docs/`](docs/) | 第十一、十二轮计划、报告与验证 |

## 许可

个人项目，暂未附开源许可证。如需引用或复用，请先联系作者。
