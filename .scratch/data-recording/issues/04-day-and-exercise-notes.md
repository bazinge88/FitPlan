# 04 — 当日备注 + 动作备注

**What to build:** 双轨备注：当日备注（天 + 训练者一条，主页清单顶部入口，训练日与休息日皆可写）与动作备注（天 + 动作一条，动作卡展开区入口）；重复输入覆盖；下次训练同一动作时，展开区回显当天之前最近一次的动作备注。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 新增当日备注表（天 + 训练者唯一，重复覆盖）与动作备注表（天 + 动作 + 训练者唯一，重复覆盖，外键级联删除动作），非破坏迁移（以当时库版本号为准追加）
- [x] 主页清单顶部可写/改/清当日备注；休息日同样可写（如「出差」）
- [x] 动作卡展开区可写动作备注；同天同动作一条
- [x] 下次训练该动作时展开区显示上次动作备注；无历史备注不显示
- [x] 与「记录」Tab 自由记录的条目备注互不相干（两套数据、两套入口）
- [x] 切换训练者互不可见；「清空所有数据」连带清理两张表
- [x] 单元测试覆盖覆盖语义/回显口径/多训练者隔离；全绿 + 构建零新增警告

## 实施备注（2026-10-02）

- 数据库 v10→v11：一张迁移建两张表——`day_notes`（date+userId 唯一，无外键）与 `exercise_notes`（date+exerciseId+userId 唯一，外键 exercises.id 级联删除动作，范式同 session_weights）；`MIGRATION_10_11` SQL 与 Room 生成规则逐字符一致（reviewer 复核），迁移正确性按 spec Testing Decisions 由真机覆盖安装验收覆盖（JVM 不测迁移），待办见 HANDOVER。
- 状态构建走唯一路径：`buildWorkoutUiState` 扩为 11 参，带出 `WorkoutUiState.dayNote` 与每动作 `todayNote`/`lastNote`；休息日与训练日两条路径都带当日备注（沿用票 03 教训）。combine 七条流经新增 `WorkoutCoreBundle`（settings/sessionWeights/skips/bodyWeights）+ 扩为六字段的 `WorkoutFlowBundle` 两级载体接入，嵌套 combine 保持五参数重载类型安全；ViewModel 与 WorkoutSessionManager 通知路径逐字同步。
- 回显口径：lastNote = 同动作同训练者、date < 当天 的 max(date) 正文；无历史为 null，当天自己写的备注不算「上次」（有测试钉死）。展开区输入框预填当天备注、下方灰字「上次：X」，lastNote 为 null 时不显示。
- 入口共四处：训练日（开始/结束训练按钮 → 体重行 → 当日备注行 → 清单）与休息日卡（副文案 → 体重行 → 当日备注行）的 `DayNoteRow`/`DayNoteDialog`（多行 TextField，空文本确定 = 清除删行，DayNoteDao.delete(date,userId)）；动作卡展开区 `ExerciseNoteField`（保存按钮空文本禁用 + VM 端 blank 直接 return 双保险——票面未授权动作备注清空入口，只写/改）。
- 与「记录」Tab 的 `LogEntryEntity.note` 两套数据两套入口，本轮零触碰（reviewer grep 确认 ui/log 无任何新表引用）。
- 连带清理沿用票 03 范式：clearAllData 与 deleteUser 均加两表清理（deleteUser 超出票面字面，沿用防孤儿行惯例）。
- 测试基建补缺：FakeLogDao 原为全 TODO 占位，clearAllData 测试需要真实空实现（clearSets()/clearEntries() 两方法，内存 fake 无行可清）；其余 TODO 方法未动。
- JVM 测试 92 个全绿（基线 82 + NotesTest 10 条）、零新增警告（基线 2 条外）。
