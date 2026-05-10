# 工作流节点扩展能力与设计器升级计划

## 背景

当前 Mango 工作流已经完成基础 Flowable 集成、流程定义发布、发布版本记录和第一版可视化设计器。但现状仍有几个硬伤：

- 新增/编辑流程仍是抽屉内单页编辑，和正式流程配置的复杂度不匹配。
- 节点目录由后端代码硬编码，新增节点需要改代码，业务节点容易越堆越死。
- 设计器节点只有名称、类型、服务处理器等少量属性，无法表达 Bean 调用、HTTP 调用、远程服务调用、事件广播等通用能力。
- 画布视觉还不够接近 Pigx 的流程编排体验：节点颜色、连线、分支结构、缩放、步骤检查都需要加强。
- 当前发布只把节点类型简单映射为 `userTask/serviceTask/gateway`，缺少统一执行器和节点能力治理。

本计划继续吸收 Pigx UI 和 beer-brace-flowable 的优质思路，但不直接复制代码。

## 参考项目取舍

### Pigx UI 吸收点

- 新增流程采用三步骤：
  - 流程基础信息
  - 表单信息
  - 流程设计
- 发布前逐步校验每个步骤，错误定位到对应步骤。
- 流程设计器采用节点树画布，发起人、审批、抄送、条件分支、并行分支、结束节点清晰可见。
- 支持缩放、分支列、节点抽屉配置。

### Pigx UI 不直接吸收

- 不直接复制组件。Pigx 前端依赖其 store、i18n、组件体系和样式，直接搬会污染 Mango 前端。
- 不沿用数字型节点类型。Mango 需要更清晰、可扩展的字符串编码。
- 不把业务节点继续写死为前端常量。

### beer-brace-flowable 吸收点

- HTTP 服务任务执行前可构造流程上下文，执行后记录调用日志。
- 远程服务任务可以把 `DelegateExecution` 封装成安全 DTO 后发给业务服务。
- 流程/节点完成后广播事件，便于单体和微服务架构接入。
- Provider local/remote 分层思想值得保留：单体直接调用，微服务通过远程客户端调用。
- 表单定义、部署快照、流程实例和任务操作 API 分层值得参考。

### beer-brace-flowable 不直接吸收

- 不使用反射读取 Flowable 内部 `CompleteTaskCmd` 私有字段。该方式依赖 Flowable 内部实现，版本升级风险高。
- 不直接继承/替换 Flowable HTTP Client 作为主扩展点。Mango 优先用自己的服务任务执行器封装 HTTP 调用，更容易审计和治理。
- 不复制旧项目的 JPA、Redis、远程事件桥实现。
- 不让 URL/Bean 调用在设计器里裸奔，必须有节点定义、参数 schema、访问边界和执行日志。

## 设计目标

### 产品目标

- 前端新增流程按三步骤完成，适合真实配置。
- 流程设计器具备清晰画布：不同节点类型不同颜色，节点之间有连线，分支视觉稳定。
- 节点可以由平台维护，不再只能靠代码硬编码。
- 平台可提供通用节点和业务模板节点，业务模板节点底层仍映射到通用能力。

### 技术目标

- Mango DSL 继续作为设计器源数据，Flowable BPMN 是发布产物。
- 节点定义与节点执行分离：
  - 节点定义描述“设计器里能选什么、配置什么、发布成什么”。
  - 节点执行描述“运行时如何调用 Bean、URL、远程服务、事件”。
- 服务任务统一走 Mango 执行器，不让每个节点直接生成不同 JavaDelegate 类。
- 单体和微服务都能用：
  - 单体：Spring Bean、本地事件、内部服务接口。
  - 微服务：HTTP URL、服务名 + 路径、消息/事件广播。
- 所有执行都必须可审计、可失败重试、可配置超时和错误策略。

## 核心概念

### 节点定义 `workflow_node_definition`

节点定义是“可维护的节点目录”，不是流程实例节点。

建议字段：

