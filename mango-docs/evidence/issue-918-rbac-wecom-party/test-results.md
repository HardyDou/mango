# Issue #918 测试结果

## 1. 基线与环境

- 基线提交：`870f1bbdb7a064c455c8654c94c5ccf3e570e5a8`
- 工作分支：`issue-918-rbac-wecom-party`
- Node.js：`22.23.1`
- pnpm：`11.14.0`
- Java：`21.0.10`
- 浏览器：Playwright Chromium `1.61.1`
- 后端：`http://127.0.0.1:18005`，Actuator `UP`
- 前端：`http://127.0.0.1:30005`
- 数据库：MySQL `8.4.8`，`mango_dev_mango_issue_918_rbac_wecom_party_005`
- 账号与租户：芒果集团 `admin`，`tenantId=1`；不记录密码或 token

## 2. 验证结果

| 验证 ID | 范围                             | 命令或检查                                                                                                                                                    | 结果                                             | 证据                                                                                                               |
| ------- | -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |
| VAL-001 | RBAC 菜单树纯函数                | `pnpm --dir mango-ui --filter @mango/rbac test`                                                                                                               | PASS，3 个测试文件、8 项测试全部通过             | Vitest 退出码 0                                                                                                    |
| VAL-001 | RBAC 包生产构建                  | `pnpm --dir mango-ui --filter @mango/rbac build`                                                                                                              | PASS，726 modules transformed                    | Vite 与类型生成退出码 0                                                                                            |
| VAL-002 | Notice 企微同步集成              | `mvn -f mango/pom.xml -pl mango-platform/mango-notice/mango-notice-core -am -Dtest=NoticeServiceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS，22 tests，0 failures，0 errors             | Surefire 与 52 模块依赖 Reactor `BUILD SUCCESS`                                                                    |
| VAL-002 | Notice 模块静态门禁              | `mvn -f mango/pom.xml -pl mango-platform/mango-notice/mango-notice-core -am -DskipTests verify`                                                               | PASS，52 模块依赖 Reactor `BUILD SUCCESS`        | compile、Checkstyle、PMD、SpotBugs 退出码 0                                                                        |
| VAL-003 | Identity/Authorization migration | 任务库执行 Flyway V6/V2、错/正确/重复样本校验及幂等回查                                                                                                       | PASS，错误主体 0、重复组 0、重复执行前后计数一致 | `.runtime/issue-918/migration-verification.txt`                                                                    |
| VAL-004 | E2E selector 规范                | `pnpm --dir mango-ui e2e-selectors:check`                                                                                                                     | PASS                                             | 业务 spec 无禁止的实现级 selector                                                                                  |
| VAL-004 | 前端架构                         | `pnpm --dir mango-ui architecture:check`                                                                                                                      | PASS，errorCount=0                               | checker SHA-256 `78440c05c5b6b69e733f634b5ec988d114b0b394af2e5ee72d8fb97e5954bbe5`                                 |
| VAL-004 | 前端边界                         | `pnpm --dir mango-ui frontend-boundaries:check`                                                                                                               | PASS，无本次新增违规                             | checker SHA-256 `ffef6fb34b6c798626ec086ff372bdc6ae03b30c292c32ad5e0cfecd507a30b5`                                 |
| VAL-004 | Changeset 发布影响闭包           | `node mango-ui/scripts/release/check-release-changes.mjs --base 870f1bbdb7a064c455c8654c94c5ccf3e570e5a8 --head HEAD --include-working-tree`                 | PASS；直接变更包为 `@mango/admin-shell`、`@mango/auth`、`@mango/rbac`、`@mango/system` | Release change checker 退出码 0 |
| VAL-004 | 文档与证据门禁                   | README 结构/源码事实/业务指南审计，生命周期 handoff、实施计划与 acceptance evidence checker                                                                   | PASS                                             | 所有命令退出码 0                                                                                                   |
| VAL-005 | 角色授权浏览器回归               | Playwright Chromium 执行 `e2e/specs/role-permission.spec.ts`，`--workers=1 --trace=on`                                                                        | PASS，2/2                                        | [role-menu-hydration.png](./role-menu-hydration.png)；原始 trace 位于 `.runtime/playwright/mango-admin/artifacts/` |
| VAL-006 | Demo 资源声明                    | System、Org、Identity、Authorization 四个 Resource 契约测试；Auth 包生产构建                                                                                  | PASS；仅 tenant 1，Org demo 为 A/B 公司及各 2 个部门；7 项契约测试和 Auth build 通过 | Maven/Vite `BUILD SUCCESS`                                                                                         |
| VAL-006 | 租户与组织前端                   | `@mango/system`、`@mango/rbac` build/test；`@mango/admin-shell` test                                                                                           | PASS；两个业务包 11 项测试、Admin Shell 73 项测试通过 | 包级测试与构建退出码 0                                                                                             |
| VAL-006 | Mango changed-only 门禁          | 全部 47 个变更文件，`changedOnly=true`、`gate=no-new-violations`、空 `baseRef`                                                                                | PASS；`newIssues=0`                              | `target/issue-918-mango-check-report.json`                                                                          |
| VAL-007 | 专属数据库冷启动                 | 精确删除并重建 `mango_dev_mango_issue_918_rbac_wecom_party_005`；仓库内 Mango CLI `1.2.7` 执行 cold bootstrap generation 1 和 runtime                          | PASS；后端 Actuator `UP`，datasource 与 workspace DB `PASS` | `.mango/run/logs/mango-backend.log`                                                                                 |
| VAL-007 | 冷库 SQL 回查                    | 只读查询 `sys_tenant`、`sys_org`、`tenant_member`、`org_post`、`authorization_role`、`calendar`、`calendar_day`                                                | PASS；1 租户、7 组织、1 admin 成员、6 岗位、3 基础角色、1 日历、730 日历日，全部 tenant 1 | 本文件与任务会话原始命令输出                                                                                       |
| VAL-007 | 单租户关键浏览器回归             | Chromium 单 worker 执行 tenant、platform metadata、menu、org/post、role、calendar 定向用例，`--trace=on`                                                     | PASS，10/10；唯一租户自动登录、租户管理隐藏、组织/岗位 CRUD、角色授权、日历与完整菜单均通过 | `.runtime/playwright/mango-admin/artifacts/`                                                                        |
| VAL-007 | 系统管理页面回归                 | Chromium 单 worker 执行 `system-management-pages.spec.ts`，真实 18005/30005 服务，`--trace=on`                                                               | PASS，13/13；成员、角色、组织、岗位、应用、菜单、套餐、字典、参数、行政区划与日志页面通过 | `.runtime/playwright/mango-admin/artifacts/`                                                                        |
| VAL-008 | 网站配置后端合同                 | 定向运行 `AdminBrandingServiceTest`、`SystemApiContractTest`、`SystemMenuResourceContractTest`                                                               | PASS，3 个测试类、10 项断言全部通过；`@PublicAccess` 合并后 access mode 为 `PUBLIC` 且 permission 为空 | Surefire 退出码 0 |
| VAL-008 | System 前端生产构建              | `pnpm --dir mango-ui --filter @mango/system build`                                                                                                            | PASS                                             | Vite 与类型生成退出码 0 |
| VAL-008 | 测试质量门禁                     | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`                                                                                              | PASS，26 个受影响测试文件无恒真、同值或 mock 被测对象问题 | checker 退出码 0 |
| VAL-008 | Resource 与数据库回读            | 回读 `resource_module_receipt`、`authorization_api_resource`、`sys_config`、`authorization_menu`                                                             | PASS；System receipt generation=3/FINALIZED；公开接口为 `PUBLIC`；13 个配置分组及菜单名均为“网站配置” | 任务专属数据库只读 SQL |
| VAL-008 | 匿名公开接口                     | 不携带 token 请求 `GET http://127.0.0.1:18005/system/admin-branding/public`                                                                                   | PASS，HTTP 200，返回网站展示配置                  | 2026-09-01 运行态回读 |
| VAL-008 | 网站配置 Chromium 验收           | 单 worker 执行 `menu-navigation.spec.ts` 的“网站配置菜单打开对应配置页面”用例                                                                                  | PASS；菜单 API、左侧入口、路由、ready 状态、标题和旧名称消失均符合预期；console/page/4xx/5xx 为空 | `.runtime/issue-918/website-config-page.png` |

