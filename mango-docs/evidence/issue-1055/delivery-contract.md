# Issue #1055 信息采集中心 交付契约

## 1. 目标

在 Mango 中新增独立的信息采集中心，与 CMS 解耦、不建立站点概念：授权人员管理表单与采集项，访客通过托管入口或业务自建入口提交，运营人员处理当前表单的提交记录，形成真实数据库、真实租户隔离和真实公开入口的完整闭环。

## 2. 范围

- 后端新增 `mango/mango-platform/mango-information`（api/core/starter）、Flyway migration、Resource Registry 菜单与公开 API 声明。
- 前端新增 `mango-ui/packages/information`（表单管理、提交记录、托管表单、API factory、样式），接入 Admin full 与样式聚合。
- 宿主应用 `mango-ui/apps/mango-admin` 登记信息采集公开路由与免登录白名单。
- 测试资产：后端 migration 契约测试、前端 API 测试、admin E2E 验收脚本与截图。

## 3. 不做什么

- 不实现文件上传（P1 设计预留）。
- 不做工作流、营销自动化、复杂条件编排、支付与签名。
- 不修改 CMS 表、接口与页面；不把采集数据写入 CMS 文章、栏目或内容扩展字段。
- 不提供独立提交记录全局菜单（提交记录必须从当前表单进入）。

## 4. 设计输入

- 需求：`mango-docs/designs/issue-1055-information-collection/business-requirements.md`
- 系统需求：`mango-docs/designs/issue-1055-information-collection/system-requirements.md`
- 技术设计：`mango-docs/designs/issue-1055-information-collection/technical-design.md`
- 实施计划：`mango-docs/designs/issue-1055-information-collection/implementation-plan.md`

## 5. 设计说明

### 5.1 影响模块

| 模块 | 改动 | 说明 |
|---|---|---|
| `mango-information-api` | 新增 | Command、Query、VO、枚举、错误码与 Api 契约 |
| `mango-information-core` | 新增 | Entity、Mapper、Service、迁移脚本、字段校验与 SK 校验 |
| `mango-information-starter` | 新增 | 后台/公开/服务端 Controller、自动配置、资源声明 |
| `@mango/information` | 新增 | 页面、API factory、样式、页面注册 |
| `@mango/admin` | 修改 | full 注册、样式聚合与依赖声明（生成物） |
| `apps/mango-admin` | 修改 | 公开路由与免登录白名单 |
| `@mango/admin-extension` | 修改 | source 模式构建期常量回退（范围外使能修复） |

### 5.2 接口变化

| 接口 | 方法 | 访问模式 |
|---|---|---|
| `/information/forms/page` | GET | LOGIN |
| `/information/forms/detail` | GET | LOGIN |
| `/information/forms/save` | POST | LOGIN |
| `/information/forms` | DELETE | LOGIN |
| `/information/submissions/page` | GET | LOGIN |
| `/information/submissions/detail` | GET | LOGIN |
| `/information/submissions/export` | GET | LOGIN |
| `/information/submissions/process` | PUT | LOGIN |
| `/information/open/forms/detail` | GET | PUBLIC |
| `/information/open/forms/submit` | POST | PUBLIC |

### 5.3 数据变化

| 表 | 说明 |
|---|---|
| `information_form` | 表单主数据、字段定义 JSON、状态、服务端 SK 与摘要；`tenant_id + form_code` 唯一 |
| `information_submission` | 提交记录、值 JSON、来源页面、幂等键、处理状态；`tenant_id + form_id + idempotency_key` 唯一 |
| `information_submission_snapshot` | 提交时字段定义快照，保证历史记录按原编码与名称可读 |

### 5.4 菜单、页面与权限变化

- 菜单：`信息采集中心` → `表单管理`（`information:forms`）唯一可见菜单；表单详情、配置、接入方式和提交记录是表单上下文内的隐藏路由，不作为全局菜单。
- 页面：表单列表（PG-001）、表单详情（PG-002）、表单可视化配置（PG-003）、提交记录与处理抽屉（PG-004）、接入方式与托管表单（PG-005）。
- 权限：后台接口按 `ApiAccess(LOGIN)` 声明并复用 `information:*` 权限码；公开入口按 `PUBLIC` 声明；租户由服务端确定，客户端不得覆盖。

### 5.5 测试范围

后端 migration 契约、表单保存与唯一性、删除保护、公开提交校验与幂等、SK 校验、提交查询与状态流转、导出脱敏、租户归属；前端 API factory、页面构建、后台页面与匿名托管表单端到端验收；后端与前端门禁。

