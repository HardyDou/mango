# 标准交付记录

## 1. 元数据

- 任务 ID：工作流默认通知框架缺口（F-1、F-2、F-3）
- 交付模式：STANDARD
- 需求影响：L2 - 改变工作流默认通知的装配方式、消息模板资源同步语义和默认通知的站内信目标契约，影响所有启用 Workflow 和声明消息模板的应用
- 方案风险：L2 - 涉及自动配置装配、资源同步模式语义和跨模块 VO 契约，但不涉及数据库、权限和不可逆数据
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-notice-framework-gaps`，分支 `feat/workflow-notice-framework-gaps`，基于 `origin/main`）

## 2. 目标与范围

- 目标：消除三个框架缺口——默认工作流通知无法关闭、消息模板声明会覆盖运营改动、默认通知的站内信目标与按钮文案不可配置。
- 成功条件：
  1. `mango.workflow.notice.enabled=false` 后不注册默认通知订阅器，业务不再需要修改 Spring Bean 工厂。
  2. 所有通过 `NoticeMessageTemplateResourceDeclarations` 声明的消息模板使用 `SYNC_ONCE`，声明升级不再覆盖运营改过的文案。
  3. 业务申请快照可以声明 `viewPath`、`viewQuery`、`viewActionLabel`，终态通知按声明生成站内信目标与按钮文案。
- 处理范围：`mango-workflow-api`、`mango-workflow-core`、`mango-workflow-starter`、`mango-notice-support`；Workflow 与 Notice 模块 README、能力地图、业务集成指南。
- 不处理范围：不新增业务通知模板或 bizType；不改 Workflow 数据库表；不改前端 `@mango/notice` 与 `@mango/workflow` 行为；不实现节点 `eventNotify` 执行器；不实现渠道能力矩阵和 `paramsSchema` 校验；不提交、Push、创建 PR 或发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1 | 应用启动装配 | 默认配置（未设置 `mango.workflow.notice.enabled`） | 注册一个 `WorkflowNoticeDomainEventSubscriber`，默认通知行为与改动前一致 | 装配失败时启动失败，不静默降级 | `WorkflowNoticeAutoConfigurationTest` 断言存在单例订阅器 |
| AC-2 | 应用启动装配 | `mango.workflow.notice.enabled=false` | 不注册默认通知订阅器，审批事件不再产生默认消息 | 其它工作流能力（事件、待办、回写）不受影响 | 断言无该 Bean |
| AC-3 | 应用启动装配 | `mango.workflow.enabled=false` | 默认通知订阅器同样不注册 | 与既有工作流关闭语义一致 | 断言无该 Bean |
| AC-4 | 消息模板资源声明 | 业务通过 `fourChannels(...)` 声明模板，运营在通知中心改过文案后升级声明版本 | 同步只更新 registry 元数据，不覆盖业务类型与渠道模板的标题、正文、启用状态 | 同步不得因为声明消失而停用运营维护的模板 | `WorkflowMessageTemplateResourceProviderTest` 断言所有声明 `syncMode=SYNC_ONCE` |
| AC-5 | 业务申请详情 | 快照 `customConfig` 声明 `viewPath`、`viewQuery`、`viewActionLabel` | VO 输出规范化后的路径、只保留单层标量查询参数、校验通过的按钮文案 | 空值、嵌套对象、集合、超长文案、控制字符被丢弃，不报错 | `WorkflowBusinessApplyServiceImplIntegrationTest` 断言派生结果 |
| AC-6 | 终态通知 | `workflow.process.rejected` 携带 `viewPath`、`viewQuery`、`viewActionLabel` | 站内信目标为该路径，只携带声明的查询参数和 `fallbackTargetKey`，按钮文案为声明值 | 路径非法或为空时回退通用工作流页面和默认文案 | `WorkflowNoticeDomainEventSubscriberTest` 断言目标与按钮 |
| AC-7 | 待办到达通知 | `workflow.task.advanced` 携带同一组 `customConfig` 字段 | 目标仍为 `workflow:task:detail`，按钮文案仍为默认「去审批」 | 不因业务声明改变待办通知语义 | 断言目标与按钮文案 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | AC-1、AC-2、AC-3 | 新增 `WorkflowNoticeAutoConfiguration`，由它声明订阅器 Bean，并加 `@ConditionalOnProperty(prefix="mango.workflow.notice", name="enabled", matchIfMissing=true)`；同时移除订阅器类上的 `@Component`，避免被 `WorkflowAutoConfiguration` 的 `@ComponentScan` 重复注册 | `mango-workflow-starter`（`notice/WorkflowNoticeAutoConfiguration.java`、`notice/WorkflowNoticeDomainEventSubscriber.java`、`META-INF/spring/...AutoConfiguration.imports`） | 删除自动配置类并恢复 `@Component`；属性默认 `true`，不配置时行为与旧版一致 |
| D-2 | AC-4 | `NoticeMessageTemplateResourceDeclarations` 统一声明 `syncMode(ResourceSyncMode.SYNC_ONCE)` | `mango-notice-support`（`resource/NoticeMessageTemplateResourceDeclarations.java`） | 删除 `syncMode(...)` 调用，恢复 `SYNC_ALWAYS` 默认 |
| D-3 | AC-5 | `WorkflowBusinessApplyVO` 与 `WorkflowEventPayloadVO` 新增只读派生字段 `viewQuery`、`viewActionLabel`；纯派生自 `formJsonSnapshot.customConfig`，不新增数据库列 | `mango-workflow-api`（两个 VO）、`mango-workflow-core`（`WorkflowBusinessApplyService`、`WorkflowEventPublisher`） | 删除新增字段和派生逻辑；旧数据不需要迁移 |
| D-4 | AC-5 | `viewQuery` 只接受单层标量，嵌套对象和集合丢弃；`viewActionLabel` 最多 32 个字符且拒绝控制字符；`viewPath` 保留既有 `safeInternalPath` 校验 | `mango-workflow-core`（`WorkflowBusinessApplyService`） | 同等回退 |
| D-5 | AC-6、AC-7 | 订阅器只在终态事件且目标命中自定义 `viewPath` 时使用 `viewQuery` 和 `viewActionLabel`；未声明时保持原行为（整份通知参数 + 默认文案） | `mango-workflow-starter`（`WorkflowNoticeDomainEventSubscriber`） | 同等回退 |
| D-6 | AC-4 | 在 Notice README 8.2 明确消息模板属运营可改数据，强制 `SYNC_ONCE`；代码侧需要强制回写时显式声明 `SYNC_ALWAYS` | `mango-notice` README | 文档回退 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IM-01 | D-1 | 1 | `WorkflowNoticeAutoConfiguration.java`、`WorkflowNoticeDomainEventSubscriber.java`、`AutoConfiguration.imports`、`WorkflowNoticeAutoConfigurationTest.java` | 三类装配场景测试通过 |
| IM-02 | D-2 | 2 | `NoticeMessageTemplateResourceDeclarations.java`、`WorkflowMessageTemplateResourceProviderTest.java` | 所有消息模板声明为 `SYNC_ONCE` |
| IM-03 | D-3、D-4 | 3 | `WorkflowBusinessApplyVO.java`、`WorkflowEventPayloadVO.java`、`WorkflowBusinessApplyService.java`、`WorkflowBusinessApplyServiceImplIntegrationTest.java` | 派生字段与过滤规则测试通过 |
| IM-04 | D-5 | 4 | `WorkflowEventPublisher.java`、`WorkflowNoticeDomainEventSubscriber.java`、`WorkflowNoticeDomainEventSubscriberTest.java` | 终态与待办通知的目标、按钮断言通过 |
| IM-05 | M08 | 5 | `mango-workflow/README.md`、`mango-notice/README.md`、`mango-docs/capabilities/README.md`、`mango-docs/guides/business-integration/workflow-business-approval.md` | 能力文档与指南说明新开关和字段 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1、AC-2、AC-3 | M11 集成测试（自动配置装配） | `mvn -pl mango-platform/mango-workflow/mango-workflow-starter test -Dtest=WorkflowNoticeAutoConfigurationTest` | 通过 | Tests run: 3, Failures: 0 |
| AC-4 | M10 单元测试（资源声明契约） | `mvn -pl mango-platform/mango-workflow/mango-workflow-starter test -Dtest=WorkflowMessageTemplateResourceProviderTest` | 通过 | Tests run: 3, Failures: 0 |
| AC-5 | M11 集成测试（H2 + 真实 Mapper） | `mvn -pl mango-platform/mango-workflow/mango-workflow-core test -Dtest=WorkflowBusinessApplyServiceImplIntegrationTest` | 通过 | Tests run: 9, Failures: 0 |
| AC-6、AC-7 | M10 单元测试（通知目标与按钮） | `mvn -pl mango-platform/mango-workflow/mango-workflow-starter test -Dtest=WorkflowNoticeDomainEventSubscriberTest` | 通过 | Tests run: 12, Failures: 0 |
| AC-6 | M10 单元测试（事件 payload 扁平化） | `mvn -pl mango-platform/mango-workflow/mango-workflow-core test -Dtest=WorkflowEventPublisherTest` | 通过 | Tests run: 6, Failures: 0 |
| 全部 | M08 能力文档门禁 | `node mango-pmo/tools/check-business-guides.mjs` | 通过 | 5 guides inspected |
| 全部 | M09 编译与模块质量门禁 | 见第 7 节实际命令 | 通过 | 见第 7 节 |

## 6.1 实际验证命令与结果

```bash
# 编译（含上游模块）
mvn -o -Drevision=1.0.0-SNAPSHOT-mango-031 \
  -pl mango-platform/mango-notice/mango-notice-support,mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter \
  -am -DskipTests install
