# Mango Workflow

`mango-workflow` 是 Mango 的审批流能力模块。它基于 Flowable 7 承载流程定义、流程发布、流程实例、待办、已办、抄送、业务申请和进度查询；业务模块只保存自己的业务单据、业务快照和审批完成后的业务状态。

## 1. 概览
`mango-workflow` 解决“业务单据如何接入审批流”的平台问题，不负责保存业务主数据。

子模块：

| 子模块 | Maven 坐标 | 用途 |
|--------|------------|------|
| `mango-workflow-api` | `io.mango.platform.workflow:mango-workflow-api` | API 契约、命令、查询、VO、枚举、事件类型 |
| `mango-workflow-core` | `io.mango.platform.workflow:mango-workflow-core` | Flowable 集成、流程定义、任务运行时、业务申请、模板、mapper、migration |
| `mango-workflow-starter` | `io.mango.platform.workflow:mango-workflow-starter` | 自动配置、MapperScan、ComponentScan、HTTP Controller、模块元数据 |

核心能力：

- 流程分类：按租户和业务域管理流程分组。
- 流程定义：保存设计器 JSON、动态表单 JSON、流程管理员、状态和发布信息。
- 流程发布：把设计器 JSON 转成 BPMN，部署到 Flowable，并保存发布版本快照。
- 流程模板：把流程定义固化为模板，支持模板导入为租户自己的定义草稿，支持推送到目标租户。
- 业务申请：按 `businessType + businessKey` 建立业务单据和流程实例关系。
- 发起流程：按 Mango 流程定义 ID 或定义编码发起最新已发布流程。
- 任务处理：待办、已办、详情、通过、驳回、暂存、转办、加签、认领、释放、抄送已阅。
- 进度查询：按业务主键查询最新申请进度、批量查询业务列表审批状态。
- 节点执行：支持 `NONE`、`EVENT_PUBLISH`、`SPRING_BEAN`，并保留但默认拒绝 `HTTP_URL`、`REMOTE_SERVICE`。
- 菜单权限：migration 初始化流程管理菜单、流程模板、流程定义和任务按钮权限。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 请假、报销、合同、用印、采购、变更申请等需要审批流的业务单据 | Maven 依赖 / HTTP API / Java API |
| 业务列表需要展示每条记录的最新审批状态、当前任务和当前处理人 | Maven 依赖 / HTTP API / Java API |
| 管理端需要配置流程定义、模板和节点目录 | Maven 依赖 / HTTP API / Java API |
| 多租户场景中，每个租户维护自己的流程定义，平台可推送模板给目标租户 | Maven 依赖 / HTTP API / Java API |
| 业务模块需要内置流程定义，并在启动或安装时确保流程已发布 | Maven 依赖 / HTTP API / Java API |
| 审批节点需要调用受控的 Spring Bean 或发布 Spring 事件 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 请假、报销、合同、用印、采购、变更申请等需要审批流的业务单据。
- 业务列表需要展示每条记录的最新审批状态、当前任务和当前处理人。
- 管理端需要配置流程定义、模板和节点目录。
- 多租户场景中，每个租户维护自己的流程定义，平台可推送模板给目标租户。
- 业务模块需要内置流程定义，并在启动或安装时确保流程已发布。
- 审批节点需要调用受控的 Spring Bean 或发布 Spring 事件。

## 4. 边界说明
- 不替代业务单据主表、业务状态机、业务快照、业务附件关系。
- 不建议把完整业务对象塞进流程变量；变量只放流程判断、展示和审批表单需要的字段。
- 不作为定时任务或批处理引擎使用，调度归属 `mango-job`。
- 当前 `HTTP_URL` 和 `REMOTE_SERVICE` 节点执行器默认直接失败，生产不能把它们当成可用远程调用能力。
- 当前任务列表的 `/workflow/tasks/initiated` Controller 返回空页；“我的发起”使用 `/workflow/processes/initiated`。
- 不替代业务模块的权限判断；workflow 只能判断当前用户是否可操作当前流程任务。

## 5. 模块组成
workflow 保存平台审批运行时数据：

- 流程分类、流程定义、流程发布版本、模板和节点目录。
- Flowable `ACT_*` 引擎表。
- 表单实例快照、任务处理记录、抄送记录。
- 业务申请、当前任务摘要、申请状态流水。

业务模块负责：

