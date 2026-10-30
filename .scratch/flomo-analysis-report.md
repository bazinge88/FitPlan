# flomo (浮墨笔记) Android 客户端 · 完整逆向分析报告

> 分析对象：`flomo浮墨笔记.apk`（用户自备安装包，取自 UU 远程传输目录）
> 分析日期：2026-10-01 · 分析环境：macOS + Android 15 模拟器（AVD `flomo_test`，root）
> 方法：静态 Manifest/资源解析 → 模拟器运行时观察 → root 内存 carve 脱壳 → jadx 反编译源码审查

---

## 1. 基本信息

| 项 | 值 |
|---|---|
| 包名 | `com.flomo.app` |
| 版本 | 5.4.3（versionCode 504003），2026-07-03 构建 |
| SDK 范围 | minSdk 24（Android 7.0）→ targetSdk 35 |
| ABI | arm64-v8a / armeabi-v7a（+ armeabi 兼容壳） |
| 大小 | 51 MB（3841 个条目，res 占 23MB） |
| 加固 | 360 加固（`libjiagu.so` + `com.stub.StubApp` 壳类，真实代码运行时解密） |
| 官方域名 | flomoapp.com（产品）/ v.flomoapp.com（Web 视图）/ help.flomoapp.com（帮助） |

## 2. 技术架构

原生 Android（Kotlin + Java 混合，非 Flutter/RN），模块划分清晰：

```
com.flomo.app
├── api/          Retrofit 接口层（MemoAPI/UserAPI/TagAPI/PayAPI/ContourApi…，共 18 个文件）
├── data/         ObjectBox 实体 + 网络模型（net/ 子包）
├── ui/           activity / view / fragment（88 个 Activity 声明在 Manifest）
├── vm/           ViewModel 层（MVVM）
├── widget/       12+ 种桌面小组件
├── service/      LocalNotificationService、RecordingService
├── floatWindow/  悬浮速记窗
├── push/         华为/小米/OPPO/vivo/荣耀 五通道推送封装
└── wxapi/        微信登录/分享回调
```

**本地存储（离线优先）**
- 主数据库：**ObjectBox**（`files/objectbox/data.mdb`，实体含 Memo、Tag、Link、StoreFile、MemoAction/TagAction（操作日志）、MemoContentHistory（内容历史）、MemoRevision（服务端修订）、MemoSlug、TagSort、HistoryDraft、AIFindHistory 等 15+ 实体）
- KV：**MMKV**（常量/配置）+ **Hawk**（AES 加密偏好：access token、协议同意状态、小组件事件等）
- 离线策略：本地先写库，`memo/updated/` + `latest_slug` 增量同步；断网时走 OkHttp 20MB 缓存 + `only-if-cached` 兜底（最长 28 天 stale）

**网络栈**：OkHttp + Retrofit（含 RxJava CallAdapter），图片 Fresco，下载 OkDownload，拼音搜索 pinyindb。

## 3. 脱壳过程（零下载方案）

1. 360 加固在运行时把真实 dex 解密进内存（磁盘上 `.jiagu/` 只有壳库，是虚拟路径）。
2. 模拟器 root 后直接读 `/proc/<pid>/mem`：按 `/proc/<pid>/maps` 逐区域 `dd` dump，扫描 `dex\n03x` 魔数，按 dex header 的 `file_size` 切割、SHA-256 去重。
3. 得到 8 个 dex，其中 5 个真实业务 dex（合计约 38MB，6500+ 类），jadx 反编译成功（7095 个 .java，仅 15 处错误）。
4. 产物已备份：`.scratch/flomo-unpacked/`（dex + jadx 源码）。

未使用 frida（避免下载大文件），全程走 USB adb。

## 4. API 全景

### 4.1 环境与鉴权（`RetrofitAdapter.java`）

| 环境 | Base URL |
|---|---|
| 正式 | `https://flomoapp.com/api/v1/`、`/api/v2/` |
| 测试 | `https://stage.flomoapp.com/api/v1/`、`/api/v2/`（由 MMKV `IS_TEST_ENVIRONMENT` 开关切换） |
| AI 搜索 | `https://flomoapp.com/api/v1/search`（POST，SSE 流式返回） |
| AI 反馈 | `https://chat.flomoapp.com/v1/search/{chatId}/feedback` |
| 自建统计 | `https://analytica.flomoapp.com/api/v1/event/report`（AppId=1000） |
| 推送 SDK 后端 | `https://push.flomoapp.com/api/v1/sdk/` |
| 对象存储 | 阿里云 OSS（`file/oss_signature` 下发签名直传） |

