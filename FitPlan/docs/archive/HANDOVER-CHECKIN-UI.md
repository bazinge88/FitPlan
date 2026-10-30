# FitPlan 交接文档 · 第二轮迭代（打卡形态重设计 + Google 风格精修）

- 交接日期：2026-09-28
- 项目路径：`/Users/suyi/projects/exercise/FitPlan`
- 前置阅读：`HANDOVER.md`（项目基础信息、技术栈、构建方式，仍然有效）
- 状态：**已完成并于 2026-09-28 通过真机验收（PHB110），本文档已归档至 `docs/archive/`**；最新项目状态以根目录 `HANDOVER.md` 为准。
- 参考截图（已随文档保存）：
  - `docs/reference/01-checkin-checklist-reference.jpg` —— **图1：目标打卡形态**（清单式勾选打卡）
  - `docs/reference/02-google-m3-style-reference.jpg` —— **图2：目标视觉风格**（Google 官方 App 的 M3 卡片风格）

---

## 1. 用户需求（原话要点）

> 我的目的是建立图1一样的打卡方式，但是每个具体的训练动作可以展开多次打卡，从而对应动作次数；UI 风格可参考图2谷歌的截图。

拆解为三条硬性要求：

1. **打卡形态对齐图1**：清单式（checklist）——每个训练动作一行，行首一个圆形勾选圈，点击完成打卡；已完成的行变为「实心圆 + 白色对勾 + 文字置灰」。
2. **动作可展开出多次打卡**：单个动作行可展开，展开后出现多条打卡记录（对应多组），**每一次打卡分别记录该组的完成次数**。
3. **视觉风格对齐图2**：Google 官方 App 的 Material 3 卡片风格——淡色页面底、白色大圆角卡片分组、极简图标与留白。

## 2. 当前实现现状（第一轮迭代成果，本轮的改造基础）

### 2.1 已有功能

- 周计划页（`ui/plan/PlanScreen.kt`）：训练日卡片点击进入训练详情页，卡片尾部编辑按钮打开原排程 BottomSheet。
- 训练详情页（`ui/plan/WorkoutScreen.kt`，路由 `workout/{date}`）：
  - 每个动作一张**可展开/收起卡片**（`animateContentSize`），收起态显示 动作名 + 目标摘要（`setsSummary`）+ 进度「已完成 x/y 组」+ 尾部展开箭头；
  - 展开态：每组一行「第 N 组 + （−）当前次数（+） + **行尾 48dp 圆圈**」；未完成=描边圆圈显示组号，点击打卡；已完成=实心圆显示次数，再点取消；未完成态 stepper 仅本地暂存，打卡时一并写入；已完成态 stepper 直接持久化。
- 数据层（本轮**无需改动**，见 §4）：
  - `set_checkins` 表（Room v2，`MIGRATION_1_2` 非破坏迁移已就绪）：一行 = 某天某组合动作的某一组，字段 `id / date / comboItemId / setIndex / reps`，唯一索引 `(date, comboItemId, setIndex)`，**行存在即该组已完成**，`reps` 为该组完成次数（可 null=只记完成不计次）。
  - `SetCheckinDao`：`upsert`（`@Insert OnConflictStrategy.REPLACE`）、`delete(date, comboItemId, setIndex)`、`observeForDate(date): Flow`、`clearAll`。
  - `WorkoutViewModel`：`combine(dayPlans, combosWithItems, exercises, checkinsForDate)` → `WorkoutUiState`（`items: List<WorkoutItemUi>`，含 `sets: List<WorkoutSetUi(setIndex, reps)>`、`doneCount`、目标字段）。**该 VM 的数据供给已完全满足新交互，建议原样复用。**
- 全局 UI：`Theme.kt` 已启用 Material You 动态取色（绿色 fallback）；各屏卡片已统一 0 elevation + 20dp 圆角 + `surfaceContainerLow` 底色。

### 2.2 与目标形态的差距（本轮要改什么）

| 维度 | 现状 | 目标（图1/图2） |
| --- | --- | --- |
| 勾选圈位置 | 展开后每组的**行尾**右侧 | **行首**左侧（图1 清单式），收起态也要有动作级聚合圈 |
| 收起态形态 | 整卡一块，圈不出现 | 清单行：行首聚合圈 + 动作文案（全部组完成时实心✓+文字置灰，图1 语言） |
| 组行形态 | − / 次数 / + / 圆圈（右侧） | 行首每组的勾选圈（点击打卡）+ 次数记录（stepper 或等价轻量控件可保留，位置与样式重排） |
| 视觉 | 卡片 `surfaceContainerLow` 底、20dp 圆角 | 图2：淡色页面底（`surfaceContainerLow`），**白色卡片（`surface`）+ 更大圆角（24–28dp）**，卡片内条目 + 细分隔线，行首单色图标，留白更充分 |

## 3. 目标交互设计（建议方案，下一轮 agent 可在此基础上微调）

### 3.1 训练详情页（核心改动）

