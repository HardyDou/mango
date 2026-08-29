# 标准交付记录

Issue #732 自动派单策略扩展

## 1. 元数据

- 任务 ID：Issue #732
- 交付模式：STANDARD
- 需求影响：L2 - 改变审批节点公开配置和运行时办理人选择，影响租户内任务分配结果
- 方案风险：L2 - 涉及 Flowable 活动任务查询、参与关系历史读取、事务内派单和前端设计器联动
- 最终风险：L2
- 工作区决策：REUSE (`/Users/hardy/Work/mango-issue-732`)

## 2. 目标与范围

- 目标：在现有 `assignmentMode=AUTO` 基础上支持按当前活动任务量最少和流程实例亲和性选择办理人。
- 成功条件：自动派单节点可配置三种策略；旧定义继续按 `ROUND_ROBIN`；候选人、租户和事务边界保持不变；策略选择有单测和模块验证。
- 处理范围：Workflow API 枚举与节点配置、BPMN 配置解析、运行时候选人选择、设计器配置 UI、能力文档和策略测试。
- 不处理范围：不改变 `CLAIM` 认领流程、候选人 Provider 授权、历史 BPMN 文件、业务状态机或已有数据库表结构。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-732-01 | 流程设计器 | 节点 `assignmentMode=AUTO` | 展示 `ROUND_ROBIN`、`LEAST_TASKS`、`AFFINITY` 三个策略 | 缺失策略按轮询兼容 | 组件测试看到三个策略选项 |
| AC-732-02 | 工作流运行时 | 有效候选人和 `LEAST_TASKS` | 统计当前租户活动任务并选择数量最少者 | 无候选仍返回 `AUTO_ASSIGN_NO_CANDIDATE` | 策略单测和模块测试通过 |
| AC-732-03 | 工作流运行时 | 有效候选人和 `AFFINITY` | 优先选择同一流程实例最近完成任务且仍在候选集中的用户；未命中回退 `LEAST_TASKS` | 不扩大租户或任务权限 | 亲和策略单测和模块测试通过 |
| AC-732-04 | 历史流程定义 | 缺失 `autoAssignmentStrategy` | 继续使用 `ROUND_ROBIN` | 不改变旧定义语义 | BPMN 转换测试通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-732-01 | AC-732-01/04 | 新增 `WorkflowAutoAssignmentStrategy`，配置字段 `autoAssignmentStrategy` 缺失时默认 `ROUND_ROBIN` | workflow api/core、前端 api | 删除字段读取和 UI 选项，保留原默认 |
| TD-732-02 | AC-732-02 | 通过 `ACT_RU_TASK` 与 `workflow_form_instance` 按当前租户统计活动 assignee 任务；并列按稳定 userId | WorkflowTaskRuntimeService | 策略回退 `ROUND_ROBIN` |
| TD-732-03 | AC-732-03 | 从 `workflow_process_participant` 读取同一流程实例最近 `COMPLETED_HANDLER`；不命中回退 `LEAST_TASKS` | WorkflowTaskRuntimeService | 关闭策略后仍使用轮询 |
| TD-732-04 | AC-732-01/02/03 | 复用现有 `assignmentMode=AUTO`、候选解析、游标和事务；不新增表或权限 | workflow runtime/UI | 回退到已有 AUTO 实现 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IM-732-01 | TD-732-01 | 1 | workflow API enum/model/converter、前端 API | 配置可序列化并兼容旧定义 |
| IM-732-02 | TD-732-02/03/04 | 2 | `WorkflowTaskRuntimeService` | 三种策略在同一事务内选择并记录策略 |
| IM-732-03 | TD-732-01 | 3 | `WorkflowNodeApprovalConfig.vue` 及组件测试 | AUTO 模式展示策略并可更新配置 |
| IM-732-04 | M08 | 4 | Workflow README、前端 README、能力地图、业务指南 | 公开配置和回退语义完整说明 |
| IM-732-05 | M10/M11/M13 | 5 | 策略单测、BPMN 转换测试、组件测试 | 受影响测试通过，环境阻塞单独记录 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-732-01 | M10 组件测试 + 浏览器 Playwright | `pnpm -C mango-ui/packages/workflow test --run src/views/workflow-definition/components/workflow-designer/__tests__/WorkflowNodeApprovalConfig.spec.ts`；真实浏览器进入流程定义设计器，切换 AUTO 后依次选择三种策略并保存、重新打开 | 通过（组件 2 tests）；浏览器看到“轮询 / 任务量最少 / 流程亲和”，切换选中状态正确，保存后重新打开仍为 `AUTO + AFFINITY` | `mango-docs/evidence/2026-08-29-issue-732-auto-assignment/` |
| AC-732-02 | M10/M11 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core verify` | 通过（88 tests，0 failures，0 errors，4 skipped） | Maven Surefire/verify 输出 |
| AC-732-03 | M10/M11 | `mvn -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowTaskRuntimeServiceImplIntegrationTest test` | 通过（13 tests）；H2 验证租户任务统计与最近参与人查询 | Maven Surefire |
| AC-732-04 | M10 | `WorkflowDesignerBpmnConverterTest`（包含于上述 verify） | 通过 | Maven Surefire |
| AC-732-05 | M10 | `pnpm -C mango-ui/packages/http-client build && pnpm -C mango-ui/packages/workflow test` | 通过（10 suites / 50 tests） | Vitest 输出 |
| AC-732-06 | M10/M13 | Mango CLI 启动 `mango_050`（后端 `18050`、前端 `30050`）；Playwright 浏览器登录并操作 `/workflow/manage/definition`；检查 network/console | 通过：创建、保存、列表查询均 200；重新打开设计器回显正确；network 无 4xx/5xx；console 0 errors、仅 Element Plus `el-radio label` 弃用 warning（58 条） | `network.log`、`console.log`、`affinity.png`、各 snapshot |

## 7. 例外与剩余风险

- 前端 Workflow 套件在未先构建本地 `@mango/http-client` 时会因包入口缺失而失败；按 workspace 正常构建顺序补建依赖后，完整套件已通过。该环境前置条件需在 CI/本地保持。
- 已在 H2 集成测试中验证 Flowable 运行表与 Workflow 表关联、租户过滤和参与关系排序；本次真实 MySQL 工作区和浏览器验收已执行。浏览器仅发现 Element Plus 现有 `el-radio` `label` API 弃用 warning，不影响本次交互结果，建议后续按 Element Plus 3.0 迁移到 `value`。
