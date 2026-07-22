# Mango Workflow

`mango-workflow` 提供审批流平台能力。业务模块可以用它管理流程定义、发布流程、创建业务申请、发起审批、处理待办、查询审批进度和维护流程模板。

## 1. 概览

这个模块面向两类使用者：

- 后端业务模块：引入 API 契约，创建申请、发起流程、查询业务单据最新审批状态。
- 管理后台：注册流程定义、流程模板、任务列表、任务详情和自定义申请页面。

后端包分为三层：

| 包 | Maven 坐标 | 什么时候用 |
|----|------------|------------|
| `mango-workflow-api` | `io.mango.platform.workflow:mango-workflow-api` | 业务模块开发期依赖。只需要命令、查询、VO、枚举和 Java API 时引入它。 |
| `mango-workflow-core` | `io.mango.platform.workflow:mango-workflow-core` | workflow 内部实现包。业务模块通常不直接依赖。 |
| `mango-workflow-starter` | `io.mango.platform.workflow:mango-workflow-starter` | 部署 workflow 服务能力时引入。它注册 mapper、service、controller 和模块元数据。 |

前端包是 `@mango/workflow`，包含三类能力：

| 标识 | 能力 |
|------|------|
| `admin-pages` | 流程定义、流程模板、任务列表、任务详情、发起流程、自定义申请页面。 |
| `business-component` | 运行时表单渲染、审批进度树、审批时间线、业务申请和业务审批组件。 |
| `api-client` | workflow 后端接口封装。 |

## 2. 功能清单

| 能力 | 使用入口 |
|------|----------|
| 流程分类 | `/workflow/categories` 接口和流程管理页面。 |
| 流程定义 | `/workflow/definitions` 接口和流程定义页面。 |
| 流程发布 | `/workflow/definitions/deploy` 或 `WorkflowDefinitionApi.ensurePublished()`。 |
| 流程模板 | `/workflow/templates` 接口和流程模板页面。 |
| 业务申请 | `WorkflowBusinessApplyApi` 或 `/workflow/business-applies`。 |
| 发起流程 | `WorkflowProcessApi.start()` 或 `/workflow/processes/start`。 |
| 待办和已办 | `/workflow/tasks/todo`、`/workflow/tasks/done`、任务列表页面。 |
| 审批处理 | `/workflow/tasks/complete`、`/workflow/tasks/complete-result`、`/workflow/tasks/reject`、`/workflow/tasks/return`、`/workflow/tasks/save`、`/workflow/tasks/transfer`、`/workflow/tasks/add-sign`。 |
| 抄送 | `/workflow/tasks/copied`、`/workflow/tasks/copied/read`。 |
| 业务进度查询 | `WorkflowBusinessProcessApi.latestByBusinessKeys()` 或 `/workflow/business-applies/progress/latest-batch`。 |

## 3. 后端接入

业务模块开发时只依赖 API 契约：

```xml
<dependency>
    <groupId>io.mango.platform.workflow</groupId>
    <artifactId>mango-workflow-api</artifactId>
</dependency>
```

同一个后端应用需要提供 workflow 服务能力时，再引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.workflow</groupId>
    <artifactId>mango-workflow-starter</artifactId>
</dependency>
```

starter 的启用条件：

- classpath 中存在 `WorkflowDefinitionMapper`。
- `mango.workflow.enabled=true` 或未配置。
- 自动扫描 `io.mango.workflow.core.mapper`。
- 自动扫描 `io.mango.workflow.core` 和 `io.mango.workflow.starter`。

模块元数据位于 `mango-workflow-starter/src/main/resources/META-INF/mango/module.properties`：

```properties
module-name=mango-workflow
module-path=/workflow
```

常用 Java API：

| API | 用途 |
|-----|------|
| `WorkflowBusinessApplyApi.create()` | 创建业务申请记录。 |
| `WorkflowBusinessApplyApi.page()` | 查询申请分页。 |
| `WorkflowBusinessApplyApi.detail()` | 查询申请详情。 |
| `WorkflowBusinessApplyApi.history()` | 按业务类型和业务主键查询历史申请。 |
| `WorkflowBusinessApplyApi.latestProgress()` | 查询单个或批量业务单据的最新申请进度。 |
| `WorkflowBusinessApplyApi.latestByBusinessKeys()` | 批量查询业务主键对应的最新申请。 |
| `WorkflowBusinessProcessApi.latestByBusinessKeys()` | 业务列表补充流程状态时使用的窄接口。 |
| `WorkflowDefinitionApi.ensurePublished()` | 业务模块内置流程定义时，幂等确保流程已发布。 |
| `WorkflowProcessApi.start()` | 发起流程实例。 |

业务模块只能依赖 `mango-workflow-api` 暴露的 `XxxApi`、Command、Query、VO、Enum 和事件 payload 契约。禁止业务模块注入或调用 `mango-workflow-core` 的 `IWorkflow*Service`、`Workflow*ServiceImpl`、`WorkflowEventPublisher`、`WorkflowDomainEvents` 等内部实现；本地单体由 `mango-workflow-starter` 的 Controller 承载 API Bean，远程调用由 `mango-workflow-starter-remote` 的 Feign client 承载 API Bean。

创建申请字段：

| 字段 | 必填 | 含义 |
|------|------|------|
| `businessType` | 是 | 业务类型，最长 128。 |
| `businessKey` | 是 | 业务主键，最长 128。 |
| `applyCode` | 否 | 申请编号；为空时后端生成。 |
| `applyTitle` | 是 | 申请标题，最长 255。 |
| `applySummary` | 否 | 申请摘要，最长 1000。 |
| `processDefinitionId` | 否 | Mango 流程定义 ID。 |
| `processDefinitionKey` | 否 | 流程定义编码。 |
| `renderMode` | 否 | 渲染模式，默认 `DYNAMIC_FORM`。 |
| `applyPageKey` | 否 | 自定义申请页 key。 |
| `approvePageKey` | 否 | 自定义审批页 key。 |
| `formKey` | 否 | 表单 key。 |
| `formVersion` | 否 | 表单版本。 |
| `formJsonSnapshot` | 否 | 动态表单 JSON 快照。 |
| `formDataSnapshot` | 否 | 动态表单数据快照。 |
| `snapshotRef` | 否 | 业务快照引用，最长 255。 |
| `snapshotDigest` | 否 | 业务快照摘要，最长 128。 |
| `reapplyFromApplyId` | 否 | 重新申请来源申请 ID。 |
| `variables` | 否 | 流程变量。 |
| `extension` | 否 | 扩展配置。 |

发起流程字段：

| 字段 | 规则 | 含义 |
|------|------|------|
| `definitionId` | 和 `definitionKey` 至少传一个 | Mango 流程定义 ID。 |
| `definitionKey` | `definitionId` 为空时使用 | 按流程定义编码发起最新已发布流程。 |
| `businessKey` | 否 | 业务主键；为空时后端生成。 |
| `businessType` | 否 | 业务类型。 |
| `applyId` | 否 | 已创建的业务申请 ID。 |
| `renderMode` | 否 | 申请审批渲染模式。 |
| `applyPageKey` | 否 | 自定义申请页 key。 |
| `approvePageKey` | 否 | 自定义审批页 key。 |
| `snapshotRef` | 否 | 业务快照引用。 |
| `variables` | 否 | 发起表单变量。 |
| `selectedAssignees` | 否 | 发起人自选审批人，key 为节点 ID 或节点定义 key。 |

Java 调用示例：

```java
CreateWorkflowBusinessApplyCommand apply = new CreateWorkflowBusinessApplyCommand();
apply.setBusinessType("expense");
apply.setBusinessKey(String.valueOf(expenseId));
apply.setApplyTitle("费用报销审批");
apply.setProcessDefinitionKey("expense_reimbursement");
apply.setSnapshotRef("expense:" + expenseId);
apply.setVariables(Map.of("amount", amount, "deptId", deptId));

