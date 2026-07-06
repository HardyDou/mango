# Workflow API Boundary Issues 394 397 交付契约

## 1. 目标

处理 GitHub issues #394 和 #397：将工作流能力通过 `mango-workflow-api` 对业务暴露，禁止业务直接查询工作流表或直接依赖 `mango-workflow-core` service/event 实现；本地调用由 Controller API Bean 承载，远程调用由 Feign API Bean 承载。

## 2. 范围

- 后端 workflow API、core、starter、starter-remote 模块。
- 工作流业务申请启动、流程进度、任务动作结果、认领状态和候选人快照。
- 工作流标准事件的公开事件类型和公开 payload VO。
- `@mango/workflow` API TypeScript 类型与请求封装同步。
- README 使用说明和边界说明。

## 3. 不做什么

- 不新增页面、菜单、权限点或前端交互。
- 不迁移既有业务表数据。
- 不发布 npm 包或 Maven 制品。
- 不改变业务侧审批语义，只补齐 API 契约和边界治理。

## 4. 设计输入

- 用户要求：业务必须通过 API 使用工作流，禁止直接使用 service 层代码；事件也是。
- GitHub issue #394：业务不应直接查询工作流数据库处理进度与审批任务。
- GitHub issue #397：service/adapter 不应实现 API，API 应由 Controller 或 Feign 承载。
- PMO preflight 输出的后端、前端和交付规则。

## 5. 设计说明

### 5.1 影响模块

- `mango/mango-platform/mango-workflow/mango-workflow-api`
- `mango/mango-platform/mango-workflow/mango-workflow-core`
- `mango/mango-platform/mango-workflow/mango-workflow-starter`
- `mango/mango-platform/mango-workflow/mango-workflow-starter-remote`
- `mango-ui/packages/workflow`
- `mango/mango-platform/mango-workflow/README.md`

### 5.2 接口变化

- `WorkflowProcessApi` 新增 `startBusinessWorkflow(StartBusinessWorkflowCommand)`。
- `WorkflowTaskRuntimeApi` 新增带结果的完成、驳回、保存、认领、取消认领接口。
- `WorkflowBusinessApplyProgressVO` 和当前任务 VO 增加认领状态、候选用户和候选用户组。
- 新增 `WorkflowStartResultVO`、`WorkflowTaskActionResultVO`、`WorkflowEventPayloadVO` 和 `WorkflowTaskClaimStatus`。
- Controller 实现 workflow API，Feign Client 实现远程 API；core service 不再继承或实现 API。

### 5.3 数据变化

- 工作流业务当前任务快照增加 `claim_status`、`candidate_users`、`candidate_groups`。
- 新增 Flyway 增量脚本 `V4__workflow_business_progress_snapshot.sql`。
- 初始建表脚本同步补齐新字段。

### 5.4 菜单/页面/权限变化

- EXCEPTION: 本次只改 API、事件契约和前端请求类型，不新增页面、菜单或权限点。

### 5.5 测试范围

- 后端编译覆盖 API、core、starter、starter-remote 及依赖模块。
- 定向测试覆盖事件 payload、业务流程启动和 Controller/API 契约。
- 静态扫描覆盖 service/adapter API 实现违规和业务侧 core 依赖违规。
- 前端包构建因本地 `mango-ui/node_modules` 不存在无法执行，记录为环境例外。

### 5.6 交付物料同步判断