## 3. 行为变化

- 角色菜单弹窗初始化只勾选已授权叶子，必要祖先呈半选，未授权兄弟不再被级联选中。
- 保存提交 checked 与 half-checked 的去重集合，保留必要祖先且不扩大权限。
- 企微用户同步把 `INTERNAL_ORG.partyId` 归一为当前租户 ID，部门 ID 仍只用于组织成员关系。
- Identity V6 与 Authorization V2 修复历史错误主体；Authorization 在归一化前按完整唯一键保留最小 ID。
- 默认 demo 只提供 1 个租户“芒果集团”，租户下保留 A/B 两个公司，每个公司 2 个部门；公司和部门不是独立租户。
- 登录选项唯一时自动使用默认租户且不渲染选择器；`system:tenant` 菜单停用，不进入默认导航。
- 内部 `tenant_id`、租户拦截器、租户 API 和存量兼容能力保持不变。
- “后台品牌配置”对外统一改名为“网站配置”，菜单码、路由、Java 类型、权限码和 `admin.branding.*` 配置键保持兼容。
- `GET /system/admin-branding/public` 使用标准 `@PublicAccess` 组合注解注册 `PUBLIC` 合同，仍允许匿名读取且不绑定 permission。

## 4. 限制

- Notice 集成测试使用可控企微目录与 Mango gateway 测试替身，未调用真实企微网络；本次不改变企微外部 API 契约。
- 浏览器菜单树用例只拦截待验证的菜单树读取和保存接口，以精确验证前端回显与 payload；同文件另一用例使用真实后端验证默认租户角色 CRUD，并确认停用的租户管理菜单不能授权。
- 本次仅重建已授权的任务专属本地演示库；正式环境、其它工作区数据库和既有存量租户未触碰。存量库不会因删除 `INIT_ONLY` demo 声明而自动清理历史租户。
- 未宣称全量 E2E 通过：9 个既有多租户隔离 spec 仍硬编码 `tenantId=2/company_a`。默认 demo 不再提供该租户，后续应让这些能力测试自行创建隔离租户，不能通过恢复第二默认租户或改用 tenant 1 来伪造隔离证据。
- 完整导航长用例并发执行时曾在“通知管理图标”断言处出现一次 flaky；该项与网站配置断言无关，改为单 worker 后重跑通过，因此不把并发首次结果表述为稳定全量通过。
