# Issue #923 工作流默认通知规范化交付记录

## 1. 元数据

- 任务 ID：GitHub Issue #923
- 交付模式：STANDARD
- 需求影响：L2 - 改变 Workflow 公共事件载荷、默认通知内容和 Notice 默认投递语义。
- 方案风险：L2 - 涉及 Workflow、Notice、Auth 三个模块及资源声明、事件和持久化发送记录协作。
- 最终风险：L2
- 工作区决策：REUSE - `/Users/hardy/Work/mango-issue-923-workflow-notices`，分支 `issue-923-workflow-notices`。

## 2. 目标与范围

- 目标：统一 Mango 工作流默认通知，使用户只看到可读流程名称、业务标题、待审核状态或最终审核结果；移除登录成功通知；默认启用系统消息和企业微信，并把无通道或无绑定识别为静默取消。
- 成功条件：首次提交和后续节点流转通知实际办理人；通过或驳回只向发起人发送一条结果通知；编码不进入默认用户文案；登录成功不发布通知事件；无外部通道或无企业微信绑定不形成失败发送。
- 处理范围：`mango-workflow-api/core/starter`、`mango-notice-support/core`、`mango-auth-core/starter`、相关 README、业务审批接入指南、能力地图和定向测试。
- 不处理范围：业务应用专用模板、业务字段命名、通知管理页面改版、第三方渠道配置创建、Maven 发布、业务应用升级和部署。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 当前审批办理人 | 流程首次提交或进入下一审批节点 | 收到一条待审核通知，包含流程名称、业务标题和已提交审核 | 缺少实际收件人时不创建通知 | 首次任务和后续任务均产生目标明确、幂等隔离的通知事件 |
| SR-002 | 流程发起人 | 流程最终通过 | 收到一条审核通过通知 | 不发送额外流程结束通知 | 仅 `workflow.process.completed` 产生默认结果通知 |
| SR-003 | 流程发起人 | 流程被驳回 | 收到一条审核未通过通知，可包含原因 | 不向驳回操作人发送任务驳回通知，不再追加流程结束通知 | 仅 `workflow.process.rejected` 产生默认结果通知 |
| SR-004 | 通知用户 | 任一默认工作流通知 | 用户文案使用可读流程名称和业务标题 | 字段缺失时显示通用文本，不回退为内部编码 | 模板和通知参数不向用户展示 `businessType`、`definitionKey`、`taskDefinitionKey` 或 `businessKey` |
| SR-005 | 登录用户 | 密码登录成功 | 不产生登录成功通知 | 登录日志和安全锁定提醒保持不变 | 登录成功链路不发布 `auth.login.success`，资源中不再声明该模板 |
| SR-006 | 通知投递 | 调用方未显式指定渠道 | 系统消息和企业微信模板默认启用，邮件和短信默认关闭 | 用户偏好显式关闭时继续取消 | 资源声明和落库状态分别反映业务类型启用与渠道默认启用 |
| SR-007 | 外部渠道投递 | 没有可用渠道配置或企业微信身份未绑定 | 对应发送记录标记取消，不调用渠道发送器 | 外部查询异常仍保留失败语义，不伪装为未绑定 | 任务不因不可投递默认渠道记为失败，取消原因可审计 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001~SR-004 | `WorkflowEventPayloadVO` 增加申请标题/摘要，所有相关事件稳定填充定义名称和申请快照；默认通知禁止回退内部编码 | Workflow API、事件发布器、通知订阅器 | 回退事件字段填充与模板，但保留原事件类型 |
| TD-002 | SR-001~SR-003 | 默认通知只消费任务到达、流程完成、流程拒绝；首次任务额外发布任务到达事件，忽略任务驳回和通用流程结束通知 | Workflow process/task runtime、notice subscriber | 移除首次发布并恢复旧支持事件集合 |
| TD-003 | SR-005 | 删除登录成功事件发布和模板声明，保留登录锁定安全通知 | Auth core/starter | 恢复成功事件和对应资源声明 |
| TD-004 | SR-006 | MESSAGE_TEMPLATE 资源拆分业务类型 `enabled` 与渠道 `channelEnabled`；内置模板默认仅 SITE/WECOM 开启 | Notice support/core resource handler | 让 `channelEnabled` 回退为 `enabled` 并恢复四渠道默认开启 |
| TD-005 | SR-007 | 发送记录创建前检查外部渠道可用性和企业微信绑定；确定不可投递时创建 CANCELED 记录，不进入 worker 失败/重试 | Notice delivery service | 恢复执行期通道解析和失败记录语义 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | TD-001、TD-002 | 1 | `mango-workflow-api/core/starter` | 事件字段、首次待办、结果去重和模板测试通过 |
| TASK-002 | TD-003 | 2 | `mango-auth-core/starter` | 登录成功不再发布通知且安全锁定通知保持 |
| TASK-003 | TD-004、TD-005 | 3 | `mango-notice-support/core` | 默认渠道和不可投递取消语义通过资源/持久化集成测试 |
| TASK-004 | TD-001~TD-005 | 4 | Workflow、Notice、Auth README、业务审批接入指南与能力地图 | 使用说明与当前公开行为一致 |
| TASK-005 | TD-001~TD-005 | 5 | 受影响模块测试与质量门禁 | M09、M10、M11 定向证据完成，无新增测试质量问题 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001~SR-004 | M10 单元测试 + M11 Workflow 模块协作测试 | 联合执行 `WorkflowEventPublisherTest`、`WorkflowProcessServiceImplIntegrationTest`、`WorkflowNoticeDomainEventSubscriberTest`、`WorkflowMessageTemplateResourceProviderTest` | PASS | Workflow 相关测试 19 个通过 |
| SR-005 | M10 Auth 流程测试 | 联合执行 `AuthSecurityFlowTest`、`AuthMessageTemplateResourceProviderTest` | PASS | Auth 相关测试 13 个通过 |
| SR-006~SR-007 | M11 Notice 资源和发送持久化集成测试 | 联合执行 `NoticeMessageTemplateResourceHandlerIntegrationTest`、`NoticeServiceIntegrationTest` | PASS | Notice 相关测试 31 个通过 |
| SR-001~SR-007 | M09 定向模块 `mvn verify` 与测试质量检查 | `mvn -f mango/pom.xml -Drevision=1.0.0-mango-009-SNAPSHOT -pl mango-platform/mango-workflow/mango-workflow-starter,mango-platform/mango-notice/mango-notice-core,mango-platform/mango-auth/mango-auth-starter -am -Dtest=WorkflowEventPublisherTest,WorkflowProcessServiceImplIntegrationTest,WorkflowNoticeDomainEventSubscriberTest,WorkflowMessageTemplateResourceProviderTest,NoticeMessageTemplateResourceHandlerIntegrationTest,NoticeServiceIntegrationTest,AuthSecurityFlowTest,AuthMessageTemplateResourceProviderTest -Dsurefire.failIfNoSpecifiedTests=false verify`；`node mango-pmo/tools/test-quality-check.mjs --base origin/main`；backend mock audit；module README audit；README source facts audit；`git diff --check` | PASS | Maven Reactor 83 个模块全部成功；相关测试 63 个通过；测试质量检查覆盖 7 个文件并通过；mock audit block=0、warn=0；两项 README 检查通过；diff whitespace 检查通过 |

## 7. 例外与剩余风险

- 本机未配置 `MANGO_DB_USERNAME`、`MANGO_DB_PASSWORD`，默认全量 `verify` 在未改动的 `FileServiceConcurrentSaveIntegrationTest` 中把占位符当作 MySQL 用户名，无法作为本次交付证据；已改用覆盖全部受影响路径的 8 个精确测试类执行联合 Reactor `verify` 并通过。
- 未进行真实企业微信发送验收；自动化测试已覆盖无通道配置、无有效绑定、Identity 查询异常和匹配 CorpID 后固定渠道配置等分支，真实发送仍依赖运行环境中的企业微信渠道配置与用户绑定。