```

结果：成功。随后执行定向测试：

| 模块 | 命令 | 结果 |
|---|---|---|
| `mango-workflow-starter` | `mvn -o -Drevision=1.0.0-SNAPSHOT-mango-031 -pl mango-platform/mango-workflow/mango-workflow-starter test -Dtest='WorkflowNoticeDomainEventSubscriberTest,WorkflowNoticeAutoConfigurationTest,WorkflowMessageTemplateResourceProviderTest'` | Tests run: 18, Failures: 0, Errors: 0，BUILD SUCCESS |
| `mango-workflow-core` | `mvn -o -Drevision=1.0.0-SNAPSHOT-mango-031 -pl mango-platform/mango-workflow/mango-workflow-core test -Dtest='WorkflowBusinessApplyServiceImplIntegrationTest,WorkflowEventPublisherTest'` | Tests run: 15, Failures: 0, Errors: 0，BUILD SUCCESS |
| 四模块全量测试 | `mvn -o -Drevision=1.0.0-SNAPSHOT-mango-031 -pl mango-platform/mango-workflow/mango-workflow-starter,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-notice/mango-notice-support verify` | core 102 项（4 skip）、starter 45 项全部通过，BUILD SUCCESS |
| 部分 Reactor 架构与 Java 质量门禁 | `mvn -o -Drevision=1.0.0-SNAPSHOT-mango-031 -pl <四个模块>,:mango-architecture-verification -DskipTests -Dmango.architecture.skip=false -Dmango.architecture.mode=changed -Dmango.architecture.requireFullReactor=false -Dmango.architecture.base=$(git merge-base HEAD origin/main) -Dmango.check.changedOnly=true -Dmango.check.gate=no-new-violations -Dmango.check.baseRef=<同一 SHA> verify` | `gateStatus=PASS`、`newIssueCount=0`、`baselineIssueCount=246`、`toolFailureCount=0` |
| 架构债务预算 | `node mango-pmo/tools/check-architecture-debt-budget.mjs --baseline-only --base-ref <同一 SHA>` | PASS，`current=110, action=historical-increase-recorded`（未新增 identity、未上调预算） |
| 静态规则 | `mvn -o -Drevision=1.0.0-SNAPSHOT-mango-031 -pl <四个模块> checkstyle:check` | 0 Checkstyle violations |
| 文档 | `node mango-pmo/tools/check-business-guides.mjs` | 5 guides inspected，通过 |
| 文档 | `node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs` | 受影响的 `mango-workflow`、`mango-notice` README 均为 OK |
| 交付记录 | `node mango-pmo/tools/check-standard-delivery-record.mjs mango-docs/evidence/2026-11-10-workflow-notice-framework-gaps/standard-delivery-record.md` | PASS |
| 运行态（默认开关） | `mango dev start backend`（workspace slot 31，后端 18031，数据库 `mango_dev_mango_notice_framework_gaps_031`） | `ready http://127.0.0.1:18031/actuator/health`，`/actuator/health` 返回 200，`db` 组件 `UP`；日志无 `Application run failed`、无 Bean 重复定义错误。说明将订阅器从 `@Component` 改为自动配置声明后，真实 Spring Boot 上下文仍只注册一份。 |
| 运行态（关闭开关） | `JAVA_TOOL_OPTIONS="-Dmango.workflow.notice.enabled=false" mango dev start backend` | 进程环境确认含该属性；`/actuator/health` 返回 200，`Application run failed` 计数为 0。证明关闭默认通知不阻断启动。 |
| 运行态（模板同步模式） | 启动后回读 `select sync_mode, count(*) from resource_registry where resource_type='MESSAGE_TEMPLATE' group by sync_mode` | `SYNC_ONCE 72`，即所有已同步的消息模板声明均为 `SYNC_ONCE`。 |

