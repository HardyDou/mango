# Grid Layout 历史债务治理验收证据

## 1. 验收范围

- 页面：`/#/home/<homeId>` 作为 `@mango/grid-layout` 组件的现有真实消费入口；当前 `mango-admin` 不直接消费 `gridLayoutPersonalApi`。
- 接口：个人布局查询、保存、更新和删除；空 `pageCode` 参数校验。
- 权限：默认租户和 A 公司管理员登录态，验证同一用户名称跨租户不可读取布局。
- 数据：独立空库完成 Flyway 迁移；API 与浏览器数据均使用唯一键并在 `finally` 中精确清理。
- 部署形态：Mango 单体后端与 `mango-admin` 源码前端。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30187`。
- 后端地址：`http://127.0.0.1:18187`，`/actuator/health` 返回 `UP`，数据库组件为 MySQL。
- 数据库或租户：独立新库 `mango_dev_mango_grid_layout_debt_187`；默认租户 `1/default`、A 公司租户 `2/company_a`。
- 测试账号：两个租户各自的 `admin`，凭据取自仓库既有 E2E 测试配置。
- 浏览器：Playwright Chromium（Chrome channel），单 worker。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| GRID-DEBT-001 | TC-001 | Grid API/Core/Starter Maven 模块 | 单元、真实 Mapper 和 Controller/API 校验元数据 | H2 MySQL mode；tenant 1/2；用户 1001/2002；有效、越界、错页和畸形 JSON | 治理前 6/6；治理后 12/12，failures/errors/skips 均为 0；真实 Mapper 覆盖插入、更新、查询、删除、租户/用户隔离、org 与审计填充 | 后端模块没有 Personal 管理页面；Controller 约束由 API 继承且元数据解析成功 | Surefire 三模块构建成功，Controller 测试未出现约束声明冲突 | `GridLayoutPersonalServiceTest`、`GridLayoutPersonalServiceIntegrationTest`、`GridLayoutPersonalControllerValidationTest` Surefire 报告 | PASS |
| GRID-DEBT-002 | TC-002 | Grid API/Core/Starter 静态与架构入口 | 清理实体、Service、Controller 历史债务 | 治理前定向完整扫描：dependency 0、ArchUnit 5、PMD 21，总计 26 | 同一完整扫描治理后 dependency=0、archunit=0、pmd=0、blocking=0；Checkstyle/PMD 各模块 0；SpotBugs XML 精确计数 API=0、Core=0、Starter=0 | 静态规则不承担浏览器行为证明，UI 消费链由 TC-006 单独验证 | 静态报告 `totalIssueCount=0`、`toolFailureCount=0` | `mango/target/mango-static-report.json` 与三个模块 `target/spotbugsXml.xml` | PASS |
| GRID-DEBT-003 | TC-003 | 单体启动、Grid Flyway 与 MySQL schema | 从空库形成唯一 V1 数据结构 | `mango_dev_mango_grid_layout_debt_187`，启动前 drop/create | 源目录和 clean 后 `target/classes` 均只有 `V1__init_grid_layout.sql`；history 只有 baseline+V1；表具有 id、tenant、org、user、page、schema、layout 与四个审计列，唯一键为 tenant+user+page | 数据库形成不依赖 UI；应用健康检查确认真实 MySQL 连接 | 后端监听 18187，MySQL health component 为 UP，Flyway success=1 | MySQL `SHOW CREATE TABLE`、Flyway history 与启动健康回读 | PASS |
| GRID-DEBT-004 | TC-004 | `/grid-layout/personal` | 真实服务入口 CRUD、稳定 ID、隔离和失败语义 | `grid-layout-api-<timestamp>`；布局从 x=0/w=6 更新为 x=1/w=5 | 首查 null；PUT 新增；第二次 PUT 保持同一 ID 且 JSON 更新；tenant 2 查询 null；空 `pageCode` 返回 400 和明确校验消息；删除后查询 null | Personal API 当前没有 Admin 直接消费页面，因此本行不声称浏览器调用该 API | GET/PUT/DELETE 目标请求均符合预期状态；失败响应不含 `ConstraintDeclarationException`；未产生 5xx | `grid-layout-personal-live.spec.ts` 的 `@api` 用例；Chromium repeat 三轮均通过 | PASS |
| GRID-DEBT-005 | TC-005 | Personal API 与 `mango_user_grid_layout` | 真实 MySQL 租户、组织列、审计和删除语义 | `grid-layout-audit-<timestamp>`，默认租户平台管理员 | 表记录 tenant_id=1、user_id=1、created_by=1、updated_by=1，创建/更新时间均已填充；平台管理员无组织上下文时 org_id 为 NULL；删除后行数为 0 | API/MySQL 联合回读承担持久化证明；不存在 Personal UI 可见结果 | 保存与删除请求成功，数据库回读与 API 返回一致 | MySQL 行回读；H2 集成用例另证明存在 ORG 上下文时 org_id 自动填充 | PASS |
| GRID-DEBT-006 | TC-006 | `/#/home/<homeId>` | 现有 Home 页面真实消费 Grid 设计器组件的编辑、恢复默认和保存 | `栅格布局E2E-<timestamp>-<worker>`，用例结束删除 Home 页面 | 进入 editing 状态后显示 grid designer 与组件库；确认恢复默认；`/home/pages/layout` 保存响应 200；返回可编辑状态 | 仅证明 `@mango/grid-layout` 前端组件真实消费链，不宣称 Home 调用 Personal API | Chromium repeat-each=3：3/3；console error=0、pageerror=0、requestfailed=0、5xx=0 | `grid-layout-personal-live.spec.ts` 的 `@ui` 用例；`grid-layout-ui-success.png`；Playwright list 报告 | PASS |
| GRID-DEBT-007 | TC-007 | 测试资产审计 | 防止替身冒充真实持久化与低价值断言 | 本次新增或修改的 11 个测试资产文件 | `test-quality-check` 通过；后端测试替身审计 block=0、warn=0；Mapper 替身只用于单元分支，另有真实 Mapper 集成和 MySQL/API 证据 | E2E 未拦截或伪造业务响应，所有请求到真实源码服务 | 两个治理脚本退出码均为 0 | `test-quality-check.mjs --base origin/main`；`audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `mango-grid-layout` 后端 | Personal 无直接页面 | 真 API CRUD、失败语义与跨租户隔离 | 唯一 V1、标准租户/组织/审计实体和物理删除 | M13 对 Personal 为 N/A；不以 Home 页面代替 Personal 服务入口证明 | Playwright `@api` 报告与 MySQL 回读 | PASS |
| `@mango/grid-layout` 前端包 | `/#/home/<homeId>` | Grid designer 进入编辑和恢复默认 | 通过实际 Home 消费者保存布局 | 设计器、组件库、确认浮层和编辑状态均有业务断言 | `grid-layout-ui-success.png` 与 Playwright `@ui` 报告 | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| Personal API 的直接 UI E2E（M13） | 精确搜索确认当前 `mango-admin` 没有页面调用 `gridLayoutPersonalApi` | 不能宣称已有产品页面完成 Personal API 用户交互闭环；本次以真实 API/入口流承担 M12 | 业务页面未来显式接入 Personal API 时，在该消费者模块补对应 UI E2E | 用户要求按真实入口验证，禁止伪造 UI 结论 |
| 平台管理员的 `org_id` 为空 | 该账号登录上下文没有组织 ID；列允许 NULL，集成用例已用真实审计处理器证明 ORG 上下文填充 | 不影响 tenant+user+page 隔离和现有接口行为 | 有组织上下文的消费者继续由标准 `TenantEntity` 自动填充 | 已按上下文真实值记录，不伪造组织数据 |
| `starter-remote` | Grid Layout 没有 remote starter，调用契约为本地 Java API 和 HTTP Controller | 本次不存在 remote 聚合边界可修复或验证 | 未来新增 remote starter 时单独治理依赖边界 | 按模块现状判定 N/A |
| 非 Grid Layout 的全仓模块 | 用户明确要求不重复执行全仓检查，本次只验证目标模块和真实消费入口 | 本证据不能外推为所有 Mango 模块完成回归 | 其它模块按各自历史债务批次验证 | 已确认定向验证 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango Grid Layout 维护者 | 可复用 Personal 真 API CRUD/隔离/校验套件、真实 Mapper 集成、唯一 V1 空库验证和现有 Grid UI 消费回归 | `mango/mango-platform/mango-grid-layout`；`mango-ui/apps/mango-admin/e2e/specs/grid-layout-personal-live.spec.ts` | 定向 Maven 三模块测试；外部服务模式下执行定向 Playwright Chromium 用例 | 每次使用独立新库；租户 1/2；测试数据唯一并在 finally 清理 | 任一架构/静态缺陷、Mapper/校验失败、租户越界、Flyway 多版本、浏览器错误或 5xx 均阻断；Personal UI 继续按 N/A 记录 | DONE |