- 保存业务主表、明细、附件和业务快照。
- 在发起审批前完成业务校验和单据落库。
- 在审批通过、驳回或结束事件中回写业务状态。
- 维护业务页面、业务按钮权限和业务数据范围。
- 决定驳回后是重新申请、复制申请还是修改原业务单据。

## 6. 接入方式
同进程启用 workflow 服务端能力：

```xml
<dependency>
    <groupId>io.mango.platform.workflow</groupId>
    <artifactId>mango-workflow-starter</artifactId>
</dependency>
```

只引用命令、查询、VO、枚举或窄接口：

```xml
<dependency>
    <groupId>io.mango.platform.workflow</groupId>
    <artifactId>mango-workflow-api</artifactId>
</dependency>
```

starter 自动生效条件：

- classpath 中存在 `WorkflowDefinitionMapper`。
- `mango.workflow.enabled=true` 或未配置。
- 自动扫描 `io.mango.workflow.core.mapper`。
- 自动扫描 `io.mango.workflow.core` 和 `io.mango.workflow.starter`。

模块元数据位于 `META-INF/mango/module.properties`：

```properties
module-name=mango-workflow
module-path=/workflow
```

## 7. 配置说明
### 6.1 `mango.workflow.*`

| 配置 | 默认值 | 代码位置 | 含义 |
|------|--------|----------|------|
| `mango.workflow.enabled` | `true` | `WorkflowAutoConfiguration` | 是否启用 workflow 自动配置。设为 `false` 后不扫描 mapper、service、controller 和 initializer |

### 6.2 `mango.workflow.samples.*`

来源：`WorkflowSampleProperties`。示例流程由 `WorkflowSampleDefinitionInitializer` 在应用启动时补齐。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `enabled` | `true` | 是否自动补齐内置示例流程 |
| `tenant-id` | `1` | 示例流程写入的租户 ID |
| `category-code` | `COMMON` | 示例流程所属流程分类编码 |
| `category-name` | `通用流程` | 示例流程所属流程分类名称 |
| `domain-code` | `COMMON` | 示例流程所属业务域编码 |

生产环境如果不想自动创建示例流程，应显式关闭：

```yaml
mango:
  workflow:
    samples:
      enabled: false
```

需要保留演示流程时，可以指定租户和业务域：

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

启动器本身不暴露 Flowable 数据源配置；Flowable 和业务表共用应用 `DataSource`，数据库迁移由 `mango-infra-persistence` 的模块化 Flyway 执行。

## 8. API 与扩展
### 7.1 HTTP 接口和权限码

| 能力 | 接口 | 权限码 | 说明 |
|------|------|--------|------|
| 创建业务申请 | `POST /workflow/business-applies` | `workflow:business-apply:create` | 创建业务单据和流程的关联申请，初始状态 `DRAFT` |
| 业务申请分页 | `POST /workflow/business-applies/page` | `workflow:business-apply:list` | 查询申请中心列表 |
| 业务申请详情 | `GET /workflow/business-applies/{applyId}` | `workflow:business-apply:detail` | 查询申请详情和当前任务 |
| 业务申请历史 | `GET /workflow/business-applies/history` | `workflow:business-apply:detail` | 按 `businessType`、`businessKey` 查询历史申请 |
| 最新进度 | `GET /workflow/business-applies/progress/latest` | `workflow:business-apply:detail` | 查询某个业务单据最新审批进度 |
| 批量最新进度 | `POST /workflow/business-applies/progress/latest-batch` | `workflow:business-apply:list` | 业务列表批量查进度 |
| 按实例查申请 | `GET /workflow/business-applies/progress/by-process-instance` | `workflow:business-apply:detail` | 用 Flowable 实例 ID 反查申请 |
| 发起流程 | `POST /workflow/processes/start` | `workflow:process:start` | 发起已发布流程 |
| 我的发起 | `GET /workflow/processes/initiated` | `workflow:task:list` | 查询当前用户发起的流程 |
| 流程详情 | `GET /workflow/processes/detail` | `workflow:process:detail` | 查询流程表单、变量和审批轨迹 |
| 流程历史 | `GET /workflow/processes/history` | `workflow:process:detail` | 按 `businessKey` 查询流程实例历史 |
| 待办 | `GET /workflow/tasks/todo` | `workflow:task:list` | 查询当前用户待办、可认领或全部相关任务 |
| 已办 | `GET /workflow/tasks/done` | `workflow:task:list` | 查询当前用户已处理任务 |
| 任务详情 | `GET /workflow/tasks/detail` | `workflow:task:detail` | 查询任务、表单权限、渲染配置和审批记录 |
| 审批通过 | `POST /workflow/tasks/complete` | `workflow:task:complete` | 完成当前任务 |
| 审批驳回 | `POST /workflow/tasks/reject` | `workflow:task:reject` | 删除流程实例并标记申请驳回 |
| 暂存 | `POST /workflow/tasks/save` | `workflow:task:save` | 保存审批意见和变量，不推进流程 |
| 转办 | `POST /workflow/tasks/transfer` | `workflow:task:transfer` | 把任务转给其他用户 |
| 加签 | `POST /workflow/tasks/add-sign` | `workflow:task:add-sign` | 为当前多实例节点追加办理人 |
| 认领 | `POST /workflow/tasks/claim` | `workflow:task:claim` | 认领候选任务 |
| 释放 | `POST /workflow/tasks/unclaim` | `workflow:task:unclaim` | 释放自己认领的候选任务 |
| 抄送列表 | `GET /workflow/tasks/copied` | `workflow:task:list` | 查询抄送给当前用户的记录 |
| 抄送已阅 | `POST /workflow/tasks/copied/read` | `workflow:task:read-copied` | 标记自己的抄送记录为已读 |

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

