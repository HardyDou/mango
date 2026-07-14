# Workflow 历史债务治理交付契约

## 1. 目标与范围

一次性治理 `mango-platform/mango-workflow` 的 API、Core、Starter、Remote、DDL 与初始化资源债务，保持既有 Java/HTTP/Feign 契约、流程定义、任务逻辑、权限租户、状态、快照和领域事件不变。只支持全新数据库，不提供旧 Flyway history 原地升级。

设计输入为 `mango-docs/designs/workflow-architecture-debt/` 下已批准的 BRD、SRS、TDD 与实施计划。代码范围为 Workflow 四个 Maven 子模块、模块 README、必要前端 README、能力地图、测试和本证据；不修改其它业务模块或前端生产逻辑。

## 2. 测试用例登记与自动化判断

| 用例 ID | 来源 AC | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 稳定契约 | 执行入口 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | AC-001 | 定义保存、发布、版本与业务错误保持 | P0 | 单元 | AUTO | 定义、版本、重复编码与非法设计数据 | 状态、版本、WorkflowCode、API 指纹 | `mvn -f mango/mango-platform/mango-workflow/pom.xml clean test` | `report.md` | AUTOMATED |
| TC-002 | AC-002 | 发起和全部任务动作、变量、记录、快照与事件顺序保持 | P0 | API | AUTO | 真实 H2 Flowable 定义、实例、任务和申请 | 引擎状态、业务表、返回和事件 | 同一 Maven test | `report.md` | AUTOMATED |
| TC-003 | AC-003 | 5 个 Java API 与全部 HTTP/Feign endpoint 保持 | P0 | API | AUTO | 反射、路由和权限 fixture | 方法、字段、泛型、verb/path/binding/权限 | 同一 Maven test | `report.md` | AUTOMATED |
| TC-004 | AC-004 | 单一纯 DDL V1、Flowable metadata 和正式新库启动 | P0 | API | AUTO | 独立 workspace 新 MySQL | schema 指纹、零 Flyway DML、metadata、health | Maven test 与 Mango CLI backend start | `report.md` | AUTOMATED |
| TC-005 | AC-005 | 正式与 Demo 资源分层、三套示例声明 | P1 | API | AUTO | 默认正式新库与独立 Demo 声明加载 | 默认零示例、三套 Workflow Demo 声明完整且仅 INIT_ONLY | Maven test、声明加载与正式库启动 | `report.md` | AUTOMATED |

## 3. 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求、SAC-001 至 SAC-004 | 先建立有价值的前置测试基线 | 修复测试基础设施、补关键特征测试后在生产代码未变时记录 before | `mango/mango-platform/mango-workflow/*/src/test` | `mango/mango-platform/mango-workflow/README.md` | `mango-docs/designs/workflow-architecture-debt/technical-design.md` | EXCEPTION: 后端领域与协议由单元/API/真实引擎测试充分观察，无新增浏览器交互 | `report.md` | before 43/43 与 after 48/48 | DONE | `report.md` |
| TASK-002 | 用户要求、SAC-001 至 SAC-004 | API/Core/Starter/Remote 一次到最终边界 | Service 去 R、实现名无 Impl、Entity/Mapper 规范、适配器逐项实现 API | `mango/mango-platform/mango-workflow` | `mango/mango-platform/mango-workflow/README.md` | `mango-docs/designs/workflow-architecture-debt/technical-design.md` | EXCEPTION: 前端生产代码不改，HTTP 消费由完整 endpoint 指纹覆盖 | `report.md` | 48/48、845→0 | DONE | `report.md` |
| TASK-003 | 用户要求、SAC-005 | Flyway 只负责最终 DDL且必需数据独立 | V1-V4 折叠为纯 DDL V1，Flowable metadata 引擎前正式初始化 | `mango/mango-platform/mango-workflow/mango-workflow-core` | `mango/mango-platform/mango-workflow/README.md` | `mango-docs/designs/workflow-architecture-debt/technical-design.md` | EXCEPTION: 数据库结构无浏览器结果，由 schema 与真实启动验证 | `report.md` | 新库 schema、metadata、health UP | DONE | `report.md` |
| TASK-004 | 用户要求、SAC-005 | 正式、必需和 Demo 分层登记 | 正式资源默认，三套示例仅显式 Demo | `mango/mango-platform/mango-workflow/mango-workflow-starter/src/main/resources/META-INF/mango` | `mango/mango-platform/mango-workflow/README.md` | `mango-docs/designs/workflow-architecture-debt/implementation-plan.md` | EXCEPTION: 无新增 UI 交互；声明加载和正式启动可自动验证 | `report.md` | 默认零 Demo，Workflow Demo 声明 3 条 | DONE | `report.md` |
| TASK-005 | 用户要求、SAC-001 至 SAC-005 | 启动服务终验并提交 PR | 同组 after、定向门禁、新库/API 冒烟、同步 main 后 PR 自动合并 | `mango/mango-platform/mango-workflow` | `mango/mango-platform/mango-workflow/README.md` | `mango-docs/designs/workflow-architecture-debt/implementation-plan.md` | EXCEPTION: 后端债务治理不冒充浏览器 UI 验收 | `report.md` | health UP；PR 状态在提交后补充 | IN_PROGRESS | `report.md` |