Long applyId = workflowBusinessApplyApi.create(apply).getData().getId();

StartWorkflowProcessCommand start = new StartWorkflowProcessCommand();
start.setDefinitionKey("expense_reimbursement");
start.setBusinessType("expense");
start.setBusinessKey(String.valueOf(expenseId));
start.setApplyId(applyId);
start.setVariables(apply.getVariables());
workflowProcessApi.start(start);
```

### 3.1 业务模块使用指南

业务模块通常只接入 `mango-workflow-api`，通过 Java API 发起流程、查询进度和订阅事件；只有承载 workflow 运行时的应用才接入 `mango-workflow-starter`。

典型接入步骤：

| 步骤 | 业务模块动作 | Workflow 入口 |
|------|--------------|---------------|
| 1 | 保存业务主表、明细、附件关系和业务快照引用 | 业务模块自有 service |
| 2 | 创建业务申请记录并发起流程实例 | `WorkflowProcessApi.startBusinessWorkflow()` / `POST /workflow/processes/start-business` |
| 3 | 业务列表批量展示审批状态、当前处理人和候选人 | `WorkflowBusinessApplyApi.latestProgress(businessType, businessKeys)` / `POST /workflow/business-applies/progress/latest-batch` |
| 4 | 审批页办理任务后立即刷新页面状态 | `POST /workflow/tasks/complete-result`、`reject-result`、`save-result`、`claim-result`、`unclaim-result` |
| 5 | 兼容旧代码时手工创建申请后发起流程 | `WorkflowBusinessApplyApi.create()` + `WorkflowProcessApi.start()` |
| 6 | 业务后台异步回写通过、驳回、当前节点或通知摘要 | 订阅 workflow 领域事件 |

业务模块推荐保存的关联字段：

| 字段 | 来源 | 用途 |
|------|------|------|
| `businessType` | 业务模块定义 | 区分业务类型，事件订阅和流程查询都依赖它。 |
| `businessKey` | 业务主键或业务编号 | 连接业务单据和 workflow 申请/流程。 |
| `applyId` | `WorkflowBusinessApplyApi.create()` 返回 | 查询申请详情、进度和历史记录。 |
| `processInstanceId` | `WorkflowProcessApi.start()` 返回 | 排查流程运行时、幂等回写和审计。 |
| `snapshotRef` | 业务模块定义 | 审批时读取稳定业务快照，避免审批中业务数据漂移。 |

业务模块不应直接查询 `workflow_business_apply`、`workflow_business_apply_current_task` 或 Flowable 运行时表，也不应依赖 workflow core service 读取这些数据。列表页使用批量进度 API，详情页使用申请/任务详情 API，办理动作使用 result API 返回的快照；这样租户、数据权限、候选人和分页规则都由 workflow 模块统一处理。

`POST /workflow/processes/start-business` 一次完成申请创建和流程发起，返回 `applyId`、`processInstanceId`、`processStatus`、第一个当前任务、`claimStatus`、`candidateUsers`、`candidateGroups` 和 `currentTasks`。业务侧已有申请记录创建逻辑时，仍可保留 `WorkflowBusinessApplyApi.create()` + `WorkflowProcessApi.start()` 兼容模式。

业务页面处理“审批通过”时有两种模式：

| 模式 | 入口 | 适合场景 |
|------|------|----------|
| 兼容模式 | `POST /workflow/tasks/complete` | 只关心任务完成成功，随后由列表或详情重新查询状态。 |
| 结果模式 | `POST /workflow/tasks/complete-result` | 完成任务后立即拿到刷新后的申请状态、当前节点、认领状态和候选人。 |

业务模块订阅事件时，实现 `DomainEventSubscriber` 并按 `businessType` 过滤：

```java
package com.example.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.DomainEventSubscriber;
import io.mango.workflow.api.WorkflowEventTypes;
import io.mango.workflow.api.vo.WorkflowEventPayloadVO;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ExpenseWorkflowEventSubscriber implements DomainEventSubscriber {

    private static final String BUSINESS_TYPE = "expense";
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            WorkflowEventTypes.TASK_ADVANCED,
            WorkflowEventTypes.PROCESS_COMPLETED,
            WorkflowEventTypes.PROCESS_REJECTED);

    private final ExpenseApprovalService expenseApprovalService;
    private final ObjectMapper objectMapper;

    public ExpenseWorkflowEventSubscriber(ExpenseApprovalService expenseApprovalService,
                                          ObjectMapper objectMapper) {
        this.expenseApprovalService = expenseApprovalService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String eventType() {
        return "*";
    }

    @Override
    public void onEvent(DomainEvent event) {
        if (event == null
                || !SUPPORTED_EVENTS.contains(event.getEventType())
                || !BUSINESS_TYPE.equals(event.getBusinessType())) {
            return;
        }
        String businessKey = event.getBusinessKey();
        WorkflowEventPayloadVO payload = objectMapper.convertValue(event.getPayload(), WorkflowEventPayloadVO.class);
        if (WorkflowEventTypes.TASK_ADVANCED.equals(event.getEventType())) {
            expenseApprovalService.syncCurrentApprovalTask(businessKey, payload);
        } else if (WorkflowEventTypes.PROCESS_COMPLETED.equals(event.getEventType())) {
            expenseApprovalService.markApproved(businessKey, payload);
        } else if (WorkflowEventTypes.PROCESS_REJECTED.equals(event.getEventType())) {
            expenseApprovalService.markRejected(businessKey, payload);
        }
    }
}
```

事件订阅方使用 `WorkflowEventTypes` 判断类型，使用 `WorkflowEventPayloadVO` 解析 payload。不要引用 `io.mango.workflow.core.event.WorkflowDomainEvents` 或 `WorkflowEventPublisher`，这些类只属于 workflow 内部发布实现。

业务事件处理建议：

| 业务目标 | 使用事件 | 说明 |
|----------|----------|------|
| 同步下一节点、当前办理人、待办摘要 | `workflow.task.advanced` | 该事件在 `workflow_business_apply_current_task` 刷新后发布。 |
| 记录办理动作审计 | `workflow.task.completed` | 该事件只表示当前任务刚完成，不代表下一节点快照已刷新。 |
| 回写业务通过状态 | `workflow.process.completed` | 流程正常结束后发布。 |
| 回写业务驳回状态 | `workflow.process.rejected` | 流程被驳回后发布。 |
| 做流程结束清理 | `workflow.process.ended` | 驳回或终止都会触发。 |

事件订阅方应把 `eventId`、`processInstanceId + completedTaskId` 或 `businessType + businessKey + eventType` 作为幂等键，避免跨实例或跨服务投递时重复回写业务状态。

## 4. 前端接入

安装前端包：

```bash
pnpm add @mango/workflow
```

管理后台需要注册页面时，从 `@mango/workflow` 的 `admin-pages` 子入口导入注册函数：

```ts
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';