### 7.2 业务申请字段

`CreateWorkflowBusinessApplyCommand` 用于先创建申请记录：

| 字段 | 是否必填 | 说明 |
|------|----------|------|
| `businessType` | 是 | 业务类型，最长 128，例如 `expense`、`contract_seal` |
| `businessKey` | 是 | 业务主键，最长 128，通常是业务单据 ID |
| `applyCode` | 否 | 申请编号；为空时后端按时间生成 |
| `applyTitle` | 是 | 申请标题，最长 255 |
| `applySummary` | 否 | 摘要，最长 1000 |
| `processDefinitionId` | 否 | Mango 流程定义 ID |
| `processDefinitionKey` | 否 | 流程定义编码 |
| `renderMode` | 否 | `DYNAMIC_FORM` 或 `CUSTOM_PAGE`，默认 `DYNAMIC_FORM` |
| `applyPageKey` | 否 | 自定义申请页 key |
| `approvePageKey` | 否 | 自定义审批页 key |
| `formKey` | 否 | 表单 key |
| `formVersion` | 否 | 表单版本 |
| `formJsonSnapshot` | 否 | 动态表单 JSON 快照 |
| `formDataSnapshot` | 否 | 动态表单数据快照 |
| `snapshotRef` | 否 | 业务快照引用，最长 255 |
| `snapshotDigest` | 否 | 业务快照摘要，最长 128 |
| `reapplyFromApplyId` | 否 | 重新申请来源申请 ID |
| `variables` | 否 | 流程变量 |
| `extension` | 否 | 扩展配置 |

创建申请后状态是 `DRAFT`，并会把同一 `businessType + businessKey` 下旧申请的 `latestFlag` 清掉。

### 7.3 发起流程字段

`StartWorkflowProcessCommand` 用于启动 Flowable 实例：

| 字段 | 规则 | 说明 |
|------|------|------|
| `definitionId` | 与 `definitionKey` 至少一个有效 | Mango 流程定义 ID，优先使用 |
| `definitionKey` | `definitionId` 为空时必填 | 按编码查最新已发布流程 |
| `businessKey` | 可空 | Flowable business key；为空时后端生成 `<definitionKey>-<timestamp>` |
| `businessType` | 可空 | 写入变量 `businessType`，业务进度查询建议必传 |
| `applyId` | 可空 | 已创建的业务申请 ID；传入后会绑定流程实例 |
| `renderMode` | 可空 | 覆盖申请渲染模式 |
| `applyPageKey` | 可空 | 自定义申请页 key |
| `approvePageKey` | 可空 | 自定义审批页 key |
| `snapshotRef` | 可空 | 业务快照引用 |
| `variables` | 可空 | 发起表单变量 |
| `selectedAssignees` | 可空 | 发起人自选审批人，key 是节点 ID 或节点定义 key |

启动时会自动写入这些流程变量：