## 4. 验收证据记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | TC-001,TC-002,TC-003 | Workflow 模块测试入口 | 定义、发起、任务、事件与接口契约 | before 43、after 48 | 两组均全绿；API/HTTP 指纹和关键副作用保持 | EXCEPTION: 后端基线无 UI | EXCEPTION: 自动测试不经过浏览器 | `report.md` | PASS |
| TASK-003 | TC-004 | Workflow migration/启动 | 最终结构和引擎必需数据 | 独立 MySQL 8.4 新库 | 单一纯 DDL V1、12 表、metadata 正常、health UP | EXCEPTION: 数据库结构无 UI | HTTP 200/UP | `report.md` | PASS |
| TASK-004 | TC-005 | Workflow Resource Registry | 正式/Demo 分层和三套示例 | 正式新库与 Demo 声明加载 | 默认零示例；Workflow Demo 恰为三条且资产合法 | EXCEPTION: 资源同步无新增 UI 交互 | 正式应用启动成功 | `report.md` | PASS |

## 5. 测试结果基线

| 基线 ID | 覆盖台账 ID | 覆盖用例 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BASELINE-001 | TASK-001 | TC-001,TC-002,TC-003 | EXCEPTION: 前端生产代码未变，后端改前基线 | Workflow 四模块 `test` | Java 21.0.10、Maven 3.9.13 | H2 Flowable fixture | tenant 1、admin/发起人/审批人 | Core 37/37、Starter 6/6，总计 43/43 | 无 | `report.md` 与 surefire reports | 生产代码尚未修改 |
| BASELINE-002 | TASK-001 至 TASK-004 | TC-001 至 TC-005 | EXCEPTION: 后端/API/数据库交付，无新增浏览器行为 | Workflow 四模块 `test`、定向 architecture/static、单体新库启动 | Java 21.0.10、Maven 3.9.13、MySQL 8.4 | H2 fixture 与独立新库 | tenant 1 | Core 40/40、Starter 8/8；架构 0；通用静态库存 989→511 且新问题 0；health UP | 本机文件预览需关闭并提供测试 SM4 key，均非 Workflow 缺陷 | `report.md` | 内部架构和初始化政策有意调整，业务结果受测试保护 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango Workflow 业务开发者 | 使用新库最终态 V1；正式资源默认，Demo 显式；复用同组测试和任务/事件契约 | `mango/mango-platform/mango-workflow/README.md` 与本交付契约 | Workflow 四模块定向 test | 每个 workspace 独立新库；Demo tenant 1/admin | 测试失败查 surefire；启动/同步失败查 workspace 应用日志 | READY |
