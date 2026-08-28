# 标准交付记录

## 1. 元数据

- 任务 ID：workflow-assignee-identity
- 任务名称：Workflow 办理人身份契约
- 交付模式：STANDARD
- 需求影响：L2 - 改变 Workflow 公共查询、动作结果、事件和前端类型中的办理人身份契约
- 方案风险：L2 - 引入 Identity 与 Workflow 的批量协作，但保持 Flowable assignee 存储和权限判断不变
- 最终风险：L2
- 工作区决策：REUSE（复用 `feat/workflow-assignee-identity` 任务工作区）

## 2. 目标与范围

- 目标：为 Workflow 输出稳定的原始办理人键、租户内 Mango 用户 ID 和显示名，消除页面查询中的逐任务身份请求。
- 成功条件：`assigneeName` 保持原始 Flowable 值；`assigneeId` 和 `assigneeDisplayName` 由当前租户 Identity 批量解析；Identity 不可用时查询仍成功；每个列表或结果集最多调用一次批量身份解析。
- 处理范围：Identity 批量 API，Workflow 待办/已办/抄送、业务当前任务、动作结果、事件和前端类型/显示，能力说明及定向测试。
- 不处理范围：Flowable assignee 存储迁移、数据库新增显示名列、候选组展开为用户、权限/租户/任务可见性改变、发布和部署。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | Workflow 任务查询 | 已分配 username 办理人 | 原值写入 `assigneeName`，租户内用户写入 `assigneeId`，昵称或 username 写入 `assigneeDisplayName` | Identity 不可用或未找到时两个增强字段为空 | 待办、已办、抄送和详情契约一致 |
| SR-002 | 候选任务查询 | 任务尚未认领 | 不虚构办理人，增强字段为空，候选和认领状态保持原语义 | 候选 Identity 缺失不影响查询 | 角色、岗位、组织候选任务可正常返回 |
| SR-003 | 业务进度与动作结果 | 当前任务发生认领、转交、加签或推进 | 当前任务、顶层快捷字段和后续任务使用同一身份语义 | Identity 失败时保留原始办理人键 | 结果内字段相互一致 |
| SR-004 | Workflow 事件消费者 | 发布含当前任务的标准事件 | `assignee`/`assigneeName` 保留原始键，新增显示名字段 | Identity 失败时事件仍发布 | 单任务和任务列表载荷一致 |
| SR-005 | Workflow 前端 | 后端返回增强字段 | 显示顺序为显示名、原始键、认领状态文本、`-` | 缺失字段不导致渲染异常 | 类型、归一化和展示测试通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001, SR-002 | Identity 提供按去重 `userIds`/`usernames` 批量查询的当前租户接口，只返回已解析用户 | `mango-identity-*` | 删除新增批量契约和适配器 |
| TD-002 | SR-001, SR-003, SR-004 | Workflow Core 依赖内部 `IWorkflowAssigneeIdentityProvider`，Starter 通过 `IdentityUserApi` 适配 | `mango-workflow-core`, `mango-workflow-starter` | 移除 provider 和增强调用，原始字段不受影响 |
| TD-003 | SR-001, SR-003 | 不新增数据库列；当前任务实体继续保存原始办理人键，读取/事件时批量增强 | Workflow 当前任务快照 | 回滚代码，无数据修复 |
| TD-004 | SR-005 | 前端 ID 使用 `WorkflowId` 字符串语义，显示名单独建模 | `@mango/workflow` | 删除新增可选字段和显示函数 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | TD-001 | 1 | `mango-identity-api/core/starter/starter-remote` | 批量契约、租户过滤和边界测试完成 |
| TASK-002 | TD-002 | 2 | `mango-workflow-core/starter` | 单结果集一次解析且失败开放 |
| TASK-003 | TD-003 | 3 | Workflow VO、服务和事件 | 原始键、ID、显示名语义一致 |
| TASK-004 | TD-004 | 4 | `mango-ui/packages/workflow` | 类型、归一化和展示回退一致 |
| TASK-005 | TD-001 - TD-004 | 5 | README、能力地图、业务指南、测试 | 门禁和定向验证完成 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001 | M10/M11/M12/M13 | Identity 与 Workflow 定向测试、模块编译、真实审批 E2E | PASS（Identity 批量查询与 Workflow 身份服务/事件测试通过；真实待办和详情契约一致） | Identity Core 25 用例、Workflow Core 26 用例、Identity Starter 3 用例、Workflow Starter 1 用例；Core 编译通过；清理修正后 Chromium 串行重复 E2E `5 passed (43.6s)` |
| SR-002 | M10/M11 | Flowable 候选、认领/取消认领场景 | PASS（现有 Core/Starter 定向测试通过） | 候选组未认领保持空办理人，claim/unclaim 结果保留原始 key 的既有测试 |
| SR-003 | M11/M12/M13 | 转交、加签、推进、动作结果测试；真实两节点审批 | PASS（动作结果、当前任务和完成进度一致） | Workflow Core 定向套件；`start-business`、`complete-result` 和 `progress/latest` 真实链路；终审后 `APPROVED` 且 `currentTasks=[]` |
| SR-004 | M10/M11 | 事件载荷契约测试 | PASS | `WorkflowEventPublisherTest`、`WorkflowEventPublisher` 单一必需构造器 |
| SR-005 | M09/M10/M13 | workflow 包测试、类型检查、构建和浏览器真实审批 | PASS（前端 9 文件 47 用例及生产构建通过；待办、详情和已办真实页面显示 `Administrator`） | Node `v22.23.1`、pnpm `11.14.0`；清理修正后 Chromium 串行重复 E2E `5 passed (43.6s)`；验收证据与截图见 `mango-docs/evidence/2026-08-28-workflow-assignee-identity/`；Node 26 被项目运行时约束拒绝；包级 raw typecheck 的历史错误单列为剩余风险 |

