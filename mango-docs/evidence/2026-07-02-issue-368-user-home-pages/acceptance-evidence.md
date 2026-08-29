# Issue #368 用户多首页工作台验收证据

## 1. 验收范围

- 页面：管理端 `/#/home`、`/#/home/:homeId`。
- 接口：`/home/pages`、`/home/pages/resolve`、`/home/pages/name`、`/home/pages/duplicate`、`/home/pages/sort`、`/home/pages/default`、`/home/pages/layout`。
- 权限：登录态访问，租户与用户来自 `MangoContextHolder`，指定 `homeId` 只能访问当前用户自己的首页。
- 数据：`sys_user_home_page`、`sys_user_home_preference`，测试数据前缀 `E2E首页`。
- 部署形态：monolith 后端 `mango-backend` + `mango-admin` 前端。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30013`
- 后端地址：`http://127.0.0.1:18013`
- 数据库或租户：`mango_dev_mango_issue_368_013` / 租户 `default`
- 测试账号：`admin / admin123`
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| ISSUE-368 | TC-001 | `/#/home`、`GET /home/pages/resolve` | 默认首页解析与首屏展示 | 登录用户 `admin`，初始无可见 `E2E首页` 数据 | `resolve` 返回内置系统工作台或当前默认首页；页面存在 `data-page="home.workbench"` 和布局编辑入口 | 首页页签、默认 Home 图标、内置标签、工作台小组件区域可见 | Playwright 等待 `resolve` 和首页渲染完成；未触发测试框架记录的请求失败断言 | `历史验收图片已清理（可从 Git 历史恢复）` | PASS |
| ISSUE-368 | TC-002 | `POST /home/pages`、`PUT /home/pages/default`、`GET /home/pages/resolve` | 创建个人首页并设置默认首页 | `E2E首页<timestamp>-销售` | 创建后当前激活页签显示新首页；`resolve` 返回该首页名称；默认首页 Home 图标可见 | 新建弹窗、名称输入、保存按钮、默认首页状态切换可操作 | Playwright 显式等待创建请求和默认解析请求完成，断言返回数据中的 `name` 与测试数据一致 | `历史验收图片已清理（可从 Git 历史恢复）` | PASS |
| ISSUE-368 | TC-003 | `POST /home/pages`、`PUT /home/pages/default`、`/#/home` | 第二个首页设置为默认并由 `/home` 解析 | `E2E首页<timestamp>-项目` | 重新访问 `/#/home` 后激活页签显示项目首页；`resolve` 返回项目首页名称 | 首页切换后未落入 404；默认 Home 图标跟随当前首页变化 | Playwright 显式等待默认设置请求完成，并通过后端解析接口校验默认偏好 | `历史验收图片已清理（可从 Git 历史恢复）` | PASS |
| ISSUE-368 | TC-004 | `PUT /home/pages/name`、`POST /home/pages/duplicate`、`PUT /home/pages/sort` | 重命名、复制、排序 | `E2E首页<timestamp>-项目看板` 与副本 | 重命名后当前激活页签显示新名称；复制后生成 `副本`；排序请求完成后副本索引小于原页面索引 | 重命名弹窗、复制按钮、前移按钮可操作；排序后页签仍保持当前页 | Playwright 显式等待重命名、复制、排序请求完成，并通过 `listMyPages` 数据顺序断言排序结果 | `历史验收图片已清理（可从 Git 历史恢复）` | PASS |
| ISSUE-368 | TC-005 | `/#/home/:homeId`、`PUT /home/pages/layout` | 指定首页路由和布局保存 | `projectPage.id` | 访问 `/#/home/{id}` 后 URL 保持指定首页路径；激活页签显示对应首页；布局保存请求完成后返回查看态 | `/home/:homeId` 未落入 404；布局编辑器组件库可见；保存后编辑按钮恢复 | Playwright 显式等待布局保存请求完成；路由断言匹配 `#/home/{id}` | `历史验收图片已清理（可从 Git 历史恢复）` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| Admin Shell | `/#/home` | 内置系统工作台仍可展示默认小组件 | 首页工具栏新增多首页操作且图标按钮不遮挡内容 | 顶部首页条、右侧布局工具、工作台小组件在 1280px 宽度下布局稳定 | `历史验收图片已清理（可从 Git 历史恢复）` | PASS |
| Admin App Router | `/#/home/:homeId` | 动态首页路由注册到 Layout 下 | 后端菜单模式下隐藏路由优先于 404 兜底 | 直接访问指定首页后标签栏仍显示首页上下文 | `历史验收图片已清理（可从 Git 历史恢复）` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 角色/租户级默认首页模板 | Issue #368 V1 明确聚焦用户个人首页 | 不影响个人多首页和默认首页配置 | 后续单独设计 role/tenant default 与模板市场 | 已按方案 C 的 V1 边界处理 |
| 复杂小组件配置面板 | 本次复用现有 grid widget 注册与布局 JSON，不扩展业务组件配置模型 | 不影响布局持久化和业务小组件过滤 | 后续由各 widget 自身提供配置能力 | 已按方案 C 的 V1 边界处理 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 后端 `mango-home` starter、API、表结构、装配和边界说明 | `mango/mango-platform/mango-home/README.md` | 引入 `io.mango.platform.home:mango-home-starter` | 租户、组织、用户上下文由后端登录态写入 | 指定无权 `homeId` 返回业务错误 | PASS |
| 前端开发者 | `@mango/home` API 包和 `@mango/admin-shell` 首页宿主接入说明 | `mango-ui/packages/home/README.md`、`mango-ui/packages/admin-shell/README.md` | `homePageApi`、`/#/home`、`/#/home/:homeId` | 前端不传租户和用户 ID，只传首页 ID 与布局 JSON | 历史布局中失效 widget 按 grid-layout 既有规则处理 | PASS |
| QA | E2E 脚本和截图证据 | `mango-ui/apps/mango-admin/e2e/specs/home-pages.spec.ts`、`mango-docs/evidence/2026-07-02-issue-368-user-home-pages/e2e/` | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/home-pages.spec.ts --project=chromium` | 使用 `admin/admin123`，测试数据名前缀 `E2E首页` | E2E 后已硬清理本地 `E2E首页` 测试残留，剩余 0 条 | PASS |
