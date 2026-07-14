# CMS 历史债务治理交付契约

## 1. 目标与范围

一次性治理 `mango-platform/mango-cms` 的 API、领域服务、HTTP 适配、DDL 与初始化资源债务，保持既有 Java/HTTP 契约、业务状态、权限、租户和公开消费逻辑。只支持全新数据库，不提供旧 Flyway history 原地升级。

设计输入为 `mango-docs/designs/cms-architecture-debt/` 下已批准的 BRD、SRS、TDD 与实施计划。代码交付范围为 CMS 的 api、core、starter、starter-remote、README、测试和本基线；不修改其它业务模块或前端生产代码。

## 2. 测试用例登记与自动化判断

| 用例 ID | 来源 AC | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 稳定契约 | 执行入口 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | AC-001 | 管理聚合、内容状态与租户边界保持 | P0 | 单元 | AUTO | 独立 CMS 测试对象与双租户边界 | 状态、副作用、错误消息 | `mvn -f mango/mango-platform/mango-cms/pom.xml clean test` | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | AUTOMATED |
| TC-002 | AC-002 | Java API、HTTP 路由、权限与文件响应保持 | P0 | API | AUTO | Controller 契约与公开文件 fixture | API 指纹、verb/path、R 泛型、响应头 | `mvn -f mango/mango-platform/mango-cms/pom.xml clean test` | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | AUTOMATED |
| TC-003 | AC-003 | 新 V1 复现最终结构且只含 DDL | P0 | API | AUTO | workspace 新建 MySQL 数据库 | 12 表列与索引指纹、零业务行 | `mvn -f mango/mango-platform/mango-cms/pom.xml clean test` | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | AUTOMATED |
| TC-004 | AC-004 | 正式资源与 71 条 Demo 资源隔离并可同步 | P0 | API | AUTO | 默认新库与显式 Demo 新库 | 默认零 Demo、显式 71 条、无文件依赖 | `mvn -f mango/mango-platform/mango-cms/pom.xml clean test` | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | AUTOMATED |
| TC-005 | AC-005 | 新库服务、公开接口和 Demo 前端代理冒烟 | P1 | 手工 | MANUAL | workspace `mango_182`、Demo 站点 | health、公开数据数量、前端 HTML 与 `/api` 代理 | `node mango-ui/packages/mango-cli/src/index.mjs dev start backend` | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | MANUAL |

## 3. 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求、AC-001 | 先建立有价值的前置测试基线 | 先补特征测试再改生产代码 | `mango/mango-platform/mango-cms/mango-cms-core/src/test` | `mango/mango-platform/mango-cms/README.md` | `mango-docs/designs/cms-architecture-debt/technical-design.md` | EXCEPTION: 后端状态与租户规则由单元/API 层充分观察，无浏览器结果 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | before 35/35 与 after 同入口对比 | DONE | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` |
| TASK-002 | 用户要求、AC-002 | API、Service 与 Controller 一次到最终边界 | 保持公共契约，按 11 个管理聚合拆分内部服务 | `mango/mango-platform/mango-cms/mango-cms-core/src/main/java/io/mango/cms/core/service` | `mango/mango-platform/mango-cms/README.md` | `mango-docs/designs/cms-architecture-debt/technical-design.md` | EXCEPTION: 无前端生产变更，HTTP 消费契约由 API 指纹和 Controller 测试覆盖 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | API 指纹、42 条测试、定向架构门禁 | DONE | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` |
| TASK-003 | 用户要求、AC-003 | Flyway 只负责最终 DDL | 将 V1-V10 折叠为纯 DDL V1，仅支持新库 | `mango/mango-platform/mango-cms/mango-cms-core/src/main/resources/db/migration/mango-cms/V1__init_mango_cms.sql` | `mango/mango-platform/mango-cms/README.md` | `mango-docs/designs/cms-architecture-debt/technical-design.md` | EXCEPTION: 数据库结构没有浏览器用户结果，由新库 schema 对比充分验证 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | 新库执行、列与索引逐表对比、零数据核验 | DONE | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` |
| TASK-004 | 用户要求、AC-004 | 正式、必须和 Demo 数据分层登记 | 正式资源默认加载，Demo 资源显式开关且 `INIT_ONLY` | `mango/mango-platform/mango-cms/mango-cms-starter/src/main/resources/META-INF/mango` | `mango/mango-platform/mango-cms/README.md` | `mango-docs/designs/cms-architecture-debt/implementation-plan.md` | EXCEPTION: 资源注册与数据库结果由真实应用/API 入口验证，无新增 UI 交互 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | 默认/显式 Demo 两次新库启动与 71 条同步核验 | DONE | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` |
| TASK-005 | 用户要求、AC-005 | 启动服务完成最终功能验证 | 使用 Mango CLI 启动 backend 与 Demo app，核对真实数据与公开 API | `mango/mango-platform/mango-cms/mango-cms-starter/src/main/java/io/mango/cms/starter/resource` | `mango/mango-platform/mango-cms/README.md` | `mango-docs/designs/cms-architecture-debt/implementation-plan.md` | EXCEPTION: 当前会话缺少 in-app browser 控制入口；已验证前端 HTTP、HTML 和 `/api` 代理，不声明完整浏览器 UI 验收 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | health、DB、六组公开 API、前端与代理冒烟 | DONE | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` |

