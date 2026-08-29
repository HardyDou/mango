# link-page 业务首页交付契约

## 1. 目标

将 `@mango/link-page` 落地为面向保函业务人员的快捷导航首页：页面从数据库读取内置导航卡片，按“业务相关、工具相关、其他”三组展示，并支持关键词搜索、整卡点击打开。

## 2. 范围

- 改造 `@mango/link-page` 首页展示和交互。
- 复用 `@mango/link-openapi` 的公开导航列表接口。
- 新增 `mango-link` Flyway migration，初始化保函业务导航分类和卡片。
- 更新 `@mango/link-page` README 和开发示例。
- 验证前端构建、页面展示、搜索、链接属性、控制台错误和本地预览。

## 3. 不做什么

- 不新增主表。
- 不新增 Open API。
- 不做登录、退出、收藏、个人导航、前台新增、前台编辑和重复提示。
- 不做卡片级权限、公私区分和后台维护页面。
- 不接入百度联想或外部搜索引擎结果。

## 4. 设计输入

- 详细设计：`mango-docs/designs/2026-07-10-link-page-home-design.md`
- 原型草稿：`D:/Project/mango/.superpowers/brainstorm/link-page-v9-design-draft.html`
- 用户确认：本版直接内置数据库，卡片只展示 logo、名称、地址、一句话介绍，整张卡片点击，新标签页打开。

## 5. 交付物料同步判断

| 物料 | 是否需要更新 | 路径或 EXCEPTION 依据 |
|---|---|---|
| 代码 | 是 | `mango-ui/packages/link-page/**`、`mango/mango-platform/mango-link/**` |
| README/使用说明 | 是 | `mango-ui/packages/link-page/README.md` |
| 需求文档 | EXCEPTION | 本次需求来自当前会话确认，已在详细设计中追踪 BO/BF/BR/PG/AC |
| 详细设计文档 | 是 | `mango-docs/designs/2026-07-10-link-page-home-design.md` |
| E2E 脚本 | EXCEPTION | 本次先完成包级页面走查；宿主页面正式接入后再补正式 E2E |
| 测试结果基线 | 是 | `mango-docs/plans/2026-07-10-link-page-business-home-ledger.md` 第 9 节 |

## 6. 测试用例登记与自动化判断

| 用例 ID | 来源 AC | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 稳定契约 | 执行入口 | 证据 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | AC-001 | 默认展示三组内置卡片 | P0 | API E2E 截图 | AUTO | 内置导航数据 | `data-page`、`data-surface`、`data-record-key` | `http://127.0.0.1:30029/index.html` | `历史验收图片已清理（可从 Git 历史恢复）` | CANDIDATE |
| TC-002 | AC-002 | 关键词搜索显示未分组结果 | P0 | E2E 截图 | AUTO | `建行`、`保费` | `data-field=link.keyword`、`data-action=link.search` | `http://127.0.0.1:30029/index.html` | `历史验收图片已清理（可从 Git 历史恢复）` | CANDIDATE |
| TC-003 | AC-003 | 整张卡片具备新标签页打开契约 | P1 | E2E 手工 | MANUAL | 内置卡片 URL | `a[data-action=link.open][target=_blank]` | `http://127.0.0.1:30029/index.html` | 浏览器 DOM 属性与控制台检查 | MANUAL |
| TC-004 | AC-004 | 加载、空、失败、logo 失败和长 URL 稳定展示 | P1 | 组件 截图 手工 | MANUAL | 长 URL、缺失 logo、接口返回数据 | `data-state`、文字 logo 兜底 | `http://127.0.0.1:30029/index.html` | `历史验收图片已清理（可从 Git 历史恢复）` | MANUAL |

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | AC-001 | 默认展示业务相关、工具相关、其他三组卡片 | 复用 Open API，前端按三组白名单聚合 | `mango-ui/packages/link-page/src/components/LinkPage.vue`、`mango-ui/packages/link-page/src/style.css`、`mango/mango-platform/mango-link/mango-link-core/src/main/resources/db/migration/link/V6__seed_guarantee_navigation.sql` | `mango-ui/packages/link-page/README.md` | `mango-docs/designs/2026-07-10-link-page-home-design.md` | EXCEPTION: 宿主页面正式接入后补正式 E2E | `mango-docs/plans/2026-07-10-link-page-business-home-ledger.md` | 页面截图和 API 数据检查 | DONE | `历史验收图片已清理（可从 Git 历史恢复）` |
| TASK-002 | AC-002 | 搜索后在分组上方展示未分组结果 | 使用 `keyword` 查询，前端去重 | `mango-ui/packages/link-page/src/components/LinkPage.vue` | `mango-ui/packages/link-page/README.md` | `mango-docs/designs/2026-07-10-link-page-home-design.md` | EXCEPTION: 宿主页面正式接入后补正式 E2E | `mango-docs/plans/2026-07-10-link-page-business-home-ledger.md` | 搜索按钮和回车交互走查 | DONE | `历史验收图片已清理（可从 Git 历史恢复）` |
| TASK-003 | AC-003 | 整张卡片点击，新标签页打开目标 | 卡片使用原生 `<a target="_blank" rel="noopener noreferrer">`，目标地址优先 `redirectUrl` 或 `url` | `mango-ui/packages/link-page/src/components/LinkPage.vue`、`mango-ui/packages/link-page/src/style.css` | `mango-ui/packages/link-page/README.md` | `mango-docs/designs/2026-07-10-link-page-home-design.md` | EXCEPTION: 宿主页面正式接入后补正式 E2E | `mango-docs/plans/2026-07-10-link-page-business-home-ledger.md` | DOM 链接属性和控制台检查 | DONE | `历史验收图片已清理（可从 Git 历史恢复）` |
| TASK-004 | AC-004 | 加载、空、失败、logo 失败、长 URL 有稳定反馈 | 页面内状态区、长地址省略和文字 logo 兜底 | `mango-ui/packages/link-page/src/components/LinkPage.vue`、`mango-ui/packages/link-page/src/style.css` | `mango-ui/packages/link-page/README.md` | `mango-docs/designs/2026-07-10-link-page-home-design.md` | EXCEPTION: 宿主页面正式接入后补正式 E2E | `mango-docs/plans/2026-07-10-link-page-business-home-ledger.md` | 截图、DOM 尺寸和控制台检查 | DONE | `历史验收图片已清理（可从 Git 历史恢复）` |
| TASK-005 | DEV-001 | 内置导航数据入库 | 新增 link 模块 migration，不改历史 migration | `mango/mango-platform/mango-link/mango-link-core/src/main/resources/db/migration/link/V6__seed_guarantee_navigation.sql` | `mango-ui/packages/link-page/README.md` | `mango-docs/designs/2026-07-10-link-page-home-design.md` | EXCEPTION: 宿主页面正式接入后补正式 E2E | `mango-docs/plans/2026-07-10-link-page-business-home-ledger.md` | migration SQL 审查；真实后端启动后 Open API 返回内置导航数据 | DONE | `历史验收图片已清理（可从 Git 历史恢复）` |

