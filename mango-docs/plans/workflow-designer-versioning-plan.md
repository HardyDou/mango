# 工作流设计器与版本管理改造计划

## 背景

当前 Mango 工作流模块已经接入 Flowable 7.0.0，但流程定义页面仍以 `bpmnXml` 文本框作为主编辑方式。这种方式不适合作为管理后台的正式流程配置能力，也无法表达审批人、抄送、条件分支、并行分支、业务服务节点等业务语义。

Pigx 的可取点是：前端维护一份业务节点树，后端发布时转换为 Flowable `BpmnModel` 并部署。Mango 应按这个方向重新实现，不直接搬 Pigx 代码。

节点扩展、三步骤设计器、通用执行器、Bean/HTTP/远程服务/事件广播能力的进一步规划见：

- [工作流节点扩展能力与设计器升级计划](./workflow-node-extension-capability-plan.md)

## Flowable 版本语义

Flowable 的流程版本来自部署行为：

- 同一个 `process id/key` 每部署一次，会在 `ACT_RE_PROCDEF` 生成一个新版本号。
- 已启动流程实例继续绑定启动时的流程定义版本，不会自动切到新版本。
- Flowable 只管理运行时可执行定义，不管理业务草稿、设计器快照、发布说明、回滚来源等产品语义。

因此 Mango 不能只在 `workflow_definition` 上覆盖保存。正式模型需要区分：

- 流程主档：稳定流程编码、名称、分组、当前状态。
- 设计器草稿：未发布的业务节点 JSON。
- 发布版本：每次发布形成一条不可变记录，关联 Flowable deployment/processDefinition。

## 目标模型

### `workflow_definition`

流程主档和当前草稿：

- `definition_key`：稳定流程编码，对应 Flowable process id。
- `designer_json`：当前设计器草稿 JSON。
- `bpmn_xml`：最近一次由设计器生成或兼容导入的 BPMN XML。
- `published_version_no`：最近发布版本号。
- `deployment_id`、`process_definition_id`、`process_definition_version`：最近发布到 Flowable 的产物。

### `workflow_definition_version`

流程发布版本记录：

- `definition_id`：流程主档 ID。
- `version_no`：Mango 发布版本号，从 1 递增。
- `designer_json`：发布时设计器快照。
- `bpmn_xml`：发布时 BPMN XML 快照。
- `deployment_id`、`process_definition_id`、`process_definition_version`：Flowable 产物。
- `publish_status`：发布状态。
- `publish_message`：发布失败原因或发布说明。

### 设计器 JSON

第一阶段使用 Mango 自定义 DSL：

- `ROOT`：发起节点。
- `APPROVAL`：人工审批节点，发布为 Flowable `userTask`。
- `CC`：抄送节点，第一阶段发布为空执行服务节点或跳过执行，保留设计语义。
- `EXCLUSIVE_GATEWAY`：条件分支，发布为排他网关。
- `PARALLEL_GATEWAY`：并行分支，发布为并行网关。
- `SERVICE`：服务任务节点，发布为 Flowable `serviceTask`。
- `GUARANTEE_*`：保函业务节点模板，底层仍映射到 `APPROVAL` 或 `SERVICE`，不创造不可执行 BPMN 类型。

## 保函业务节点目录

平台提供节点模板，不直接固化业务流程：

- 客户提交资料：人工/外部任务模板。
- 资料补正：人工审批模板。
- 元丰行风控初审：人工审批模板。
- 签约资料整理：人工审批模板。
- 下游担保机构审批：人工审批模板。
- 银行资料提交：服务任务模板。
- 银行反馈/补件：人工审批模板。
- 出函归档：服务任务模板。

## Pigx Flow 特性取舍

### 吸收的优质特性

- 可视化流程设计器：吸收“发起人、审批人、抄送、条件分支、并行分支、结束”的节点式设计体验。
- 业务 DSL 驱动发布：吸收“前端保存节点树，后端转换为 Flowable `BpmnModel`”的模型，不让前端直接拼 BPMN XML。
- 发布时部署 Flowable：吸收 `RepositoryService.createDeployment().addBpmnModel(...).deploy()` 的正式部署路径。
- 条件/并行分支：吸收分支节点和合并网关的结构，避免画布上能配置但引擎不可执行。
- 节点原始数据快照：吸收“发布时保存节点 JSON”的思想，用 `workflow_definition_version.designer_json` 保存不可变快照。
- 审批、抄送、服务节点分层：吸收“节点类型映射为 Flowable userTask/serviceTask/gateway”的做法。

### 暂不吸收的特性和原因

- 直接复用 Pigx UI 组件：不吸收。Pigx 组件依赖其前端工程、状态管理、国际化和样式体系，直接搬会破坏 Mango 前端规范。
- 直接复用 Pigx `ModelUtil`：不吸收。包名、远程服务、任务数据保存、监听器、租户上下文都与 Mango 不一致。
- Pigx 表单设计器整套能力：暂不吸收。Mango 当前还没有统一动态表单模块，先保留 `formCode` 作为关联点。
- Pigx 远程任务服务和节点原始数据服务：暂不吸收。Mango 单体优先，后续用本模块版本表和节点实例表承接。
- Pigx 审批监听器和多实例处理器：暂不直接吸收。审批人解析必须接入 Mango 的机构、组织、岗位、角色、成员模型后再实现。
- Pigx 租户上下文实现：不吸收。Mango 已经把租户语义调整为机构上下文，应统一使用 MangoContextHolder 和机构隔离策略。

## Mango 必要增强

- 机构隔离：流程分组、流程定义、发布版本必须带机构 ID。平台级模板可在后续通过模板表共享，机构发布时复制为本机构流程。
- 机构模板初始化：新增机构时可按机构类型初始化默认流程分组、流程模板和节点策略。
- 保函节点模板：为常见保函协作提供节点模板，但发布时仍映射为 Flowable 标准节点，保证引擎可执行。
- 跨机构节点：后续支持“提交下游担保机构”“提交银行”等跨机构参与节点，发布为服务任务 + 外部待办/协同任务。
- 版本管理：Mango 发布版本与 Flowable 引擎版本同时保存，支持查看历史版本、复制为新草稿和后续回滚。
- 业务数据边界：流程定义按机构隔离，平台节点目录和通用模板不按机构隔离；业务流程实例按业务单据参与方控制可见范围。

## API 调整

保留已有路径，补充语义：

- `POST /workflow/definitions`：新增流程主档和草稿。
- `PUT /workflow/definitions`：保存当前设计器草稿。
- `POST /workflow/definitions/deploy`：发布当前草稿，生成 BPMN、部署 Flowable、写版本记录。
- `GET /workflow/definitions/versions`：查询发布版本。
- `GET /workflow/definitions/version-detail`：查询版本详情。
- `GET /workflow/definitions/node-catalog`：查询设计器节点目录。

所有 GET 查询继续使用 query 参数和 `@ParameterObject`，不使用 path 参数。

## 前端改造

工作流定义页面改为：

- 列表区：流程主档、状态、最近发布版本、Flowable 版本。
- 编辑区：基础信息 + 可视化设计器。
- 设计器：中间画布、节点添加面板、节点属性侧栏、版本抽屉。
- XML 只作为发布产物调试查看，不作为主编辑入口。

## E2E 验收

- 创建流程分组。
- 创建流程定义。
- 在设计器中添加审批节点、条件分支、保函节点模板。
- 保存草稿。
- 发布流程。
- 查看版本列表，确认版本号、deploymentId、processDefinitionId 存在。
- 刷新页面后流程仍可编辑，设计器 JSON 不丢失。