| 字段 | 说明 |
|---|---|
| `node_code` | 节点编码，全局唯一，如 `APPROVAL`、`SERVICE_HTTP`、`GUARANTEE_BANK_SUBMIT` |
| `node_name` | 节点名称 |
| `category_code` | 分类，如 `COMMON`、`INTEGRATION`、`GUARANTEE` |
| `category_name` | 分类名称 |
| `bpmn_type` | 发布时映射类型：`userTask`、`serviceTask`、`exclusiveGateway`、`parallelGateway`、`none` |
| `execution_type` | 执行类型：`NONE`、`USER_TASK`、`SPRING_BEAN`、`HTTP_URL`、`REMOTE_SERVICE`、`EVENT_PUBLISH` |
| `property_schema` | 前端属性表单 schema |
| `default_properties` | 默认属性 |
| `color` | 画布颜色 |
| `icon` | 节点图标 |
| `system_builtin` | 是否系统内置 |
| `tenant_scoped` | 是否机构可自定义 |
| `status` | 启用/停用 |

### 节点实例属性

流程设计器 JSON 中每个节点保存：

```json
{
  "id": "risk_review_001",
  "nodeName": "元丰行风控初审",
  "nodeType": "APPROVAL",
  "nodeDefinitionCode": "GUARANTEE_RISK_REVIEW",
  "bpmnType": "userTask",
  "executionType": "USER_TASK",
  "properties": {
    "assigneeMode": "ROLE",
    "roleCodes": ["risk_manager"],
    "completionMode": "ANY"
  },
  "childNode": null,
  "conditionNodes": []
}
```

`nodeType` 表示运行语义，`nodeDefinitionCode` 表示选择的节点模板。保函节点不应该成为独立不可控的 BPMN 类型，而是模板化配置。

### 执行类型

| 执行类型 | 适用场景 | 发布方式 |
|---|---|---|
| `NONE` | 根节点、空节点、纯结构节点 | 不生成任务或生成结构元素 |
| `USER_TASK` | 人工审批、补正、确认、经办 | Flowable `UserTask` |
| `SPRING_BEAN` | 单体内调用 Spring Bean 方法 | Flowable `ServiceTask` + Mango 统一 JavaDelegate |
| `HTTP_URL` | 调用明确 URL | Flowable `ServiceTask` + Mango 统一 JavaDelegate |
| `REMOTE_SERVICE` | 服务名 + 路径，适合微服务 | Flowable `ServiceTask` + Mango 统一 JavaDelegate |
| `EVENT_PUBLISH` | 节点完成后广播事件 | Flowable Listener 或 Mango 统一事件执行器 |

## 通用执行器设计

### 统一 JavaDelegate

所有服务节点发布为同一个执行器：

```text
io.mango.workflow.core.engine.MangoWorkflowServiceTaskDelegate
```

Flowable serviceTask 上只保存必要字段：

- `definitionId`
- `versionNo`
- `nodeId`
- `nodeDefinitionCode`
- `executionType`

运行时执行器根据发布版本快照读取节点配置，构造 `WorkflowNodeExecutionContext`，再交给对应 `WorkflowNodeExecutor`。

### 执行器 SPI

```java
public interface WorkflowNodeExecutor {
    String executionType();
    WorkflowNodeExecutionResult execute(WorkflowNodeExecutionContext context);
}
```

内置实现：

- `SpringBeanNodeExecutor`
- `HttpUrlNodeExecutor`
- `RemoteServiceNodeExecutor`
- `EventPublishNodeExecutor`
- `NoopNodeExecutor`

### Spring Bean 调用

设计器配置：

- Bean 名称
- 方法名
- 参数来源：流程变量、业务 key、固定 JSON、上下文对象
- 返回结果写入变量名
- 失败策略

约束：

- 只能调用被平台允许的 Bean 或实现指定接口的 Bean。
- 不允许任意类名反射。
- 方法签名优先约束为 `WorkflowNodeExecutionContext -> WorkflowNodeExecutionResult`。
- 后续可支持少量安全参数绑定，不做任意反射泛化。