## 4. 验收证据记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | TC-001 | CMS 模块测试入口 | 状态、租户、公开资格与文件授权 | 35 条前置基线、42 条最终套件 | 42/42，失败/错误/跳过 0 | EXCEPTION: 后端规则无 UI | EXCEPTION: 单元/API 测试不经过浏览器 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | PASS |
| TASK-002 | TC-002 | `/cms/**`、`/cms/open/**` | Java/HTTP/权限协议 | API 反射指纹与 MockMvc fixture | 两个指纹不变，关键路由与响应头不变 | EXCEPTION: 前端生产代码未改 | EXCEPTION: 契约由 API 自动化验证 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | PASS |
| TASK-003 | TC-003 | CMS Flyway | 纯 DDL 与结构等价 | workspace 新建数据库 | 12 表列/索引一致且全空 | EXCEPTION: 数据库结构无 UI | EXCEPTION: 数据库验证不经过浏览器 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | PASS |
| TASK-004 | TC-004 | Resource Registry | 正式/Demo 分层和同步 | 默认新库、显式 Demo 新库 | 默认零 Demo；9 类 71 条全部成功 | EXCEPTION: 资源同步由应用入口验证 | EXCEPTION: 数据与 API 断言替代不了浏览器，故未声明 UI 通过 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | PASS |
| TASK-005 | TC-005 | `http://127.0.0.1:38182/` | Demo 页面服务与后端代理 | `demo` 站点、域名 `127.0.0.1:5193` | HTML 标题/挂载点存在，`/api` 返回 demo 站点；六组后端 API 数量正确 | EXCEPTION: 当前会话无 in-app browser 控制入口，未做完整视觉/交互验收 | EXCEPTION: 未取得浏览器 console/network；Vite 日志无启动错误且代理响应成功 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | EXCEPTION |

## 5. 测试结果基线

| 基线 ID | 覆盖台账 ID | 覆盖用例 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BASELINE-001 | TASK-001,TASK-002,TASK-003,TASK-004,TASK-005 | TC-001,TC-002,TC-003,TC-004,TC-005 | EXCEPTION: 前端生产代码未变且当前浏览器控制入口不可用，保留 API/前端代理冒烟证据 | `mvn -f mango/mango-platform/mango-cms/pom.xml clean test` | Java 21.0.10、Maven 3.9.13 | `mango_dev_mango_cms_architecture_debt_182` 新库 | Demo tenant `1`，匿名公开入口 | 42/42；架构 0；新增静态问题 0；71 条 Demo 同步成功 | 失败 0、阻塞 0、浏览器 UI 例外 1 | `mango-docs/evidence/baselines/cms-architecture/latest/report.md` | 公共契约与业务逻辑无未批准变化；旧 Demo 栏目状态纠正后根栏目由 0 恢复为 5 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango CMS 业务开发者 | 使用纯 DDL V1；正式资源默认登记；Demo 仅显式开启；复用 42 条测试和公开 API 验收 | `mango/mango-platform/mango-cms/README.md` | `mvn -f mango/mango-platform/mango-cms/pom.xml clean test` | 每个 workspace 使用独立新库；Demo tenant `1`；不包含密码或 token | 测试失败先查对应 JUnit；启动/同步失败查 workspace 应用日志；浏览器 UI 例外需在具备控制入口的环境补验 | DONE |