## 7. 例外与剩余风险

- `mvn pmd:check` 在本机报 17 个 `PMDException`，全部为 PMD 6.42.0 处理 Java 21 源文件的解析失败，涉及 `WorkflowAutoConfiguration`、各 Controller 等未改动文件。把本次改动 stash 后在 `origin/main` 基线重跑仍为 17 个，属既有环境问题，不是本次引入；Java 质量结论以 `mango:architecture` 的 `gateStatus=PASS` 和 `newIssueCount=0` 为准。
- 未做“关闭开关后不产生审批消息”的端到端运行态验收：需要登录并真实提交一条审批流程。Bean 不注册和事件不发布已由 `WorkflowNoticeAutoConfigurationTest` 在真实自动配置装配下证明，`WorkflowNoticeDomainEventSubscriberTest` 证明事件转换逻辑不受影响；如需补齐，在 18031 上提交一条演示流程后对比 `notice_task` 新增行。
- 前端未改动：`@mango/notice` 的 `resolveNoticeTargetLocation` 已支持 query 与 `fallbackTargetKey`，本次只在后端产出结构化目标，未新增前端契约。
- 未处理 F-4（渠道能力矩阵）、F-5（`paramsSchema` 发送前校验）、F-6（实现节点 `eventNotify` 回调）。三者经评估不构成当前具体应用问题：
  - F-4：企业微信只输出 `title + 换行 + content`（`WecomNoticeChannelSender`），不读 `messageActions`；全部 `*MessageTemplateResourceProvider` 无 HTML 标签；邮件 HTML 已有嗅探。加能力声明拦不住模板写作错误，无观察到的收益。
  - F-5：现有 Provider 的 `paramsSchema` 与发送端参数一致，Workflow 订阅器已为可选字段补默认值；真实风险是运营拼错变量名，正确拦截点是模板保存校验，不是发送时。
  - F-6：已改为只补文档。`WorkflowTaskRuntimeService.triggerEventNotify` 只写 `EVENT_NOTIFY` 审计记录、不发 HTTP 请求；实现回调涉及 SSRF、超时、重试和幂等，属于新需求；删除 UI 需要跨前后端和存量 `designerJson` 迁移。
- `mango:check` 单独运行会因空基线报 618 条存量问题；按 CI 的部分 Reactor 入口（带 `baseRef` + `no-new-violations`）运行得到 `gateStatus=PASS`，以该入口为准。
- 存量应用升级后，已声明的 `workflow.*` 消息模板资源会以 `SYNC_ONCE` 生效：这是修正语义，不是破坏性变更，但运营此前被覆盖的文案无法自动恢复，需要重新编辑一次。
- 未提交、未 Push、未创建 PR，按用户未授权处理。