### HTTP URL 调用

设计器配置：

- URL
- HTTP Method
- Header 模板
- Query 模板
- Body 模板
- 超时时间
- 成功状态码
- 响应写入变量

约束：

- URL 必须经过白名单/域名策略校验。
- 默认不允许调用内网敏感地址，除非平台显式配置允许。
- 调用日志必须记录请求摘要、响应状态、耗时、失败原因。
- 支持幂等键，避免重试造成重复副作用。

### 远程服务调用

设计器配置：

- 服务名
- 路径
- HTTP Method
- 参数模板
- 认证传递策略

单体模式：

- 可先退化为内部 HTTP 或本地 provider。

微服务模式：

- 后续适配服务发现、Feign 或网关路由。

### 事件广播

事件分两类：

- 引擎事件：流程开始、流程结束、节点开始、节点结束、任务创建、任务完成。
- 节点配置事件：某个节点完成后按配置广播业务事件。

单体模式：

- 先用 Spring `ApplicationEventPublisher`。

微服务模式：

- 后续适配 MQ / Spring Cloud Stream。

事件载荷统一：

- 流程定义 ID、发布版本、流程实例 ID、业务 key、机构 ID。
- 节点 ID、节点编码、节点名称、执行类型。
- 当前变量摘要，不直接广播大对象和敏感字段。

## 表单信息设计

三步骤中的“表单信息”不等于必须立即做完整动态表单平台。

P0 先支持：

- 表单编码 `formCode`
- 表单名称 `formName`
- 表单路由/组件标识 `formRoute`
- 表单权限 JSON `formPermissionJson`

P1 再考虑动态表单：

- 表单设计器
- 表单版本
- 表单与流程版本绑定
- 节点字段权限

## 前端设计器升级

### 页面结构

流程定义列表保留，点击“新增流程/设计”进入独立设计页或大尺寸抽屉。建议使用独立页面：

```text
/system/workflow/design?id=xxx
```

设计页结构：

- 顶部：步骤条 + 保存草稿 + 发布按钮
- 步骤 1：流程基础信息
- 步骤 2：表单信息
- 步骤 3：流程设计

### 画布视觉

- 根节点：蓝色
- 审批节点：绿色
- 抄送节点：紫色
- 服务节点：橙色
- HTTP/远程调用节点：青色或琥珀色
- 事件节点：靛蓝色
- 条件/并行网关：灰黑结构色
- 保函模板节点：在通用颜色基础上加业务角标，不另起不可控颜色体系

节点之间必须有连线：

- 纵向主线。
- 分支节点横向连线 + 分支列。
- 分支合并后回到主线。
- 结束节点固定显示。

### 节点属性面板

属性面板由节点定义的 `property_schema` 驱动：

- 人工节点显示审批人、角色、组织、岗位、会签/或签。
- Bean 节点显示 Bean、方法、参数映射、返回变量。
- HTTP 节点显示 Method、URL、Header、Body、超时、成功码。
- 事件节点显示事件类型、主题、载荷模板。
- 保函模板节点显示业务预设字段，但本质还是通用节点属性。

## 后端 API 规划

### 节点定义管理

- `GET /workflow/node-definitions/page`
- `GET /workflow/node-definitions/list`
- `GET /workflow/node-definitions/detail`
- `POST /workflow/node-definitions`
- `PUT /workflow/node-definitions`
- `PUT /workflow/node-definitions/status`
- `DELETE /workflow/node-definitions`

所有接口必须中文描述，GET 使用 `@ParameterObject`，不使用 path 参数。

### 设计器目录

当前 `GET /workflow/definitions/node-catalog` 改为读取节点定义表，并按分类返回。

### 发布校验

发布前后端必须校验：

- 基础信息完整。
- 表单信息符合当前阶段约束。
- 至少有一个有效业务节点。
- 分支至少有两个分支。
- 条件分支非默认分支必须有条件。
- 服务节点必须有执行配置。
- Bean/HTTP/远程调用配置必须通过安全校验。
- 节点定义已启用，且执行类型与 BPMN 类型匹配。