```text
mangoInitiator
mangoInitiatorName
mangoDefinitionId
mangoDefinitionAdminUsers
businessType
businessKey
applyId
mangoSelectedAssignees
```

流程启动后会保存 `workflow_form_instance`、起始任务记录，并刷新业务申请当前任务。若流程在启动后立即结束，会把申请标记为 `APPROVED`。

### 7.4 流程定义和发布

`SaveWorkflowDefinitionCommand` 的关键字段：

| 字段 | 是否必填 | 说明 |
|------|----------|------|
| `id` | 修改必填 | 新增为空 |
| `categoryId` | 否 | 流程分类，用于配置分组 |
| `domainCode` | 是 | 业务域编码，最长 64 |
| `orgId` | 否 | 所属组织 ID |
| `adminUsers` | 否 | 流程管理员；审批人为空且策略为转管理员时使用 |
| `icon` | 否 | 图标 |
| `definitionName` | 是 | 流程名称，最长 128 |
| `definitionKey` | 是 | 流程编码，对应 Flowable process id，同租户内应唯一 |
| `designerJson` | 是 | 前端流程设计器 JSON，发布时由后端转换成 BPMN |
| `bpmnXml` | 否 | 调试兼容字段，正式发布以后端转换结果为准 |
| `formCode` | 否 | 表单编码 |
| `formJson` | 否 | 动态表单 JSON |
| `status` | 否 | `DRAFT`、`PUBLISHED`、`DISABLED` |
| `remark` | 否 | 备注 |

发布流程：

1. `POST /workflow/definitions` 或 `PUT /workflow/definitions` 保存草稿。
2. `POST /workflow/definitions/deploy?id=<id>` 发布。
3. 后端用 `WorkflowDesignerBpmnConverter` 把 `designerJson` 转成 BPMN。
4. Flowable 生成 deployment 和 process definition。
5. workflow 写入 `workflow_definition_version`，并更新 `workflow_definition` 的 `deploymentId`、`processDefinitionId`、`processDefinitionVersion`、`publishedVersionNo`、`status=PUBLISHED`。

业务模块内置流程定义时使用内部接口 `WorkflowDefinitionApi.ensurePublished(EnsureWorkflowDefinitionCommand)`。它会按当前租户和 `definitionKey` 幂等检查：已发布且 Flowable 中可查询时直接返回现有部署信息，否则创建或重新发布。

### 7.5 任务处理和查询

待办查询参数里 `todoType` 的行为：

| `todoType` | 行为 |
|------------|------|
| 空或 `ASSIGNED` | 只查当前用户已分配任务 |
| `CLAIMABLE` | 查当前用户或候选组可认领任务；admin 还可看未分配任务 |
| `ALL` | 查当前用户已分配、候选组可处理和 admin 可见未分配任务 |

候选组由 `WorkflowCandidateGroupProvider` 从当前上下文和组织权限表计算：

- 角色：`ROLE:<roleId>`。
- 岗位：`POST:<postId>`。
- 组织：`ORG:<orgId>`。
- 组织负责人：`ORG_LEADER:<orgId>`。
- 同时追加当前 `userId` 和 `memberId` 的同类候选组，兼容不同配置方式。

审批操作：

| 操作 | 命令 | 结果 |
|------|------|------|
| 通过 | `CompleteWorkflowTaskCommand` | 合并变量、完成 Flowable 任务、写处理记录、发布任务完成事件、刷新当前任务 |
| 驳回 | `RejectWorkflowTaskCommand` | 删除流程实例、状态置为 `REJECTED`、发布驳回和结束事件 |
| 暂存 | `SaveWorkflowTaskDraftCommand` | 合并变量、保存表单实例和处理记录，不推进流程 |
| 转办 | `TransferWorkflowTaskCommand` | 修改任务办理人为目标用户，不能转给自己 |
| 加签 | `AddSignWorkflowTaskCommand` | 在当前多实例节点追加目标用户，不能加签给自己 |
| 认领 | `ClaimWorkflowTaskCommand` | 认领未分配候选任务 |
| 释放 | `ClaimWorkflowTaskCommand` | 只能释放自己通过认领获得的任务 |
| 抄送已阅 | `ReadWorkflowCopiedTaskCommand` | 只能标记自己的抄送记录 |

节点动作可在审批节点配置里控制：

```text
complete
reject
save
transfer
addSign
claim
unclaim
```