### 5.6 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | SAC-001 | 提供独立信息采集后端模块与数据模型 | 新增 `mango-information` api/core/starter 与三张业务表 | `mango/mango-platform/mango-information/mango-information-api`、`mango-information-core` | `mango/mango-platform/mango-information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 模块构建与 migration 契约测试 | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-002 | SAC-002 | 表单管理：列表、详情、可视化配置与启停 | 字段定义 JSON + 提交值 JSON + 快照 JSON | `InformationFormService`、`FormsView.vue`、`FormDetailView.vue`、`FormConfigView.vue`、`FormHostedView.vue` | `mango-ui/packages/information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 浏览器端到端验收 | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-003 | SAC-003 | 提交记录：动态列、详情抽屉与处理 | 固定列 + 当前表单字段动态列 + 操作列；处理在抽屉内保存；已关闭不可再处理 | `FormSubmissionsView.vue`、`InformationSubmissionService` | `mango-ui/packages/information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 浏览器端到端与接口验收 | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-004 | SAC-004 | 托管表单匿名访问与提交 | 宿主登记公开路由并加入免登录白名单；浏览器不携带 SK | `HostedFormView.vue`、`apps/mango-admin/src/router/index.ts`、`src/config/menuConfig.ts` | `mango-ui/packages/information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 匿名浏览器提交验收 | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-005 | SAC-005 | 服务端 SK 接入与校验 | 服务端生成 SK，库中仅存摘要，服务层比对 | `InformationPublicController`、`InformationSubmissionService` | `mango/mango-platform/mango-information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 接口验收（正确/错误 SK） | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-006 | SAC-006 | 历史提交按字段快照可读 | 提交时写 `information_submission_snapshot` | `InformationSubmissionSnapshotEntity`、`V1__init_mango_information.sql` | `mango/mango-platform/mango-information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 数据库与接口核对 | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-007 | SAC-007 | 多租户隔离，客户端不得覆盖租户 | 服务端按表单解析租户并设置上下文 | `InformationFormService`、`InformationSubmissionService` | `mango/mango-platform/mango-information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 数据库租户列核对 | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-008 | SAC-008 | 菜单与公开 API 资源随启动同步 | Resource Registry `AUTH_MENU` 与 `API_RESOURCE` 声明 | `information-common-menu.json`、`information-common-public-api.yml` | `mango/mango-platform/mango-information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 启动后菜单与匿名接口核对 | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-009 | SAC-010 | 导出当前筛选结果并默认脱敏 | 服务层组装 CSV，手机号/邮箱在列表、详情和导出统一脱敏 | `InformationSubmissionService.exportCsv`、`FormSubmissionsView.vue`、`domain/fields.ts` | `mango-ui/packages/information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 接口验收（脱敏结果） | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-010 | SAC-011 | 表单与提交记录失败语义 | 重复编码、已关闭、有记录不可删除均返回业务错误码 | `InformationCode`、`InformationFormService`、`InformationSubmissionService` | `mango/mango-platform/mango-information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 接口验收（失败分支） | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-011 | SAC-012 | 前端包边界、原型页面结构与架构门禁 | FE2 依赖 FE1 `@mango/admin-extension`；五个原型页面按原型结构实现；不新增 FE2→FE3 例外 | `packages/information/package.json`、`vite.config.ts`、`src/views`、`src/components`、`src/domain` | `mango-ui/packages/information/README.md` | `mango-docs/designs/issue-1055-information-collection/technical-design.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 架构与边界检查 | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |
| TASK-012 | SAC-001 | 后端字段校验、租户、SK 与状态的自动化集成测试 | H2 MySQL 兼容模式集成测试覆盖服务层真实持久化链路 | `mango-platform/mango-information/mango-information-core/src/test/java/io/mango/information/core/service/impl/InformationServiceIntegrationTest.java` | `mango/mango-platform/mango-information/README.md` | `mango-docs/designs/issue-1055-information-collection/implementation-plan.md` | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | `mvn -pl mango-platform/mango-information/mango-information-core test` | DONE | `mango-docs/evidence/issue-1055/acceptance-evidence.md` |

### 5.7 测试用例登记与自动化判断

