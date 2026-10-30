# G3 发布前门独立一致性复核（verifier，2026-10-18 18:36）

- 阶段：发布前独立复核（只读，不改文件；仅本记录为新写）
- 结论：**GO（自动化部分）**；唯一 CONDITIONAL：真机验收由用户执行（本机无设备）

## 1. 阶段结论

对 T-D1 收口后的交付物做独立一致性复核，四项全部通过：

| # | 复核项 | 结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 交付 APK 存在性/字节数/sha256 | **PASS** | `FitPlan-iteration11-debug.apk` 63,249,629 bytes；sha256 `c2185560dc179829d2e28e0025420bfc4bc179311033f87b8eb67023ca3976d9`，与 Lead 声明逐字节一致；与构建产物 `FitPlan/app/build/outputs/apk/debug/app-debug.apk` sha256 完全相同 |
| 2 | Lead 最终构建日志之后源码冻结 | **PASS** | `find FitPlan/app/src -newer .scratch/iteration11/gates/lead-final-build.log -type f` 输出为空；最新源码 `TransactionRollbackTest.kt` 18:28:50，早于日志 18:32 |
| 3 | 文档与计划一致性 | **PASS（1 项轻微同步项，见 §1.1）** | HANDOVER/README/iteration11-report/plan §11 + gates：131/0/0、APK 字节数与哈希、DB v11、无新增依赖、G0-G3 记录齐全，见 §2 |
| 4 | AC-14 可转 PASS | **PASS** | 四份文档均已更新且字段互相一致；plan §11 G0-G3 四段记录齐全；gates 目录 G0/G1/G2/G3 + verifier-G2 + lead-final-build.log 齐全 |

### 1.1 唯一轻微同步项（不阻断 GO）

`docs/iteration11-verification.md` §1 矩阵 AC-14 行仍为「进行中（verifier 部分完成，待 Lead T-D1）」（该文件最后更新 18:31，早于 T-D1 收口）。按本轮只读要求 verifier 未修改该文件；AC-14 的证据已齐备（见 §2.4），建议 Lead 收口时把该行改为 PASS，或授权 verifier 更新。该同步项不影响交付物与 G3 结论。

## 2. 文档一致性抽查明细（只读）

### 2.1 测试数与构建证据

- Lead 最终构建日志 `.scratch/iteration11/gates/lead-final-build.log`（18:32）：`BUILD SUCCESSFUL in 15s`、`48 actionable tasks: 47 executed, 1 up-to-date`、`warning:` 计数 **2**（仅既有 `AppDatabase.kt:270` fallbackToDestructiveMigration 与 `Common.kt:103` menuAnchor）。
- 该次构建生成的 test-results XML（20 个类，timestamp 2026-10-18T10:32:19Z）汇总：**tests=131 / failures=0 / errors=0**；其中 `TransactionRollbackTest 2/0/0`、`Iteration11AcceptanceTest 4/0/0`。
- 文档表述一致：HANDOVER「131 个 JVM 单元测试与全量构建」「tests=131, failures=0, errors=0」；README「迭代 11 起 131 个用例全绿」；report「106 → 131（20 个类，failures=0 / errors=0）」；plan §11 G2/G3「131 tests / 0 failures / 0 errors」；gates G2/G3 同口径。

### 2.2 APK 字节数与哈希一致性

声明的 `63,249,629` 字节与 sha256 `c2185560...76d9` 在以下位置与实际文件一致、无矛盾：
- `FitPlan/HANDOVER.md`：2 处哈希、2 处字节数（§4 第十一轮块、§4 产物行）；
- `docs/iteration11-report.md`：2 处哈希、2 处字节数（§1 TLDR、§5.1）；
- `docs/iteration-plan.md` §11 G3：1 处哈希、2 处字节数；
- `.scratch/iteration11/gates/G3.md`：1 处哈希、1 处字节数；
- `FitPlan/README.md` 不含哈希（只指向报告与产物路径），无冲突。

### 2.3 约束复核

- DB：`AppDatabase.kt:29 version = 11`；`object : Migration` 计数 **10**；`Entities.kt` 与 backup diff 为空 → 无新表/列/迁移。
- 依赖：`app/build.gradle.kts` 与 backup 的 `dependencies { ... }` 块 diff 为空；全文件唯一差异为 `buildConfig = true`（T-F6 允许）。HANDOVER 未单独复述「无新增依赖」，但 report §1/§6、plan §11 G1/G3、gates G1/G3 均明确记录，且实际 diff 为空，无矛盾。
- 写作用域：G2 已核 39 项 src 变更全部在计划 §6.1；本轮复核源码冻结（§1 第 2 项），无新增变更。