## 数据模型规划

P0 新增：

- `workflow_node_definition`
- `workflow_node_execution_log`

P1 新增：

- `workflow_form_definition`
- `workflow_form_definition_version`
- `workflow_instance_record`
- `workflow_node_record`
- `workflow_task_record`

P2 新增：

- `workflow_event_outbox`
- `workflow_node_template`
- `workflow_definition_template`

## 发布转换策略

发布时仍然按 Mango DSL 转 Flowable `BpmnModel`。

- `USER_TASK` -> `UserTask`
- `SPRING_BEAN/HTTP_URL/REMOTE_SERVICE/EVENT_PUBLISH` -> `ServiceTask` + `MangoWorkflowServiceTaskDelegate`
- `EXCLUSIVE_GATEWAY` -> `ExclusiveGateway`
- `PARALLEL_GATEWAY` -> `ParallelGateway`
- `CC` P0 可先作为 `ServiceTask` 发送事件/通知，P1 接入消息模块

发布版本快照必须保存：

- 设计器 JSON
- 节点定义快照
- BPMN XML
- 发布校验结果

这样节点定义后续被修改，不影响已发布流程版本。

## 执行日志

服务节点执行必须写日志：

| 字段 | 说明 |
|---|---|
| `definition_id` | 流程定义 |
| `version_no` | Mango 发布版本 |
| `process_instance_id` | Flowable 实例 |
| `execution_id` | Flowable execution |
| `node_id` | 节点 ID |
| `node_definition_code` | 节点定义编码 |
| `execution_type` | 执行类型 |
| `business_key` | 业务 key |
| `request_summary` | 请求摘要 |
| `response_summary` | 响应摘要 |
| `status` | 成功/失败 |
| `duration_ms` | 耗时 |
| `error_message` | 错误信息 |

## 分阶段计划

### P0：节点能力基础与设计器三步化

- 新增节点定义表和默认数据。
- `node-catalog` 改为查表。
- 新增节点定义管理接口。
- 服务节点统一执行器骨架。
- 支持 `SPRING_BEAN`、`HTTP_URL`、`EVENT_PUBLISH` 三类执行能力的模型和校验。
- 前端新增三步骤设计页。
- 画布增加颜色、连线、分支视觉和缩放。
- 当前保函节点改为节点模板，不再由后端代码硬编码返回。
- E2E 覆盖新增流程三步骤、保存草稿、发布、版本记录。

### P1：任务/表单/审批能力完善

- 接入机构成员、组织、岗位、角色选择。
- 支持会签、或签、依次审批。
- 支持节点字段权限。
- 表单定义和表单版本管理。
- 抄送节点接入通知模块。
- 节点执行日志页面。

### P2：微服务与跨机构协同

- 远程服务调用接入服务发现或网关。
- 事件 outbox，支持 MQ。
- 跨机构节点：提交下游担保机构、提交银行、银行反馈。
- 平台流程模板按机构类型初始化。
- 流程版本回滚、复制版本为草稿。

## 本轮优先级

本轮先做 P0，不碰保函业务模块实现：

1. 先落节点定义数据模型和接口。
2. 再改 `node-catalog` 为数据驱动。
3. 再改发布转换和统一执行器骨架。
4. 最后改前端三步骤设计器和画布视觉。
5. 每完成一块跑后端构建和工作流 E2E。

## 验收

- 后端：
  - `mvn -pl :mango-workflow-core -am package -DskipTests` 通过。
  - `mvn -pl :mango-monolith-app -am package -DskipTests` 通过。
  - Flyway 正常执行，不禁用迁移。
- 接口：
  - 节点定义管理接口可用。
  - `node-catalog` 返回数据库中的节点定义。
  - 发布时服务节点配置缺失会返回中文错误。
- 前端：
  - 新增流程分三步。
  - 流程设计器节点有颜色、连线、分支结构和结束节点。
  - 不同节点显示不同属性配置。
  - 保存草稿、发布、版本查看 E2E 通过。
