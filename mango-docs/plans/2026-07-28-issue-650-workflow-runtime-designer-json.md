# Issue #650 Workflow 业务流程图快照交付记录

## 1. 元数据

- 任务 ID：ISSUE-650-WORKFLOW-RUNTIME-DESIGNER-JSON
- Issue：[HardyDou/mango#650](https://github.com/HardyDou/mango/issues/650)
- 交付模式：STANDARD
- 需求影响：L2 - `mango-workflow-api` 的业务流程实例和任务详情公共响应缺少前端公开契约已声明的流程图字段，导致业务审批详情无法展示流程轨迹。
- 方案风险：L2 - 修改公开 VO，在 Workflow 运行时服务中增加发布版本快照读取，并将两个业务详情入口从资源权限访问改为仅登录访问；涉及 API、Core、Starter 消费链、访问边界和前端业务渲染，但不改变流程状态机、数据库结构或管理接口。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-issue-650`，分支 `fix/workflow-runtime-designer-json`
- 保障措施：M01、M08、M09、M11、M12

## 2. 目标与范围

- 目标：让已登录业务页面从流程实例或任务详情响应取得实例实际运行版本的 `designerJson`，供 `WorkflowProgressTree`、`WorkflowSidebar` 和流程图弹窗渲染。
- 成功条件：任务详情和流程实例详情只要求登录、不要求资源权限，并返回与运行实例 `processDefinitionId` 精确匹配的发布版本设计树；定义发布新版本后历史实例结果不漂移；前端不调用流程定义管理接口。
- 处理范围：`WorkflowTaskDetailVO`、`WorkflowProcessDetailVO`、Workflow 运行时详情组装、发布版本 Mapper 查询、后端定向集成测试、前端业务详情回归测试、Workflow 能力说明和业务接入文档。
- 不处理范围：流程定义管理接口、独立流程图接口、新权限码、菜单、数据库 migration、流程实例或任务的既有行级数据过滤规则、流程设计器、业务项目页面接线和版本发布部署。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | 业务只读详情页调用 `GET /workflow/processes/detail` | 当前账号已经登录，实例引用已发布流程版本 | 不检查 `workflow:process:detail` 资源权限；响应 `designerJson` 来自实例实际使用的发布版本 | 未登录时拒绝；实例不存在时保持原错误；无精确快照时字段为空 | Controller 访问模式为 `LOGIN`，响应字段存在且能由前端解析为流程设计树 |
| REQ-002 | 当前办理人调用 `GET /workflow/tasks/detail` | 当前账号已经登录，任务引用已发布流程版本 | 不检查 `workflow:task:detail` 资源权限；响应返回与所属实例相同版本的 `designerJson` | 未登录时拒绝；任务不存在时保持原错误；无精确快照时字段为空 | Controller 访问模式为 `LOGIN`，任务详情与流程详情对同一实例返回相同设计树 |
| REQ-003 | 历史实例详情 | 同一 Mango 定义已经从 V1 发布到 V2，历史实例仍引用 V1 的 `processDefinitionId` | 返回 V1 快照，不读取 V2 当前定义 | 禁止按 `definitionId` 或定义 key 回退到最新版本 | 自动化测试证明历史实例不发生版本漂移 |
| REQ-004 | 普通业务账号渲染流程图 | 账号已登录，但没有 `workflow:process:detail`、`workflow:task:detail` 或任何 `workflow:definition:*` 权限 | 页面只消费任务详情或流程详情中的 `designerJson` | 快照缺失时沿用现有审批记录降级，不发起管理接口请求 | 访问模式测试为 `LOGIN`；前端测试证明定义管理 API 调用次数为零 |
| REQ-005 | 多租户业务调用 | 实例、发布版本和当前上下文处于既有租户边界 | 快照查询沿用 MyBatis 租户过滤；`LOGIN` 只替代资源权限判断，不新增匿名或管理接口旁路 | 当前租户查不到精确版本时不得读取其它租户或最新定义 | 实现不增加匿名访问、跨租户查询或管理权限依赖 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001、REQ-002 | 在 `WorkflowProcessDetailVO` 和 `WorkflowTaskDetailVO` 增加可选字符串字段 `designerJson`；这是向后兼容的响应扩展，并与现有 `@mango/workflow` TypeScript 契约对齐 | `mango-workflow-api` | 删除字段并恢复旧响应 |
| DEC-002 | REQ-001、REQ-002、REQ-003、REQ-005 | 运行时服务按 Flowable `processDefinitionId` 精确查询 `workflow_definition_version`，读取不可变 `designer_json`；不通过管理 Service/API 获取定义 | `mango-workflow-core` | 删除版本 Mapper 依赖和详情组装逻辑 |
| DEC-003 | REQ-003 | 兼容旧数据时，只允许使用 `processDefinitionId` 完全相同的当前定义；版本表和当前定义均不匹配时返回空，不按 `definitionId`、definition key 或 publishedVersion 读取最新定义 | `mango-workflow-core` | 移除兼容回退，只保留精确版本查询 |
| DEC-004 | REQ-001、REQ-002、REQ-004、REQ-005 | 将 `/workflow/processes/detail`、`/workflow/tasks/detail` 和兼容入口 `/workflow/tasks/process-detail` 的 `ApiAccess` 改为 `LOGIN`；登录态由 Mango Access 统一校验，不要求详情权限码，不开放匿名访问；不新增接口，不调用 `definitionDetail()`、`definitionVersionDetail()` 或其它管理 API | `mango-workflow-starter`、`@mango/workflow` | 将三个 Controller 方法恢复为 `PERMISSION` 并恢复原权限码 |
| DEC-005 | 全部 | M08 启用：同步后端 Workflow README、前端 Workflow README、业务审批接入指南和能力地图，明确字段来源、历史版本语义及无需定义管理权限 | `mango/mango-platform/mango-workflow/README.md`、`mango-ui/packages/workflow/README.md`、`mango-docs/**` | 随代码回滚同步撤销对应说明 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | DEC-001 | 1 | `mango-workflow-api/.../WorkflowProcessDetailVO.java`、`WorkflowTaskDetailVO.java` | 两个公共 VO 都声明带中文 Schema 的 `designerJson` |
| TASK-002 | DEC-002、DEC-003 | 2 | `mango-workflow-core/.../WorkflowTaskRuntimeService.java` | 任务和流程详情复用同一精确快照解析逻辑，历史版本不漂移 |
| TASK-003 | DEC-002、DEC-003 | 3 | `mango-workflow-core/src/test/**` | 集成测试覆盖任务详情、流程详情、V1/V2 历史快照和缺失快照语义 |
| TASK-004 | DEC-004 | 4 | `mango-workflow-starter/src/test/**`、`mango-ui/packages/workflow/src/views/task-detail/__tests__/taskDetail.spec.ts` | Controller 合同固定三个详情入口为 `LOGIN`；业务详情携带设计树时可渲染进度，并确认定义管理 API 未调用 |
| TASK-005 | DEC-005 | 5 | Workflow README、业务指南、能力地图 | 公开合同、权限边界、版本来源和验证入口说明一致 |
| TASK-006 | 全部 | 6 | 受影响模块与文档 | 定向测试、直接模块质量检查、能力文档检查和差异检查全部得到真实结论 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001、REQ-002、REQ-003、REQ-005 | Workflow Core 集成测试 | Maven 定向执行 `WorkflowTaskRuntimeServiceImplIntegrationTest` | PASS | 10 个测试通过；V1 实例的任务详情和流程详情均返回 V1 `designerJson`，未读取当前 V2 定义 |
| REQ-001、REQ-002、REQ-004 | API/Core/Starter 直接模块质量验证与 Controller 访问合同测试 | `mvn -q -f mango/pom.xml -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter -Dmaven.repo.local=.mango/m2/repository verify` | PASS | 直接修改的三个 Maven 模块 `verify` 通过；Controller 定向合同测试 4 个通过，三个详情方法均为 `LOGIN` 且 permission 为空；HTTP surface fingerprint 已按访问合同变更更新 |
| REQ-004 | 前端组件测试与构建 | `pnpm exec vitest run --config vitest.config.ts --reporter=dot`；`pnpm build`，目录 `mango-ui/packages/workflow` | PASS | 定向任务详情测试 16 个通过；补齐工作区依赖产物后，全包 7 个测试文件、33 个测试通过，生产构建通过；运行时设计树成功渲染且定义管理 API 调用次数为零 |
| 全部 | 测试质量与能力说明检查 | `node mango-pmo/tools/test-quality-check.mjs --base main`；`node mango-pmo/tools/audit-module-readmes.mjs`；`node mango-pmo/tools/audit-readme-source-facts.mjs`；`node mango-pmo/tools/check-business-guides.mjs` | PASS | 测试质量检查覆盖 4 个文件；模块 README、源码事实和 5 份业务指南检查全部通过 |
| 全部 | 差异检查 | `git diff --check`、占位标记扫描和最终差异审查 | PASS | 无空白错误；占位标记命中均为既有测试数据或业务状态枚举，无交付占位；未发现无关改动 |

## 7. 例外与剩余风险

- 本次按用户要求将流程实例和任务详情从资源权限访问调整为仅登录访问；仍沿用现有租户和详情数据查询语义，不开放匿名访问，也不新增绕过详情入口的独立读取能力。
- 极早期或异常数据若既没有发布版本记录，也没有 `processDefinitionId` 完全相同的当前定义，将继续降级为审批记录视图；为避免历史版本漂移，不使用最新定义猜测快照。
- 本任务不执行 Maven/npm 发布。业务项目需要在后续发布完成后升级包含该修复的 `mango-workflow-api` 与 `mango-workflow-starter` 版本。