```
┌ 白色大圆角卡片（动作一卡，或按分组一卡多行，见 §3.3）────────┐
│ ○ 标准硬拉 50kg        5*5 · 已完成 2/5 组        ⌄ │   ← 收起态清单行
│   （全部组完成后：●+✓，动作文案置灰，图1 同款）              │
└──────────────────────────────────────────────┘
展开后（卡片内部追加组行，细线分隔）：
│ ○ 第 1 组   8 次（− +）                                  │   ← 每组行首勾选圈
│ ● 第 2 组   已完成 · 8 次（− +）                          │   ← 已打卡组：实心圈✓+置灰
│ ○ 第 3 组   —                                            │
```

- **收起态行首圆圈 = 动作级聚合态**：该动作全部组已打卡 → 实心✓；部分完成 → 可用「实心但半透明/环形进度」或保持描边并在文案里给出 x/y（二选一，倾向简单：描边 + 行内进度文案）；点击圆圈或整行 → 展开/收起（圆圈本身不建议承担批量打卡，避免误触语义不清）。
- **展开态每组行首圆圈 = 该组打卡开关**：点击完成该组（写入 `set_checkins`，次数默认取组合目标 `targetReps`，否则沿用前面组次数，否则 null→显示✓）；再点取消（删行）。
- **完成次数记录**：每组打卡后在其行内展示次数，`−/+` stepper（或点击次数弹小输入框）调整并即时持久化（沿用现有 `WorkoutViewModel.updateReps`）。未完成组 stepper 的暂存逻辑可保留或简化（简化为：打卡后默认写入 targetReps，用户再微调——**推荐简化**，更贴近图1 的轻量心智）。
- **文字置灰**：组已打卡 → 该组文案 `onSurfaceVariant`；动作全部完成 → 动作名置灰（图1 同款）。

### 3.2 视觉规范（图2 → M3 语义色映射，全页统一）

| 图2 观察 | 落地规范 |
| --- | --- |
| 淡蓝灰页面底 | `Scaffold containerColor = colorScheme.surfaceContainerLow`（动态取色下即图2 的淡色底） |
| 白色大圆角卡片、一组一张卡 | 卡片 `containerColor = colorScheme.surface`，`RoundedCornerShape(24.dp)`（列表整体卡可到 28dp），0 elevation、无边框 |
| 卡内多条目 + 细分隔线 | 卡内组行间用 `HorizontalDivider(color = outlineVariant, thickness 1.dp)`，或改用 1dp 高的 `Box`；条目内边距 ≥16dp |
| 行首单色图标/圆圈 | 勾选圈自绘（`Box` + `CircleShape` + `border`/`background`，完成后叠 `Icons.Filled.Check`），不用带阴影组件 |
| 描边胶囊按钮 | 次级操作用 `OutlinedButton`（胶囊形），主操作 `Button` |
| 充足留白、少边框 | 页面水平 16dp，卡片间距 12dp，行高 ≥56dp |

注意：**保持动态取色开启**（这是 Material You 的核心），上述只用语义色，不硬编码 Google 的具体蓝灰色值。

### 3.3 可选增强（非硬性，量力而行）

- **按分组分卡**：图1 有「后链肌群强化 / 垂直推拉+抗旋钮…」小节标题。FitPlan 的动作库自带 `category` 字段——可在组合编辑里沿用动作分类，在训练详情页按分类分卡/加小节标题（`titleMedium`）。若分类普遍为空则跳过此增强，不要为此加新数据。
- 完成组数环形进度（如 Google Fit 的小圆环）替代聚合圈，视觉更 Google，但优先保证 §5 验收项。

## 4. 改造范围与约束

- **建议只改 UI 层**：`WorkoutScreen.kt`（重写布局与交互）、必要时 `PlanScreen.kt` 卡片样式同步到图2 语言；`WorkoutViewModel.kt` 原样复用（若采纳 §3.1 的「简化暂存」则删掉未完成态 stepper 暂存相关代码即可）；`set_checkins` 数据层**零改动**。
- 全局视觉统一：若把图2 的「淡底白卡」落到计划/组合/动作/记录/设置页，需同步修改各屏 `Scaffold` 背景与卡片配色（原第一轮规范中的 `surfaceContainerLow 卡片` 全部改为 `surface 白卡 + 淡色页面底`），一次改齐，避免两套风格并存。
- 硬约束：
  - 不改数据库 schema（v2 已定），不动 `log_entries/log_sets`/导出器；
  - 不破坏既有功能：周计划排程、训练记录增删改、动作/组合管理、设置与清空数据（`clearAllData` 含 `setCheckinDao.clearAll()`）；
  - 遵守 `HANDOVER.md` 的技术栈约束（勿升 AGP/Compose BOM，用 `./gradlew` 构建）；
  - 全部中文文案；只用 `MaterialTheme.colorScheme` 语义色；
  - 若多 agent 并行，按文件划分所有权，同一时刻 ≤3 个。