| 用例 ID | 来源 AC | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 稳定契约 | 执行入口 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | AC-001 | 表单列表、详情、配置与返回路径 | P0 | E2E | AUTO | 已发布表单 `customer-contact` | `data-page="information.forms"` | `playwright test e2e/specs/information-collection.spec.ts` | `acceptance-evidence.md` | AUTOMATED |
| TC-002 | AC-002 | 匿名托管表单渲染与提交 | P0 | E2E | AUTO | 已发布表单 `customer-contact` | 表单标签与成功结果 | 同上 | `acceptance-evidence.md` | AUTOMATED |
| TC-003 | AC-003 | 提交记录列表、动态列与详情 | P0 | E2E | AUTO | 真实提交记录 | `data-page="information.submissions"` | 同上 | `acceptance-evidence.md` | AUTOMATED |
| TC-004 | AC-004 | 服务端 SK 校验与失败语义 | P0 | API | MANUAL | 错误与正确 SK | 错误码 `105507` | `POST /information/open/forms/submit` | `acceptance-evidence.md` | MANUAL |
| TC-005 | AC-005 | 必填、未知字段与幂等提交 | P0 | API | MANUAL | 缺必填、未知字段、重复幂等键 | 错误码 `105505` 与同一提交编号 | 同上 | `acceptance-evidence.md` | MANUAL |
| TC-006 | AC-006 | 处理状态流转与已关闭限制 | P0 | API | MANUAL | COMPLETED/CLOSED/PROCESSING | 错误码 `105506` | `PUT /information/submissions/process` | `acceptance-evidence.md` | MANUAL |
| TC-007 | AC-007 | 导出当前筛选结果与默认脱敏 | P1 | API | MANUAL | 手机号字段 | CSV 脱敏 `138****00` | `GET /information/submissions/export` | `acceptance-evidence.md` | MANUAL |
| TC-008 | AC-008 | 字段定义变更后历史记录快照 | P1 | API | MANUAL | 修改定义后读旧记录 | 快照表与提交值对齐 | 数据库核对 | `acceptance-evidence.md` | MANUAL |
| TC-009 | AC-009 | 多租户隔离与租户归属 | P0 | API | MANUAL | 匿名提交 | `tenant_id` 由服务端确定 | 数据库核对 | `acceptance-evidence.md` | MANUAL |
| TC-010 | AC-010 | 后端自动化集成测试全覆盖 | P0 | API | AUTO | H2 MySQL 兼容内存库、两租户 | 服务层真实 Mapper 与租户拦截器 | `mvn -pl mango-platform/mango-information/mango-information-core test` | `acceptance-evidence.md` | AUTOMATED |

### 5.8 测试结果基线