`WorkflowNodeActionConfig` 支持 `enabled`、`label`、`requireComment`、`confirmText`、`danger`、`order`、`disabled`、`tooltip`。

### 7.6 审批人和节点配置

审批人来源 `WorkflowAssigneeType`：

```text
SPECIFIED_USER
SPECIFIED_ROLE
SPECIFIED_POST
SPECIFIED_ORG
ORG_LEADER
INITIATOR
INITIATOR_SELECT
FORM_USER
EXPRESSION
```

多人审批方式 `WorkflowApprovalMode`：

```text
COUNTERSIGN
OR_SIGN
SEQUENTIAL
```

审批人为空策略 `WorkflowEmptyAssigneeStrategy`：

```text
AUTO_PASS
AUTO_REJECT
AUTO_END
TO_ADMIN
TO_USER
```

驳回策略枚举有 `END_PROCESS` 和 `BACK_TO_START`，但当前 `WorkflowTaskRuntimeServiceImpl.reject()` 的运行时行为是直接删除流程实例并标记业务申请为 `REJECTED`。配置设计时不要把 `BACK_TO_START` 当成已经完整实现的运行能力。

表单字段权限 `WorkflowFormPermission`：

```text
HIDDEN
READONLY
EDITABLE
```

### 7.7 节点执行扩展

可用执行器：

