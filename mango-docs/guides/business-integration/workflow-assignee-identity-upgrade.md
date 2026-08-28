# Workflow 办理人身份特性升级指南

本文面向已经接入 Mango Workflow 的业务项目，说明升级到“办理人身份增强”版本时的依赖、权限、代码适配和验收要求。

## 1. 特性概述

Workflow 查询和动作结果中的办理人字段现在统一为：

| 字段 | 含义 | 业务用途 |
|------|------|----------|
| `assigneeName` | Flowable 原始办理人 key，通常是 username | 业务判断、审计、幂等和账号兜底显示 |
| `assigneeId` | 当前租户解析出的 Mango 用户 ID | 关联用户、鉴权校验和稳定引用 |
| `assigneeDisplayName` | 昵称优先、username 兜底的显示名 | 页面展示 |

增强字段覆盖任务列表/详情、业务申请当前任务、业务进度、发起结果、任务动作结果和 Workflow 事件。字段是加法变更，旧客户端可以继续只读取 `assigneeName`。

## 2. 版本升级

后端和前端应按同一发布批次升级，不要混用旧后端和新前端的未匹配版本：

1. 后端将业务项目父 POM 的 `<mango.version>` 升级到本特性发布说明指定的 Maven 版本。
2. 前端将 `@mango/workflow` 升级到同一发布批次指定的 npm 版本；与其配套的 `@mango/common`、`@mango/admin-shell` 等包按发布矩阵一并对齐。
3. 业务后端继续只依赖 `mango-workflow-api`；运行时应用继续由 `mango-workflow-starter` 装配，不依赖 Workflow core 实现。
4. 本特性不新增业务数据库列、不要求 Flowable 表迁移、不要求历史任务回填，Flowable assignee 存储值保持原样。

## 3. 接口与权限

本特性新增 Identity 内部批量查询接口：

```http
POST /identity/user/info/batch
Authorization: Bearer <登录态>
Content-Type: application/json

{
  "userIds": [1001, 1002],
  "usernames": ["admin", "reviewer"]
}
```

接口只查询当前登录上下文租户内的成员资料，资源访问模式明确为 `LOGIN`：

- 不新增 `system:user:*` 或其它业务权限码。
- 只要请求带有效登录态即可访问，不需要给每个业务角色额外授权。
- 未登录仍返回认证失败；接口不会变成公开接口。
- 租户过滤由 Identity 服务执行，跨租户 userId 或 username 不会返回其它租户成员。

Workflow 任务列表、发起、办理、退回、认领和撤回等原有接口权限不因本特性改变。业务菜单仍需声明自己实际调用的 Workflow API 权限，业务后端仍需校验单据权限、任务可见性和状态机。

## 4. 后端适配

### 4.1 DTO 和映射

业务 DTO 或快照需要展示当前办理人时，新增并透传 `assigneeId`、`assigneeDisplayName`，不要把显示名覆盖写回 `assigneeName`：

```java
target.setAssigneeName(source.getAssigneeName());
target.setAssigneeId(source.getAssigneeId());
target.setAssigneeDisplayName(source.getAssigneeDisplayName());
```

如果业务只需要原始账号或只依赖任务动作，不必为了兼容性强制改造所有历史 DTO；页面要停止二次查询时，需要让新字段穿过 Controller、Facade、业务 VO 和前端响应。

### 4.2 事件消费者

事件 payload 新增显示字段，旧消费者按 JSON 忽略未知字段即可。事件处理规则：

- 使用 `assigneeId` 或 `assigneeName` 做权限、账号匹配、审计和幂等，不使用 `assigneeDisplayName`。
- `assignee` 和 `assigneeName` 仍是原始 Flowable key，不要把它们当成昵称。
- Identity 解析失败时 `assigneeId`、`assigneeDisplayName` 为空，事件和审批结果仍应继续处理。
- `workflow.task.advanced` 才代表当前任务快照已经刷新；`workflow.task.completed` 只代表源任务完成。

### 4.3 安全查询不能删除

本特性减少的是展示用的重复查询，不删除以下必要查询：

- 校验操作者是否属于当前租户；
- 校验账号是否启用；
- 校验角色、岗位、组织资格；
- 校验历史审批操作者和业务归属；
- 解析候选用户或通知接收人。

