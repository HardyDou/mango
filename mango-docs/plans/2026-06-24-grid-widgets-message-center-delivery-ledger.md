# 消息中心系统小组件交付台账

## 1. 目标

新增消息中心系统小组件，作为 `@mango/grid-widgets` 的预制系统小组件接入工作台。

## 2. 范围

- `@mango/grid-widgets` 新增 `system.message-center`。
- 工作台默认布局增加消息中心小组件。
- 消息中心小组件复用 `@mango/notice` 已有接口。
- 更新 `@mango/grid-widgets` README、能力地图和设计文档。

## 3. 不做范围

- 不修改 `@mango/grid-layout`。
- 不新增后端接口、数据库表、菜单和权限。
- 不做小组件级权限过滤。
- 不把通知业务查询逻辑写入工作台页面。

## 4. 设计依据

- [消息中心系统小组件设计方案](../designs/mango-grid-widgets-message-center-design.md)
- [Grid Widgets 注册聚合设计方案](../designs/mango-grid-widgets-registry-design.md)
- [用户信息系统小组件设计方案](../designs/mango-grid-widgets-user-profile-design.md)

## 5. 影响模块

- `mango-ui/packages/grid-widgets`
- `mango-ui/packages/admin-shell/src/views/home/index.vue`
- `mango-docs/capabilities/README.md`
- `mango-docs/designs/mango-grid-widgets-message-center-design.md`

## 6. 数据与权限

无数据库结构变化。消息中心小组件只读取 `@mango/notice` 已有接口，未读状态和全部已读状态由通知后端维护。

无菜单和角色授权变化。小组件库可见性第一版不做过滤，数据权限由通知接口控制。

## 7. 验证矩阵

| 编号 | 模块 | 场景 | 输入 | 期望 | 验证命令或方式 | 状态 |
|------|------|------|------|------|----------------|------|
| MC-001 | `@mango/grid-widgets` | 包构建 | 新增消息中心小组件 | 产出 `message-center.js`、类型和样式 | `pnpm.cmd -F @mango/grid-widgets build` | DONE |
| MC-002 | `@mango/admin-shell` | 工作台构建 | 默认布局新增 `system.message-center` | admin-shell 构建通过 | `pnpm.cmd -F @mango/admin-shell build` | DONE |
| MC-003 | 样式聚合 | admin 样式门禁 | 新增 `@mango/grid-widgets` 样式内容 | 样式声明和模块样式检查通过 | `pnpm.cmd admin:styles:check`、`pnpm.cmd admin:module-styles:check` | DONE |
| MC-004 | 工作台页面 | 默认展示 | 新用户或无个人布局 | 用户信息、快捷入口、消息中心同时展示 | 本地启动后人工验证 | DONE |
| MC-005 | 消息中心小组件 | 查看全部 | 点击“查看全部” | 跳转 `/notice/site-message` | 本地页面人工验证 | DONE |
| MC-006 | 消息中心小组件 | 全部已读 | 存在未读消息时点击“全部已读” | 调用接口成功后刷新未读数 | 本地页面人工验证 | DONE |
| MC-007 | 能力文档 | README 和能力地图 | 新增公开系统小组件 | 使用说明、排障入口和能力地图同步 | `node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs`、`check-capability-docs` | DONE |

## 8. 任务清单

| 编号 | 来源 | 任务 | 实现说明 | 影响文件 | 状态 |
|------|------|------|----------|----------|------|
| TASK-001 | 用户要求 | 新增消息中心小组件 | 放入 `src/system/message-center/`，展示未读、最新未读、分类统计和操作按钮 | `MessageCenterWidget.vue`、`message-center.ts`、`index.ts` | DONE |
| TASK-002 | 项目规范 | 系统小组件注册导出 | 加入 `systemGridWidgets`，提供 `./message-center` 子路径导出 | `system/index.ts`、`package.json`、`vite.config.ts` | DONE |
| TASK-003 | 用户要求 | 工作台默认布局展示消息中心 | 默认布局新增 `system.message-center` | `admin-shell/src/views/home/index.vue` | DONE |
| TASK-004 | 项目规范 | 样式随包发布 | 消息中心样式放入 `@mango/grid-widgets/style.css` | `src/style.css` | DONE |
| TASK-005 | PMO 要求 | 能力说明同步 | 更新 README、能力地图、设计文档和交付台账 | `README.md`、`mango-docs/**` | DONE |

## 9. 验证结果

已完成：

- `pnpm.cmd -F @mango/grid-widgets build` 通过，构建产物包含 `message-center.js` 和 `style.css`。
- `pnpm.cmd -F @mango/admin-shell build` 通过。
- `pnpm.cmd admin:styles:check` 通过。
- `pnpm.cmd admin:module-styles:check` 通过。
- `node mango-pmo/tools/audit-module-readmes.mjs` 通过。
- `node mango-pmo/tools/audit-readme-source-facts.mjs` 通过。
- `node mango-pmo/tools/check-capability-docs.mjs --base origin/main --head HEAD` 通过。
- 本地启动后登录工作台，消息中心默认展示、查看全部跳转、全部已读真实接口调用通过。

## 10. 风险与遗留

- 若本地通知接口暂无未读数据，页面只能验证空状态和跳转，未读统计需要用已有通知数据配合验证。
- 分类统计口径待确认：消息中心小组件默认按“系统/业务/审批/告警”四类业务分组统计未读消息。真实接口验证中，当前登录消息返回的业务分组为 `AUTH`，因此会出现总未读数大于 0，但四个默认分类均为 0 的情况。该问题不是 mock 或接口异常，而是现有通知业务类型分组与小组件默认分类口径未完全对齐，需由产品/领导确认后决定是否调整默认分类或后端业务类型分组配置。
- 本次不发布 npm 包，PR 合并后需按发布流程评估 `@mango/grid-widgets` 是否升版发布。