registerMangoWorkflowAdminPages();
```

公开导出：

| 导出 | 标识 | 用途 |
|------|------|------|
| `WorkflowDefinitionView` | `admin-pages` | 流程定义管理页。 |
| `WorkflowTemplateView` | `admin-pages` | 流程模板管理页。 |
| `WorkflowTaskListView` | `admin-pages` | 待办、已办、抄送任务列表页。 |
| `WorkflowTaskDetailView` | `admin-pages` | 任务详情和审批处理页。 |
| `WorkflowStartProcessView` | `admin-pages` | 发起流程页。 |
| `WorkflowCustomApplyView` | `admin-pages` | 自定义申请页。 |
| `RuntimeFormRenderer` | `business-component` | 动态表单运行时渲染组件。 |
| `WorkflowProgressTree` | `business-component` | 审批进度树。 |
| `WorkflowApprovalTimeline` | `business-component` | 审批时间线。 |
| `WorkflowNodeTimeline` | `business-component` | 节点时间线。 |
| `api/workflow` | `api-client` | workflow HTTP API 封装。 |
| `workflowFormConfig` | `business-component` | workflow 表单配置。 |
| `components/runtimeForm` | `business-component` | 运行时表单组件导出。 |
| `components/businessApply` | `business-component` | 业务申请组件导出。 |
| `components/businessApproval` | `business-component` | 业务审批组件导出。 |

页面注册 key：

| 页面 key | 用途 |
|----------|------|
| `workflow/definition/index` | 流程定义管理。 |
| `system/workflow-definition/index` | 兼容流程定义管理入口。 |
| `workflow/template/index` | 流程模板管理。 |
| `workflow-template/index` | 兼容流程模板管理入口。 |
| `workflow/task/todo/index` | 待办任务。 |
| `workflow/task/initiated/index` | 已发起任务列表入口。 |
| `workflow/task/done/index` | 已办任务。 |
| `workflow/task/copied/index` | 抄送任务。 |
| `workflow/task-list/index` | 通用任务列表。 |
| `workflow/task/detail/index` | 任务详情。 |
| `workflow/start-process/index` | 发起流程。 |
| `workflow/custom-apply/index` | 自定义申请。 |

额外前端路由：

| 路由 | 组件 key | 权限码 |
|------|----------|--------|
| `/workflow/task/detail` | `workflow/task/detail/index` | `workflow:task:detail` |
| `/workflow/custom-apply` | `workflow/custom-apply/index` | `workflow:custom-apply` |

## 5. 资源注入

工作流默认分类、模板分类和设计器节点定义通过 `mango-resource` 注入，不在 Flyway 中写业务配置数据。资源文件放在：

```text
mango-workflow-starter/src/main/resources/META-INF/mango/resources/workflow-common-definition.yml
mango-workflow-starter/src/main/resources/META-INF/mango/resources/workflow-common-domain.yml
```

`workflow` 作为资源消费者提供以下处理器：

| 资源类型 | 落库表 | 合并键 |
|----------|--------|--------|
| `WORKFLOW_CATEGORY` | `workflow_category` | `tenantId + categoryCode` |
| `WORKFLOW_TEMPLATE_CATEGORY` | `workflow_template_category` | `tenantId + categoryCode` |
| `WORKFLOW_NODE_DEFINITION` | `workflow_node_definition` | `tenantId + nodeDefinitionCode` |
| `WORKFLOW_DEFINITION` | `workflow_definition` | `tenantId + definitionKey` |

`workflow` 作为资源提供方还会向其它模块注入：

| 资源类型 | 目标模块 | 声明入口 | 内容 |
|----------|----------|----------|------|
| `BUSINESS_DOMAIN` | `domain` | `workflow-common-domain.yml` | 工作流业务域 |
| `MESSAGE_TEMPLATE` | `notice` | `WorkflowMessageTemplateResourceProvider` | `workflow.task.assigned`、`workflow.task.claimable`、`workflow.task.cc`、`workflow.task.rejected`、`workflow.process.completed`、`workflow.process.rejected`、`workflow.process.ended`、`workflow.task.empty-assignee` |

通知模板通过 Java `ResourceProvider` 声明，字段契约以 `mango-notice` 的 `MESSAGE_TEMPLATE` 说明为准。工作流节点只发布 `NoticeSendEvent`，由 notice 本地或远程 starter 在事务提交后发送，通知失败不阻断流程主链路。

通用约束：

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，使用雪花 ID 字符串。 |
| `version` | `INT` | 是 | 资源版本，声明内容升级时递增。 |
| `biz-key` | `STRING` | 是 | 资源业务键，例如 `workflow.node.approval`。 |
| `target-module` | `STRING` | 是 | 固定为 `workflow`。 |

删除规则：

| 操作 | 行为 |
|------|------|
| `disable` | 将目标资源 `status` 更新为 `0`。 |
| `delete` | 物理删除目标资源行。 |

### 5.1 WORKFLOW_CATEGORY

`WORKFLOW_CATEGORY` 用于初始化流程分类。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `categoryId` | `LONG` | 否 | 流程分类稳定 ID，不填时使用资源 ID。 |
| `tenantId` | `LONG` | 否 | 租户 ID，默认 `1`。 |
| `categoryCode` | `STRING` | 是 | 分类编码，租户内唯一。 |
| `categoryName` | `STRING` | 是 | 分类名称。 |
| `domainCode` | `STRING` | 否 | 业务域编码，默认 `COMMON`。 |
| `sort` | `INT` | 否 | 排序号，默认 `0`。 |
| `status` | `INT` | 否 | `1` 启用，`0` 禁用，默认 `1`。 |
| `remark` | `STRING` | 否 | 备注。 |

### 5.2 WORKFLOW_TEMPLATE_CATEGORY

`WORKFLOW_TEMPLATE_CATEGORY` 用于初始化流程模板分类。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `categoryId` | `LONG` | 否 | 模板分类稳定 ID，不填时使用资源 ID。 |
| `tenantId` | `LONG` | 否 | 租户 ID，默认 `1`。 |
| `parentId` | `LONG` | 否 | 父分类 ID。 |
| `categoryCode` | `STRING` | 是 | 模板分类编码，租户内唯一。 |
| `categoryName` | `STRING` | 是 | 模板分类名称。 |
| `icon` | `STRING` | 否 | 前端图标标识。 |
| `sort` | `INT` | 否 | 排序号，默认 `0`。 |
| `status` | `INT` | 否 | `1` 启用，`0` 禁用，默认 `1`。 |
| `remark` | `STRING` | 否 | 备注。 |

### 5.3 WORKFLOW_NODE_DEFINITION

`WORKFLOW_NODE_DEFINITION` 用于初始化流程设计器可选节点定义。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `nodeDefinitionId` | `LONG` | 否 | 节点定义稳定 ID，不填时使用资源 ID。 |
| `tenantId` | `LONG` | 否 | 租户 ID，默认 `1`。 |
| `nodeDefinitionCode` | `STRING` | 是 | 节点定义编码，租户内唯一。 |
| `nodeType` | `STRING` | 是 | 节点类型，例如 `APPROVAL`、`SERVICE`。 |
| `nodeName` | `STRING` | 是 | 节点名称。 |
| `categoryCode` | `STRING` | 是 | 节点分类编码。 |
| `categoryName` | `STRING` | 是 | 节点分类名称。 |
| `description` | `STRING` | 否 | 节点说明。 |
| `bpmnType` | `STRING` | 是 | BPMN 节点类型。 |
| `executionType` | `STRING` | 是 | 执行类型。 |
| `color` | `STRING` | 否 | 前端展示颜色。 |
| `icon` | `STRING` | 否 | 前端图标标识。 |
| `propertySchema` | `JSON` | 否 | 节点属性 Schema。 |
| `defaultProperties` | `JSON` | 否 | 节点默认属性。 |
| `sort` | `INT` | 否 | 排序号，默认 `0`。 |
| `status` | `INT` | 否 | `1` 启用，`0` 禁用，默认 `1`。 |

### 5.4 WORKFLOW_DEFINITION

`WORKFLOW_DEFINITION` 用于初始化业务内置流程定义。资源处理器会按 `tenantId + definitionKey` 幂等创建或更新流程定义，并发布到 Flowable。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `tenantId` | `LONG` | 否 | 租户 ID，默认 `1`。 |
| `domainCode` | `STRING` | 是 | 业务域编码。 |
| `categoryCode` | `STRING` | 是 | 流程分类编码。 |
| `categoryName` | `STRING` | 是 | 流程分类名称。 |
| `categorySort` | `INT` | 是 | 流程分类排序。 |
| `categoryRemark` | `STRING` | 否 | 流程分类备注。 |
| `orgId` | `LONG` | 否 | 所属组织 ID。 |
| `adminUsers` | `LIST` | 否 | 流程管理员用户名列表。 |
| `startEntryVisible` | `BOOLEAN` | 否 | 启动入口是否可见，默认 `true`。业务内嵌流程可设置为 `false`。 |
| `icon` | `STRING` | 否 | 流程图标。 |
| `definitionKey` | `STRING` | 是 | 流程编码，租户内唯一。 |
| `definitionName` | `STRING` | 是 | 流程名称。 |
| `designerJson` | `JSON` / `FILE` / `STRING` | 是 | 设计器 JSON 内容。`FILE` 只支持 `classpath:` 位置。 |
| `formCode` | `STRING` | 否 | 表单编码。 |
| `formJson` | `JSON` / `FILE` / `STRING` | 否 | 动态表单 JSON 配置。`FILE` 只支持 `classpath:` 位置。 |
| `remark` | `STRING` | 否 | 备注。 |

## 6. 数据权限

流程定义管理已接入角色数据权限。分页、已发布分页、详情、发布版本列表、发布版本详情，以及依赖详情读取的编辑、删除、状态调整、撤回和发布操作，都会按 `workflow:definition:list` 解析当前角色的数据范围。

字段映射：

| 数据权限语义 | workflow 映射 |
|--------------|---------------|
| 表名 | `workflow_definition` |
| 本人 | `created_by` |
| 组织、本人成员主部门、本人主部门及下级 | `org_id` |
| 租户 | `tenant_id` |

未安装授权数据权限能力时，workflow 保持原查询行为；安装后由 `DataScopeApplier` 追加本人、指定组织、本人主部门或本人主部门及下级范围条件，并校验 `workflow_definition` 存在当前规则需要的映射字段。租户隔离仍由 persistence 租户插件处理。

开启 demo 资源后，Workflow 会为租户 `1` 的默认 `ROLE_ADMIN` 以 `INIT_ONLY` 声明初始化 `workflow:definition:list = ALL`。该规则只作用于租户内的流程定义管理数据范围；不会影响 `startEntryVisible`，该字段仍只控制审批中心的发起流程入口。若管理员已经维护过该数据范围，后续同步会保留运行时配置。

## 7. 快速开始

1. 业务后端引入 `mango-workflow-api`；部署 workflow 能力的应用引入 `mango-workflow-starter`。
2. 管理后台安装 `@mango/workflow`，注册 workflow 页面。
3. 启动后确认 workflow starter 的 `AUTH_MENU` 资源已同步，流程菜单和权限已经进入 `authorization_menu`。
4. 在流程定义页面维护流程分类、流程定义、表单和节点配置。
5. 调用 `/workflow/definitions/deploy` 发布流程，或在业务初始化逻辑中调用 `WorkflowDefinitionApi.ensurePublished()`。
6. 业务单据提交审批前，先保存业务主表、业务明细、附件关系和业务快照引用。
7. 调用 `WorkflowBusinessApplyApi.create()` 创建业务申请。
8. 调用 `WorkflowProcessApi.start()` 发起流程。
9. 业务列表用 `WorkflowBusinessProcessApi.latestByBusinessKeys(businessType, keys)` 批量补充审批状态。
10. 审批页面用任务详情接口拿表单、变量、字段权限和当前节点配置，再调用任务处理接口。

### 启动入口可见性

流程定义支持 `startEntryVisible` 启动入口可见性，默认 `true`，保持既有流程可在审批中心“发起流程”入口展示。业务内嵌流程可以设置为 `false`，用于声明流程不能脱离业务对象独立发起。

- `startEntryVisible=true`：出现在审批中心发起流程列表，可由通用发起页启动。
- `startEntryVisible=false`：不出现在审批中心发起流程列表，也不作为通用发起入口展示。
- 业务页面或业务服务仍可通过 `WorkflowBusinessApplyApi.create()` 和 `WorkflowProcessApi.start()` 按 `definitionKey`、`businessType`、`businessKey` 发起流程。
- 管理端流程定义、版本、发布、启停、任务流转和业务上下文发起不受隐藏入口影响。
- 启动入口可见性不是权限控制；业务模块仍需校验业务发起权限、业务状态、快照和幂等。

## 7. 配置说明

配置写在应用的 `application.yml` 或对应环境配置文件中。

`mango.workflow.enabled` 控制 workflow starter 是否启用。默认启用。

正式资源与演示资源分开登记。生产环境默认只扫描 `META-INF/mango/resources/`；需要在空白开发或演示环境装载三条示例流程时，显式开启 Resource Registry 的 demo 扫描。

```yaml
mango:
  workflow:
    enabled: true
  resource:
    registry:
      demo-enabled: false