### 2.4 文档与门记录完整性（AC-14）

- `FitPlan/HANDOVER.md`：更新日期 2026-10-18、状态「十一轮迭代完成」、§4 第十一轮块（测试 131、APK 字节/哈希）、真机待办、归档地图含 verification/report；DB v11 明示。
- `FitPlan/README.md`：按现状重写（六 Tab、重量模型、记录扩展、131 用例、构建环境含 `GRADLE_USER_HOME`/`ANDROID_USER_HOME` 工作区路径与锁脚本）。
- `docs/iteration11-report.md`：目标达成表、编排与阶段门、改动摘要、验证结果（131/0/0、APK 字节/哈希、警告 2）、风险、遗留、G3 门、后续建议。
- `docs/iteration-plan.md` §11：G0/G1/G2/G3 四段齐全（阶段结论/风险判断/下一步行动/证据）；G2 记录首轮 NO-GO→修复后 GO；G3 记录 GO（自动化部分）与 APK 哈希。
- `.scratch/iteration11/gates/`：`G0.md`、`G1.md`、`G2.md`、`G3.md`、`verifier-G2.md`、`lead-final-build.log` 齐全；`docs/adr/0004-cross-dao-transaction-boundary.md` 存在（18:28）。

## 3. 风险判断

- 残余风险 R1：无真机，UI 交互/无障碍/Toast/跨天刷新/真实 Room 事务与通知行为仅静态审查 + JVM 纯函数测试；已列入 HANDOVER §5 真机验收清单与计划 §9.3。
- 残余风险 R2：Fake 与 Room 语义差异（事务/级联）仍建议真机「保存瞬间强杀后重进」抽查。
- 未选问题 L09/L10/L11/L13/L14/L17/L21/L22 均为 P2/P3，已记账并带方案。
- 构建环境补丁 `.gradle-home`/`.android-home` 已获 Lead 批准并写入 README/report/verification；不参与源码交付。
- 文档级同步项：verification 文档 AC-14 行待同步（见 §1.1），不阻断。

## 4. 下一步行动

1. Lead 将 `docs/iteration11-verification.md` §1 AC-14 行更新为 PASS（或授权 verifier 更新）；其余交付物无需改动。
2. 交付 `FitPlan-iteration11-debug.apk` 与真机验收清单给用户，按 HANDOVER §5 / 计划 §9.3 执行真机验收。
3. 真机发现项进入下一轮；未选 P2/P3 问题候选化。

## 5. 证据（命令与原样输出摘要）

~~~bash
# APK
ls -la FitPlan-iteration11-debug.apk
# -rw-r--r-- 1 suyi staff 63249629 Oct 18 18:32 FitPlan-iteration11-debug.apk
shasum -a 256 FitPlan-iteration11-debug.apk
# c2185560dc179829d2e28e0025420bfc4bc179311033f87b8eb67023ca3976d9
shasum -a 256 FitPlan/app/build/outputs/apk/debug/app-debug.apk
# c2185560dc179829d2e28e0025420bfc4bc179311033f87b8eb67023ca3976d9（与交付 APK 同哈希）

# 源码冻结
find FitPlan/app/src -newer .scratch/iteration11/gates/lead-final-build.log -type f
# （空输出）

# 构建日志
tail -3 .scratch/iteration11/gates/lead-final-build.log
# BUILD SUCCESSFUL in 15s / 48 actionable tasks: 47 executed, 1 up-to-date
grep -c 'warning:' .scratch/iteration11/gates/lead-final-build.log
# 2

# 测试计数（Lead 最终构建产出的 20 个 XML）
grep -h 'tests=' FitPlan/app/build/test-results/testDebugUnitTest/*.xml | ... | awk
# tests=131 failures=0 errors=0

# 约束
grep -n 'version\s*=' FitPlan/app/src/main/java/com/fitplan/app/data/db/AppDatabase.kt
# 29:    version = 11,
grep -c 'object : Migration' FitPlan/app/src/main/java/com/fitplan/app/data/db/AppDatabase.kt
# 10
diff -u <(sed -n '/^dependencies {/,/^}/p' .scratch/iteration11/backup/app-build.gradle.kts) <(sed -n '/^dependencies {/,/^}/p' FitPlan/app/build.gradle.kts)
# （空输出，exit=0；全文件仅 +buildConfig = true）
~~~

- 评审人：verifier（独立验证，本轮只读，仅本记录为新写）
- 结论：**GO（自动化部分）**；唯一 CONDITIONAL：真机验收由用户执行。
