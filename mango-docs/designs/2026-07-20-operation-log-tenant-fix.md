# 标准交付记录

## 1. 元数据

- 任务 ID：INC-20260720-runtime-persistence-warnings
- 交付模式：STANDARD
- 需求影响：L2 - 修复操作审计日志租户归属、第三方工作流表错误结构告警、审批待办通知接收人丢失，并调整通知与审批管理入口层级
- 方案风险：L2 - 变更日志入库语义、持久化校验默认排除范围、工作流通知接收人映射和跨模块菜单父级，但不改变业务接口与页面路由契约
- 最终风险：L2
- 工作区决策：CREATE - `fix/operation-log-tenant`

## 2. 目标与范围

- 目标：修复平台级操作日志空租户异常；结构校验不再把 Flowable 表当作 Mango 业务表；工作流待办通知能从办理人、候选用户和候选组得到真实接收人；通知与审批管理入口统一归入平台能力。
- 成功条件：无租户上下文的操作日志归属 `default`；普通租户日志保留原租户；`ACT_*`、`FLW_*` 表不参与 Mango 标准字段校验；分配给用户、角色、岗位或组织的待办通知均携带可解析接收目标；“通知中心”和“审批中心”成为“平台能力”的直接子菜单且原路由不变。
- 处理范围：`mango-system-core` 的操作日志租户解析；`mango-infra-persistence-starter` 的第三方表默认排除项；`mango-workflow-starter` 的通知接收人映射；Notice 与 Workflow 的 `AUTH_MENU` 父级声明；对应定向测试和能力说明。
- 不处理范围：通用租户自动填充策略、登录日志、Mango 自有 `workflow_*` 表、页面组件和数据库结构；用户侧“消息中心”仍保持独立一级入口。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | `OperationLogAspect` / `ISysLogService.recordOperationLog` | 平台级请求，命令未携带租户 ID | 日志以 `tenant_id=default` 入库 | 不再触发 `tenant_id cannot be null` | 持久化集成测试回读 `default` |
| SR-002 | `ISysLogService.recordOperationLog` | 命令携带普通租户 ID | 原租户 ID 原样归属 | 不得错误归入平台租户 | 持久化集成测试回读原租户 ID |
| SR-003 | 既有操作日志入口 | 任意操作日志记录 | HTTP/API 契约和表结构不变 | 不引入跨模块或前端回归 | 直接修改 Maven 模块 `verify` 通过 |
| SR-004 | `SchemaValidationRunner` | 数据库包含 Flowable `ACT_*`、`FLW_*` 引擎表 | 跳过 Mango 主键及审计/租户标准字段校验 | 不输出第三方表的错误结构告警 | fail-fast 模式下校验通过 |
| SR-005 | `SchemaValidationRunner` | 数据库包含 Mango 自有业务表 | 继续执行既有标准字段校验 | 不得因工作流排除项扩大而漏检业务表 | 缺标准字段的 `workflow_*` 表仍校验失败 |
| SR-006 | `workflow.task.advanced` 通知订阅器 | 新任务有直接办理人或候选用户 | 通知携带去重后的 `userId/userIds` | 不再因接收用户为空发送失败 | 订阅器测试断言用户 ID |
| SR-007 | `workflow.task.advanced` 通知订阅器 | 新任务按角色、岗位或组织分配 | 将 Flowable 候选组精确映射为 Notice `recipientTargets` | 不得把组 ID 当用户 ID，也不得扩大组织负责人范围 | 订阅器测试断言目标类型和 ID |
| SR-008 | 并行审批任务 | 事件包含多个 `currentTasks` | 汇总全部任务的办理人、候选用户和受支持候选组并去重 | 不得只通知第一个并行任务 | 多任务 payload 测试通过 |
| SR-009 | 管理后台菜单 | 当前角色拥有通知中心管理权限 | “通知中心”显示在“平台能力”下 | 不得继续作为一级菜单，也不得改变 `/notice` 路由 | Notice 资源合同与定向菜单 E2E 通过 |
| SR-010 | 管理后台菜单 | 当前角色拥有审批中心权限 | “审批中心”显示在“平台能力”下 | 不得继续作为一级菜单，也不得改变 `/workflow` 路由 | Workflow 资源合同与定向菜单 E2E 通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001, SR-002 | 在操作日志服务将空白租户规范化为既有平台租户标识 `default`；非空租户去除首尾空白后保留 | `SysLogService` | 回退租户解析方法及调用 |
| TD-002 | SR-001, SR-002 | 使用 Spring + MyBatis-Plus + H2 的真实 Mapper 入库测试覆盖非空约束和字段回读 | `SysLogServiceIntegrationTest` | 删除新增测试 |
| TD-003 | SR-003 | 不修改通用 `PersistenceAuditMetaObjectHandler`，避免扩大到所有租户表 | 无跨模块改动 | 无需额外回滚 |
| TD-004 | SR-004, SR-005 | 在结构校验默认排除项增加 Flowable 当前实际拥有的 `act_*`、`flw_*` 前缀；不按宽泛的“工作流”名称排除 Mango 自有表 | `PersistenceProperties.SchemaValidation`、Persistence README | 移除两个默认前缀 |
| TD-005 | SR-006, SR-007, SR-008 | 通知订阅器同时解析顶层任务快照和 `currentTasks`；数字办理人/候选用户映射用户 ID，`ROLE:`/`POST:`/`ORG:` 映射对应 Notice 目标；`ORG_LEADER:` 不错误扩大为全组织 | `WorkflowNoticeDomainEventSubscriber`、Workflow README | 回退接收人选择逻辑 |
| TD-006 | SR-009, SR-010 | 复用既有跨模块菜单父级协议，为 `notice`、`workflow` 根菜单声明 `parentCode=data` 并递增资源版本；菜单编码、路径、组件和权限不变 | Notice/Workflow `*-common-menu.json`、模块 README | 移除父级声明并回退资源版本后的声明内容 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IM-001 | TD-001 | 1 | `mango-system-core/.../SysLogService.java` | 入库前得到非空的有效租户 ID |
| IM-002 | TD-002 | 2 | `mango-system-core/src/test/.../SysLogServiceIntegrationTest.java` | 平台租户和普通租户场景均通过 |
| IM-003 | TD-003 | 3 | `mango-system-core` | 定向 `verify` 通过且无范围外改动 |
| IM-004 | TD-004 | 4 | `mango-infra-persistence-starter` | Flowable 表跳过、Mango 表继续校验的测试通过 |
| IM-005 | TD-004 | 5 | `mango-infra-persistence/README.md` | 默认排除项与第三方表所有权说明同步 |
| IM-006 | TD-005 | 6 | `mango-workflow-starter/.../WorkflowNoticeDomainEventSubscriber.java` | 用户和候选组接收目标完整、去重 |
| IM-007 | TD-005 | 7 | `WorkflowNoticeDomainEventSubscriberTest.java`、Workflow README | 直接、候选和并行任务场景有测试与说明 |
| IM-008 | TD-006 | 8 | `notice-common-menu.json`、`workflow-common-menu.json` | 两个管理入口归入 `data` 且资源版本递增 |
| IM-009 | TD-006 | 9 | Notice/Workflow 资源合同测试、模块 README | 菜单父级与既有路由均有自动断言和说明 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001 | M11 持久化集成测试 | `mvn -pl mango-platform/mango-system/mango-system-core -Dtest=SysLogServiceIntegrationTest,OperationLogAspectTest test` | PASS：4 tests | Surefire 与 H2 Mapper 回读输出 |
| SR-002 | M11 持久化集成测试 | `mvn -pl mango-platform/mango-system/mango-system-core -Dtest=SysLogServiceIntegrationTest,OperationLogAspectTest test` | PASS：4 tests | Surefire 与 H2 Mapper 回读输出 |
| SR-003 | M09/M10/M11 模块质量门禁 | `mvn -pl mango-platform/mango-system/mango-system-core verify` | PASS：62 tests | Maven `BUILD SUCCESS` |
| SR-004 | M10 结构校验单元测试与模块门禁 | `mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -Dtest=SchemaValidationRunnerTest test`；同模块 `verify` | PASS：定向 10 tests；模块 86 tests | Maven `BUILD SUCCESS` |
| SR-005 | M10 结构校验单元测试与模块门禁 | `mvn -pl mango-infra/mango-infra-persistence/mango-infra-persistence-starter -Dtest=SchemaValidationRunnerTest test`；同模块 `verify` | PASS：定向 10 tests；模块 86 tests | Maven `BUILD SUCCESS` |
| SR-006 | M10 通知映射单元测试与模块门禁 | `mvn -pl mango-platform/mango-workflow/mango-workflow-starter -Dtest=WorkflowNoticeDomainEventSubscriberTest test`；同模块 `verify` | PASS：定向 3 tests；模块 12 tests | Maven `BUILD SUCCESS` |
| SR-007 | M10 通知映射单元测试与模块门禁 | `mvn -pl mango-platform/mango-workflow/mango-workflow-starter -Dtest=WorkflowNoticeDomainEventSubscriberTest test`；同模块 `verify` | PASS：定向 3 tests；模块 12 tests | Maven `BUILD SUCCESS` |
| SR-008 | M10 通知映射单元测试与模块门禁 | `mvn -pl mango-platform/mango-workflow/mango-workflow-starter -Dtest=WorkflowNoticeDomainEventSubscriberTest test`；同模块 `verify` | PASS：定向 3 tests；模块 12 tests | Maven `BUILD SUCCESS` |
| SR-009 | M09/M10 资源合同、M13 定向 UI/E2E | Notice starter `verify`；Playwright `menu-navigation.spec.ts --grep "归入平台能力" --project=chromium` | PASS：Notice 13 tests；Chromium 1 test | Maven 与 Playwright `passed` 输出、测试附件截图 |
| SR-010 | M09/M10 资源合同、M13 定向 UI/E2E | Workflow starter `verify`；Playwright `menu-navigation.spec.ts --grep "归入平台能力" --project=chromium`；Shell runtime `--grep "hybrid profile loads RBAC and Workflow"` | PASS：Workflow 12 tests；菜单与 hybrid runtime 各 1 个 Chromium test | Maven 与 Playwright `passed` 输出、测试附件截图 |

## 7. 例外与剩余风险

- 不将无租户上下文推广为通用持久化默认值；仅操作日志按已有平台日志语义归属 `default`。
- 只排除仓库当前直接集成的 Flowable 引擎表前缀；未来引入其他第三方表时仍需按真实所有权显式登记。
- `ORG_LEADER:` 候选组没有等价 Notice 目标类型，本次不将其降级为全组织通知，避免越权扩大；该场景继续要求事件提供明确候选用户或新增专用负责人目标能力。
- 菜单层级调整只重挂管理侧根菜单；“消息中心”继续作为登录用户独立入口，通知与审批的既有地址、站内信动作目标和页面注册 key 不迁移。
