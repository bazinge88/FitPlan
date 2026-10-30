# 0003 — 每组重量快照式录入，PR@n 精确统计

真机实测发现同一动作同一天内各组重量常逐组波动，「每天每动作一个当次重量」（`session_weights`，date+exerciseId 唯一）无法归属到具体一组。此前 Q16 曾拍板近似口径——打卡组次数 × 当天当次重量逐组近似匹配取最大（同一天中途改重量时，早先的组按当天最终重量近似归属）。2026-10-01 用户实测后改拍板为**快照式**：打卡时把该动作当天当次重量快照进该组（`set_checkins.weight`），PR@n 随之改为**精确口径**，Q16 近似口径作废。

打卡时把该动作当天当次重量快照进该组：`set_checkins` 新增 `weight` 列（可 null=未记重量，只记次数）；没记过→weight=null；取消该组→调重量→重打=重快照。两条打卡路径（主页 `WorkoutViewModel.checkIn` 与通知 `SessionActionReceiver.quickCheckIn`）必须同源同值——都从 `session_weights`（date+exerciseId）解析。PR@n 精确口径：每组真实重量取最大（(exerciseId, reps) 分组），首达=达成最大重量的最早日期，只计 reps 与 weight 都非空的组。DB v5→v6：`ALTER TABLE set_checkins ADD COLUMN weight REAL`（`MIGRATION_5_6`）——v5 已上真机，历史迁移禁止改动，只能新增迁移。录入交互零新增：沿用重量行（记当次重量）+ 打卡圆圈，快照发生在打卡动作本身。旧数据不回填。

## Consequences

- 旧数据与未记重量的组不计 PR（无重量可归属）；旧数据不回填。
- reps=null（只记完成）的组其快照重量不进导出，导出仍显示「只记完成」。
- 同一动作同一天中途改重量：各组各记各的快照，天然精确归属。
- 取消该组重新打卡 = 重快照，纠错无需额外交互。
- `set_checkins` 再加新列必须 bump v7 并写 `MIGRATION_6_7`（勿再犯 v5 迁移 `started_at` 驼峰坑；`weight` 全小写无此问题）。
