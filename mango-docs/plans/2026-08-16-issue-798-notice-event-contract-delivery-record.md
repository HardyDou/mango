# 标准交付记录

任务：Issue #798 收窄 Notice 入站消息广播事件契约。

## 1. 元数据

- 任务 ID：Mango Issue #798
- 交付模式：STANDARD
- 需求影响：L2 - `notice.message.received` 当前广播收发件信息与附件 `fileIds`，超出下游触发消费所需的最小公开契约
- 方案风险：L2 - 删除既有 wire fields，已直接读取这些字段的消费者需要改为按 `messageId` 查询详情
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-798-notice-event-contract`，`fix/issue-798-notice-event-contract`）
- 启用能力：M01、M08、M09、M11

## 2. 目标与范围

- 目标：保持事件类型与幂等身份不变，把入站消息广播收敛为无内容、无附件标识的最小触发事件。
- 成功条件：payload 严格只有 `messageId`、`eventId`、`channelType`、`providerCode`、`sourceMessageId`、`status`；`status` 固定为 `BROADCASTED`；正文和附件仍可从现有详情与 File 链路读取。
- 处理范围：Notice inbound core 事件组装、入站与真实 File 集成测试、Notice README、能力地图。
- 不处理范围：事件 V2、兼容开关、业务仓 fallback、新 HTTP API、权限或服务间授权、Maven 发布与业务项目升级。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | `notice.message.received` 消费者 | 入站消息及附件均已持久化，消息准备广播 | 收到稳定事件类型、事件 ID 和六个白名单 payload 字段，`status=BROADCASTED` | 出现主题、地址、正文、附件或其它非白名单字段视为失败 | 两条集成链路均精确断言 payload 键集合和值 |
| REQ-002 | 广播重试任务 | 首次发布失败，持久化消息仍为可重试状态 | 重试使用原 `eventId` 并发布同一白名单契约；发布成功后数据库状态变为 `BROADCASTED` | 重试改变事件身份、暴露内容或在发布前写成功状态视为失败 | 重试测试断言稳定 `eventId`、白名单与最终状态 |
| REQ-003 | 需要正文或附件的下游服务 | 已取得事件中的 `messageId` 且具备现有查询权限 | 通过 Notice 入站详情取得正文和附件元数据，再以 `fileId` 访问 Mango File | 广播携带正文、地址或 `fileIds`，或 File 实际归档被移除视为失败 | 详情查询测试与真实 File 集成测试保持通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001、REQ-002 | 保持事件类型 `notice.message.received`、header、aggregate 与幂等 ID 不变；payload 只保留六个字段，不新增 V2 或兼容开关 | `NoticeInboundReceiverService` | 恢复旧 payload 组装，但会重新引入内容与附件标识广播 |
| DEC-002 | REQ-001、REQ-002 | `status` 使用固定 `BROADCASTED`，不读取发布时仍为 `READY_TO_BROADCAST` 的实体状态；继续先发布事件、成功后更新数据库 | `publishWhenReady`、事件合同测试 | 删除状态字段；不建议交换发布与写库顺序，以免形成漏事件窗口 |
| DEC-003 | REQ-003 | 删除仅为广播 `fileIds` 执行的附件查询；保留入站详情查询、附件持久化和 File API 链路 | Notice inbound core、File 集成测试、README | 恢复附件查询与 `fileIds` 字段 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IMP-001 | DEC-001、DEC-002、DEC-003 | 1 | `NoticeInboundReceiverService.java` | 广播不再查询附件，payload 只生成六个字段 |
| IMP-002 | 全部 | 2 | 两个 Notice inbound 集成测试 | 首次、重试和真实 File 路径精确锁定白名单，且真实附件仍可读取 |
| IMP-003 | DEC-001、DEC-003 | 3 | Notice README、能力地图 | 当前事件契约与详情访问方式可发现，历史 #765 设计证据不改写 |
| IMP-004 | 全部 | 4 | Maven 与 PMO 质量检查 | 直接修改模块验证及文档、diff 门禁通过或如实记录 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001、REQ-002、REQ-003 | 受影响集成测试与模块生命周期 | 在 `mango-notice-core` 执行 `mvn -o -Dtest=NoticeInboundReceiverServiceIntegrationTest,NoticeInboundReceiverFileIntegrationTest verify` | PASS | 6 tests，0 failures，0 errors；编译、JAR 阶段完成，BUILD SUCCESS |
| REQ-001、REQ-002、REQ-003 | 完整直接模块验证 | 在 `mango-notice-core` 执行 `mvn -o verify`，不使用 `-am` / `-amd`；并在干净 `main` 单独复现失败测试 | BASELINE BLOCKED | 本分支 64 tests 中 7 errors，均为未修改的 `NoticeChannelResourceHandlerIntegrationTest` H2 表缺少 `capability_mode`；干净 `main` 同类 8 tests 同样 7 errors |
| REQ-001、REQ-002、REQ-003 | 后端测试质量检查 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS | Test quality：2 files；mock audit：block=0、warn=0 |
| REQ-001、REQ-003 | 能力说明检查 | `audit-module-readmes.mjs`、`audit-readme-source-facts.mjs`、`check-standard-delivery-record.mjs` | PASS | Notice README 与能力地图均为 OK；STANDARD 记录 PASS |
| 全部 | 工作树静态检查 | 扫描事件构造中的旧 payload 字段；`git diff --check` | PASS | 事件构造只有六个白名单字段；无空白错误 |

## 7. 例外与剩余风险

- 删除的是既有公开事件字段；当前仓库内未提供消费者兼容层。发布说明必须明确业务消费者需要改为用 `messageId` 查询详情。
- 完整 `mango-notice-core` 验证仍被 `NoticeChannelResourceHandlerIntegrationTest` 的既有 H2 schema 漂移阻断；该错误已在干净 `main` 复现，不属于 #798 改动。本任务未扩大范围修复该基线问题。
- 首次在线 Maven 验证访问私有 Nexus SNAPSHOT metadata 时收到 HTTP 504；使用本地已解析依赖执行离线验证后，受影响测试与模块生命周期均通过。
- 本任务不执行 Maven 发布，也不修改保函业务仓；业务环境只有在包含本修复的新 Mango 正式制品发布并升级后生效。
