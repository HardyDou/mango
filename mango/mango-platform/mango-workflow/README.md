# Mango Workflow 业务接入说明

`mango-workflow` 是通用流程编排与审批运行时模块。它负责流程定义、流程实例、待办、审批记录、节点表单权限和必要流程变量；具体业务数据、申请快照、审批页面和审批完成后的业务状态变更，应由接入业务模块自己负责。

## 模块边界

工作流模块应该保存：

- 流程定义、流程版本、BPMN/设计器 JSON。
- 流程实例 ID、业务主键、发起人、当前任务、历史任务。
- 流程判断需要的轻量变量，例如金额、类型、部门、申请人、审批结果。
- 申请时的流程变量快照，用于流程详情回显和审计辅助。
- 动态表单 JSON 以及字段权限 `HIDDEN`、`READONLY`、`EDITABLE`。

业务模块应该保存：

- 业务主表当前态，例如报销单、采购单、合同、用印单。
- 每次提交审批的申请快照，例如 `applyId`、`snapshotId`、申请内容 JSON、附件 ID、提交人、提交时间。
- 审批完成后需要回写的业务状态、业务日志、领域事件消费结果。
- 复杂业务申请页和复杂业务审批页。

不要把完整业务库表、所有历史版本、所有审批展示数据都塞进工作流变量。工作流变量只放流程判断、列表检索、通用详情回显和事件路由所需的关键数据。

## 推荐接入模型

一个业务单据可以发起多次审批，每次审批都生成新的申请记录和新的流程实例。

推荐字段关系：

| 字段 | 保存位置 | 说明 |
| --- | --- | --- |
| `businessType` | 工作流变量、业务申请记录 | 业务类型，例如 `EXPENSE_REIMBURSEMENT`。用于前端选择业务审批组件，也用于事件路由。 |
| `businessKey` | 工作流实例、业务主表 | 业务主键，例如报销单号。由业务模块生成，不建议用户在通用发起页手输。 |
| `applyId` / `snapshotId` | 工作流变量、业务申请记录 | 本次申请快照 ID。查看历史审批时按它读取当时的业务快照。 |
| `processInstanceId` | 业务申请记录 | 本次申请关联的流程实例 ID。 |
| `status` | 业务主表、业务申请记录、工作流实例 | 业务状态和流程状态可以不同，但需要可映射。 |

驳回后再次申请时，不激活旧实例。旧流程实例保持 `REJECTED` 或已结束状态，新提交创建新的申请记录和新的流程实例。业务详情页应能看到同一 `businessKey` 下的多次申请历史。

## 发起流程

当前后端接口：

```http
POST /workflow/processes/start
```

请求体：

```json
{
  "definitionId": 1001,
  "businessKey": "EXP-202605-001",
  "variables": {
    "businessType": "EXPENSE_REIMBURSEMENT",
    "businessKey": "EXP-202605-001",
    "applyId": "APPLY-EXP-202605-001-002",
    "title": "费用报销 EXP-202605-001",
    "summary": "差旅费 ¥1280.00",
    "amount": 1280,
    "category": "差旅费",
    "applicant": "admin",
    "businessPermissions": {
      "manager_approve": {
        "expenseReason": "READONLY",
        "invoiceInfo": "READONLY",
        "paymentInfo": "HIDDEN",
        "financeReview": "HIDDEN"
      },
      "finance_review": {
        "expenseReason": "READONLY",
        "invoiceInfo": "READONLY",
        "paymentInfo": "READONLY",
        "financeReview": "EDITABLE"
      }
    }
  },
  "selectedAssignees": {
    "manager_approve": ["zhangsan"]
  }
}
```

业务模块发起前应先保存业务当前态和本次申请快照。`start` 成功后，把返回的 `processInstanceId` 回写到申请记录。

## 审批处理

当前后端接口：

```http
GET  /workflow/tasks/detail?taskId=...
POST /workflow/tasks/complete
POST /workflow/tasks/reject
GET  /workflow/processes/detail?processInstanceId=...
GET  /workflow/processes/history?businessKey=...
```

审批页有两类实现方式：

- 动态表单：适合请假、通用申请、简单信息采集。前端按 `formJson` 渲染，并按 `formPermissions` 控制隐藏、只读、可编辑。
- 业务审批页：适合报销、采购、合同、用印等复杂业务。前端按 `businessType` 注册业务组件，再用 `businessKey + applyId` 展示业务申请快照和必要审批字段。
- 业务详情页可以按 `businessKey` 拉取历史流程实例列表，展示同一业务单据的多次申请记录。

节点可见性建议使用两层配置：

- 通用动态字段权限：流程定义节点上的 `formPermissions`，后端返回到任务详情。
- 业务页面区块权限：业务变量中的 `businessPermissions[taskDefinitionKey]`，由业务前端或业务后端解释。

审批通过提交变量示例：

```json
{
  "taskId": "250012",
  "comment": "票据齐全，同意报销",
  "variables": {
    "approvedAmount": 1280,
    "financeResult": "APPROVED"
  }
}
```

后端会把提交变量合并到流程实例保存的变量快照中，并写入审批记录。业务模块如果需要不可变审批快照，应在自己的申请记录或审批记录中保存，不要只依赖工作流变量当前值。

## 动态表单的定位

动态表单仍然有价值，但不要把它当作所有业务页面的替代品。

适合动态表单的场景：

