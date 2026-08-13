---
documentId: PLAN-NOTICE-765
documentType: implementation-plan
pmoVersion: 1.3.13
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: 继承 TDD-NOTICE-765：需求与方案均为 L3，最终等级=max(requirement,solution)=L3
status: APPROVED
action: NEXT
owner: Mango Notice 实施负责人
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: TDD-NOTICE-765
upstreamDocumentHash: ad4882206fce9d98e0c684cf8ffdc47fa40b5aadd18d7e6db320f51e438c1074
---

# Notice 统一消息接收能力实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-001, DEC-002, DEC-003, DEC-004, DEC-009, MOD-001, MOD-002, MOD-004, MOD-005 | 入站协议、SPI 与标准模型 | mango-notice-api/support/channel-email/channel-wecom | 公共契约、结构化解析和能力边界可编译并有定向测试 | SAC-001, SAC-002, SAC-005 | 不实现短信、钉钉收件和未核实厂商收件 |
| DEL-002 | DM-001, DM-002, DM-003, DM-004, DB-001, DB-002, DB-003, MOD-003 | Inbox、附件、游标与状态持久化 | mango-notice-core migration | migration、Entity、Mapper 和状态事务形成真实持久化链路 | SAC-001, SAC-003, SAC-004 | 不修改历史 migration，不跨模块持有表 |
| DEL-003 | FLOW-001, FLOW-002, FLOW-003, FLOW-004, FLOW-005, API-001, API-002, API-003, MOD-006 | 调度、回调入口、ResourceProvider、管理员查询与异步处理 | mango-notice-starter + mango-ui/packages/notice | 公网 GET/POST 资源可同步，回调 ACK 与内部处理分离；管理员可分页查询入站消息和附件 File ID | SAC-002, SAC-005, SAC-006 | 不改个人消息中心和旧发送入口兼容壳 |
| DEL-004 | DEC-006, DEC-007, DEC-008, SEC-002, SEC-003, ERR-002, ERR-003, MOD-007, IMP-003 | 文件保存、稳定事件和重试死信 | mango-notice-core + mango-file + mango-infra-event | 事件载荷只含 fileId，稳定 eventId 经 Outbox 可重试 | SAC-003, SAC-004 | 不保存 URL、预签名地址或自建 Outbox |
| DEL-005 | TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007, IMP-001, IMP-002 | 测试、能力说明与交付证据 | 受影响模块 README、能力地图、mango-docs/evidence | 定向自动化、真实邮箱手工证据和能力边界说明完成 | 全部 SAC | IMAP 新邮件同步受 126 外部窗口限制，证据标记 BLOCKED_EXTERNAL_SYNC |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DEC-001, DEC-002, DEC-003, DEC-004, DEC-009, MOD-001, MOD-002, MOD-004, MOD-005 | DEL-001 | Dev | notice api/support/channel-email/channel-wecom | NONE | 按 TDD 建立标准接收模型、SPI、IMAP/POP3 与企业微信适配边界 | 无空实现；协议输入能转标准模型；不支持 provider 明确失败 | VAL-001 | B1 | PLANNED |
| TASK-002 | DM-001, DM-002, DM-003, DM-004, DB-001, DB-002, DB-003, MOD-003 | DEL-002 | Dev | notice-core | TASK-001 | 新增 V3 migration、持久化模型、Mapper、Inbox 幂等与状态事务 | 表 owner、租户字段、唯一键和状态更新符合 TDD | VAL-002 | B2 | PLANNED |
| TASK-003 | FLOW-001, MOD-004 | DEL-003 | Dev | notice starter/channel-email | TASK-001, TASK-002 | 接入账号配置调度、IMAP/POP3 游标和失败退避 | 单账号协议选择生效，未成功落库不推进游标 | VAL-003 | B3 | PLANNED |
| TASK-004 | FLOW-002, FLOW-003, API-001, API-002, API-003, MOD-005, MOD-006, SEC-001, ERR-001, ERR-004 | DEL-003 | Dev | notice starter/channel-wecom/channel-email | TASK-001, TASK-002 | 以函数式公网入口和 ResourceProvider 暴露 GET/POST，委托渠道验真并在接收链路受理后 ACK | GET/POST 资源声明完整；非法请求不入库；不把发送回执当收件 | VAL-004 | B3 | PLANNED |
| TASK-005 | FLOW-004, FLOW-005, DEC-006, DEC-007, DEC-008, SEC-002, SEC-003, ERR-002, ERR-003, MOD-007, IMP-003 | DEL-004 | Dev | notice-core | TASK-002, TASK-003, TASK-004 | 编排附件保存、稳定 eventId、Outbox 发布、重试和死信 | fileId 完整后才广播；失败可恢复；事件载荷无 URL/Secret | VAL-005 | B4 | PLANNED |
| TASK-006 | IMP-001, IMP-002, TC-001, TC-002, TC-003, TC-004, TC-005, TC-007 | DEL-005 | QA/Dev | notice modules tests/readmes/capability map | TASK-001 至 TASK-005 | 补单元、集成、API/入口流程测试和 provider 能力矩阵 | 测试真实参与被测链路；能力说明覆盖配置、入口、边界 | VAL-006 | B5 | PLANNED |
| TASK-007 | TC-006 | DEL-005 | QA/Notice owner | evidence/issue-765-notice-inbound | TASK-006 且用户提供凭据 | 从安全环境注入 126 邮箱配置，发自收测试邮件并回读 Inbox、附件和广播 | SMTP/POP3 真实证据完成；IMAP ID/解析完成但新邮件同步窗口超时；凭据不落盘 | VAL-007 | B6 | BLOCKED |
| TASK-008 | UI-001, API-004, API-005 | DEL-003 | Dev/QA | notice api/core/starter/starter-remote + mango-ui/packages/notice | TASK-002, TASK-005 | 增加管理员接收消息分页、详情、附件下载和菜单权限 | 代码、构建、组件测试、登录管理查询与权限边界已验证；因浏览器控制接口不可用且隔离库无入站记录，真实页面交互与消息行可见性未完成 | VAL-008 | B6 | BLOCKED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001 | BRD/SRS/TDD checker 通过 | 公共契约与适配器测试可执行 | 生命周期审批 | 无 | 协议或能力边界不明回到 TDD | Tech Lead |
| MS-002 | TASK-002 | 入站模型已确定 | migration、Mapper、幂等和租户集成验证通过 | MS-001 | 无 | 表 owner/租户失败停止开发 | Dev |
| MS-003 | TASK-003, TASK-004 | 持久化链路可接收标准消息 | 调度、回调、ResourceProvider 和 ACK 验证通过 | MS-002 | TASK-003 与 TASK-004 可并行 | 公网资源或验签失败停止入口交付 | Dev |
| MS-004 | TASK-005 | Inbox 与入口可用 | 附件、事件、重试和死信集成验证通过 | MS-003 | 无 | 文件或广播失败保持未完成 | Dev |
| MS-005 | TASK-006 | 业务主链路实现完成 | 定向自动化和能力文档门禁通过 | MS-004 | 无 | 任一 P0 用例失败阻断交付 | QA |
| MS-006 | TASK-007 | 126 凭据、协议和环境已提供 | 真实自发自收证据回写 | MS-005 | 无 | 外部邮箱同步超时保持 BLOCKED 并报告剩余风险 | Notice owner |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001, TC-002, TC-007 | TASK-001 | M09/M10 | `mvn -pl mango-platform/mango-notice/mango-notice-channel-email,mango-platform/mango-notice/mango-notice-channel-wecom test` | 本地隔离环境 | RFC 邮件、企业微信固定向量、官方 SES 事件样本 | 测试凭据仅进程注入 | 协议解析、验签、能力拒绝和无空实现 | mango-docs/evidence/issue-765-notice-inbound/automated.md | Dev | 修复真实协议边界，不降低断言 |
| VAL-002 | TC-003 | TASK-002 | M09/M11 | Notice core migration、Mapper、状态和租户集成测试 | `mango_dev_*` 隔离 MySQL | `IT_765_INBOUND_` 唯一数据，测试后清理 | 双租户、专用测试账号 | 唯一键、事务、状态和跨租户隔离通过 | mango-docs/evidence/issue-765-notice-inbound/automated.md | Dev | 表归属或租户失败即停止 |
| VAL-003 | TC-001 | TASK-003 | M11 | 带 IMAP/POP3 fixture 的调度测试，检查游标推进和退避 | 隔离测试邮箱服务替身，仅替换外部邮箱 | 两种协议各一账号，重复 Message-ID/UIDL | 账号配置映射单一租户 | 成功接收才推进游标，失败可重试 | mango-docs/evidence/issue-765-notice-inbound/automated.md | Dev | 不能用发送模块测试代替 |
| VAL-004 | TC-005 | TASK-004 | M09/M12 | Starter ResourceProvider 和 MockMvc/入口流程：GET、POST、非法签名、unsupported provider | 隔离 Spring 测试环境 | 固定签名、加密 XML、原始回调 | PUBLIC 资源绑定测试渠道账号；无客户端 tenantId | GET 返回 echostr 且零入库；POST 受理后 ACK；非法请求拒绝 | mango-docs/evidence/issue-765-notice-inbound/automated.md | QA | 路由、ACK、验签或敏感信息断言失败阻断 |
| VAL-005 | TC-004 | TASK-005 | M10/M11 | 文件服务、Outbox、事件重试与死信集成测试 | 隔离文件存储、Redis/KV 或等价真实测试物料 | `IT_765_FILE_` 和可清理 Inbox | 文件、事件与消息同租户 | 仅 fileId 入事件；eventId 稳定；失败可重投 | mango-docs/evidence/issue-765-notice-inbound/automated.md | QA | 不得改成只断言调用次数 |
| VAL-006 | TC-001 至 TC-005, TC-007 | TASK-006 | M09/M10/M11/M12/M14 | 受影响 Maven 模块定向质量检查、checker、README/能力门禁和专家复核 | 任务 worktree | 测试报告、provider 能力矩阵 | 不接生产或共享业务库 | checker、架构/模块/安全/测试和能力说明结果可追溯 | mango-docs/evidence/issue-765-notice-inbound/automated.md | QA/Tech Lead | 失败按所属模块修复并重跑 |
| VAL-007 | TC-006 | TASK-007 | M16/M15 | 1. 从安全环境注入 SMTP/IMAP/POP3 配置；2. 发送唯一主题和附件到 `yunxinbaokeji@126.com`；3. 按配置协议回读；4. 核对 Inbox、fileId、`notice.message.received` 和广播日志；5. 清理测试数据 | 用户提供的 126 邮箱测试环境 | 主题 `IT_765_SELF_MAIL_<runId>`，一正文一附件 | 专用测试租户；不打印授权码 | SMTP 接受、收件回读、附件归档和广播均有证据 | mango-docs/evidence/issue-765-notice-inbound/mail-test.md | Notice owner | 缺凭据或外部服务不可用标记 BLOCKED，不宣称通过 |
| VAL-008 | TC-008 | TASK-008 | M09/M10/M12 | 接收服务 H2 集成测试、Controller 权限、Feign 契约、Notice 前端包 build/test | 任务 worktree | 入站邮件及 File ID | 管理权限 `notice:inbound:view`；租户拦截保持启用 | 接收后可分页和查看详情；个人消息中心不变；附件走受保护 File 下载接口 | mango-docs/evidence/issue-765-notice-inbound/README.md | Dev/QA | 任一查询、权限、类型或构建失败阻断 |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 验证 | 失败停止条件 | 补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| DATA-001 | DB-001, DB-002, DB-003 | 隔离测试库/后续发布环境 | 确认 `mango-notice-core` migration owner、当前版本和备份 | 新增 V3 migration，创建三张入站表和索引 | 1 | 不回填既有发送记录；发布环境按数据库备份规范执行 | Flyway、schema owner、租户和唯一键检查 | migration 失败、表重复 owner 或结构不符 | 停止发布并按迁移工具恢复；不手工改库 | Dev/DBA |

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | DEL-001, DEL-003, DEL-004 | `mango/mango-platform/mango-notice/README.md` | 增加入站协议配置、回调路径、事件载荷、fileId 和失败处理 | Notice owner | 业务开发者可按 README 接入并识别 provider 不适用边界 | `node mango-pmo/tools/audit-module-readmes.mjs`, `node mango-pmo/tools/audit-readme-source-facts.mjs` | 不适用 |
| DOC-002 | IMP-001, IMP-002, IMP-003 | `mango-docs/capabilities/README.md` | 增加入站邮件/企业微信能力、阿里云/腾讯云能力矩阵和验收入口 | PMO/Notice owner | 能力地图链接到模块 README、Issue 765 和证据 | `node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD` | 不适用 |
| DOC-003 | DEL-005 | `mango-docs/evidence/issue-765-notice-inbound/` | 保存真实邮箱结果或 BLOCKED 原因 | QA | 不含凭据、token、客户敏感数据 | 人工检查证据路径和敏感信息 | 若真实邮箱未授权，保留 BLOCKED 证据 |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | 阿里云/腾讯云官方入站能力与发送回执混淆 | 伪造收件适配或错误广播 | provider SPI 必须声明真实入站契约；TC-007 | 未核实则拒绝配置并记录不适用 | Tech Lead | 开发前 | CLOSED | NONE | NONE |
| RISK-002 | L3 | RISK | 126 IMAP 新邮件同步存在第三方延迟或策略差异 | IMAP 真实自发自收不能在固定窗口内稳定复现 | 只从安全环境注入，保留 IMAP ID，生产启用前观察连续轮询和游标推进 | TASK-007 记录 BLOCKED_EXTERNAL_SYNC，不把协议解析通过当作完整收件通过 | HardyDou | 2026-08-13 | OPEN | NONE | 用户已知悉；目标环境验收时重新观察 |
| RISK-003 | L3 | RISK | 公网回调验签、租户映射或资源声明错误 | 未授权消息入库或企业微信继续 404 | 复用函数式公网入口样本、ResourceProvider 和入口测试 | 关闭入站装配并修复原任务 worktree | Notice owner | B3 | CLOSED | NONE | NONE |
| RISK-004 | L3 | RISK | 文件保存成功但广播失败或重复投递 | 附件孤儿或重复业务 | 只在 fileId 完整后广播，eventId 稳定，Outbox 重试 | 保留文件和 Inbox，进入重试/死信，禁止假成功 | Dev | B4 | CLOSED | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑数据文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001, DEC-002, DEC-003, DEC-004, DEC-005, DEC-009, MOD-001, MOD-002, MOD-004, MOD-005 | DEL-001 | TASK-001 | VAL-001 | MS-001 | 公共模型、协议适配和 provider 能力边界 |
| DM-001, DM-002, DM-003, DM-004, DB-001, DB-002, DB-003, MOD-003 | DEL-002 | TASK-002 | VAL-002 | MS-002, DATA-001 | Inbox、附件、游标、状态和数据库所有权 |
| FLOW-001, MOD-004 | DEL-003 | TASK-003 | VAL-003 | MS-003 | IMAP/POP3 轮询和游标 |
| FLOW-002, FLOW-003, API-001, API-002, API-003, MOD-005, MOD-006, SEC-001, ERR-001, ERR-004 | DEL-003 | TASK-004 | VAL-004 | MS-003 | 企业微信和邮箱公网回调、资源声明、验真和 ACK |
| FLOW-004, FLOW-005, DEC-006, DEC-007, DEC-008, SEC-002, SEC-003, ERR-002, ERR-003, MOD-007, IMP-003 | DEL-004 | TASK-005 | VAL-005 | MS-004, RISK-004 | fileId、稳定事件、Outbox、重试和死信 |
| IMP-001, IMP-002, TC-001, TC-002, TC-003, TC-004, TC-005, TC-007 | DEL-005 | TASK-006 | VAL-006 | MS-005, DOC-001, DOC-002 | 自动化、能力说明和供应商边界证据 |
| TC-006 | DEL-005 | TASK-007 | VAL-007 | MS-006, RISK-002, DOC-003 | 126 邮箱真实自发自收外部验收 |
| DEC-003 | DEL-001 | TASK-001 | VAL-001 | MS-001, RISK-001 | 供应商真实入站能力核实与发送回执边界 |
| SEC-001 | DEL-003 | TASK-004 | VAL-004 | MS-003, RISK-003 | 公网验签、租户映射和资源声明风险 |
| UI-001, API-004, API-005 | DEL-003 | TASK-008 | VAL-008 | MS-006 | 管理员接收消息列表、详情、附件 File 下载与权限 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/issue-765-notice-inbound/implementation-plan.md` |
| 生命周期 handoff | PASS | TDD-NOTICE-765 已批准并通过 checker；上游摘要在交接时写入 |
| 依赖图 | PASS | TASK-001 至 TASK-007 依赖无环，TASK-007 的 IMAP 真实回读受 BLOCKED_EXTERNAL_SYNC 限制 |
| 未关闭阻断数量 | 0 | RISK-002 已作为已知外部同步风险记录；SMTP/POP3 已验证，IMAP 需在目标环境继续观察 |
| 实施审批 | APPROVED | `review/APPROVAL.md` |
