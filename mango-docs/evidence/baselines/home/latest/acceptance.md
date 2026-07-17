# Mango Home 历史债务验收证据

## 1. 范围与结论

- 验收日期：2026-07-17
- 模块：`mango-platform/mango-home`
- 基准：`origin/main` commit `2dec9302e0c80a2d8e5c37b147b66453ff5f6aed`
- 分支：`refactor/home-debt`
- 结论：Home 的 API、Controller、Service、Entity、模块依赖和 Flyway 历史债务已收敛；既有 HTTP 路径、JSON 线格式、权限码、菜单位置、默认首页优先级和页面操作语义保持不变。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30010`
- 单体后端：`http://127.0.0.1:18010`
- 数据库：`mango_dev_mango_home_debt_010`，验收前删除并重新创建
- 租户/账号：租户 `1`、账号 `admin`；不记录密码与 token
- 浏览器：Playwright Chromium
- 组装边界：Home 当前只有本地 starter，由单体应用组装；仓库没有独立 Home capability app 或 remote starter，因此不虚构微服务、多节点验收结论。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-HOME-001 | TC-001 | 平台能力 / 首页管理、`/home/templates/**` | 模板创建、草稿、发布、授权、默认首页解析 | `E2E首页管理<timestamp>`，用户为当前登录返回的真实 userId | 发布版本可解析；个人、部门继承、角色和系统默认优先级正确；空授权列表清除全部授权 | 模板列表、用户视图和管理操作按真实接口数据回显 | 浏览器 console error、pageerror、requestfailed、HTTP 5xx 四类诊断集合均为空 | `e2e/01-home-management-template-list.png`、`e2e/02-home-user-view.png`、`e2e/03-home-management-ui-flow.png` | PASS |
| TASK-HOME-002 | TC-002 | 平台能力 / 首页管理 | 停用、删除保护、复制、独立用户首页查询、列表批量删除 | 每条用例使用时间戳前缀并在开始/结束清理 | 停用模板不再授权可见；有授权时删除被业务规则拒绝；清空授权后删除成功；复制与批量删除结果正确 | 操作列、用户选择、复制、授权和批量删除均通过浏览器真实交互完成 | 预期删除保护返回业务失败；未出现请求失败、页面异常或 HTTP 5xx | `e2e/04-home-list-default-user-pages.png`；Playwright 最终结果 5 条管理用例全部通过 | PASS |
| TASK-HOME-003 | TC-003 | `/#/home`、`/home/pages/**` | 用户创建、重命名、复制、排序、默认页和指定页路由 | `E2E首页<timestamp>` 两个个人页面，同时存在只读授权页 | 排序请求仅包含个人可排序页面；授权只读页不进入个人排序负载；默认页和路由切换结果正确 | 新建、重命名、复制、上下移动、设为默认和布局编辑状态均有可见断言 | 浏览器四类诊断集合均为空；关键 API 响应未出现 HTTP 5xx | `e2e/01-home-current-default.png` 至 `e2e/05-home-layout-editing.png`；Playwright 1/1 通过 | PASS |
| TASK-HOME-004 | TC-004 | Fresh DB / 最终 JAR | V1 DDL、canonical 字段、资源同步和启动 | 全新库 `mango_dev_mango_home_debt_010`，demo 资源显式开启 | `flyway_schema_history_home` 只有 baseline 与成功 V1；五张表均有 id/tenant/org/audit 字段；JAR 只含 V1 | `ROLE_ADMIN`、成员 1001 绑定和 4 个 Home 菜单授权实际入库后才开始 UI 验收 | 健康检查为 UP；启动日志没有 Home migration/schema 异常 | SQL、`jar tf` 和最终启动记录；Chromium 6/6 通过 | PASS |
| TASK-HOME-005 | TC-005 | Home Controller HTTP | API 接口继承校验与空列表业务契约 | 缺失页面名、缺失 id、授权列表为 null 或空列表 | 非法请求返回 HTTP 400；null 授权列表被拒绝；空列表保留“清空全部授权”语义 | 非 UI 场景；通过真实 Spring MVC 参数绑定和校验链验证 | 3 条 HTTP 校验与 1 条接口继承测试通过，未触发 Controller/API 校验冲突 | `HomeControllerHttpValidationTest`、`HomeControllerValidationInheritanceTest` 共 4/4 | PASS |

## 4. 改前基线