```

演示环境开启方式：

```yaml
mango:
  resource:
    registry:
      demo-enabled: true
```

演示声明位于 `META-INF/mango/demo/workflow-demo-definition.yml`，使用 `INIT_ONLY`，只在目标流程不存在时初始化，不覆盖用户后续维护的流程定义。

## 8. YAML 配置字段

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `mango.workflow.enabled` | `true` | 是否启用 workflow 自动配置。为 `false` 时不注册 mapper、service、controller 和初始化器。 |
| `mango.resource.registry.demo-enabled` | `false` | 是否额外扫描 `META-INF/mango/demo/`。只在空白开发或演示环境开启。 |

## 9. 返回字段

常用返回对象：

| 返回对象 | 主要字段 | 什么时候用 |
|----------|----------|------------|
| `WorkflowBusinessApplyVO` | 申请 ID、业务类型、业务主键、申请标题、流程定义、渲染模式、状态、当前任务、快照引用、变量、扩展信息。 | 申请分页、详情、历史申请和创建申请后回显。 |
| `WorkflowBusinessApplyProgressVO` | 业务类型、业务主键、申请 ID、流程实例 ID、流程状态、当前任务、当前处理人、认领状态、候选用户/组、发起和结束时间。 | 业务列表展示审批状态和当前处理节点，支持批量查询。 |
| `WorkflowBusinessProcessVO` | 业务类型、业务主键、申请 ID、流程实例 ID、流程状态和当前任务摘要。 | 业务侧批量查询最新流程状态。 |
| `WorkflowProcessInstanceVO` | 流程实例 ID、流程定义信息、业务主键、发起人、状态、开始时间。 | 发起流程后的结果回显。 |
| `WorkflowStartResultVO` | 申请 ID、流程实例 ID、流程状态、当前任务、认领状态、候选用户/组。 | 业务侧一体化创建申请并发起流程后的结果回显。 |
| `WorkflowTaskVO` | 任务 ID、流程实例、任务名称、办理人、候选信息、申请信息、创建时间。 | 待办、已办、抄送列表。 |
| `WorkflowTaskActionResultVO` | 已处理任务、业务申请、流程状态、当前任务、认领状态、候选用户/组、是否结束。 | 驳回、暂存、认领和释放后立即刷新业务状态。 |
| `WorkflowTaskDetailVO` | 任务详情、表单渲染配置、变量、审批记录、节点动作配置。 | 审批详情页渲染和按钮控制。 |
| `WorkflowDefinitionVO` | 流程定义 ID、编码、名称、分类、业务域、启动入口可见性、状态、发布版本和流程管理员。 | 流程定义管理和业务选择流程。 |
| `WorkflowDeployVO` | 部署 ID、流程定义 ID、流程定义 key、版本和发布结果。 | 发布流程或确保流程已发布后的结果。 |

## 10. API 与扩展

Workflow 参数校验约束统一由 `mango-workflow-api` 的 `XxxApi` 契约声明，starter Controller 通过接口继承约束，不再重复声明 Bean Validation 注解。HTTP 路径、请求与响应结构、校验规则、错误语义和流程业务逻辑均不变，调用方无需改造。

业务申请接口：

| 能力 | 接口 | 权限码 |
|------|------|--------|
| 创建业务申请 | `POST /workflow/business-applies` | `workflow:business-apply:create` |
| 申请分页 | `POST /workflow/business-applies/page` | `workflow:business-apply:list` |
| 申请详情 | `GET /workflow/business-applies/{applyId}` | `workflow:business-apply:detail` |
| 历史申请 | `GET /workflow/business-applies/history` | `workflow:business-apply:detail` |
| 最新进度 | `GET /workflow/business-applies/progress/latest` | `workflow:business-apply:detail` |
| 批量最新进度 | `POST /workflow/business-applies/progress/latest-batch` | `workflow:business-apply:list` |
| 按流程实例查申请 | `GET /workflow/business-applies/progress/by-process-instance` | `workflow:business-apply:detail` |
| 我的申请统计 | `GET /workflow/business-applies/my/summary` | `workflow:task:list` |

流程实例接口：

| 能力 | 接口 | 权限码 |
|------|------|--------|
| 发起流程 | `POST /workflow/processes/start` | `workflow:process:start` |
| 创建业务申请并发起流程 | `POST /workflow/processes/start-business` | `workflow:process:start` |
| 我的发起 | `GET /workflow/processes/initiated` | `workflow:task:list` |
| 流程详情 | `GET /workflow/processes/detail` | `workflow:process:detail` |
| 流程历史 | `GET /workflow/processes/history` | `workflow:process:detail` |

任务接口：

| 能力 | 接口 | 权限码 |
|------|------|--------|
| 待办 | `GET /workflow/tasks/todo` | `workflow:task:list` |
| 已发起任务入口 | `GET /workflow/tasks/initiated` | `workflow:task:list` |
| 已办 | `GET /workflow/tasks/done` | `workflow:task:list` |
| 任务详情 | `GET /workflow/tasks/detail` | `workflow:task:detail` |
| 审批通过 | `POST /workflow/tasks/complete` | `workflow:task:complete` |
| 审批通过并返回推进结果 | `POST /workflow/tasks/complete-result` | `workflow:task:complete` |
| 审批驳回 | `POST /workflow/tasks/reject` | `workflow:task:reject` |
| 审批驳回并返回结果 | `POST /workflow/tasks/reject-result` | `workflow:task:reject` |
| 审批退回 | `POST /workflow/tasks/return` | `workflow:task:return` |
| 暂存 | `POST /workflow/tasks/save` | `workflow:task:save` |
| 暂存并返回结果 | `POST /workflow/tasks/save-result` | `workflow:task:save` |
| 转办 | `POST /workflow/tasks/transfer` | `workflow:task:transfer` |
| 加签 | `POST /workflow/tasks/add-sign` | `workflow:task:add-sign` |
| 认领 | `POST /workflow/tasks/claim` | `workflow:task:claim` |
| 认领并返回结果 | `POST /workflow/tasks/claim-result` | `workflow:task:claim` |
| 释放 | `POST /workflow/tasks/unclaim` | `workflow:task:unclaim` |
| 释放并返回结果 | `POST /workflow/tasks/unclaim-result` | `workflow:task:unclaim` |
| 待办统计 | `GET /workflow/tasks/todo/summary` | `workflow:task:list` |
| 我的任务统计 | `GET /workflow/tasks/my/summary` | `workflow:task:list` |
| 抄送列表 | `GET /workflow/tasks/copied` | `workflow:task:list` |
| 抄送已阅 | `POST /workflow/tasks/copied/read` | `workflow:task:read-copied` |

`POST /workflow/tasks/complete`、`reject`、`save`、`claim`、`unclaim` 保持兼容，只返回布尔成功结果。业务审批页在办理后需要立即刷新业务申请状态、当前任务、当前办理人、认领状态或判断流程是否结束时，优先使用对应的 `*-result` 接口；返回体包含已处理任务、流程实例、是否结束、业务申请状态和刷新后的当前任务快照。

`complete-result` 返回字段：

| 字段 | 含义 |
|------|------|
| `completedTaskId` | 刚完成或发起退回的源 Flowable 任务 ID。 |
| `completedTaskDefinitionKey` | 刚完成或发起退回的源 BPMN 任务定义 key。 |
| `processInstanceId` | 流程实例 ID。 |
| `ended` | 流程是否已经结束。 |
| `applyId` | 业务申请 ID。 |
| `businessType` | 业务类型。 |
| `businessKey` | 业务主键。 |
| `applyStatus` | 刷新后的业务申请状态。 |
| `applyStatusName` | 刷新后的业务申请状态名称。 |
| `currentTaskNames` | 刷新后的当前节点名称，多个任务用逗号拼接。 |
| `currentTaskDefinitionKeys` | 刷新后的当前节点定义 key，多个任务用逗号拼接。 |
| `currentAssigneeNames` | 刷新后的当前处理人名称，多个任务用逗号拼接。 |
| `currentTaskId` / `currentTaskName` / `taskDefinitionKey` | 第一个当前任务的 ID、名称和定义 key。 |
| `assigneeId` / `assigneeName` | 第一个当前任务的处理人。 |
| `claimStatus` | 当前任务认领状态：`NONE`、`UNCLAIMED`、`ASSIGNED`。 |
| `candidateUsers` / `candidateGroups` | 当前任务候选用户和候选组。 |
| `currentTasks` | 刷新后的当前任务快照，来源于 `workflow_business_apply_current_task`，包含认领状态和候选人。 |

`POST /workflow/tasks/return` 用于把当前任务退回到历史用户任务节点，流程实例保持运行。入参支持 `targetTaskDefinitionKey`；不传时默认退回当前流程实例中最近一个已完成的不同用户任务节点。默认退回策略面向串行用户任务链路；并行、多实例、重复节点或需要固定业务语义的复杂流程，应在节点动作配置或业务审批页中显式传入 `targetTaskDefinitionKey`。接口返回结构与 `complete-result` 一致，业务审批页可以直接使用刷新后的 `currentTasks` 快照同步当前节点和当前办理人。退回场景也会发布 `workflow.task.advanced`，其中 `completedTask*` 表示发起退回的源任务。

工作流领域事件：

| 事件类型 | 发布时机 | 主要用途 |
|----------|----------|----------|
| `workflow.task.completed` | 当前任务完成记录写入后、流程推进快照刷新前 | 记录“哪个任务刚被完成”，不保证 `workflow_business_apply_current_task` 已是下一节点。 |
| `workflow.task.advanced` | 完成或退回任务后，流程运行时任务和业务申请当前任务快照刷新完成后 | 同步下一节点待办、刷新业务侧当前任务、发送 `workflow.task.assigned` 通知。 |
| `workflow.task.rejected` | 任务驳回并结束流程后 | 回写业务驳回状态和通知。 |
| `workflow.task.saved` | 任务暂存并刷新当前任务快照后 | 同步草稿或审批页状态。 |
| `workflow.task.claimed` | 候选任务被认领并刷新当前任务快照后 | 同步当前处理人和认领状态。 |
| `workflow.task.unclaimed` | 候选任务释放并刷新当前任务快照后 | 同步待领取状态和候选人。 |
| `workflow.process.completed` | 流程正常完成后 | 回写业务通过状态。 |
| `workflow.process.rejected` | 流程被驳回后 | 回写业务驳回状态。 |
| `workflow.process.ended` | 流程被驳回或终止后 | 做流程结束类清理。 |

事件通过 `mango-infra-event` 的 `IDomainEventPublisher` 发布。单体单实例默认可使用内存总线；单体多实例、微服务或微服务多实例部署时，应启用 `mango.event.outbox.enabled=true`，跨进程分发再配置 `mango.event.transport=redis-stream`。事件是至少一次投递语义，订阅方必须按 `eventId`、`processInstanceId + completedTaskId` 或业务主键自做幂等。需要同步拿到刷新后快照的前端/业务调用，不要依赖异步事件回读，应使用 `complete-result` 或 `return` 响应。业务订阅方只使用 `mango-workflow-api` 中的 `WorkflowEventTypes` 和 `WorkflowEventPayloadVO` 作为契约，禁止引用 workflow core 事件实现类。

`workflow.task.advanced` payload 字段：

| 字段 | 含义 |
|------|------|
| `processInstanceId` | 流程实例 ID。 |
| `eventType` | 事件类型，和 `DomainEvent.eventType` 保持一致。 |
| `tenantId` | 当前租户 ID。 |
| `appCode` / `realm` | 事件产生时的应用编码和登录域；异步 Notice 消费时用于恢复角色解析上下文。 |
| `operatorId` / `operatorName` | 当前操作人。 |
| `businessType` | 业务类型。 |
| `businessKey` | 业务主键。 |
| `applyId` | 业务申请 ID。 |
| `applicantId` / `applicantName` | 原申请人；流程完成、驳回或结束通知使用 `applicantId` 作为接收人。 |
| `completedTaskId` | 刚完成或发起退回的源任务 ID。 |
| `completedTaskDefinitionKey` | 刚完成或发起退回的源任务定义 key。 |
| `completedTaskName` | 刚完成或发起退回的源任务名称。 |
| `comment` | 审批意见。 |
| `ended` | 流程是否已经结束。 |
| `applyStatus` | 刷新后的业务申请状态编码。 |
| `applyStatusName` | 刷新后的业务申请状态名称。 |
| `currentTaskNames` | 刷新后的当前节点名称。 |
| `currentTaskDefinitionKeys` | 刷新后的当前节点定义 key。 |
| `currentAssigneeNames` | 刷新后的当前处理人名称。 |
| `currentTask` | 第一个当前任务快照。 |
| `taskId` / `taskDefinitionKey` / `taskName` | 第一个当前任务的 ID、定义 key 和名称。 |
| `assignee` | 第一个当前任务的处理人 ID。 |
| `assigneeId` | 第一个当前任务的处理人 ID。 |
| `assigneeName` | 第一个当前任务的处理人名称。 |
| `claimStatus` | 第一个当前任务认领状态：`NONE`、`UNCLAIMED`、`ASSIGNED`。 |
| `candidateUsers` / `candidateGroups` | 第一个当前任务候选用户和候选组。 |
| `currentTasks` | 刷新后的当前任务明细，包含 `taskId`、`taskDefinitionKey`、`taskName`、`assigneeId`、`assigneeName`、`claimStatus`、`candidateUsers`、`candidateGroups`、`arrivedAt`。 |
| `variables` | 流程变量快照。 |

`workflow.task.assigned` 按运行时任务发送：任务已有 `assigneeId` 时，只通知该办理人；任务尚未到人时，将候选用户以及 `ROLE:<id>`、`POST:<id>`、`ORG:<id>` 转成同一个任务的 Notice 接收目标，目标中的全部有效成员收到指向同一 `taskId` 的通知。并行或多实例产生多个运行时任务时，每个任务分别发送并使用 `eventId + taskId` 幂等，不能把接收人聚合到第一条任务。流程已经结束或没有可解析接收人时跳过通知；`ORG_LEADER:<id>` 不会降级为全组织通知。

`workflow.process.completed`、`workflow.process.rejected` 和 `workflow.process.ended` 使用业务申请中的 `applicantId` 通知原申请人。事件异步转为 Notice 时会连同 `tenantId`、`appCode`、`realm` 恢复应用上下文，确保角色候选目标按原应用和登录域解析。

事件消费选择：

| 场景 | 推荐入口 |
|------|----------|
| 审批页点通过后立即刷新页面、业务卡片或跳转判断 | 调用 `POST /workflow/tasks/complete-result`，直接使用响应里的刷新后快照。 |
| 业务模块异步回写审批中状态、下一节点处理人或待办摘要 | 订阅 `workflow.task.advanced`。 |
| 审计“某个任务已完成”或记录办理动作 | 订阅 `workflow.task.completed`。 |
| 回写业务通过状态 | 订阅 `workflow.process.completed`。 |
| 回写业务驳回状态 | 订阅 `workflow.task.rejected` 或 `workflow.process.rejected`，按业务状态机选择一个入口并保持幂等。 |

部署形态建议：

| 部署形态 | 事件分发方式 | 说明 |
|----------|--------------|------|
| 单体单实例 | 本地事件总线即可 | 订阅方和 workflow 在同一 JVM 内，适合本地开发和简单部署。 |
| 单体多实例 | Outbox + 跨进程 transport | 多个实例都可能完成任务或消费事件，订阅方按事件 ID 或业务幂等键去重。 |
| 微服务单实例 | Outbox + 跨服务 transport | workflow 服务和业务服务不在同一进程，业务侧不要依赖本地 Spring 事件。 |
| 微服务多实例 | Outbox + 跨服务 transport | 按至少一次投递处理，业务回写、通知、待办同步都要有幂等键。 |

常用配置示例：

```yaml
mango:
  event:
    outbox:
      enabled: true
    transport: redis-stream
