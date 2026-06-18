# 业务审批接入

## 1. 适用场景

业务单据需要发起审批，审批结束后回写业务状态，并能在业务页面查看流程进度。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
|------|------|--------|
| 1 | [Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md) | 流程定义、实例、任务、事件和配置 |
| 2 | [@mango/workflow README](../../../mango-ui/packages/workflow/README.md) | 流程页面、设计器、任务 API |
| 3 | [Workflow Example README](../../../mango-ui/packages/workflow-business-example/README.md) | 业务接入示例和页面 key |
| 4 | [能力地图：业务审批闭环](../../capabilities/README.md#3-组合接入入口) | 组合验证入口 |

## 3. 接入检查点

| 环节 | 检查点 |
|------|--------|
| 业务状态 | 业务单据状态区分草稿、审批中、通过、驳回、撤回等业务语义 |
| 流程定义 | 业务类型、流程 key、表单编码和版本关系清晰 |
| 发起审批 | 业务保存和流程发起的事务边界可解释，失败时能回滚或补偿 |
| 审批回调 | 监听流程完成、驳回、撤回等事件并回写业务状态 |
| 页面入口 | 业务详情页展示流程进度、当前任务和审批记录 |
| 权限 | 发起、审批、撤回、查看记录按业务角色和流程任务共同判断 |

## 4. 最小闭环

1. 新建业务单据并保存为草稿。
2. 发起审批后业务状态变为审批中。
3. 审批人能在任务列表看到待办。
4. 审批通过后业务状态变为通过。
5. 业务详情页能看到流程实例和审批记录。

## 5. 常见失败

| 现象 | 优先检查 |
|------|----------|
| 发起审批后没有待办 | 流程定义版本、节点办理人表达式、当前租户和组织数据 |
| 业务状态不更新 | 事件监听、回写服务、业务 ID 与流程 businessKey 映射 |
| 审批页打开空白 | 前端 workflow 包是否引入，页面 key 是否注册，接口是否 401/403 |
| 驳回后业务不可再次提交 | 业务状态流转是否覆盖驳回到草稿或重新提交 |
| 多租户流程串数据 | 流程定义、实例、任务和业务表 tenantId 是否一致 |

## 6. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-workflow -am test
pnpm -F @mango/workflow build
pnpm -F @mango/workflow-business-example build
```

模块验证入口：

- [Workflow 验证方式](../../../mango/mango-platform/mango-workflow/README.md#10-验证方式)
- [Workflow Frontend 验证方式](../../../mango-ui/packages/workflow/README.md#10-验证方式)
- [Workflow Example 验证方式](../../../mango-ui/packages/workflow-business-example/README.md#10-验证方式)

## 7. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 8. 变更影响记录

- PR #193 新增 `mango-resource` 注册中心并将工作流分类、模板分类、节点定义和消息模板默认数据迁移为资源声明同步；不改变业务审批发起、审批回调、状态回写、流程页面 key、权限、租户隔离和页面验收步骤。排查流程定义或节点定义缺失时，需要同时确认 `WORKFLOW_CATEGORY`、`WORKFLOW_TEMPLATE_CATEGORY` 和 `WORKFLOW_NODE_DEFINITION` 声明是否已同步。
- PR #171 将流程定义列表作为角色数据权限业务接入样例，`workflow:definition:list` 可按显式数据权限过滤；不改变业务审批发起、审批回调、状态回写、页面 key 和租户隔离方式。
- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变业务审批的公开 API、配置、权限、租户、页面和运行时行为。
- PR #157 支付异常单依赖环修复和 workflow API/core 边界收敛只调整内部 Bean 依赖，不改变业务审批接入的公开 API、配置、权限、租户、页面和运行时行为。
- PR 本次持久化基线与 README 发布物料治理只补充业务开发查看 Mango 能力文档的入口，并让 npm 包携带 package README；不改变业务审批发起、审批回调、状态回写、流程页面 key、权限、租户隔离、启动和运行时行为。
