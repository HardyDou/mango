# Workflow API Boundary Issues 394 397 验收证据

## 1. 验收范围

- 页面：EXCEPTION: 本次未新增页面、菜单或浏览器交互。
- 接口：workflow API、Controller API Bean、Feign API Bean、task result API、start-business API。
- 权限：EXCEPTION: 本次未新增权限点。
- 数据：当前任务快照新增认领状态、候选用户和候选用户组字段。
- 部署形态：本地单体后端编译和单元/API 测试；远程调用通过 Feign 契约编译验证。

## 2. 执行环境

- 前端地址：EXCEPTION: 本次未启动前端页面。
- 后端地址：EXCEPTION: 本次执行 Maven 编译和测试，不启动 HTTP 服务。
- 数据库或租户：H2 测试库；静态扫描不需要数据库。
- 测试账号：EXCEPTION: 单元/API 契约测试不需要登录账号。
- 浏览器：EXCEPTION: 本次没有浏览器验收范围。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | TC-001 | Maven compile | workflow API/core/starter/remote 编译 | Java 21 Maven reactor | Reactor summary 中 `mango-workflow-api`、`mango-workflow-core`、`mango-workflow-starter`、`mango-workflow-starter-remote` 均为 SUCCESS | EXCEPTION: 无页面 UI，接口契约由编译验证 | EXCEPTION: 无浏览器网络请求，Maven reactor 输出为验证证据 | 终端命令输出已在交付上下文记录 | DONE |
| TASK-002 | TC-005 | 源码扫描 | API 承载边界 | `mango/mango-platform/mango-workflow` | `extends/implements Workflow*Api` 扫描结果仅包含 `starter/controller` 和 `starter-remote/FeignClient` | EXCEPTION: 无页面 UI，边界由源码扫描验证 | EXCEPTION: 非浏览器验证，无 network 记录 | 终端命令输出已在交付上下文记录 | DONE |
| TASK-003 | TC-002 | 单元测试 | 事件 payload API 化 | `WorkflowEventPublisherTest` | 事件 payload 包含 `WorkflowEventPayloadVO` 对应字段，并保持 `applyId`、`assignee` 历史 Map 数字兼容 | EXCEPTION: 无页面 UI，事件由单元测试验证 | EXCEPTION: 非浏览器验证，无 network 记录 | 终端命令输出已在交付上下文记录 | DONE |
| TASK-004 | TC-003 | 单元测试 | Controller/API 契约 | `WorkflowApiControllerContractTest` | Controller 是 API 实例，task runtime 关键方法委托 service；旧 adapter 已删除且残留扫描为空 | EXCEPTION: 无页面 UI，契约由单元测试验证 | EXCEPTION: 非浏览器验证，无 network 记录 | 终端命令输出已在交付上下文记录 | DONE |
| TASK-001 | TC-004 | API 测试 | 业务流程启动 | `WorkflowProcessServiceImplIntegrationTest` | `startBusinessWorkflow` 创建业务申请并返回流程启动结果和进度信息 | EXCEPTION: 无页面 UI，API 行为由集成测试验证 | EXCEPTION: 非浏览器验证，无 network 记录 | 终端命令输出已在交付上下文记录 | DONE |
| TASK-005 | TC-006 | 前端构建 | `@mango/workflow` API 包类型构建 | `mango-ui/packages/workflow` | 原因: `mango-ui/node_modules` 不存在，`vite` 命令不可用；本次没有页面交互变更 | EXCEPTION: 无页面 UI，未启动浏览器 | EXCEPTION: 本地依赖未安装，未发起 network 验收 | `test -d mango-ui/node_modules` 返回不存在 | EXCEPTION |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| workflow 后端 | EXCEPTION: 无页面 | API 编译通过 | 定向测试通过 | EXCEPTION: 本次无 UI 变更 | EXCEPTION: 无浏览器截图范围 | DONE |
| workflow 前端 API 包 | EXCEPTION: 无页面 | TypeScript API 文件同步 | 构建因缺本地依赖未执行 | EXCEPTION: 本次无 UI 变更 | EXCEPTION: 无浏览器截图范围 | EXCEPTION |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| `pnpm --filter @mango/workflow build` | 本地 `mango-ui/node_modules` 不存在，`vite` 不可用 | 前端 API 类型构建未在当前机器完成 | 在安装依赖的前端环境或 CI 中执行包构建 | 用户要求测试没问题后提交；此处作为环境例外随 PR 报告 |
| 浏览器 E2E | 本次无页面、菜单或交互变更 | 不影响 API 边界交付 | 后续如接入业务页面审批流，再补页面 E2E | 用户要求本次处理 API 暴露和 core 禁用边界 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 通过 `WorkflowProcessApi`、`WorkflowTaskRuntimeApi`、`WorkflowBusinessApplyApi` 使用工作流；事件订阅使用 `WorkflowEventTypes` 和 `WorkflowEventPayloadVO` | `mango/mango-platform/mango-workflow/README.md` | `mvn -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter,mango-platform/mango-workflow/mango-workflow-starter-remote -am -DskipTests compile` | 业务侧不得依赖 workflow core service/event，不得直接查询 workflow 表 | API 错误按 `R` 处理；事件反序列化错误先检查 API 包版本 | DONE |
