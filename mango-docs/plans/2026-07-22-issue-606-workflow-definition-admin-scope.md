# 标准交付记录

## 1. 元数据

- 任务 ID：ISSUE-606-WORKFLOW-DEFINITION-ADMIN-SCOPE
- 交付模式：STANDARD
- 需求影响：L2 - 全新演示数据库的默认管理员虽然拥有流程定义管理菜单和 API 权限，但缺少数据范围，管理分页以成功空列表隐藏真实定义。
- 方案风险：L2 - 调整 Workflow demo 初始化资源与 Authorization 的 `INIT_ONLY` 接管语义，涉及权限和持久化初始化，但不改变租户隔离、公开 API 或流程定义表结构。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issue-606`，`fix/issue-606-workflow-definition-admin-scope`
- 保障措施：M01、M08、M09、M10、M11

## 2. 目标与范围

- Issue：[HardyDou/mango#606](https://github.com/HardyDou/mango/issues/606)。
- 目标：开启 demo 初始化的全新数据库中，租户 `1` 的默认 `ROLE_ADMIN` 获得 `workflow:definition:list = ALL`，可以查看租户内全部流程定义。
- 成功条件：Resource Registry 幂等创建有效角色数据范围；已有人工数据范围不会在新声明首次接管时被覆盖；管理分页不因 `startEntryVisible=false` 隐藏内嵌流程。
- 处理范围：Workflow demo 资源声明、Authorization 角色数据范围 `INIT_ONLY` 保护、定向集成测试和模块能力说明。
- 不处理范围：生产环境默认授权、跨租户授权、查询层权限绕过、流程定义启动入口可见性、前端布局和版本发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | 默认管理员 / 流程定义管理分页 | 全新租户 `1` 数据库、demo 资源开启、`ROLE_ADMIN` 已初始化 | Resource Registry 创建 `workflow:definition:list = ALL` | API 返回成功空分页，管理员误判无流程定义 | 声明可被正式 Loader 装载，字段与 `INIT_ONLY` 模式准确 |
| REQ-002 | 已维护数据范围的管理员 | 相同角色与资源已存在 `ORG` 等运行时规则，新版本首次加载声明 | 保留既有规则，不改写为 `ALL` | 升级扩大管理员可见范围 | 真实 Mapper 集成测试证明目标行和字段均保持不变 |
| REQ-003 | 流程定义管理与发起流程入口 | 定义同时包含 `startEntryVisible=true/false` | 数据范围控制管理权限；启动可见性只控制发起入口 | 通过启动可见性过滤管理列表 | 不修改管理查询，能力说明明确两者边界 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001、REQ-003 | 在 Workflow demo 声明中增加租户 `1`、`internal-admin`、`ROLE_ADMIN`、`workflow:definition:list`、`ALL` 的 `AUTH_ROLE_DATA_SCOPE`，不在查询层为管理员绕过数据权限 | `mango-workflow-starter` demo 资源 | 移除新增声明；既有运行时目标由资源生命周期按规则管理 |
| DEC-002 | REQ-002 | `AuthRoleDataScopeResourceHandler` 首次处理 `INIT_ONLY` 且目标已存在时返回既有目标，不更新范围、状态或范围值 | `mango-authorization-starter` | 移除目标存在分支，恢复原 upsert 行为 |
| DEC-003 | REQ-001、REQ-002 | 以资源装载契约测试和 H2 + MyBatis-Plus 集成测试覆盖声明与持久化结果 | Workflow、Authorization 测试目录 | 删除对应回归测试 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-001 | DEC-001 | 1 | Workflow demo YAML | 新声明通过 Loader 解析且字段精确 |
| IMP-002 | DEC-002 | 2 | Authorization resource handler | `AUTO` 继续更新，`INIT_ONLY` 保留已有目标 |
| IMP-003 | DEC-003 | 3 | 两个 starter 的定向测试 | 新增回归用例通过 |
| IMP-004 | DEC-001、DEC-002 | 4 | Workflow、Authorization README 与业务接入指南 | 初始化行为、可见性边界、角色菜单/按钮排查边界可追踪 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001 | M09、M10 | `mvn -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-starter -Dtest=WorkflowResourceDeclarationContractTest test`；模块 `verify` | PASS - 声明装载测试 4/4；Workflow Starter 全量 16/16 | Maven 输出，2026-07-22 |
| REQ-002 | M11 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-starter -Dtest=AuthRoleDataScopeResourceHandlerIntegrationTest test`；模块 `verify` | PASS - 真实 H2 + MyBatis-Plus 测试 8/8；Authorization Starter 全量 64/64 | Maven 输出，2026-07-22 |
| REQ-003 | M09、M08 | `git diff --check`；`node mango-pmo/tools/test-quality-check.mjs --base main`；能力说明与 README 审计 | PASS - 无空白错误；2 个修改测试通过质量检查；能力说明、模块 README 与源码事实审计通过 | 本次命令输出，2026-07-22 |

## 7. 例外与剩余风险

- 本任务不执行破坏性的空库重建；最终业务消费项目仍需在发布后用全新数据库回归 `ROLE_ADMIN` 的真实 HTTP 分页。
- 本任务不执行版本发布、部署、提交、Push、PR 或关闭 Issue。
