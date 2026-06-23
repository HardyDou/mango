# 用户信息系统小组件交付契约

## 1. 目标

新增用户信息系统小组件，并整理系统小组件目录结构。

## 2. 范围

- `@mango/grid-widgets` 新增 `system.user-profile`。
- 快捷入口小组件迁入独立文件夹。
- 工作台默认布局增加用户信息小组件。
- 更新 README、设计说明、能力地图和交付台账。

## 3. 不做什么

- 不改 `@mango/grid-layout`。
- 不新增后端接口和数据库。
- 不新增菜单、按钮权限和角色授权。
- 不在小组件内直接依赖宿主 store 或 router。

## 4. 设计输入

- [用户信息系统小组件设计方案](../designs/mango-grid-widgets-user-profile-design.md)
- [Grid Widgets 注册聚合设计方案](../designs/mango-grid-widgets-registry-design.md)

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/grid-widgets`
- `mango-ui/packages/admin-shell/src/views/home/index.vue`
- `mango-docs/designs`
- `mango-docs/plans`
- `mango-docs/capabilities/README.md`

### 5.2 接口变化

无后端接口变化。前端 runtime 类型增加用户头像、角色、应用标识和租户展示字段。

### 5.3 数据变化

无数据库变化。默认工作台布局增加用户信息小组件项。

### 5.4 菜单/页面/权限变化

不新增菜单、页面或权限。用户信息小组件按钮复用 `/profile` 和 `/password`。

### 5.5 测试范围

执行包构建、后台构建、样式聚合检查、package exports 检查、能力文档检查和交付契约检查。

## 6. 风险与限制

- runtime 缺少用户或租户字段时展示兜底文案。
- 跳转由宿主 `runtime.navigate` 执行。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求 | 新增用户信息系统小组件 | 放入 `@mango/grid-widgets` 的 `src/system/user-profile/`，通过 runtime 消费当前登录人信息，并展示租户、角色和应用信息块 | `UserProfileWidget.vue`、`user-profile.ts`、`index.ts`、样式和类型 | 包构建、后台构建、页面人工验证 | DONE | `pnpm.cmd -F @mango/grid-widgets build`、`pnpm.cmd -F @mango/admin-shell build` 已通过 |
| TASK-002 | 用户要求 | 个人中心和修改密码都跳转到已有功能页面 | 通过 `runtime.navigate` 分别跳转 `/profile` 和 `/password` | `UserProfileWidget.vue` | 页面人工验证按钮跳转 | DONE | 代码确认使用已有 `/profile`、`/password` 路由，并复用工作台 `runtime.navigate` |
| TASK-003 | 用户要求 | 每个系统小组件相关文件放在独立文件夹，包括快捷入口 | 将快捷入口迁入 `src/system/quick-entry/`，保持公开 API 不变 | `quick-entry/` 目录、`system/index.ts`、`vite.config.ts` | 包构建和 exports 检查 | DONE | `@mango/grid-widgets` 构建产物包含 `index.js`、`quick-entry.js`、`user-profile.js`、`style.css`；全量前端构建后执行 `pnpm.cmd package-exports:check` 已通过 |
| TASK-004 | 项目规范 | 工作台默认可看到用户信息小组件 | 默认布局新增 `system.user-profile`，工作台 runtime 传入用户头像和租户信息 | `admin-shell/src/views/home/index.vue` | 后台构建和页面人工验证 | DONE | `pnpm.cmd -F @mango/admin-shell build`、`pnpm.cmd admin:styles:check`、`pnpm.cmd admin:module-styles:check` 已通过 |
| TASK-005 | PMO 规范 | 更新能力说明和交付文档 | 更新 README、能力地图、设计说明和交付台账 | README、capabilities、design、ledger | 文档检查和交付契约检查 | DONE | `audit-module-readmes`、`audit-readme-source-facts`、`delivery-contract-check --mode verify` 已通过 |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | 工作台 | 用户信息小组件展示 | 当前登录人 runtime | 头像、姓名、账号、租户、角色、应用标识和两个按钮正常展示 | 代码和构建通过；页面展示留给人工验收确认 | 构建阶段无新增报错 | `pnpm.cmd -F @mango/grid-widgets build`、`pnpm.cmd -F @mango/admin-shell build` | DONE |
| TASK-002 | 工作台 | 两个按钮跳转 | `/profile`、`/password` | 点击后进入已有功能页面 | 代码确认路由存在并通过宿主跳转函数处理 | 构建阶段无新增报错 | `rg "profile|password" mango-ui\packages\admin-shell\src`、后台构建 | DONE |
| TASK-003 | 包构建 | 快捷入口目录整理 | `@mango/grid-widgets` | 公开导出和构建产物正常 | 无 UI 变化 | 构建阶段无新增报错 | `pnpm.cmd -F @mango/grid-widgets build` | DONE |
| TASK-004 | 工作台 | 默认布局接入 | 默认布局 | 新用户默认可见用户信息和快捷入口 | 代码确认默认布局包含 `system.user-profile` 和 `system.quick-entry` | 构建阶段无新增报错 | `pnpm.cmd -F @mango/admin-shell build`、样式检查命令 | DONE |
| TASK-005 | 文档检查 | 文档和能力说明 | 本次设计/台账 | PMO 检查通过 | 无 UI 项 | 文档检查无新增报错 | `audit-module-readmes`、`audit-readme-source-facts`、交付契约检查 | DONE |
