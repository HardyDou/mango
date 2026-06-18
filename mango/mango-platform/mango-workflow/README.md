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
| 审批处理 | `/workflow/tasks/complete`、`/workflow/tasks/reject`、`/workflow/tasks/save`、`/workflow/tasks/transfer`、`/workflow/tasks/add-sign`。 |
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

## 5. 数据权限

流程定义管理已接入角色数据权限。分页、已发布分页、详情、发布版本列表、发布版本详情，以及依赖详情读取的编辑、删除、状态调整、撤回和发布操作，都会按 `workflow:definition:list` 解析当前角色的数据范围。

字段映射：

| 数据权限语义 | workflow 映射 |
|--------------|---------------|
| 表名 | `workflow_definition` |
| 本人 | `created_by` |
| 组织、本人成员主部门、本人主部门及下级 | `org_id` |
| 租户 | `tenant_id` |

未安装授权数据权限能力时，workflow 保持原查询行为；安装后由 `DataScopeApplier` 追加本人、指定组织、本人主部门或本人主部门及下级范围条件，并校验 `workflow_definition` 存在当前规则需要的映射字段。租户隔离仍由 persistence 租户插件处理。

## 6. 快速开始

1. 业务后端引入 `mango-workflow-api`；部署 workflow 能力的应用引入 `mango-workflow-starter`。
2. 管理后台安装 `@mango/workflow`，注册 workflow 页面。
3. 启动后确认 workflow migration 已执行，流程菜单和权限已经进入 `authorization_menu`。
4. 在流程定义页面维护流程分类、流程定义、表单和节点配置。
5. 调用 `/workflow/definitions/deploy` 发布流程，或在业务初始化逻辑中调用 `WorkflowDefinitionApi.ensurePublished()`。
6. 业务单据提交审批前，先保存业务主表、业务明细、附件关系和业务快照引用。
7. 调用 `WorkflowBusinessApplyApi.create()` 创建业务申请。
8. 调用 `WorkflowProcessApi.start()` 发起流程。
9. 业务列表用 `WorkflowBusinessProcessApi.latestByBusinessKeys(businessType, keys)` 批量补充审批状态。
10. 审批页面用任务详情接口拿表单、变量、字段权限和当前节点配置，再调用任务处理接口。

## 7. 配置说明

配置写在应用的 `application.yml` 或对应环境配置文件中。

`mango.workflow.enabled` 控制 workflow starter 是否启用。默认启用。

`mango.workflow.samples.*` 控制启动时是否补齐内置示例流程。示例流程适合演示环境；生产环境不需要演示流程时应关闭。

```yaml
mango:
  workflow:
    enabled: true
    samples:
      enabled: false
```

需要保留示例流程时，可以指定写入租户、业务域和分类：

```yaml
mango:
  workflow:
    samples:
      enabled: true
      tenant-id: 1
      domain-code: COMMON
      category-code: COMMON
      category-name: 通用流程
```

## 8. YAML 配置字段

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `mango.workflow.enabled` | `true` | 是否启用 workflow 自动配置。为 `false` 时不注册 mapper、service、controller 和初始化器。 |
| `mango.workflow.samples.enabled` | `true` | 是否自动补齐内置示例流程。 |
| `mango.workflow.samples.tenant-id` | `1` | 示例流程写入的租户 ID。 |
| `mango.workflow.samples.category-code` | `COMMON` | 示例流程分类编码。 |
| `mango.workflow.samples.category-name` | `通用流程` | 示例流程分类名称。 |
| `mango.workflow.samples.domain-code` | `COMMON` | 示例流程业务域编码。 |

## 9. 返回字段

常用返回对象：

| 返回对象 | 主要字段 | 什么时候用 |
|----------|----------|------------|
| `WorkflowBusinessApplyVO` | 申请 ID、业务类型、业务主键、申请标题、流程定义、渲染模式、状态、当前任务、快照引用、变量、扩展信息。 | 申请分页、详情、历史申请和创建申请后回显。 |
| `WorkflowBusinessApplyProgressVO` | 业务类型、业务主键、申请 ID、流程实例 ID、状态、当前任务、当前处理人、发起人、更新时间。 | 业务列表展示审批状态和当前处理节点。 |
| `WorkflowBusinessProcessVO` | 业务类型、业务主键、申请 ID、流程实例 ID、流程状态和当前任务摘要。 | 业务侧批量查询最新流程状态。 |
| `WorkflowProcessInstanceVO` | 流程实例 ID、流程定义信息、业务主键、发起人、状态、开始时间。 | 发起流程后的结果回显。 |
| `WorkflowTaskVO` | 任务 ID、流程实例、任务名称、办理人、候选信息、申请信息、创建时间。 | 待办、已办、抄送列表。 |
| `WorkflowTaskDetailVO` | 任务详情、表单渲染配置、变量、审批记录、节点动作配置。 | 审批详情页渲染和按钮控制。 |
| `WorkflowDefinitionVO` | 流程定义 ID、编码、名称、分类、业务域、状态、发布版本和流程管理员。 | 流程定义管理和业务选择流程。 |
| `WorkflowDeployVO` | 部署 ID、流程定义 ID、流程定义 key、版本和发布结果。 | 发布流程或确保流程已发布后的结果。 |

