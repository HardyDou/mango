# Infra Persistence 历史债务治理验收证据

## 1. 验收范围

- 页面：`/#/system/config` 参数配置。
- 接口：参数配置查询、新增、修改、删除；日历记录新增、分页查询、删除作为标准租户/审计实体的真实 MySQL 验证入口。
- 权限：默认租户平台管理员允许维护参数；A 公司管理员不可见参数配置入口，查询与新增接口返回 403。
- 数据：独立新库完成全部 Flyway 迁移；E2E 与 API 验证均使用唯一键并在结束时清理。
- 部署形态：Mango 单体后端与 `mango-admin` 源码前端。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30181`。
- 后端地址：`http://127.0.0.1:18181`，`/actuator/health` 为 `UP`，数据库组件为 MySQL。
- 数据库或租户：独立新库 `mango_dev_mango_infra_persistence_debt_181`；默认租户 `1/default`、A 公司租户 `2/company_a`。
- 测试账号：两个租户均使用各自 `admin`；凭据取自既有 E2E 测试配置。
- 浏览器：Playwright Chromium，单 worker。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| PERSIST-DEBT-001 | TC-001 | Persistence 三个 Maven 模块 | 单元、真实 Mapper、租户隔离、数据源路由、事务提交/回滚、Flyway、Schema 校验、Excel 事务 | H2 隔离库与测试迁移 fixture | 治理前 96 个用例中 1 failure、1 error；修复 `link` 迁移清单并新增 9 个边界用例后为 105/105，failures/errors/skips 均为 0 | 后端模块无独立 UI；对应浏览器消费结果由 TC-003/TC-004 验证 | Maven 构建成功 | Surefire 报告；命令：`mvn -f mango/pom.xml -pl :mango-infra-persistence-api,:mango-infra-persistence-starter,:mango-infra-persistence-web-starter test` | PASS |
| PERSIST-DEBT-002 | TC-002 | `MangoArchUnitChecker` | 抽象 Controller 分类兼容 | 无注解抽象 `LegacyBaseController`；带 `@RestController` 的抽象适配器 | 无注解抽象 Controller 后缀不再产生 9 个误报；真实 Controller stereotype 仍命中规则；合并 Fileproc 后 LocalCapabilityContract 语义同时保留；Checker 68/68 | 静态架构规则无 UI；真实页面仍由 TC-003/TC-004 验证 | Maven 构建成功 | `MangoArchUnitCheckerTest` Surefire 报告 | PASS |
| PERSIST-DEBT-003 | TC-003 | `/#/system/config`；`/api/system/config` | 平台管理员新增、编辑、删除参数 | `e2e.param.<timestamp>`，值从 `value-1` 修改为 `value-2`，finally 精确清理 | POST/PUT/DELETE 均返回 200 且业务成功；列表出现新行、更新值并在删除后消失 | 新增/编辑弹窗字段与保存按钮可操作；删除确认文案包含参数名；成功消息正确 | 无 401/403、未授权、拒绝访问、加载失败等页面错误；目标请求状态正确 | `mango-ui/apps/mango-admin/e2e/specs/config-management.spec.ts`；同步 main 后 `2 passed (11.4s)` | PASS |
| PERSIST-DEBT-004 | TC-004 | 参数配置菜单；配置查询/新增接口 | A 公司租户隔离与权限拒绝 | 租户 `2/company_a` | 查询与新增接口均返回 403；不写入测试参数 | 系统管理可见，但基础数据与参数配置入口均不可见 | 预期 403 已断言；页面无额外认证错误提示 | 同 TC-003 Playwright 用例 | PASS |
| PERSIST-DEBT-005 | TC-005 | 单体启动与全部 Flyway 模块 | 独立空库形成 | `mango_dev_mango_infra_persistence_debt_181` | 健康状态 `UP`；形成 222 张表、21 张 Flyway history 表；正式资源模式和启用 demo 资源后的启动均成功 | 前端可登录并进入真实页面 | 后端数据库健康检查为 MySQL/UP | `.mango/run/logs/mango-backend.log` 与健康检查回读 | PASS |
| PERSIST-DEBT-006 | TC-006 | 日历真实 API 与 MySQL 表 | 标准实体租户、创建人、更新人和时间审计填充 | `PERSIST_AUDIT_1784096705`，记录 ID `2077278259777499138`，测试后删除 | 数据库记录 `tenant_id=1`、`created_by=1`、`updated_by=1`，创建/更新时间均已填充；分页接口命中 1 条；删除后数据库剩余 0 | API/MySQL 证据验证底层填充；用户页面链路由 TC-003 验证 | 创建、分页、删除接口成功且无 4xx/5xx | MySQL 查询与 API 回读记录 | PASS |
| PERSIST-DEBT-007 | TC-007 | 测试资产审计 | 防止低价值测试和持久化替身冒充真实覆盖 | 本次新增/修改的 5 个测试文件 | `test-quality-check` PASS；后端 mock 审计 `block=0`、`warn=0`；新增测试未使用 Mapper、事务或持久化 mock | 测试治理无 UI；浏览器脚本本身未使用路由 mock | 审计命令退出码为 0，block/warn 均为 0 | `test-quality-check.mjs --base origin/main`；`audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `mango-infra-persistence` / `mango-system` 消费入口 | `/#/system/config` | 平台管理员参数真实 CRUD | A 公司菜单隔离与接口 403 | 列表、弹窗、当前值回显、删除确认、成功消息均有业务断言 | Playwright 定向报告与 `config-management.spec.ts` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 非 Persistence 的全仓模块 | 用户明确要求不重复执行全仓检查，本次只验证直接修改模块和真实消费入口 | 不能用本证据宣称所有 Mango 模块均完成回归 | 各模块按独立历史债务批次验证 | 已确认定向验证 |
| `SysConfig` 旧实体的 actor 字段为空 | 该系统实体未继承 Persistence 标准实体，属于 `mango-system` 既有模型边界；本次用标准 Calendar 实体验证租户/审计自动填充 | 不影响本次 Persistence 公共填充能力结论，但不能把 SysConfig 作为标准审计实体样本 | 后续治理 `mango-system` 时单独评估实体迁移兼容性 | 未纳入本次范围 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango 模块维护者 | 可复用 QueryWrapper 数组边界、数据源上下文嵌套/事务保护、真实 Mapper/事务/Flyway 套件和参数配置浏览器回归 | `mango/mango-infra/mango-infra-persistence`；`mango-ui/apps/mango-admin/e2e/specs/config-management.spec.ts` | 上述定向 Maven 命令；`PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm --dir apps/mango-admin exec playwright test e2e/specs/config-management.spec.ts --project=chromium --workers=1` | 每次使用独立新库；默认租户与 A 公司租户；测试数据唯一且清理 | 任一 Mapper/事务/Flyway失败、权限越界、浏览器业务断言失败均阻断；不得用 mock 替代持久化验证 | DONE |
