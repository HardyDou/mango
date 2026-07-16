# Mango System 历史债务治理验收证据

## 1. 验收范围

- 页面：行政区划、参数配置、字典管理、机构管理。
- 接口：System 字典、配置、行政区划、机构、国际化公开接口。
- 权限：平台管理员可维护；A 公司管理员不显示平台管理入口，维护接口返回 403；公共选择接口可读。
- 数据：System 9 张表、默认与 demo 租户、行政区划、国际化、管理员菜单套餐绑定。
- 部署形态：`mango-monolith-app`，全新 MySQL 数据库。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30001`
- 后端地址：`http://127.0.0.1:18001`
- 数据库：MySQL 8.4，`mango_dev_system_debt_20260716`，验收前删除并重建。
- 测试账号：平台租户与 A 公司租户的 `admin` 演示账号。
- 浏览器：本机 Chrome，Playwright `chromium` 项目，单 worker。
- 本地能力配置：文件预览关闭；SM4 使用仅限本次本地测试的 32 位测试密钥；demo 资源显式开启。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| SYSTEM-DEBT | TC-001 | 行政区划 | 平台维护自定义区划 | E2E 唯一区划 | 新增、读取、删除成功 | 新增弹窗、列表回显和删除反馈均正确 | CRUD 请求均为 HTTP 200，无非预期 4xx/5xx | Playwright list report | PASS |
| SYSTEM-DEBT | TC-002 | 行政区划 | 机构权限隔离与选择器读取 | A 公司 | 管理入口不可见、写接口 403、选择器可读 | 侧栏无行政区划管理入口，业务选择器正常展示 | 写接口为预期 403，选择接口为 200，无非预期错误 | Playwright list report | PASS |
| SYSTEM-DEBT | TC-003 | 参数配置 | 平台 CRUD | E2E 唯一配置键 | 新增、修改、删除成功 | 表单提交、列表回显和成功消息均正确 | CRUD 请求均为 HTTP 200，无非预期 4xx/5xx | Playwright list report | PASS |
| SYSTEM-DEBT | TC-004 | 参数配置 | 机构权限隔离 | A 公司 | 管理入口不可见、维护接口 403 | 侧栏无参数配置入口，页面无越权功能按钮 | 维护接口为预期 403，无非预期 5xx | Playwright list report | PASS |
| SYSTEM-DEBT | TC-005 | 字典管理 | 类型与数据 CRUD | E2E 唯一字典 | 新增、修改、选项读取、删除成功 | 类型和数据列表正确回显，操作消息正确 | CRUD 与选项请求均为 HTTP 200，无非预期错误 | Playwright list report | PASS |
| SYSTEM-DEBT | TC-006 | 字典管理 | 机构权限隔离与选项读取 | A 公司 | 管理入口不可见、维护接口 403、选项接口可读 | 侧栏无字典管理入口，业务选项仍可正常使用 | 维护接口为预期 403，选项接口为 200 | Playwright list report | PASS |
| SYSTEM-DEBT | TC-007 | 机构管理 | 禁用/归档使旧 token 失效 | E2E 临时机构 | 登录选项移除、旧 token 不可继续访问 | 状态更新后登录机构选项同步消失 | 状态接口为 200，旧 token 请求返回预期拒绝且无 5xx | Playwright list report | PASS |
| SYSTEM-DEBT | TC-008 | 机构管理 | 类型与套餐持久化 | ENTERPRISE/package 2 | API 与页面回读一致 | 机构类型和套餐列展示与保存值一致 | 创建与查询请求均为 HTTP 200，无非预期错误 | Playwright list report | PASS |
| SYSTEM-DEBT | TC-009 | 机构管理 | 新增、编辑、状态、依赖删除阻断 | E2E 临时机构 | 完整闭环成功，存在依赖时禁止删除 | 页面状态、编辑结果和阻断反馈均正确 | 正常操作为 200，删除返回预期业务阻断且无 5xx | Playwright list report | PASS |
| SYSTEM-DEBT | TC-010 | 机构管理 | 机构管理员隔离 | A 公司 | 入口不可见、维护接口 403 | 侧栏无机构管理入口，页面无越权操作按钮 | 维护接口为预期 403，无非预期 5xx | Playwright list report | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| mango-system | 四个系统管理页面 | 平台 CRUD | 机构越权隔离 | 真实登录、真实菜单、真实接口 | 10 条 Playwright 用例，48.3 秒 | PASS |
| mango-system | 新库启动 | Flyway 仅 V1 | 资源与权限派生关系 | 健康检查 UP | 数据库查询记录 | PASS |

## 5. 自动化与数据库证据

- 改前基线：System 60 条单元/接口测试，0 失败。
- 改后同层测试：System 62 条（core 57、starter 5），0 failures、0 errors、0 skipped；resource-api 2 条同样通过。
- 跨模块回归：`AuthRoleResourceHandlerIntegrationTest` 通过；Identity 用户、密码策略、安全策略三组定向测试通过。
- 定向架构门禁：使用 PMO 规定的 partial Reactor 模式扫描 `mango-system-api/core/starter`，0 条违规；未执行全仓扫描。
- 测试质量门禁：扫描 18 个变更测试文件，无恒真/同值断言，未 mock/spy 被测对象。
- E2E 命令：`pnpm exec playwright test area-management.spec.ts config-management.spec.ts dict-management.spec.ts tenant-management.spec.ts --project=chromium --reporter=list --workers=1`。
- E2E 结果：10 passed，48.3s。
- 新库 System Flyway：成功版本仅 `1`；System 表数量 9。
- 初始化数据：`sys_tenant=4`、`sys_area=524`、`sys_i18n=20`。
- 管理员菜单：tenant 1 为 110 条；tenant 2/3/4 各 61 条。
- System 资源处理器写入的 `tenant_id` 均使用运行时主键 `1`。

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| LibreOffice 文件预览 | 本机未安装对应能力，本次范围为 System | 不影响 System 页面和接口 | 文件预览模块单独验收 | 不适用 |
| 全仓门禁 | 用户明确要求不重复执行全仓检查 | 本证据只证明目标模块、直接消费者与真实单体路径 | PR 使用目标范围检查 | 已按要求执行 |

## 7. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | System API、资源分层、Flyway DDL、测试与空库验收结论 | 本文与 `mango-platform/mango-system/README.md` | Maven 定向测试 + 四个 Playwright spec | demo 需显式开启 | 任一单元/API/E2E 失败均不得判定完成 | READY |