```

流程配置接口：

| 能力 | 接口 | 权限码 |
|------|------|--------|
| 流程分类分页 | `GET /workflow/categories/page` | `workflow:definition:list` |
| 流程分类列表 | `GET /workflow/categories/list` | `workflow:definition:list` |
| 流程分类详情 | `GET /workflow/categories/detail` | `workflow:definition:query` |
| 新增流程分类 | `POST /workflow/categories` | `workflow:definition:add` |
| 修改流程分类 | `PUT /workflow/categories` | `workflow:definition:edit` |
| 删除流程分类 | `DELETE /workflow/categories` | `workflow:definition:delete` |
| 流程定义分页 | `GET /workflow/definitions/page` | `workflow:definition:list` |
| 流程定义详情 | `GET /workflow/definitions/detail` | `workflow:definition:query` |
| 新增流程定义 | `POST /workflow/definitions` | `workflow:definition:add` |
| 修改流程定义 | `PUT /workflow/definitions` | `workflow:definition:edit` |
| 删除流程定义 | `DELETE /workflow/definitions` | `workflow:definition:delete` |
| 调整状态 | `PUT /workflow/definitions/status` | `workflow:definition:status` |
| 撤回未发布修改 | `POST /workflow/definitions/discard-draft` | `workflow:definition:edit` |
| 发布流程 | `POST /workflow/definitions/deploy` | `workflow:definition:deploy` |
| 确保已发布 | `POST /workflow/definitions/internal/ensure-published` | 内部接口 |
| 发布版本列表 | `GET /workflow/definitions/versions` | `workflow:definition:query` |
| 发布版本详情 | `GET /workflow/definitions/version-detail` | `workflow:definition:query` |
| 节点目录 | `GET /workflow/definitions/node-catalog` | `workflow:definition:query` |

模板接口：

| 能力 | 接口 | 权限码 |
|------|------|--------|
| 模板分类分页 | `GET /workflow/template-categories/page` | `workflow:template:list` |
| 模板分类列表 | `GET /workflow/template-categories/list` | `workflow:template:list` |
| 模板分类详情 | `GET /workflow/template-categories/detail` | `workflow:template:query` |
| 新增模板分类 | `POST /workflow/template-categories` | `workflow:template:add` |
| 修改模板分类 | `PUT /workflow/template-categories` | `workflow:template:edit` |
| 删除模板分类 | `DELETE /workflow/template-categories` | `workflow:template:delete` |
| 模板分页 | `GET /workflow/templates/page` | `workflow:template:list` |
| 模板详情 | `GET /workflow/templates/detail` | `workflow:template:query` |
| 新增模板 | `POST /workflow/templates` | `workflow:template:add` |
| 删除模板 | `DELETE /workflow/templates` | `workflow:template:delete` |
| 流程转模板 | `POST /workflow/templates/from-definition` | `workflow:template:add` |
| 模板导入流程 | `POST /workflow/templates/create-definition` | `workflow:template:create-definition` |
| 批量导入模板 | `POST /workflow/templates/import` | `workflow:template:create-definition` |
| 推送模板 | `POST /workflow/templates/push` | `workflow:template:push` |

节点执行扩展：

| 类型 | 用途 |
|------|------|
| `NONE` | 不执行额外动作。 |
| `EVENT_PUBLISH` | 发布 `WorkflowNodeExecutionEvent` Spring 事件。 |
| `SPRING_BEAN` | 调用实现 `WorkflowNodeExecutable` 的 Spring Bean。 |
| `HTTP_URL` | 当前执行器会失败，不能作为可用远程调用能力。 |
| `REMOTE_SERVICE` | 当前执行器会失败，不能作为可用远程调用能力。 |

Spring Bean 节点示例：

```java
package com.example.workflow;

