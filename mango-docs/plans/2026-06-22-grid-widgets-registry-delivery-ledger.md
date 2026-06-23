# Mango 小组件注册聚合交付台账

## 1. 目标

新增 `@mango/grid-widgets` 前端包，沉淀 Mango 系统小组件与小组件聚合工具；工作台改为通过该包注入系统小组件，并保留一个“快捷入口”小组件用于验证。

## 2. 范围

- 新增 `mango-ui/packages/grid-widgets`。
- 新增快捷入口系统小组件 `system.quick-entry`。
- 工作台页面接入 `@mango/grid-widgets`。
- 删除工作台原本用于验证的本地小组件定义。
- `@mango/admin` 和 `@mango/admin-shell` 增加 `@mango/grid-widgets` 依赖声明。
- admin 样式聚合新增 `@mango/grid-widgets/style.css`。
- 同步小组件注册聚合设计文档和工作台设计文档。

## 3. 不做什么

- 不修改 `@mango/grid-layout`。
- 不实现小组件权限过滤。
- 不新增后端接口或数据库表。
- 不把业务系统小组件写入 Mango 主项目。
- 不让 `@mango/grid-widgets` 读取宿主 store、router、菜单或登录态。
- 不让工作台页面长期承载快捷入口或其它系统小组件的通用适配逻辑。

## 4. 设计输入