| `executionType` | 执行器 | 当前行为 |
|-----------------|--------|----------|
| `NONE` | `NoopWorkflowNodeExecutor` | 不执行动作，只完成流程流转 |
| `EVENT_PUBLISH` | `EventPublishWorkflowNodeExecutor` | 发布 `WorkflowNodeExecutionEvent` Spring 事件 |
| `SPRING_BEAN` | `SpringBeanWorkflowNodeExecutor` | 读取 `beanName`，只允许调用实现 `WorkflowNodeExecutable` 的 Spring Bean |
| `HTTP_URL` | `HttpUrlWorkflowNodeExecutor` | 当前直接失败，提示未配置服务边界 |
| `REMOTE_SERVICE` | `RemoteServiceWorkflowNodeExecutor` | 当前直接失败，提示未配置服务注册与调用边界 |

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
        // 这里只做受控、幂等的业务动作。复杂业务状态建议监听流程完成事件处理。
    }
}
```

设计器节点 properties 中配置：

```json
{
  "beanName": "orderApprovalCallback"
}
```

workflow 领域事件类型：

```text
workflow.process.started
workflow.task.completed
workflow.task.rejected
workflow.process.completed
workflow.process.rejected
workflow.process.ended
```

业务模块可以监听这些事件或 `WorkflowNodeExecutionEvent`，但回写业务状态必须幂等。

## 9. 数据与初始化
Flyway 路径：

```text
mango-workflow-core/src/main/resources/db/migration/workflow/V1__init_workflow.sql
mango-workflow-core/src/main/resources/db/migration/workflow/V2__workflow_domain.sql
```

核心表：

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

`V1__init_workflow.sql` 还包含 Flowable 7 官方 `ACT_*` 引擎表。

初始化数据：

| 初始化内容 | 位置 | 幂等方式 |
|------------|------|----------|
| 默认流程分类 `COMMON` | `workflow_category` | `ON DUPLICATE KEY UPDATE` |
| 默认模板分类 `COMMON_TEMPLATE` | `workflow_template_category` | `ON DUPLICATE KEY UPDATE` |
| 设计器节点目录 | `workflow_node_definition` | `ON DUPLICATE KEY UPDATE` |
| 发起流程、任务、业务申请、流程管理菜单和按钮权限 | `authorization_menu` | `ON DUPLICATE KEY UPDATE` |
| 菜单包关系 | `authorization_menu_package_item` | `INSERT IGNORE` |
| 角色菜单关系 | `authorization_role_menu` | `INSERT IGNORE` |
| 业务域字段和索引 | `V2__workflow_domain.sql` | `ALTER TABLE` 和索引创建 |

启动期初始化器：

| 初始化器 | 触发条件 | 写入内容 | 幂等键 |
|----------|----------|----------|--------|
| `WorkflowSampleDefinitionInitializer` | `mango.workflow.samples.enabled=true` | 示例流程分类和 3 个示例流程：费用报销审批、合同用印审批、请假申请 | `tenantId + definitionKey`；已发布且 Flowable 可查询时跳过 |

示例初始化器会临时写入 Mango 上下文：用户 ID `1`、用户名 `admin`、租户为 `mango.workflow.samples.tenant-id`。它会创建或刷新示例设计器 JSON 后调用 `definitionService.deploy()` 发布流程。

## 10. 管理入口
菜单由 `V1__init_workflow.sql` 写入 `authorization_menu`，应用编码是 `internal-admin`。

主要菜单：

| 菜单 | 路径 | 组件 | 权限 |
|------|------|------|------|
| 流程管理 | `/workflow/manage` | 无，重定向到定义页 | 无 |
| 流程模板 | `/workflow/manage/template` | `@/views/workflow/template/index.vue` | `workflow:template:list` |
| 流程定义 | `/workflow/manage/definition` | `@/views/workflow/definition/index.vue` | `workflow:definition:list` |

主要按钮权限：

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

租户边界：

- workflow 表继承 Mango 持久化租户隔离，常规查询按当前 `MangoContextHolder.tenantId()` 过滤。
- 流程部署时 Flowable deployment tenantId 使用当前租户。
- 业务申请按 `tenant_id`、`businessType`、`businessKey` 和 `latestFlag` 维护最新进度。
- 候选组查询依赖当前 `tenantId`、`userId`、`memberId` 和组织权限表。
- 示例流程默认写租户 `1`，生产环境要明确关闭或改成目标租户。

## 11. 快速开始
1. 业务模块引入 `mango-workflow-api`；同进程提供 workflow 服务时引入 `mango-workflow-starter`。
2. 设计或初始化流程定义，确保状态是 `PUBLISHED` 且有 `processDefinitionId`。
3. 业务单据提交审批前，先保存业务主表、业务快照和附件关系。
4. 调用 `WorkflowBusinessApplyApi.create()` 或 `POST /workflow/business-applies` 创建申请，传入 `businessType`、`businessKey`、标题、快照引用和变量。
5. 调用 `WorkflowProcessApi.start()` 或 `POST /workflow/processes/start`，传入 `applyId`、`definitionId` 或 `definitionKey`、`businessType`、`businessKey`、变量。
6. 业务列表用 `WorkflowBusinessProcessApi.latestByBusinessKeys(businessType, keys)` 或批量进度接口补审批状态。
7. 审批页面用 `/workflow/tasks/detail` 获取表单 JSON、变量、字段权限和当前节点渲染配置。
8. 审批通过、驳回、转办、加签等动作调用 `/workflow/tasks/*` 接口。
9. 业务模块监听流程完成、驳回或结束事件，幂等回写业务状态。
10. 驳回后重新申请时，创建新的申请记录，传 `reapplyFromApplyId`，不要覆盖旧流程历史。

示例发起代码：

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

## 12. 问题排查
**流程定义保存了，但发起时报“只有已发布流程可以发起”**

保存只是草稿。必须调用 `/workflow/definitions/deploy` 发布，确保 `workflow_definition.status=PUBLISHED` 且 `processDefinitionId` 不为空。

**业务列表怎么显示审批状态**

不要 join workflow 表。用 `WorkflowBusinessProcessApi.latestByBusinessKeys(businessType, keys)` 或 `/workflow/business-applies/progress/latest-batch` 批量查询最新进度。

**审批人为空怎么办**

节点配置 `emptyAssigneeStrategy`。`TO_ADMIN` 会优先使用流程定义 `adminUsers`，为空时退到 `admin`。`AUTO_PASS`、`AUTO_REJECT`、`AUTO_END` 会在运行时自动推进或结束。

**为什么 HTTP URL 节点不能用**

当前 `HttpUrlWorkflowNodeExecutor` 和 `RemoteServiceWorkflowNodeExecutor` 默认直接失败，因为还没有服务边界、白名单、超时和安全策略。生产只使用 `EVENT_PUBLISH` 或 `SPRING_BEAN`，远程调用能力要先补安全设计。

**驳回后能不能回到发起人节点**

当前 `reject()` 运行时会删除流程实例并标记业务申请 `REJECTED`。如果业务要重新提交，应新建申请或新流程实例，并用 `reapplyFromApplyId` 关联来源。

**为什么 `/workflow/tasks/initiated` 没有数据**

当前 Controller 里该接口返回空页。查询“我的发起”使用 `/workflow/processes/initiated`。

## 13. 相关文档
- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