| 物料 | 是否需要更新 | 路径或 EXCEPTION 依据 |
|---|---|---|
| 代码 | 是 | `mango/mango-platform/mango-workflow`、`mango-ui/packages/workflow/src/api/workflow.ts` |
| README/使用说明 | 是 | `mango/mango-platform/mango-workflow/README.md` |
| 需求文档 | 是 | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` |
| 详细设计文档 | 是 | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` |
| E2E 脚本 | 否 | EXCEPTION: 本次没有新增页面、菜单或浏览器交互；API 契约由后端测试和静态扫描覆盖 |
| 测试结果基线 | 是 | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` |

### 5.7 测试用例登记与自动化判断

| 用例 ID | 来源 AC | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 稳定契约 | 执行入口 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | AC-001 | workflow API/core/starter/remote 编译通过 | P0 | API | AUTO | 本地 Maven reactor | Java 编译期契约 | `mvn -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter,mango-platform/mango-workflow/mango-workflow-starter-remote -am -DskipTests compile` | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` | AUTOMATED |
| TC-002 | AC-002 | 事件发布 payload 包含公开 VO 字段并保持历史 Map 兼容 | P0 | 单元 | AUTO | `WorkflowEventPublisherTest` | 事件类型和 payload key 稳定 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am -Dtest=WorkflowEventPublisherTest,WorkflowProcessServiceImplIntegrationTest,WorkflowApiControllerContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` | AUTOMATED |
| TC-003 | AC-003 | Controller 实现 API 且 task/process/business apply 方法委托到 service | P0 | 单元 | AUTO | `WorkflowApiControllerContractTest` | Controller/Feign 承载 API，core service 不承载 API | `mvn -pl mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am -Dtest=WorkflowEventPublisherTest,WorkflowProcessServiceImplIntegrationTest,WorkflowApiControllerContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` | AUTOMATED |
| TC-004 | AC-004 | 业务启动流程创建申请并返回启动结果 | P0 | API | AUTO | `WorkflowProcessServiceImplIntegrationTest` | start-business 结果包含 apply/progress/process 信息 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am -Dtest=WorkflowEventPublisherTest,WorkflowProcessServiceImplIntegrationTest,WorkflowApiControllerContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` | AUTOMATED |
| TC-005 | AC-005 | 静态扫描确认 service/adapter 不实现 workflow API | P0 | 手工 | MANUAL | 源码文本扫描 | 只允许 Controller 和 Feign 实现 workflow API | `rg -n "Workflow[A-Za-z0-9]*Api" mango/mango-platform/mango-workflow` 后核对 extends/implements 行 | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` | MANUAL |
| TC-006 | AC-006 | 前端 workflow API 包类型和请求封装同步 | P1 | API | EXCEPTION | `mango-ui/packages/workflow` | 本地需存在依赖安装目录 | `pnpm --filter @mango/workflow build` | EXCEPTION: 本地 `mango-ui/node_modules` 不存在，`vite` 不可用；仅完成代码层同步和后端契约验证 | MANUAL |

## 6. 风险与限制

- 前端包构建未在当前机器完成，原因是 `mango-ui/node_modules` 不存在；改动仅涉及 TypeScript API 类型和请求封装，没有页面运行时改动。
- 事件对外仍通过 `DomainEvent.payload` Map 发布，同时新增 `WorkflowEventPayloadVO` 供业务按 API 包反序列化；为保持兼容，历史数字字段在 Map 中继续保持数字值。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | issue #394 | 业务不得直接查 workflow 表处理申请进度 | 新增 start-business、progress、result API，业务通过 API 获取进度和当前任务快照 | `mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api/WorkflowProcessApi.java`、`mango/mango-platform/mango-workflow/mango-workflow-core/src/main/java/io/mango/workflow/core/service/impl/WorkflowProcessServiceImpl.java` | `mango/mango-platform/mango-workflow/README.md` | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` | EXCEPTION: 后端 API 契约变更，无页面流程 | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` | 编译、定向测试、README 核对 | DONE | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` |
| TASK-002 | 用户要求 | 禁止业务直接使用 core service 层代码 | core service 不继承 API，Controller 和 Feign 作为 API Bean 承载层 | `mango/mango-platform/mango-workflow/mango-workflow-core/src/main/java/io/mango/workflow/core/service/IWorkflowProcessService.java`、`mango/mango-platform/mango-workflow/mango-workflow-starter/src/main/java/io/mango/workflow/starter/controller/WorkflowProcessController.java`、`mango/mango-platform/mango-workflow/mango-workflow-starter-remote/src/main/java/io/mango/workflow/starter/remote/WorkflowBusinessProcessFeignClient.java` | `mango/mango-platform/mango-workflow/README.md` | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` | EXCEPTION: 依赖边界治理由编译和静态扫描验证 | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` | 静态扫描仅剩 Controller/Feign 实现 API | DONE | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` |
| TASK-003 | 用户要求 | 事件也必须通过 API 使用 | 新增 `WorkflowEventPayloadVO` 和公开事件类型；README 要求业务使用 API 包反序列化 payload | `mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api/vo/WorkflowEventPayloadVO.java`、`mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api/WorkflowEventTypes.java`、`mango/mango-platform/mango-workflow/mango-workflow-core/src/main/java/io/mango/workflow/core/event/WorkflowEventPublisher.java` | `mango/mango-platform/mango-workflow/README.md` | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` | EXCEPTION: 事件契约由单元测试覆盖，无浏览器流程 | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` | `WorkflowEventPublisherTest` 验证 payload 字段和兼容性 | DONE | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` |
| TASK-004 | issue #397 | 删除 service/adapter 实现 API 的违规结构 | 删除 `WorkflowTaskRuntimeApiAdapter`，补 Controller 契约测试 | `mango/mango-platform/mango-workflow/mango-workflow-starter/src/main/java/io/mango/workflow/starter/controller/WorkflowTaskController.java`、`mango/mango-platform/mango-workflow/mango-workflow-starter/src/test/java/io/mango/workflow/starter/controller/WorkflowApiControllerContractTest.java` | `mango/mango-platform/mango-workflow/README.md` | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` | EXCEPTION: API 承载层治理由单元测试和源码扫描验证 | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` | Controller 契约测试和适配器残留扫描 | DONE | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` |
| TASK-005 | issue #394 | 前端 workflow API 包同步新接口和返回字段 | 在 `@mango/workflow` API 文件增加新类型、请求方法和 normalizer | `mango-ui/packages/workflow/src/api/workflow.ts` | `mango/mango-platform/mango-workflow/README.md` | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-delivery.md` | EXCEPTION: 未新增页面、菜单或交互 | EXCEPTION: 本地 `mango-ui/node_modules` 不存在，前端构建无法执行；后端契约和类型代码已同步 | 代码检查和环境例外登记 | EXCEPTION | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` |