| 项目 | 结果 |
|---|---|
| Home Core 单元测试 | 8/8 通过，主要依赖 Mockito，未覆盖模板管理完整语义 |
| Home API / Starter 测试 | 0 条，未覆盖 API 继承校验和 HTTP 参数绑定 |
| 数据库与浏览器闭环 | 未形成 Fresh DB、最终 JAR migration 清单、真实角色菜单同步和浏览器诊断的同一基线 |

改前绿灯只能证明局部 Service 行为，不能排除迁移重复、API/Controller 校验冲突、前端大整数精度和只读授权页误入个人排序等问题。

## 5. 改后自动化与构建

| 层级 | 命令/范围 | 结果 |
|---|---|---|
| 后端单元/契约/HTTP | `mango-home-api/core/starter` 定向 `clean install` | Core 13/13、Starter 4/4，共 17/17 通过 |
| 测试质量 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | 18 个变更测试文件检查通过；被测 Service 真实执行，仅 mock 外部依赖 |
| 架构 | API、Core、Starter 分别执行 `mango:architecture` full mode | 三个模块均为 dependency=0、archunit=0、pmd=0、blocking=0 |
| 静态质量 | Home 三模块分别生成 PMD、Checkstyle、SpotBugs 报告 | 三类报告在 API、Core、Starter 中均为 0 |
| 前端单元 | `pnpm -F @mango/admin-shell test` | 9 个文件、40/40 通过；包含 2 条首页排序域测试 |
| 前端构建 | `@mango/admin-shell`、`@mango/home`、`mango-admin` | 三个定向生产构建全部成功 |
| 浏览器 E2E | 两个 Home spec，Chromium，`--workers=1` | 6/6 通过，耗时约 1.1 分钟 |

## 6. Fresh DB 与最终构件

- Flyway 只负责 DDL；Home 没有必须初始化的业务数据，也没有模块自带 demo 数据。内置默认首页由运行时代码提供。
- 验收为管理员开启通用 demo 资源注册，等待 `ROLE_ADMIN`、成员绑定和 Home 菜单授权实际就绪后再执行 E2E，避免把“健康端点已 UP”误当成资源同步完成。
- `sys_home_template`、`sys_home_template_version`、`sys_home_template_authorization`、`sys_user_home_page`、`sys_user_home_preference` 均包含 `id`、`tenant_id`、`org_id`、`created_by`、`created_at`、`updated_by`、`updated_at`。
- 工作区与本地 Maven 仓库中的 `mango-home-core` JAR 均只包含 `db/migration/home/V1__init_home.sql`，不含已删除 V2。

## 7. 本次固定的经验

1. API 集合参数需要区分 `null` 与空列表；清空型接口不能用 `@NotEmpty` 改坏既有业务语义。
2. API 接口声明 Bean Validation 后，Controller 不重复声明同一套 `@Valid`；同时必须用 HTTP 测试证明继承生效。
3. 浏览器收到的 Snowflake ID 全程按字符串处理，排序等数值运算使用 `BigInt`，禁止经过 JavaScript `Number`。
4. 个人页面排序必须过滤授权/内置只读页；不能把接口返回的所有首页 ID 原样提交给个人排序接口。
5. 资源注册是异步派生过程，Fresh DB 验收必须等待实际角色、成员和菜单绑定，而不是只等待健康检查。
6. 共享管理员状态的 E2E 应串行执行；并发 worker 会制造相互覆盖的假失败。
7. 多表模板、版本、授权聚合属于领域行为，不为形式统一强行改成单表 `MangoCrudService`。
8. Partial Reactor 只约束 Maven 模块，不保证聚合插件的每条自有规则都按相同路径收敛；遇到范围外失败必须按报告文件路径归因，并单独确认目标模块的静态报告，不能反复跑全仓命令。

## 8. 未验证项与风险

| 项目 | 原因 | 影响 | 后续处理 |
|---|---|---|---|
| Firefox/WebKit | 本次日常验收矩阵只要求 Chromium | 已证明主浏览器和真实后端主链路；跨浏览器差异未纳入本次结论 | 发布批次或夜间矩阵执行 |
| Home 独立微服务/多节点 | 仓库当前不存在 Home 独立应用和远程 starter | 不影响现有单体组装；不能声称不存在的部署形态已验证 | 若未来新增部署形态，补独立 JVM、服务发现和多节点测试 |

## 9. 结论

Home 改后测试相对改前增加了迁移契约、模板领域行为、HTTP 校验、前端排序和浏览器 E2E 覆盖。最终在同一全新数据库上完成启动、资源就绪、6 条浏览器用例和最终 JAR 校验，接口、逻辑与特性保持兼容，目标模块架构阻断项为 0。
