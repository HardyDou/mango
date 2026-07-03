# 网址导航交付契约

## 1. 目标

交付 `mango-link` 网址导航能力，包含后台网址管理、Open API、独立 `link-page` 页面，以及系统内置 `企业导航` 默认分组和默认网址。

## 2. 范围

- 后端模块：`mango/mango-platform/mango-link`。
- 前端管理包：`mango-ui/packages/link`。
- 前端 Open API 包：`mango-ui/packages/link-openapi`。
- 前端独立页面包：`mango-ui/packages/link-page`、`mango-ui/packages/link-panel`。
- 管理后台与 CLI 集成清单、菜单资源、系统配置资源。
- 默认数据：`企业导航` 分组和 Mango 管理后台、百度、GitHub、Maven Central 默认网址。

## 3. 不做什么

- 不实现小组网址。
- 不实现多级分类 tree。
- 不实现网址健康检测。
- 不允许前端 mock 或本地静态数据替代真实接口。

## 4. 设计输入

- 需求文档：`mango-docs/designs/2026-06-30-url-navigation-requirements.md`
- 设计文档：`mango-docs/designs/2026-06-30-url-navigation-design.md`
- 执行计划：`mango-docs/plans/2026-06-30-url-navigation-plan.md`

## 5. 设计说明

### 5.1 影响模块

- `mango/mango-platform/mango-link`
- `mango-ui/packages/link`
- `mango-ui/packages/link-openapi`
- `mango-ui/packages/link-page`
- `mango-ui/packages/link-panel`
- `mango-ui/packages/admin`
- `mango-ui/packages/mango-cli`
- `mango/mango-app/monolith/mango-monolith-app`

### 5.2 接口变化

- 新增 `/link/open/public-links/list`
- 新增 `/link/open/jump`
- 新增 `/link/company-links/list`
- 新增 `/link/favorites/**`
- 新增 `/link/personal-categories/**`
- 新增 `/link/personal-links/**`
- 新增 `/link/categories/**`
- 新增 `/link/items/**`

### 5.3 数据变化

- 新增 `link_category`、`link_item`、`link_visibility_target`、`link_favorite`、`link_access_record`。
- 通过 `V4__seed_default_navigation.sql` 固化 `企业导航` 默认分组和默认网址。
- 通过系统配置资源初始化 `mango.link.open.jump.enabled=false`。

### 5.4 菜单/页面/权限变化

- 用户侧菜单：`网址导航 / 公司网址`、`网址导航 / 我的收藏`、`网址导航 / 我的网址`。
- 后台侧菜单：`平台能力 / 网址管理 / 公司网址`、`平台能力 / 网址管理 / 网址分类`。
- 页面注册通过 `@mango/link/admin-pages` 接入。

### 5.5 测试范围

- 后端 Maven 测试。
- 前端包构建。
- Open API 匿名返回公开网址。
- link-page 登录、分组、添加网址弹窗、跳转地址与访问统计。
- 管理端 E2E 脚本登记。

### 5.6 交付物料同步判断

| 物料 | 是否需要更新 | 路径或 EXCEPTION 依据 |
|---|---|---|
| 代码 | 是 | `mango/mango-platform/mango-link`、`mango-ui/packages/link*` |
| README/使用说明 | 是 | `mango/mango-platform/mango-link/README.md`、`mango-ui/packages/link*/README.md` |
| 需求文档 | 是 | `mango-docs/designs/2026-06-30-url-navigation-requirements.md` |
| 详细设计文档 | 是 | `mango-docs/designs/2026-06-30-url-navigation-design.md` |
| E2E 脚本 | 是 | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` |
| 测试结果基线 | 是 | 本文件第 9 章 |

### 5.7 测试用例登记与自动化判断

| 用例 ID | 来源 AC | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 稳定契约 | 执行入口 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | AC-001 | 匿名用户只能看到公开网址 | P0 | API | AUTO | `企业导航` 默认公开网址 | `/link/open/public-links/list` | `curl` | 本文件第 8 章 | AUTOMATED |
| TC-002 | AC-002 | 登录用户看到公司内网址 | P0 | E2E | AUTO | `Mango 管理后台` 公司内网址 | 登录 admin/tenant 1 | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | 本文件第 8 章 | AUTOMATED |
| TC-003 | AC-005 | 收藏和取消收藏 | P0 | E2E | AUTO | 默认公开网址 | `data-action` 和真实接口 | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | `mango-docs/evidence/2026-07-01-link-panel-preview` | AUTOMATED |
| TC-004 | AC-009 | 登录用户新增个人分组和网址 | P0 | E2E | AUTO | admin/tenant 1 | 添加网址弹窗字段 | 浏览器自动化检查 | 本文件第 8 章 | AUTOMATED |
| TC-005 | AC-021 | 系统跳转写访问记录 | P0 | API | AUTO | `https://www.baidu.com` | `/link/open/jump`、`link_access_record` | `curl` + SQL | 本文件第 8 章 | AUTOMATED |

## 6. 风险与限制