| 基线 ID | 覆盖台账 ID | 覆盖用例 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BASE-001 | TASK-001 | TC-001 | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `mvn -pl mango-platform/mango-information/mango-information-core test` | JDK 21 / Maven 3.9 | `mango_dev_mango_information_collection_084` | `tenantId=1` | PASS：migration 契约测试通过 | 无 | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 无 |
| BASE-002 | TASK-002 | TC-001, TC-003 | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true npx playwright test e2e/specs/information-collection.spec.ts --project=chromium` | Chromium / vite 7.3.6 | 真实后端 `18084` + `mango-admin` `30084` | `tenantId=1` / `admin` | PASS：2 个用例通过 | 无 | `mango-docs/evidence/issue-1055/screenshots/01-form-list.png` | 无 |
| BASE-003 | TASK-003 | TC-003, TC-006 | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | 同上 | Chromium | 真实后端 | `tenantId=1` / `admin` | PASS：提交记录与处理状态可用 | 无 | `mango-docs/evidence/issue-1055/screenshots/04-submission-list.png` | 无 |
| BASE-004 | TASK-004 | TC-002 | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | 同上（匿名会话） | Chromium | 真实后端 | 匿名访客 | PASS：匿名打开并提交成功 | 无 | `mango-docs/evidence/issue-1055/screenshots/07-hosted-submit-success.png` | 无 |
| BASE-005 | TASK-005 | TC-004, TC-005, TC-009 | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | 后端接口实测（curl） | 真实后端 | 真实库与表单 | 匿名与服务端 | PASS：校验、幂等、SK 与租户归属符合设计 | 无 | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 无 |
| BASE-006 | TASK-006 | TC-007, TC-008 | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | 后端接口实测（curl） | 真实后端 | 真实库与表单 | `tenantId=1` / `admin` | PASS：导出脱敏与快照落库 | 无 | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 无 |
| BASE-007 | TASK-011 | TC-001, TC-003 | `mango-ui/apps/mango-admin/e2e/specs/information-collection.spec.ts` | `pnpm architecture:check && pnpm frontend-boundaries:check && pnpm component-contracts:check && pnpm admin:styles:check` | mango-ui workspace | 不适用 | 不适用 | PASS：架构与门禁通过 | 无 | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 无 |
| BASE-008 | TASK-012 | TC-010 | `mango-platform/mango-information/mango-information-core/src/test/java/io/mango/information/core/service/impl/InformationServiceIntegrationTest.java` | `mvn -pl mango-platform/mango-information/mango-information-core test` | JDK 21 / H2 MySQL 模式 | 内存库 `information`，两租户 | `tenantId=1`、`tenantId=2` | PASS：8 个用例全部通过 | 无 | `mango-docs/evidence/issue-1055/acceptance-evidence.md` | 无 |

### 5.9 验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-002 | TC-001 | `/information/forms` | 表单列表、详情、配置 | 表单 `customer-contact` | 列表展示编码/字段数/提交数与操作列；详情展示托管入口与 SK | 对话框可开合，标签与按钮可见 | 页面无 Vue 运行时错误 | `screenshots/01-form-list.png`、`screenshots/02-form-detail.png`、`screenshots/03-form-config.png` | DONE |
| TASK-003 | TC-003 | `/information/submissions` | 提交记录动态列与详情 | 真实提交记录 | 固定列 + 动态列渲染，详情展示 JSON 提交值 | 列表与抽屉交互正常 | 列表接口 200 | `screenshots/04-submission-list.png`、`screenshots/05-submission-detail.png` | DONE |
| TASK-004 | TC-002 | `/#/information/hosted?formCode=customer-contact` | 匿名托管表单提交 | 姓名/手机号/需求描述 | 匿名打开成功，提交后展示成功态 | 表单校验与按钮可用 | 公开接口匿名返回 200 | `screenshots/06-hosted-form.png`、`screenshots/07-hosted-submit-success.png` | DONE |
| TASK-005 | TC-004 | `POST /information/open/forms/submit` | SK 校验 | 错误 SK / 正确 SK | 错误 SK 返回 `105507`，正确 SK 写入提交 | 非 UI | 匿名接口 200 + 业务错误码 | `acceptance-evidence.md` | DONE |
| TASK-005 | TC-005 | `POST /information/open/forms/submit` | 必填、未知字段与幂等 | 缺必填、未知字段、重复幂等键 | 返回 `105505`；重复幂等键返回同一提交编号 | 非 UI | 匿名接口 200 + 业务错误码 | `acceptance-evidence.md` | DONE |
| TASK-003 | TC-006 | `PUT /information/submissions/process` | 状态流转与已关闭限制 | COMPLETED/CLOSED/PROCESSING | 前两次成功，已关闭再处理返回 `105506` | 非 UI | 管理接口 200 + 业务错误码 | `acceptance-evidence.md` | DONE |
| TASK-009 | TC-007 | `GET /information/submissions/export` | 导出与脱敏 | 手机号字段 | CSV 仅含当前筛选记录，手机号脱敏为 `138****00` | 非 UI | 管理接口 200 | `acceptance-evidence.md` | DONE |
| TASK-007 | TC-009 | `information_submission` 表 | 租户归属 | 匿名提交 | `tenant_id=1` 由服务端写入 | 非 UI | 不适用 | `acceptance-evidence.md` | DONE |
| TASK-008 | TC-001 | `/authorization/menus/user` 与 `GET /information/open/forms/detail` | 菜单与公开资源 | 管理员与匿名 | 菜单含 `information:forms`/`information:submissions`；公开接口匿名 200 | 菜单可见 | 接口 200 | `acceptance-evidence.md` | DONE |
| TASK-012 | TC-010 | `InformationServiceIntegrationTest` | 服务层自动化集成测试 | H2 MySQL 兼容内存库、两租户 | 8 个用例覆盖 migration、校验、快照、幂等、SK、状态、导出与租户隔离 | 服务层测试，无页面交互 | 不走 HTTP，断言持久化与异常语义 | `mvn -pl mango-platform/mango-information/mango-information-core test` | DONE |

### 5.10 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango 业务开发者 | 信息采集模块的表单定义、托管入口、服务端 SK 提交、动态提交列、处理状态与导出能力及验证边界 | `mango/mango-platform/mango-information/README.md`、`mango-ui/packages/information/README.md` | `mvn -pl mango-platform/mango-information/mango-information-starter -am verify`；`pnpm --filter @mango/information build/test` | 服务端接入必须持有当前表单专属 SK；浏览器与托管页面不得携带 SK；测试数据按租户隔离 | SK 失效或校验失败不形成记录；已关闭记录不可再处理；历史记录按快照读取 | DONE |

## 6. 风险与限制

- `EXCEPTION`：`@mango/admin-extension` 的范围外使能修复需在后续发布批次单独说明。
- `EXCEPTION`：托管表单当前注册在 `mango-admin` 宿主应用；独立对外托管站点的承载应用仍待确认。
- 本契约基于 `AGENT_ONLY` 复核，不代表人工审批结论。