- 构建与真机验证：
  ```bash
  cd /Users/suyi/projects/exercise/FitPlan
  ./gradlew :app:assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk   # 真机 PHB110 已可用（adb id: 5968de20）
  ```
  真机验收时注意：屏幕实际分辨率 1440×3216，UI 自动化坐标一律以 `uiautomator` 元素树为准，勿按截图目测（截图是缩放显示的）；设备会自动锁屏（滑动锁，`adb shell input keyevent 82` 可解）。

## 5. 验收标准（下一轮完成的判定）

1. 训练详情页：每个动作一行清单式条目，行首圆圈；展开后每个目标组一行、行首圆圈，点击即完成该组打卡（数据写入 `set_checkins`），再点取消。
2. 每组打卡能记录该组完成次数（默认目标次数，可调整），退出重进后组数、次数、完成态完整恢复（Room 持久化）。
3. 动作全部组完成时：聚合圈实心✓、动作文案置灰（图1 同款视觉）；组完成行文案置灰。
4. 整页与全 App 采用图2 风格：淡色页面底 + 白色大圆角卡片（24dp+）+ 卡内细分隔线 + 充足留白，动态取色保持开启。
5. 原有功能无回归：周计划排程/编辑、训练记录、动作/组合管理、设置、清空数据；构建零新增警告。
6. 真机（PHB110）走通上述链路并截图留档（建议存 `docs/verify-iteration2/`）。

## 6. 遗留问题（沿袭第一轮，处理打卡 UI 时顺带留意）

- 编辑组合（clearItems+重插）会重置当日打卡进度（打卡以 `comboItemId` 为键）；本轮不要求解决，但若调整键位需同步迁移逻辑。
- 训练详情页 stepper 快速连点有丢增量竞态（可改 DAO 原子 `UPDATE reps = reps + 1`）。
- `set_checkins` 无外键，被删组合项的孤儿打卡行仅在「清空所有数据」时清理。
- 打卡数据未纳入 Markdown 导出。

## 7. 第一轮迭代改动基线（供 diff 参照的文件清单）

- 新建：`ui/plan/WorkoutScreen.kt`、`ui/plan/WorkoutViewModel.kt`
- 修改：`data/db/{Entities,Daos,AppDatabase}.kt`（v2）、`ui/plan/{PlanScreen,PlanDialogs}.kt`、`ui/AppNav.kt`、`ui/theme/Theme.kt`、`ui/Common.kt`、`ui/exercise/*`、`ui/log/*`、`ui/combo/*`、`ui/settings/{SettingsScreen,SettingsViewModel}.kt`
- 第一轮真机验收截图：已随归档迁移至 `docs/verify/iteration1/`（原 `FitPlan/.verify-new/`）。

---

## 8. 完成记录（2026-09-28 归档时补记）

- 交付内容：
  - `ui/plan/WorkoutScreen.kt` 重写为清单式打卡：行首打卡圈/动作级聚合圈、展开后逐组打卡与 −/+ 次数微调（即时持久化）、完成行与全部完成动作置灰；打卡默认次数由顶层纯函数 `defaultRepsFor`（WorkoutViewModel.kt）解析：目标次数 → 前一组实际次数 → 只记完成（采纳 §3.1 推荐简化，未打卡态暂存 stepper 已移除）；
  - 全 App 视觉统一到图2：页面底 `surfaceContainerLow`、卡片 `surface` + 24dp 圆角 + 0 elevation、卡内 `outlineVariant` 1dp 分隔线、卡片间距 12dp；涉及 `AppNav.kt`、`PlanScreen.kt`、`ComboListScreen.kt`、`ComboEditScreen.kt`、`ExerciseListScreen.kt`、`ExerciseEditScreen.kt`、`LogScreen.kt`（SettingsScreen 无卡片容器，零改动）；动态取色保持开启；
  - 数据层零改动：`set_checkins`（Room v2）与 `WorkoutViewModel` 数据供给原样复用。
- 测试：新增 JVM 单元测试 9 例（`app/src/test/`：`FakeDaos.kt` + `ui/plan/WorkoutViewModelTest.kt`），`./gradlew :app:testDebugUnitTest` 全绿（tests=9, failures=0）。
- 构建：`./gradlew :app:assembleDebug` 通过，APK `app/build/outputs/apk/debug/app-debug.apk`；全量重编译仅 2 条既有弃用警告（`AppDatabase.kt`、`Common.kt`，第一轮遗留基线），本轮改动零新增警告。
- 真机验收：PHB110（Android 16）覆盖打卡主链路、聚合置灰、持久化恢复、全 App 风格与动态取色、既有功能回归抽查，**用户实测确认无问题**；本次未截图存档，验收记录见 `docs/verify/iteration2/README.md`。
- 遗留事项（移交后续迭代，§6 原样有效）：编辑组合（clearItems+重插）重置当日打卡；已完成态 stepper 快速连点仍有丢增量竞态（可改 DAO 原子 `UPDATE reps = reps + 1`）；孤儿打卡行仅「清空所有数据」时清理；打卡数据未纳入 Markdown 导出。