能力说明门禁通过：`audit-module-readmes.mjs` 和 `audit-readme-source-facts.mjs` 均为 PASS。直接修改的 Maven 模块验证、Identity/Workflow 定向测试以及 `git diff --check` 均已通过。真实审批 E2E 使用专用工作区数据库、租户 `1` 和现有 `admin`，从定义发布、业务发起、首次推进、待办/详情显示、页面终审到已办和最终业务进度形成闭环；清理修正后 Chromium 串行重复结果为 `5 passed (43.6s)`，console error、page error、request failure 和 HTTP 5xx 均为 0。任务前缀在 Workflow 业务表、Flowable 运行/历史实例和引擎定义中的残留回读均为 0。证据见 `mango-docs/evidence/2026-08-28-workflow-assignee-identity/acceptance.md`。

## 7. 例外与剩余风险

- Workflow 包没有独立 `typecheck` script。准备依赖闭包后执行 raw `vue-tsc`，仍命中该包既有测试全局类型和严格类型债务；本次改动的生产文件没有新增诊断，但不能声明包级 raw typecheck 通过。
- 仓库根 shell 的 Homebrew Node 可能抢在 nvm 前解析；`mango-ui/.nvmrc` 与现有 `.node-version` 均固定为 `22.23.1`，`package.json#devEngines.runtime` 以 `onFail: error` 拒绝错误版本且不自动下载。已验证 Node 26 安装命令失败、Node 22.23.1 正常执行、`nvm use` 命中本机已安装版本。
- 同一 E2E 通过 `--repeat-each=5` 在 5 个 worker 中并发压测时，多个实例同时部署和清理同一 Flowable 引擎库，出现两次清理死锁及一次发起 500；标准单用例和 `--workers=1` 串行重复稳定通过。本任务不声明同库并发压测通过，后续如需该目标应使用隔离 schema/数据库设计独立并发用例。