## 8. 验收证据记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | TC-001 | Maven compile | workflow API/core/starter/remote 编译 | 本地 Java 21 Maven reactor | 编译完成且 reactor summary 中 workflow 四个模块为 SUCCESS | EXCEPTION: 无页面 UI | EXCEPTION: 非浏览器验证 | 终端命令输出 | DONE |
| TASK-002 | TC-005 | 源码扫描 | service/adapter API 边界 | `mango/mango-platform/mango-workflow` | 扫描结果仅包含 Controller 和 Feign Client 实现 workflow API | EXCEPTION: 无页面 UI | EXCEPTION: 非浏览器验证 | 终端命令输出 | DONE |
| TASK-003 | TC-002 | 单元测试 | 事件 payload API 化 | `WorkflowEventPublisherTest` | 事件 payload 包含公开字段，历史数字字段 Map 兼容保持 | EXCEPTION: 无页面 UI | EXCEPTION: 非浏览器验证 | 终端命令输出 | DONE |
| TASK-004 | TC-003 | 单元测试 | Controller/API 契约 | `WorkflowApiControllerContractTest` | Controller 是 API 实例且关键方法委托 service | EXCEPTION: 无页面 UI | EXCEPTION: 非浏览器验证 | 终端命令输出 | DONE |
| TASK-005 | TC-006 | 前端构建 | `@mango/workflow` API 包构建 | `mango-ui` 当前工作区 | 原因: `mango-ui/node_modules` 不存在，`vite` 不可用；本次未新增页面交互 | EXCEPTION: 无页面 UI | EXCEPTION: 本地依赖未安装，未启动浏览器 | `test -d mango-ui/node_modules` 返回不存在 | EXCEPTION |

## 9. 测试结果基线

| 基线 ID | 覆盖台账 ID | 覆盖用例 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BASELINE-001 | TASK-001,TASK-002,TASK-003,TASK-004 | TC-001,TC-002,TC-003,TC-004,TC-005 | EXCEPTION: 本次无页面 E2E 脚本 | `mvn -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter,mango-platform/mango-workflow/mango-workflow-starter-remote -am -DskipTests compile`；`mvn -pl mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -am -Dtest=WorkflowEventPublisherTest,WorkflowProcessServiceImplIntegrationTest,WorkflowApiControllerContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | macOS 本地，Java 21，Maven | H2 测试库和本地源码 | EXCEPTION: 单元/API 验证不需要登录账号 | 编译成功；定向测试 8 个用例成功；静态扫描只剩 Controller/Feign API 实现 | EXCEPTION: 前端构建因 `mango-ui/node_modules` 不存在未执行 | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` | 业务改为通过 workflow API 使用流程和事件 |
| BASELINE-002 | TASK-005 | TC-006 | EXCEPTION: 未新增浏览器交互 | `pnpm --filter @mango/workflow build` | macOS 本地，pnpm 工作区 | EXCEPTION: 前端类型构建不需要数据库 | EXCEPTION: 前端类型构建不需要登录账号 | EXCEPTION: 本地 `mango-ui/node_modules` 不存在，`vite` 不可用 | EXCEPTION: 依赖未安装导致无法构建 | `mango-pmo/baselines/backend-test/workflow-api-boundary-394-397-acceptance.md` | 前端 API 类型同步后端契约 |

## 10. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 使用 `WorkflowProcessApi.startBusinessWorkflow` 启动业务流程，使用 task result API 获取审批后进度；订阅事件时用 `WorkflowEventTypes` 和 `WorkflowEventPayloadVO`，不要引用 workflow core service/event | `mango/mango-platform/mango-workflow/README.md` | `mvn -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter,mango-platform/mango-workflow/mango-workflow-starter-remote -am -DskipTests compile` | 业务侧只依赖 `mango-workflow-api` 或 remote starter；不得查询 workflow 表 | API 返回失败时按 `R` 结果处理；事件 payload 反序列化失败时检查 API 包版本 | DONE |
