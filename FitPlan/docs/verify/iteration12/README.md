# 第十二轮真机截图索引（FitPlan iteration12）

- 设备：PHB110 / Android 16 / API 36（adb 5968de20；1440×3216，density override 640 → 360×804dp）
- 最终 APK：\`/Users/suyi/projects/exercise/FitPlan-iteration12-debug.apk\`（63,482,439 bytes；sha256 \`b96b029f5e34449cfa3d89e7cd120dfcd41d3adf5a563f416065de7b8fb2d6fe\`）
- 说明：\`verify-*\` 为 verifier 独立采集（最终 APK 为主）；\`final-*\` 为 Lead 冒烟采集。全部为前台 com.fitplan.app 时截图；未写入用户数据（对话框打开后取消）。
- 环境异常：19:36 前后设备被真实用户拿起（微信/Steam 前台），verifier 一次误触落在非 FitPlan 前台、未触及 FitPlan 数据；受限项见 \`docs/iteration12-verification.md\` §5.6/§11。

## 浅色（Lead 冒烟 + verifier 独立）

| 文件 | 内容 |
| --- | --- |
| \`final-01-home-light.png\` | 主页（休息日：品牌标记 + 今日记录 + 去计划页安排 + 6 Tab） |
| \`final-02-plan-light.png\` | 计划页（大标题/周区间、复制上周/自动排程、周切换、7 天卡、今天高亮） |
| \`final-03-workout-light.png\` | 训练详情折叠态（TopBar/大标题/进度 hero/动作卡） |
| \`final-04-workout-expanded-light.png\` | 训练详情展开态（重量行、组圈 48dp、次数 stepper、组重量 chip） |
| \`final-05-combo-light.png\` | 组合列表（大标题、摘要、更多菜单、ExtendedFAB） |
| \`final-06-combo-bottom-light.png\` | 组合列表滚到底（最后一项不被 FAB 遮挡） |
| \`final-09-exercise-light.png\` | 动作库（分类分组、字段徽标、PR 入口、FAB） |
| \`final-10-exercise-bottom-light.png\` | 动作库滚到底（FAB 不遮挡） |
| \`final-20-weight-dialog-light.png\` | 主页体重输入对话框 |
| \`final-21-daynote-dialog-light.png\` | 主页当日备注对话框 |
| \`verify-01-home-light.png\` | 主页（verifier 独立） |
| \`verify-02-plan-light.png\` / \`verify-02b-plan-light-after-dialogs.png\` | 计划页（verifier 独立） |
| \`verify-03-plan-daysheet-light.png\` | 单日编辑 BottomSheet |
| \`verify-04-plan-autoschedule-light.png\` | 自动排程 Dialog |
| \`verify-05-plan-copyweek-light.png\` | 复制上周确认 Dialog |
| \`verify-06-workout-collapsed-light.png\` / \`verify-07-workout-expanded-light.png\` | 训练详情折叠/展开 |
| \`verify-08-workout-weight-dialog-light.png\` / \`verify-09-workout-setweight-dialog-light.png\` | 当次重量 / 组重量弹窗 |
| \`verify-10*.png\` / \`verify-11-workout-skip-chips-light.png\` | 底部动作展开、标逃/取消逃标 chip |
| \`verify-12-combo-list-light.png\` / \`verify-12b-combo-list-light.png\` | 组合列表 |
| \`verify-14-combo-menu-light.png\` | 组合更多菜单（编辑/复制/删除） |
| \`detail-02-workout-expanded.png\` | 早期展开态对照（修复前基线，非验收稿） |

## 深色（Lead 冒烟 + verifier 独立）

| 文件 | 内容 |
| --- | --- |
| \`final-30-home-dark.png\` | 主页深色 |
| \`final-31-plan-dark.png\` | 计划页深色 |
| \`final-32-workout-expanded-dark.png\` | 训练详情展开深色 |
| \`final-33-combo-dark.png\` | 组合列表深色 |
| \`final-34-exercise-dark.png\` | 动作库深色 |
| \`final-35-pr-dark.png\` | PR 记录深色 |
| \`final-36-log-dark.png\` | 训练记录深色 |
| \`final-37-settings-dark.png\` | 设置深色 |
| \`verify-21-home-dark.png\` / \`verify-22-plan-dark.png\` | 最终 APK 独立复测深色 |

## 大字体 / 减少动态 / 最终 APK / 品牌

| 文件 | 内容 |
| --- | --- |
| \`final-40-home-font13.png\` | 主页 font_scale=1.3 |
| \`final-41-plan-font13.png\` | 计划页 1.3 |
| \`final-42-settings-font13.png\` | 设置 1.3 |
| \`final-43-combo-font13.png\` | 组合列表 1.3 |
| \`final-44-workout-expanded-font13.png\` | 训练详情展开 1.3（组行 FlowRow 换行 fallback） |
| \`verify-23-home-font13.png\` / \`verify-24-plan-font13.png\` | 最终 APK 独立大字体复测 |
| \`final-50-home-reducedmotion.png\` | animator_duration_scale=0 主页 |
| \`final-51-workout-reducedmotion.png\` | 减少动态训练详情 |
| \`verify-25-home-reducedmotion.png\` / \`verify-26-workout-reducedmotion.png\` | 最终 APK 独立减少动态复测 |
| \`final-60-home-finalapk.png\` | 最终 APK 冷启动首页 |
| \`final-61-workout-skipchip-finalapk.png\` | 最终 APK 训练详情（最后一卡展开） |
| \`final-62-launcher-after-verify.png\` | 验收后回到桌面 |
| \`final-splash.png\` | 启动 Splash（品牌深绿底 + 米白组环/对勾） |
| \`final-appinfo-icon.png\` | 应用详情页品牌图标（adaptive icon） |
| \`final-launcher-home.png\` | 桌面/壁纸（动态取色来源参考） |

## 原始证据（非截图）

- 构建日志：\`/Users/suyi/projects/exercise/.scratch/iteration12/verify/verify-build-final.log\`、\`/Users/suyi/projects/exercise/.scratch/iteration12/gates/lead-build-final.log\`
- uiautomator dump / touch targets：\`/Users/suyi/projects/exercise/.scratch/iteration12/verify/device/\`
- gfxinfo：\`/Users/suyi/projects/exercise/.scratch/iteration12/verify/device/gfxinfo-scroll.txt\`、\`gfxinfo-tabswitch.txt\`
- 对比度：\`/Users/suyi/projects/exercise/.scratch/iteration12/verify/contrast.out\`