## 10. API 与扩展

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

流程实例接口：

| 能力 | 接口 | 权限码 |
|------|------|--------|
| 发起流程 | `POST /workflow/processes/start` | `workflow:process:start` |
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
| 审批驳回 | `POST /workflow/tasks/reject` | `workflow:task:reject` |
| 暂存 | `POST /workflow/tasks/save` | `workflow:task:save` |
| 转办 | `POST /workflow/tasks/transfer` | `workflow:task:transfer` |
| 加签 | `POST /workflow/tasks/add-sign` | `workflow:task:add-sign` |
| 认领 | `POST /workflow/tasks/claim` | `workflow:task:claim` |
| 释放 | `POST /workflow/tasks/unclaim` | `workflow:task:unclaim` |
| 抄送列表 | `GET /workflow/tasks/copied` | `workflow:task:list` |
| 抄送已阅 | `POST /workflow/tasks/copied/read` | `workflow:task:read-copied` |

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
mango-workflow-core/src/main/resources/db/migration/workflow/V2__workflow_domain.sql
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

`V1__init_workflow.sql` 写入：

| 数据 | 表 | 幂等方式 |
|------|----|----------|
| 默认流程分类 `COMMON` | `workflow_category` | `ON DUPLICATE KEY UPDATE` |
| 默认模板分类 `COMMON_TEMPLATE` | `workflow_template_category` | `ON DUPLICATE KEY UPDATE` |
| 设计器节点目录 | `workflow_node_definition` | `ON DUPLICATE KEY UPDATE` |
| 流程菜单和按钮权限 | `authorization_menu` | `ON DUPLICATE KEY UPDATE` |
| 菜单包关系 | `authorization_menu_package_item` | `INSERT IGNORE` |
| 角色菜单关系 | `authorization_role_menu` | `INSERT IGNORE` |

`V2__workflow_domain.sql` 补充 workflow 业务域字段和索引。

启动期示例流程由 `WorkflowSampleDefinitionInitializer` 写入，配置来源是 `mango.workflow.samples.*`。默认会写入租户 `1`、分类 `COMMON`、业务域 `COMMON` 下的示例流程；不需要示例流程时关闭 `mango.workflow.samples.enabled`。

## 12. 管理入口

菜单由 `V1__init_workflow.sql` 写入 `authorization_menu`，应用编码是 `internal-admin`。

| 菜单 | 路径 | 组件 | 权限码 |
|------|------|------|--------|
| 流程管理 | `/workflow/manage` | 无，作为流程管理分组入口 | 无 |
| 流程模板 | `/workflow/manage/template` | `@/views/workflow/template/index.vue` | `workflow:template:list` |
| 流程定义 | `/workflow/manage/definition` | `@/views/workflow/definition/index.vue` | `workflow:definition:list` |

按钮和接口权限码：

```text
workflow:process:start
workflow:process:detail
workflow:task:list
workflow:task:detail
workflow:task:complete
workflow:task:reject
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

**启动后出现演示流程**

默认 `mango.workflow.samples.enabled=true`。生产环境不需要演示流程时，在环境配置中设置为 `false`。

**HTTP_URL 或 REMOTE_SERVICE 节点执行失败**

这两个执行器当前不是可用远程调用能力。需要节点回调时使用 `EVENT_PUBLISH` 或 `SPRING_BEAN`。

**菜单看不到流程管理**

检查 workflow migration 是否执行，确认 `authorization_menu` 有 `/workflow/manage/template`、`/workflow/manage/definition`，并确认当前角色已获得对应 `workflow:template:list` 或 `workflow:definition:list` 权限。

**前端打开菜单提示页面不存在**

确认管理后台已经注册 `@mango/workflow` 的 `admin-pages` 子入口，并且菜单里的组件路径能映射到 `workflow/template/index` 或 `workflow/definition/index` 页面 key。

## 14. 相关文档

- [前端 workflow 包](../../../mango-ui/packages/workflow/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
