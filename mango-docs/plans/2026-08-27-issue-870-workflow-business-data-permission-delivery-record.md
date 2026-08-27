# 标准交付记录

任务：Issue #870 Workflow 业务申请读取复用业务数据权限。

## 1. 元数据

- 任务 ID：GitHub Issue #870
- 交付模式：STANDARD
- 需求影响：L2 - 业务模块读取 Workflow 申请详情、历史和最新进度时需要复用业务数据权限。
- 方案风险：L2 - 读取接口边界扩大，必须保证租户、申请人和业务模块授权不会被绕过。
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-870`，`fix/issue-870-workflow-business-data-permission`）
- 启用能力：M01、M08、M09、M10、M11

## 2. 目标与范围

- 目标：为 Workflow 业务申请单项及批量读取提供统一的业务数据权限扩展点，消除对全局 `workflow:business-apply:detail` 的强依赖。
- 成功条件：业务 Provider 可按业务类型校验 owner、组织和租户；无权读取稳定返回 `APPLY_ACCESS_DENIED`；HTTP、Java API 和 Feign 使用同一服务层语义；内部流程链路不被用户态校验阻断。
- 处理范围：Workflow API 契约、业务申请服务、流程详情读取、批量最新进度过滤、模块 README 和定向测试。
- 不处理范围：业务模块具体 Provider 实现、Baohan 依赖升级和代理删除、Maven 制品发布及生产部署。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | 申请详情、历史、单项最新进度、流程实例申请 | 已登录且存在业务申请 | 根据 Workflow 持久化事实构造上下文并调用匹配 Provider | Provider 拒绝时返回 `WorkflowCode.APPLY_ACCESS_DENIED` | Checker 单测和服务集成测试 |
| REQ-002 | 批量最新进度和最新申请 | 批量 businessKey 请求 | 逐项执行数据权限过滤，不返回未授权 businessKey | 未授权记录从结果中移除，不以空对象泄露数据 | 批量服务测试 |
| REQ-003 | 流程详情 | 关联业务申请或纯 Workflow 流程 | 关联申请时复用服务层校验；纯流程保持原有登录语义 | 业务申请无权时拒绝，纯流程不存在申请时不受影响 | 任务运行时集成测试 |
| REQ-004 | 内部事件与任务推进 | 内部调用 `findByProcessInstance` | 保持内部读取语义 | 不因用户态数据权限阻断审批推进 | 现有任务测试回归 |

## 4. 技术决定

| ID | 决定 | 影响路径 |
|---|---|---|
| DEC-001 | 新增 `WorkflowBusinessApplyDataPermissionProvider` 和 `WorkflowBusinessApplyAccessVO`；Workflow 不直接访问业务表 | `mango-workflow-api` |
| DEC-002 | 新增统一 Checker；匹配 Provider 任一允许即通过，无 Provider 时仅允许当前租户申请人 | `mango-workflow-core` |
| DEC-003 | 用户态读取接口改为 LOGIN，由服务层统一校验；批量接口过滤未授权记录 | `WorkflowBusinessApplyController`、`WorkflowBusinessApplyService` |
| DEC-004 | 业务申请创建时保存 `applicantDeptId`，作为组织事实传给 Provider | `CreateWorkflowBusinessApplyCommand`、申请实体 |

## 5. 实施清单

| ID | 对应决定 | 改动路径 | 完成条件 |
|---|---|---|---|
| IMP-001 | DEC-001 | Workflow API 新增上下文、Provider 和注解 | 业务模块可依赖公开契约 |
| IMP-002 | DEC-002、DEC-003 | Workflow Core Checker、业务申请服务、任务运行时服务 | 单项和批量读取统一执行权限校验 |
| IMP-003 | DEC-003 | Workflow Starter Controller 与 API surface contract | 读取入口为 LOGIN 且契约指纹更新 |
| IMP-004 | 全部 | Workflow README、Issue 交付记录和定向测试 | 文档、编译和测试门禁通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001~REQ-004 | Workflow Core 编译 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core -am -DskipTests compile` | PASS | 直接修改模块编译成功 |
| REQ-001~REQ-004 | 服务与任务集成测试 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core -am -Dtest=WorkflowBusinessApplyServiceImplIntegrationTest,WorkflowTaskRuntimeServiceImplIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | 现有业务申请、任务运行时测试通过 |
| Checker 授权边界 | Checker 单测 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core -am -Dtest=WorkflowBusinessApplyAccessCheckerTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | Provider 拒绝、默认 owner/tenant、缺少上下文覆盖 |
| HTTP 契约 | Controller surface tests | `mvn -pl mango-platform/mango-workflow/mango-workflow-starter -am -Dtest=WorkflowApiControllerContractTest,WorkflowApiSurfaceContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | 读取接口 LOGIN 与指纹契约通过 |
| 文档与标准记录 | PMO 记录检查 | `node mango-pmo/tools/check-standard-delivery-record.mjs <本记录>` | PASS | 标准交付记录结构通过 |

## 7. 例外与剩余风险

- 本次未实现 Baohan 业务 Provider；业务仓库需要在升级 Workflow 依赖后接入 Provider 并删除临时代理路径。
- 尚未创建 PR、合并、发布或部署；生产权限数据和真实浏览器链路不在本次本地验证范围。