这些查询承担安全或业务规则，不是简单的页面昵称补全。

## 5. 前端适配

页面展示建议统一使用：

```ts
const displayName = task.assigneeDisplayName || task.assigneeName || '-';
```

未认领候选任务应根据 `claimStatus`/`claimable` 显示“待领取”，不能把 `candidateGroups` 中的角色或组织 key 当作用户名称。

业务项目如果有全局账号昵称拦截器或 `/account-display-names` 批量补全逻辑，应增加短路条件：

1. 响应对象已有非空 `assigneeDisplayName` 时，直接使用该值；
2. 不再为同一办理人收集 `assigneeName` 发起额外昵称请求；
3. 其它非 Workflow 字段（例如 `createdByName`、`operatorName`）仍可按原逻辑补全；
4. 显示名为空时回退 `assigneeName`，不要因补全接口失败阻断任务列表或审批结果。

如果页面需要同时展示昵称和账号，可分别显示 `assigneeDisplayName` 与 `assigneeName`。本特性不提供独立实名/姓名字段。

## 6. 历史数据与缓存

- 历史 Workflow 任务在读取时按现有 `assigneeName` 动态解析，不需要数据迁移或回填。
- 用户已被删除、移出当前租户或 username 已变化时，增强字段可能为空；页面应保留原始 key 或显示 `-`。
- 业务自己持久化的 `currentAssigneeName`、审批快照或签署快照属于业务事实，昵称变化不会触发批量重写。
- 若产品明确要求历史页面显示“当时昵称”，应由业务另行设计快照字段和回填策略，不能复用当前实时 `assigneeDisplayName` 冒充历史事实。
- 前端昵称目录缓存只用于展示优化，不参与权限判断；缓存失效或 Identity 暂时不可用时页面回退原始 key。

## 7. 升级步骤

1. 对照发布矩阵锁定后端 Maven、前端 npm 和配套包版本。
2. 检查业务 API、Facade、快照和事件 DTO 是否丢弃 `assigneeId`、`assigneeDisplayName`。
3. 给展示字段增加 `assigneeDisplayName -> assigneeName -> -` 的回退链。
4. 修改全局昵称补全器，避免对已有 Workflow 显示名重复请求。
5. 确认新增接口资源在启动后的资源表中为 `LOGIN`，且没有权限码；不要新增菜单按钮或角色授权。
6. 保留并回归业务原有的 Workflow 权限、租户过滤、操作者资格和业务状态校验。
7. 执行真实登录态验收：待办、详情、已办、业务进度、完成/驳回/认领结果和事件均检查三字段语义。

## 8. 验收清单

| 检查项 | 预期 |
|--------|------|
| 未登录访问 `/identity/user/info/batch` | 认证失败 |
| 已登录但无 `system:user:list/query` 权限访问批量接口 | 成功，接口不依赖这些管理权限 |
| 跨租户 userId/username 查询 | 不返回其它租户成员 |
| 已分配任务 | `assigneeName` 保留原始 key，两个增强字段按当前租户解析 |
| 未认领候选任务 | 不虚构办理人，增强字段为空，候选信息和认领状态保留 |
| Identity 暂不可用 | Workflow 查询/动作成功，页面回退原始 key |
| 业务页面已有 `assigneeDisplayName` | 不再请求展示用昵称补全接口 |
| 角色资格和操作者校验 | 仍执行 Identity/Role 查询，不因显示字段存在而跳过 |
| 旧事件消费者 | 能忽略新增字段并正常回写业务状态 |

## 9. 回滚

回滚只需按发布批次回退 Maven/npm 依赖和业务适配代码。无需回滚数据库或修改历史 Flowable 任务。回退后旧客户端继续读取 `assigneeName`；如果业务已依赖 `assigneeDisplayName`，应保留前端的原始 key 回退逻辑，直到所有运行实例完成版本切换。

关联文档：[业务审批接入](./workflow-business-approval.md)、[Workflow 后端 README](../../../mango/mango-platform/mango-workflow/README.md)、[@mango/workflow README](../../../mango-ui/packages/workflow/README.md)。