**请求签名方案**（每个 API 请求自动附加）：
- 查询参数：`api_token` + `user_id`（或登录后 `Authorization: Bearer <token>`）、`tz`（时区 ±HH:MM）、`app_version`、`api_key=flomo_android`、`timestamp`、`webp=1`
- 签名：全参数按字典序排序 → `k=v&` 串联（同 key 值组内排序，`tz` 只取一个）→ 末尾拼接固定密钥 `5d5f8cd25e6a7961f3b3281e9fbece7c` → **MD5 小写**作为 `sign`
- 特定接口（`/created_at`、`onboard_task/complete`）额外把 JSON body 并入签名
- 固定请求头：`platform: android`、`device-id`、`device-model`
- 另有 HMAC-SHA1 工具（统计上报鉴权用，key `KQn3bjcX9PlpchOj`）

### 4.2 端点清单（按域）

**Memo（核心，`MemoAPI` 35+ 个）**
- 增删改查：`POST /api/v2/memo`（创建，支持 file_ids、自定义 created_at、memo_from 来源标记）、`PUT /api/v2/memo/{slug}`（编辑）、`GET/DELETE memo/{slug}/`、`memo/restore`
- 列表/同步：`memo/mine/`（分页/标签/日期/搜索多态复用同一端点）、`memo/updated/`（增量）、`memo/latest_updated_desc/`、`memo/pinned/`、`memo/{slug}/revisions/`（历史版本）
- 回收站：`memo/deleted/`、`memo/batch_force_delete`
- 置顶/悬浮：`memo/{slug}/pin|unpin`、`memo/float/list|update`
- 推荐/关联：`memo/{slug}/recommended`（相关 memo）、`memo/recommend_by_tag/`、`memo/recommend_ignore/`
- 时间机器：`memo/{slug}/created_at/`（改创建时间）
- 每日回顾：`memo/notify_of_today`、`memo/daily_review_trial/start`（试用）
- 随机漫步：`memo/slugs/`（按 allow/deny 标签抽样）

**AI 功能**
- `recorded/polish/`（AI 润色，独立 300s 超时通道）
- `memo/insight/*`：洞察问答/历史/模板 CRUD/反馈（`question`、`history`、`templates`、`templates/with_category`、`template/{id}`、`template/feedback`）
- 语音：`voice/stream/sign`（实时流签名）、`voice/file/submit` + `voice/file/status/{task_id}`（文件转写轮询）、`ai/aivoice`
- 认知图谱：`contour/compute`（模型类 ContourComputeRequest/ContourResponse/ContourPeak/ContourPoint）
- 微信 Agent：`agent/initialization`、`agent/weixin/binding`
- AI 搜索：`api/v1/search`（SSE）+ `chat.flomoapp.com/v1/search/{chatId}/feedback`

**Tag**：`tag/tree`、`tag/rename`、`tag/auto/`（AI 自动打标签）、`tag/icon`、`tag/pin|unpin`、`tag/updated`、`tag/latest`、`tag/memo/delete`

**User**：`user/login_by_email`、`user/register`、`user/send_verify_code`、`phone_account/login_by_token`（运营商一键登录）、`user/me`、`user/update_profile`、`user/my_stat/`、`user/redeem_membership/`（兑换码）、问卷（`user/survey*`、`user/returning_survey`）、`wechat/info|unbind`

**会员/支付**：`pro/alipay`（支付宝传统支付）、`pro/alipay_subscription_for_app`（订阅）、`pro/education_discount/alipay`（教育优惠）、`pro/google_play`、`pro/max_benefits`；支付失败原因上报到阿里云日志服务（`flomo.cn-shanghai.log.aliyuncs.com/logstores/pay_fail_reason`）