## 8. 验收证据记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | TC-001 | link-page 首页/API | 默认分组展示 | 本地预览数据 | 三组可见，卡片字段完整，页面只有一个组件实例 | 通过，未发现横向溢出 | 通过，控制台无 error/warn | `历史验收图片已清理（可从 Git 历史恢复）` | DONE |
| TASK-002 | TC-002 | link-page 首页/API | 关键词搜索 | `建行`、`保费` | 搜索结果位于分组上方，不再分组；按钮和回车均可触发 | 通过 | 通过，控制台无 error/warn | `历史验收图片已清理（可从 Git 历史恢复）` | DONE |
| TASK-003 | TC-003 | link-page 首页 | 整卡点击 | 保费测算、建设银行保函查询 | 卡片为 `a[data-action=link.open]`，带 `href`、`target=_blank`、`rel=noopener noreferrer` | 通过；in-app browser 未暴露新标签事件，已验证原生链接契约 | 通过，控制台无 error/warn | `历史验收图片已清理（可从 Git 历史恢复）` | DONE |
| TASK-004 | TC-004 | link-page 首页 | 边界状态 | 长 URL、缺失 logo | 长 URL 不撑破；缺失 logo 使用文字兜底；DOM 仅一个 H1 和一个组件实例 | 通过 | 通过，控制台无 error/warn | `历史验收图片已清理（可从 Git 历史恢复）` | DONE |
| TASK-005 | TC-001 | 后端服务 | 内置数据 migration | `V6__seed_guarantee_navigation.sql` | migration 使用现有表字段和三组分类；Open API 可查询到内置导航数据 | 不适用 | 后端和前端代理均返回 200 | `历史验收图片已清理（可从 Git 历史恢复）` | DONE |

## 9. 测试结果基线

| 基线 ID | 覆盖台账 ID | 覆盖用例 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BASELINE-001 | TASK-001,TASK-002,TASK-003,TASK-004 | TC-001,TC-002,TC-003,TC-004 | EXCEPTION: 本次使用包级页面走查，宿主页面正式接入后补正式 E2E | `pnpm -C mango-ui --filter @mango/link-openapi build`；`pnpm -C mango-ui --filter @mango/link-page build` | worktree slot 19；link-page preview `http://127.0.0.1:30029/index.html` | 本地预览数据和真实 Open API | 租户 1 请求头示例 | 前端构建通过，默认分组、搜索、链接属性和页面稳定性通过 | 正式宿主页面 E2E 暂未新增，待页面接入后补 | `历史验收图片已清理（可从 Git 历史恢复）` | link-page 从通用导航收敛为保函业务首页 |
| BASELINE-002 | TASK-005 | TC-001 | EXCEPTION: 数据验证走 API 和页面联调，正式宿主 E2E 后续补齐 | `Invoke-WebRequest http://127.0.0.1:18019/link/open/public-links/list`；`Invoke-WebRequest http://127.0.0.1:30019/api/link/open/public-links/list` | worktree slot 19 | `mango_dev_link_page_business_home_019` | 租户 1 | 后端直连和前端代理 Open API 均返回 200，内置导航数据可查询 | 无 | `历史验收图片已清理（可从 Git 历史恢复）` | 新增三组保函业务导航 seed |

## 10. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 使用 `MangoLinkPage` 展示数据库内置导航，传入 `headline` 可配置首页大文案 | `mango-ui/packages/link-page/README.md` | `pnpm -C mango-ui --filter @mango/link-page build`；`http://127.0.0.1:30029/index.html` | 租户 1 内置 PUBLIC 数据；本版本没有公私区分 | 地址不准时新增 migration 或后台维护修正；宿主页面正式接入后补正式 E2E | DONE |