- 当前本地验收使用数据库 `mango_dev_mango_link_002`，账号标识为 admin/tenant 1，不记录密码或 token。
- `mango.link.open.jump.enabled` 默认值为 `false`；当前工作区为验证跳转已设为 `true`。
- 访问地址 `http://127.0.0.1:30004/` 为 `@mango/link-page` 本地预览服务。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求 | 新增 mango-link 模块 | 后端按 api/core/starter/remote 拆分 | `mango/mango-platform/mango-link` | `mango/mango-platform/mango-link/README.md` | `mango-docs/designs/2026-06-30-url-navigation-requirements.md`、`mango-docs/designs/2026-06-30-url-navigation-design.md` | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` | Maven 测试 | DONE | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` |
| TASK-002 | 用户要求 | 固化企业导航默认分组和网址 | Flyway migration 初始化真实数据 | `mango/mango-platform/mango-link/mango-link-core/src/main/resources/db/migration/link/V4__seed_default_navigation.sql` | `mango/mango-platform/mango-link/README.md` | `mango-docs/designs/2026-06-30-url-navigation-design.md` | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` | SQL/API 验证 | DONE | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` |
| TASK-003 | 用户要求 | Open API 支持匿名和登录态 | 登录态返回可见范围内所有数据，匿名返回公开数据 | `mango/mango-platform/mango-link/mango-link-starter/src/main/java/io/mango/link/starter/controller/LinkOpenController.java`、`mango/mango-platform/mango-link/mango-link-core/src/main/java/io/mango/link/core/service/impl/LinkOpenService.java`、`mango-ui/packages/link-openapi` | `mango-ui/packages/link-openapi/README.md` | `mango-docs/designs/2026-06-30-url-navigation-design.md` | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` | API 验证 | DONE | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` |
| TASK-004 | 用户要求 | link-page 独立页面真实调用接口 | 通过 `@mango/link-openapi` 请求真实接口，不使用替身数据 | `mango-ui/packages/link-page` | `mango-ui/packages/link-page/README.md` | `mango-docs/designs/2026-06-30-url-navigation-design.md` | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` | 构建和页面检查 | DONE | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` |
| TASK-005 | 用户要求 | 添加网址弹窗去掉说明/标签并默认当前分组 | 表单只提交 name/url/categoryId，空个人分组也展示 | `mango-ui/packages/link-page/src/components/LinkPage.vue` | `mango-ui/packages/link-page/README.md` | `mango-docs/designs/2026-06-30-url-navigation-requirements.md` | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` | 页面检查 | DONE | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` |
| TASK-006 | 用户要求 | 系统跳转可配置并写访问统计 | 配置 key 为 `mango.link.open.jump.enabled`，默认 false | `mango/mango-platform/mango-link/mango-link-core/src/main/java/io/mango/link/core/service/impl/LinkOpenService.java`、`mango/mango-platform/mango-link/mango-link-starter/src/main/resources/META-INF/mango/resources/link-common-config.yml` | `mango/mango-platform/mango-link/README.md` | `mango-docs/designs/2026-06-30-url-navigation-requirements.md` | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` | `curl` + SQL | DONE | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` |

## 8. 验收证据记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-002 | TC-001 | `/link/open/public-links/list` | 默认企业导航数据 | 企业导航、百度/GitHub/Maven Central | 返回公开默认网址和 `redirectUrl` | 不适用 | 200 | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` | PASS |
| TASK-004 | TC-004 | `http://127.0.0.1:30004/` | link-page 页面 | admin/tenant 1 | 页面展示真实链接 | 截图检查，布局可用 | 首页复查无稳定 404 | 浏览器截图 | PASS |
| TASK-005 | TC-004 | 添加网址弹窗 | 分组默认值和字段 | `Codex分组...` 临时分组，验收后清理 | 无说明/标签；默认当前分组 | 弹窗只显示分组/名称/网址 | 无稳定 console error | 浏览器截图 | PASS |
| TASK-006 | TC-005 | `/link/open/jump` | 跳转统计 | `uid=codex-final-key` | 302 跳转，访问记录 +1 | 不适用 | 302 | SQL 计数 | PASS |

## 9. 测试结果基线

| 基线 ID | 覆盖台账 ID | 覆盖用例 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BASELINE-001 | TASK-001、TASK-002、TASK-003、TASK-006 | TC-001、TC-002、TC-005 | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | `mvn -q -f mango/mango-platform/mango-link/pom.xml test -DskipTests=false` | 本地 monolith，后端 18002 | `mango_dev_mango_link_002` | tenant 1 | PASS | 无 | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` | 新增 mango-link 后端能力 |
| BASELINE-002 | TASK-004、TASK-005 | TC-003、TC-004 | 浏览器自动化检查 | `pnpm --dir mango-ui --filter @mango/link-page build` | Vite 30004 | `mango_dev_mango_link_002` | admin/tenant 1 | PASS | 无 | `mango-docs/evidence/2026-07-01-link-panel-preview` | link-page 使用真实接口和当前分组默认值 |
| BASELINE-003 | TASK-003、TASK-004 | TC-001、TC-004 | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | `pnpm --dir mango-ui --filter @mango/link-openapi build && pnpm --dir mango-ui --filter @mango/link-panel build && pnpm --dir mango-ui --filter @mango/link build` | 本地 pnpm workspace | 不适用 | 不适用 | PASS | 无 | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` | 前端包构建通过 |
| BASELINE-004 | TASK-002、TASK-003、TASK-004、TASK-006 | TC-001、TC-002、TC-003、TC-005 | `mango-ui/apps/mango-admin/e2e/specs/link-navigation.spec.ts` | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:30002 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18002 pnpm exec playwright test e2e/specs/link-navigation.spec.ts --project=chromium` | admin 30002，backend 18002 | `mango_dev_mango_link_002` | admin/tenant 1 | PASS，3 passed | 无 | `mango-docs/evidence/2026-07-01-link-navigation-delivery-contract.md` | E2E 断言使用 `/link/open/jump?url=...` |

## 10. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 后端接入、Open API、link-page 使用方式和配置项 | `mango/mango-platform/mango-link/README.md`、`mango-ui/packages/link-openapi/README.md`、`mango-ui/packages/link-page/README.md` | 引入 starter 或 npm 包 | 真实租户和登录态由宿主提供 | 接口失败看 README 排障表 | DONE |