- 用户确认：保持 `@mango/grid-layout` 不动，只关注处理小组件数据后注入。
- 用户确认：第一版不考虑小组件权限，所有小组件全部放开，数据权限交给接口控制。
- 用户确认：系统性小组件在 Mango 项目开发，业务相关小组件在业务系统开发，最终在业务系统组合。
- 用户确认：消费页面只写个性化代码，小组件能力尽可能回收到小组件内部。
- 用户确认：小组件体系需要同时考虑单体部署和微前端部署。
- 设计文档：`mango-docs/designs/mango-grid-widgets-registry-design.md`
- 关联设计文档：`mango-docs/designs/mango-grid-layout-workbench-design.md`

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/grid-widgets`
- `mango-ui/packages/admin-shell/src/views/home/index.vue`
- `mango-ui/packages/admin-shell/package.json`
- `mango-ui/packages/admin-shell/vite.config.ts`
- `mango-ui/packages/admin/admin-modules.json`
- `mango-ui/packages/admin/package.json`
- `mango-docs/designs/mango-grid-widgets-registry-design.md`
- `mango-docs/designs/mango-grid-layout-workbench-design.md`

### 5.2 接口变化

无后端接口变化。

### 5.3 数据变化

无数据库结构变化。工作台默认布局从多个本地验证小组件收敛为 `system.quick-entry` 快捷入口小组件。

### 5.4 菜单/页面/权限变化

无菜单、页面路由和权限变化。小组件权限过滤第一版不启用。

### 5.5 测试范围

- `@mango/grid-widgets` 包构建。
- `@mango/admin-shell` 构建。
- admin 样式聚合生成和检查。
- admin 官方模块样式治理检查。
- 工作台页面启动后验证快捷入口小组件展示。

## 6. 风险与限制

- 第一版小组件权限不在前端聚合层控制，必须由小组件内部接口保证数据权限。
- 快捷入口第一版在小组件内部触发点击事件，并通过宿主注入的 `runtime.navigate` 完成路由、微前端或外链跳转适配。
- 新增 `@mango/grid-widgets` 后需要发布 npm 包，业务系统才能通过包安装消费。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| GW-001 | 用户要求 | 抽出系统小组件注册聚合能力 | 新增 `@mango/grid-widgets`，只处理小组件定义和聚合，不改 `@mango/grid-layout` | `mango-ui/packages/grid-widgets` | `pnpm -F @mango/grid-widgets build` | DONE | 本文档第 8 节 |
| GW-002 | 用户要求 | 系统小组件和业务小组件能组合后注入布局组件 | 提供 `mergeGridWidgets`，按 `type` 去重并稳定排序 | `mango-ui/packages/grid-widgets/src/registry.ts` | 构建与代码检查 | DONE | 本文档第 8 节 |
| GW-003 | 用户要求 | 做一个快捷入口小组件用于验证 | 新增 `system.quick-entry` 系统小组件 | `mango-ui/packages/grid-widgets/src/system/quick-entry.ts` | 工作台页面验证 | DONE | 本文档第 8 节 |
| GW-004 | 用户要求 | 删除现有工作台验证小组件 | 工作台只保留快捷入口默认布局，删除页面本地小组件文件 | `mango-ui/packages/admin-shell/src/views/home/index.vue` | admin-shell 构建与页面验证 | DONE | 本文档第 8 节 |
| GW-005 | 前端组件规范 | 系统小组件样式随包发布并纳入 admin 聚合 | `@mango/grid-widgets` 导出 `./style.css`，admin manifest 声明样式 | `mango-ui/packages/admin/admin-modules.json` | `pnpm admin:styles:check`、`pnpm admin:module-styles:check` | DONE | 本文档第 8 节 |
| GW-006 | PMO 要求 | 同步设计与交付说明 | 新增注册聚合设计文档，更新工作台设计文档 | `mango-docs/designs/*`、本文档 | 交付契约检查 | DONE | 本文档第 8 节 |
| GW-007 | 用户要求 | 明确小组件运行上下文与能力内聚边界 | 增加 `WidgetRuntimeContext` 设计，页面只传上下文，小组件内部处理自身适配逻辑 | `mango-docs/designs/mango-grid-widgets-registry-design.md` | 文档审阅与后续实现对照 | DONE | 本文档第 8 节 |
| GW-008 | 用户要求 | 兼容单体部署和微前端部署 | 运行上下文支持 `host`、`sub-app`、`standalone`，跳转由宿主注入 | `mango-docs/designs/mango-grid-widgets-registry-design.md` | 文档审阅与后续实现对照 | DONE | 本文档第 8 节 |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| GW-001 | 前端包 | `@mango/grid-widgets` 构建 | 包源码 | 构建产出 JS、CSS 和类型声明 | 不涉及 | 不涉及 | `pnpm.cmd -F @mango/grid-widgets build` 通过 | DONE |
| GW-002 | 聚合工具 | `mergeGridWidgets` | `same` 重复 type 与 `other` 小组件 | 按 `type` 去重，保留先注册项，按 `order` 排序，触发重复回调 | 不涉及 | 不涉及 | `node -e` 运行时断言通过 | DONE |
| GW-003 | 工作台 | 快捷入口展示 | 默认布局 `system.quick-entry` | `@mango/admin-shell` 构建通过，工作台代码已引用 `systemQuickEntryWidgets` | 登录 `http://127.0.0.1:8538/index.html#/login` 后进入首页，工作台展示“快捷入口”，包含“系统设置、菜单管理、文件中心、工作日历” | 后端健康接口 `http://127.0.0.1:18848/actuator/health` 返回 200；前端代理 `http://127.0.0.1:8538/api/actuator/health` 返回 200；页面无启动阻塞 | `pnpm.cmd -F @mango/admin-shell build` 通过；浏览器登录后 URL 为 `http://127.0.0.1:8538/index.html#/home` | DONE |
| GW-004 | 工作台 | 旧验证小组件清理 | 默认布局 | 已删除 `mango-ui/packages/admin-shell/src/grid-widgets/workbench.ts`，工作台默认布局只保留 `system.quick-entry` | 登录首页后只看到快捷入口小组件，未出现旧的待办、统计、公告等验证小组件 | 页面加载依赖真实后端和 grid-layout 个性化接口；未发现旧本地小组件引用 | `rg "grid-widgets/workbench" mango-ui/packages/admin-shell/src` 无结果；浏览器首页文本只包含快捷入口小组件内容 | DONE |
| GW-005 | admin 样式 | 样式聚合 | `@mango/grid-widgets/style.css` | 生成和检查通过 | 登录首页后快捷入口小组件按卡片样式正常展示 | 不涉及 | `pnpm.cmd admin:styles:check`、`pnpm.cmd admin:module-styles:check` 通过 | DONE |
| GW-006 | PMO 文档 | 交付契约完整性 | 设计文档与本文档 | 检查通过 | 不涉及 | 不涉及 | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/designs/mango-grid-widgets-registry-design.md --ledger mango-docs/plans/2026-06-22-grid-widgets-registry-delivery-ledger.md --mode verify` 通过 | DONE |
| GW-007 | 设计文档 | 小组件运行上下文 | 用户确认的四条原则 | 文档已记录 `@mango/grid-layout` 不动、消费页面只写个性化代码、小组件能力内聚、兼容部署形态 | 不涉及 | 不涉及 | `mango-docs/designs/mango-grid-widgets-registry-design.md` 第 4.1、7.1、11 节 | DONE |
| GW-008 | 设计文档 | 部署形态兼容 | 微前端宿主、子应用、单体部署 | 文档已明确通过运行上下文注入菜单和跳转能力，不直接依赖宿主私有实现 | 不涉及 | 不涉及 | `mango-docs/designs/mango-grid-widgets-registry-design.md` 第 7.1、11 节 | DONE |