**其他**：`app/config`、`biz/config`（远程配置）、`device/register`、`report/device`、`notification/*`（通知中心）、`notify_setting/*`、`onboard_task/*`（新手任务）、`ops_popup/check`（运营弹窗）、`annual_report/status`（年度报告）、`file/my_info/`、`file/oss_signature`、`android/latest`（版本更新）、`recorded/config/`

**Web 视图页**（`v.flomoapp.com`）：忘记密码、验证码、导入说明、会员页（`/mine?source=*`，带 referral/stat/membership 等来源埋点）、教育优惠、实验室（`labs/agent-weixin`）

## 5. 功能清单（UI 层）

- **记录**：文本、语音（实时流式识别 + 文件转写，阿里云 ASR，含热词设置）、图片（多图/编辑/裁剪）、悬浮窗速记、来自系统分享（text/image/audio）、微信收藏绑定
- **整理**：多层标签树、标签图标/排序/置顶、AI 自动打标签、AI 润色
- **回顾**：每日回顾推送（可设过滤规则 `NotifySetting$Filter`）、随机漫步、AI 洞察（周报式，可自定义 prompt 模板）、年度报告、认知图谱（`CognitiveMapActivity`，可分享）
- **AI 中心**：AI Find（SSE 语义搜索）、AI Agent（含 paywall）、AI 个性化/风格设置
- **小组件**：QuickAdd、QuickVoice、日/周/月计数（网格+柱状）、日夜双主题搜索、Tiny/Small/Top 系列，事件通过加密 Hawk 分发
- **安全**：应用锁（密码+生物识别）
- **会员**：Google Play / 支付宝 / 兑换码 / 教育优惠 / 伊朗 Bazaar 市场（多渠道发行）
- **快捷指令**：`shortcut.MainBridge`/`BridgeAdd` 桥接外部自动化（Tasker 等）
- **实验室**：自定义侧栏、Live Photo 解析、语音实验室等灰度开关

## 6. 第三方 SDK 清单

| 类别 | SDK |
|---|---|
| 推送 | 华为 HMS Push、小米 MiPush、OPPO(heytap) MSP、vivo Push、荣耀 MCS + 自建 push.flomoapp.com 聚合 |
| 一键登录 | 阿里云号码认证（dypnsapi）、电信 189（21cn.com/e.189.cn）、移动 cmpassport |
| 统计/监控 | 友盟（libumeng-spy）、华为 HiAnalytics、Sentry（含 Android Replay 屏幕回放） |
| 支付 | Google Play Billing、支付宝 SDK（含香港版 alipay.hk）、Bazaar（伊朗市场） |
| 其他 | Fresco、MMKV、ObjectBox、Hawk、OkDownload、微信 OpenSDK、微博/小红书分享目标、阿里云 OSS/HTTPDNS/SLS 日志 |

## 7. 安全与工程观察

- **客户端密钥硬编码**：API 签名密钥 `5d5f8cd25e6a7961f3b3281e9fbece7c` 与统计鉴权 key 内嵌于代码（客户端签名方案天然无法保密，仅防滥用）。
- **调试残留**：dex 内硬编码内网 IP `http://10.38.162.35`（疑似开发环境地址）。
- **stage 环境开关**藏在 MMKV（加密 KV），正常用户不可达；测试环境与生产共用同一签名密钥。
- 应用锁 token、access token 均走 Hawk AES 加密存储，未发现明文凭据落盘。
- 友盟/统计权限申请较克制（READ_PHONE_STATE 在 targetSdk 35 下实际无效）。
- 360 加固只防静态反编译；本报告证明 root 环境下运行时内存 carve 即可完整还原代码。

## 8. 产物索引

| 产物 | 位置 |
|---|---|
| APK 副本 | `/Users/suyi/projects/exercise/flomo.apk` |
| 脱壳 dex + jadx 源码 | `.scratch/flomo-unpacked/`（80MB） |
| 内存 carve 脚本（逻辑） | 本报告 §3，实现于本轮会话（内联执行） |
| 运行截图 | `.scratch/flomo-0{1,2,3}.png` |
| 模拟器 | AVD `flomo_test`（API 35，root，快照已存） |

> 声明：本分析基于用户合法持有的安装包，仅用于学习与研究 flomo 的产品/技术设计，不应用于绕过付费或攻击其服务。
