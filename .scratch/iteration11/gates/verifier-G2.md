# G2 测试前门（verifier 最终，2026-10-18 18:32）

- 阶段：Phase 2 独立验证 / G2 测试前门
- 结论：**GO**

## 1. 阶段结论

修复 `TransactionRollbackTest` 后，verifier 带批准的环境前置连续 2 次执行计划命令 `clean :app:testDebugUnitTest :app:assembleDebug`，均 `BUILD SUCCESSFUL`（18s / 17s）：tests=131、failures=0、errors=0（20 个测试类，预期 ≥127）；APK 63,249,629 bytes；编译警告 2 条与基线完全一致（无新增）。verifier 新增 `Iteration11AcceptanceTest` 4/4 通过。AC-01..AC-13、AC-15 PASS；AC-14 仅剩 Lead T-D1 文档收口（verifier 部分已完成）。

## 2. 风险判断

- R1 无真机（中/高）：UI/无障碍/Toast 仅静态审查 + 纯函数测试，真机清单交用户（计划 §9.3）。
- R2 Fake 与 Room 语义差异（中/中）：事务与级联行为未在真库/真机验证。
- R12 构建沙箱限制（已规避）：G3 及后续构建统一前置 `ANDROID_USER_HOME=/Users/suyi/projects/exercise/.android-home`（Lead 18:31 批准；debug.keystore 与既有 APK 签名一致）。Kotlin daemon fallback 编译属环境噪声。
- 无生产代码 P0/P1 未决项。

## 3. 下一步行动

1. Lead 执行 T-D1：更新 HANDOVER/README、产出 iteration11-report、填写计划 §11 G1-G3 门记录。
2. Lead 产出 `FitPlan-iteration11-debug.apk` 并执行 G3（构建命令同样带 ANDROID_USER_HOME 前置）。
3. 用户按计划 §9.3 真机验收清单验收；无真机项记为残余风险。

## 4. 证据

- 最终命令：`ANDROID_USER_HOME=/Users/suyi/projects/exercise/.android-home sh /Users/suyi/projects/exercise/.scratch/iteration11/with-gradle-lock.sh clean :app:testDebugUnitTest :app:assembleDebug --console=plain`
- 结果：第一轮 `BUILD SUCCESSFUL in 18s`、第二轮 `BUILD SUCCESSFUL in 17s`，exit=0；`131 tests completed`；tests=131/failures=0/errors=0。
- APK：`FitPlan/app/build/outputs/apk/debug/app-debug.apk` 63,249,629 bytes（18:31）。
- 警告：2 条既有弃用（`AppDatabase.kt:270` fallbackToDestructiveMigration、`Common.kt:103` menuAnchor），零新增。
- 约束：DB v11、10 条迁移、Entities.kt 未改；build.gradle 仅 +buildConfig；39 项 src 变更全部在计划 §6.1 写作用域；修复窗口（18:25 后）仅 TransactionRollbackTest.kt 变更。
- 测试 XML：`FitPlan/app/build/test-results/testDebugUnitTest/`（20 个）。
- 详细记录：`docs/iteration11-verification.md` §6.1-§6.5。

- 评审人：verifier（独立验证，不修改生产代码）

- 首轮 NO-GO（18:28）已关闭：唯一阻断项 TransactionRollbackTest 顺序依赖由 logic-dev 修复，verifier 连续 2 次 clean 复验全绿。