import io.mango.workflow.core.engine.WorkflowNodeExecutable;
import io.mango.workflow.core.engine.WorkflowNodeExecutionContext;
import org.springframework.stereotype.Component;

@Component("orderApprovalCallback")
public class OrderApprovalCallback implements WorkflowNodeExecutable {

    @Override
    public void execute(WorkflowNodeExecutionContext context) {
        String processInstanceId = context.getExecution().getProcessInstanceId();
        Object businessKey = context.getVariables().get("businessKey");
        // 执行业务回调时要使用业务自己的幂等键。
    }
}
```

## 11. 数据与初始化

Flyway migration 路径：

```text
mango-workflow-core/src/main/resources/db/migration/workflow/V1__init_workflow.sql
mango-workflow-core/src/main/resources/db/migration/workflow/V2__add_workflow_audit_columns.sql
```

核心业务表：

```text
workflow_category
workflow_definition
workflow_template_category
workflow_template
workflow_node_definition
workflow_definition_version
workflow_form_instance
workflow_task_record
workflow_copied_task
workflow_business_apply
workflow_business_apply_current_task
workflow_business_apply_status_log
```

`V1__init_workflow.sql` 是当前空白数据库基线，只负责 12 张 Workflow 业务表及索引的 DDL，不写业务数据，也不写 Flowable 元数据。早期迁移的最终结构已经合并到 V1。Flowable 与 Workflow 表统一使用 `utf8mb4`；Flowable 标识列所在表使用 `utf8mb4_bin` 保持大小写敏感的二进制比较，布尔状态使用无显示宽度的 `tinyint`，可在 MySQL 8.4 空库中无 UTF8MB3 和整数显示宽度弃用告警地创建。

Mango Maven `1.0.20` 发布的 V1 缺少 7 个 Workflow 审计列，`1.0.21`/`1.0.22` 的 V1 已包含这些列。升级到包含 V2 的版本时，`beforeValidate__workflow_v1_checksum_compatibility.sql` 会把这两个已知历史 V1 checksum 修复为当前 V1 checksum，随后 V2 按 `information_schema.columns` 检查并补齐缺失列。由 `1.0.21`/`1.0.22` 创建的新数据库已有这些列，V2 会安全跳过已有列。其它未知 V1 checksum 仍由 Flyway 校验阻断，不能通过关闭 `validate-on-migrate` 绕过。

升级后可以检查 `flyway_schema_history_workflow` 中 V1、V2 均为成功状态，并核对以下列：

```text
workflow_task_record.created_by
workflow_copied_task.created_by
workflow_business_apply_current_task.created_by
workflow_business_apply_current_task.updated_by
workflow_business_apply_status_log.created_by
workflow_business_apply_status_log.updated_by
workflow_business_apply_status_log.updated_at
```

Flowable 启动所需的 `ACT_GE_PROPERTY` 元数据由 `WorkflowEngineMetadataInitializer` 在引擎初始化前按缺失项补齐。该访问明确绕过租户插件，因为 Flowable 元数据表没有 `tenant_id` 字段；随后仍由 Flowable 自己完成引擎配置项维护。

`ACT_*`、`FLW_*` 属于直接集成的 Flowable 引擎表，字段和主键由 Flowable 数据模型定义，默认不参与 Mango Persistence 的业务表 Schema 标准字段校验。Mango 自有 `workflow_*` 表仍必须通过 `id`、审计、租户和组织字段校验。

正式分类、业务域、节点目录和菜单等必需数据在 `META-INF/mango/resources/` 按资源类型声明，由 Resource Registry 同步；流程菜单、按钮权限等授权数据仍属于 authorization 数据边界。费用报销、合同盖章、请假三条示例流程单独位于 `META-INF/mango/demo/`，默认不加载，开启 `mango.resource.registry.demo-enabled=true` 后按 `INIT_ONLY` 初始化。

## 12. 管理入口

菜单由 `mango-workflow-starter/src/main/resources/META-INF/mango/resources/workflow-common-menu.json` 的 `AUTH_MENU` 资源注入，应用编码是 `internal-admin`。“审批中心”以 `parentCode=data` 挂载在“平台能力”下，既有 `/workflow` 路由、子路由和页面 key 均保持不变。

| 菜单 | 路径 | 组件 | 权限码 |
|------|------|------|--------|
| 流程管理 | `/workflow/manage` | 无，作为流程管理分组入口 | 无 |
| 流程模板 | `/workflow/manage/template` | `workflow/template/index` | `workflow:template:list` |
| 流程定义 | `/workflow/manage/definition` | `workflow/definition/index` | `workflow:definition:list` |

按钮和接口权限码：

```text
workflow:process:start
workflow:process:detail
workflow:task:list
workflow:task:detail
workflow:task:complete
workflow:task:reject
workflow:task:return
workflow:task:save
workflow:task:transfer
workflow:task:add-sign
workflow:task:claim
workflow:task:unclaim
workflow:task:read-copied
workflow:business-apply:create
workflow:business-apply:list
workflow:business-apply:detail
workflow:definition:list
workflow:definition:query
workflow:definition:add
workflow:definition:edit
workflow:definition:delete
workflow:definition:status
workflow:definition:deploy
workflow:template:list
workflow:template:query
workflow:template:add
workflow:template:edit
workflow:template:delete
workflow:template:create-definition
workflow:template:push
```

## 13. 问题排查

**引入 API 后没有 HTTP 接口**

`mango-workflow-api` 只提供契约。部署 workflow 服务能力的应用需要引入 `mango-workflow-starter`，并确认 `mango.workflow.enabled` 没有被设为 `false`。

**保存流程定义后仍不能发起**

保存只是草稿。需要调用 `POST /workflow/definitions/deploy` 发布，或调用 `WorkflowDefinitionApi.ensurePublished()` 确保流程已发布。

**业务列表要展示审批状态**

不要在业务 SQL 里直接拼 workflow 表。用 `WorkflowBusinessProcessApi.latestByBusinessKeys(businessType, keys)` 或 `POST /workflow/business-applies/progress/latest-batch` 批量查询。

**空白库没有演示流程**

这是默认行为。生产启动只同步正式资源；空白开发或演示环境需要示例流程时，设置 `mango.resource.registry.demo-enabled=true`，并确认 Resource Registry 已同步 `META-INF/mango/demo/workflow-demo-definition.yml`。

**默认管理员打开流程定义管理后列表为空**

确认 demo 资源同步成功，并检查租户 `1` 的 `ROLE_ADMIN` 是否存在有效的 `workflow:definition:list` 数据范围。Workflow demo 会以 `INIT_ONLY` 初始化 `ALL`；若该规则缺失，应检查 `AUTH_ROLE` 是否先于 `AUTH_ROLE_DATA_SCOPE` 同步成功，而不是通过修改 `startEntryVisible` 绕过管理数据权限。

**HTTP_URL 或 REMOTE_SERVICE 节点执行失败**

这两个执行器当前不是可用远程调用能力。需要节点回调时使用 `EVENT_PUBLISH` 或 `SPRING_BEAN`。

**菜单看不到流程管理**

检查 workflow starter 的 `AUTH_MENU` 资源是否同步，确认 `authorization_menu` 有 `/workflow/manage/template`、`/workflow/manage/definition`，并确认当前角色已获得对应 `workflow:template:list` 或 `workflow:definition:list` 权限。

**前端打开菜单提示页面不存在**

确认管理后台已经注册 `@mango/workflow` 的 `admin-pages` 子入口，并且菜单里的组件路径能映射到 `workflow/template/index` 或 `workflow/definition/index` 页面 key。

## 14. 相关文档

- [前端 workflow 包](../../../mango-ui/packages/workflow/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