- 通用办公申请，字段少，业务规则简单。
- 请假、外出、证明开具、简单物品领用。
- 流程定义人员希望自己调整字段和节点权限。

应使用业务自定义页面的场景：

- 数据来自多个业务表或聚合服务。
- 不同节点展示的信息差异很大。
- 有复杂附件、明细、预算占用、合同条款、风险提示、外部系统数据。
- 审批动作需要调用业务领域服务，而不是只更新几个流程变量。

推荐做法是：工作流主流程、任务列表、审批记录、节点权限保持通用；业务申请页和业务审批页通过 `businessType` 扩展。

## 审批结束如何通知业务

Workflow 通过 `mango-infra-event` 发布标准领域事件。默认是进程内同步分发；投产需要可靠投递时，开启 `mango-infra-kv` 的 Outbox capability，让 `IDomainEventPublisher` 先写入 Outbox，再由 `IOutboxDispatcher` 投递到事件总线。

```yaml
mango:
  kv:
    type: redis # memory / redis / jdbc，生产多实例建议 redis 或 jdbc
    capability:
      enabled: true
      outbox: true
  event:
    outbox:
      enabled: true
      worker-id: workflow-event-worker
      batch-size: 50
      retry-delay-seconds: 60
      dispatch-enabled: true
      dispatch-interval-millis: 1000
      dispatch-initial-delay-millis: 1000
```

业务消费方按 `businessType`、`eventType`、`processInstanceId`、`applyId` 幂等消费。Outbox 只负责可靠投递和重试；业务状态、申请快照、审批快照仍由业务模块自己落库。

建议事件：

| 事件 | 触发时机 | 业务用途 |
| --- | --- | --- |
| `workflow.process.started` | 流程实例创建成功 | 回写申请记录流程实例 ID。 |
| `workflow.task.completed` | 节点审批通过 | 写业务审批日志，必要时更新阶段状态。 |
| `workflow.task.rejected` | 节点驳回并结束实例 | 标记本次申请已驳回，业务主表可重新编辑。 |
| `workflow.process.completed` | 流程正常结束 | 将业务单据置为已通过、已生效、待付款等。 |
| `workflow.process.rejected` | 流程被驳回并结束 | 将本次申请置为驳回，业务主表可进入重新编辑态。 |
| `workflow.process.ended` | 流程被驳回、终止或自动结束 | 将业务申请记录置为结束态。 |

当前 workflow 已在流程发起、审批通过、审批驳回、流程完成、流程结束时发布上述标准事件。审批节点的事件通知配置仍会记录一条 `EVENT_NOTIFY` 审批记录，用来保留节点通知意图；`EventPublishWorkflowNodeExecutor` 是服务节点执行器，基于 Spring `ApplicationEventPublisher` 发布节点执行事件，不等同于跨模块可靠业务回调。

## 业务接入步骤

1. 建业务主表和申请记录表。
2. 设计 `businessType`，并确定 `businessKey` 生成规则。
3. 保存业务单据当前态。
4. 每次提交审批前保存申请快照，生成 `applyId`。
5. 调用 `/workflow/processes/start`，传入 `businessKey`、`businessType`、`applyId` 和流程判断所需变量。
6. 业务申请记录保存 `processInstanceId`。
7. 前端在审批详情里按 `businessType` 渲染业务审批组件。
8. 审批组件按 `taskDefinitionKey` 控制区块隐藏、只读、可编辑。
9. 审批完成后通过事件订阅更新业务状态。
10. 业务详情页按 `businessKey` 展示多次申请历史，按 `applyId` 查看当时快照。

## 示例：费用报销

费用报销是一个典型业务接入示例：

- 业务主表：保存当前报销单金额、事由、票据、收款信息、业务状态。
- 申请记录：每次提交保存当时金额、票据、收款账号脱敏策略、预算科目、`processInstanceId`。
- 工作流变量：保存 `businessType`、`businessKey`、`applyId`、金额、费用类型、申请人、节点区块权限。
- 部门经理节点：只能看报销事由和预算摘要，不显示完整收款账号。
- 财务复核节点：可以看票据和付款信息，并编辑核定金额。
- 驳回后：旧申请记录保持驳回，新申请会生成新 `applyId` 和新流程实例。

前端示例位于：

```text
mango-ui/packages/workflow/src/views/business-form/index.vue
mango-ui/packages/workflow/src/components/businessApproval.ts
mango-ui/packages/workflow/src/components/business/ExpenseApprovalDetail.vue
```

## 当前能力与待补齐能力

已具备：

- 流程分组、流程定义、发布到 Flowable。
- 发起流程、待办、已办、我的发起详情。
- 通过、驳回。
- 动态表单渲染和节点字段权限。
- 节点参与人、发起人自选审批人、空审批人策略。
- 任务审批记录和流程变量快照。
- `WorkflowTaskVO.taskDefinitionKey` 后端返回，前端可精确匹配当前节点业务区块权限。
- `mango-infra-event` 内存事件总线，以及 workflow 标准事件发布。
- `mango-infra-kv` Outbox 可靠投递能力，可用 memory / redis / jdbc 承载，不引入 MQ。

待补齐：

- 转办、加签、委托、撤回、暂存等运行时动作。
- 按 `businessKey` 查询历史流程实例和申请记录的业务侧标准接口。
- 业务申请快照/审批快照的领域建模示例和后端落库示例。
- 动态表单组件与公共数据源的后端元数据接口标准化。
