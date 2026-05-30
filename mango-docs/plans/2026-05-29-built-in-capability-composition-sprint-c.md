# Mango 内置能力包组合 Sprint C

## 1. 背景

Sprint A 已完成前端发布物契约，Sprint B 已稳定 Admin Shell options 和运行时诊断。下一步需要让系统、权限、认证、文件、工作流、通知、模板、编号、日历等 Mango 内置能力作为发布物被业务项目选择启用，而不是由 `@mango/admin-pages` 私有维护一份散落的页面映射。

## 2. 目标

每个内置能力包公开自己的页面注册清单和能力清单，`@mango/admin-pages` 只负责聚合这些公开物料。业务项目可以按需导入能力包 manifest，选择本地注册，后续也可以基于同一 moduleCode 和 component 契约切换微前端运行方式。

## 3. 范围

- 定义 Admin Pages 可消费的能力 manifest 与 page registry 结构。
- 为 `@mango/auth`、`@mango/rbac`、`@mango/system`、`@mango/file`、`@mango/workflow`、`@mango/notice`、`@mango/template`、`@mango/numgen`、`@mango/calendar` 导出能力 manifest。
- `@mango/admin-pages` 的默认注册改为聚合能力包导出的 registry。
- 增加 Admin Pages 单测，覆盖能力清单、页面注册、组件路径归一化和选择性注册。
- 增加外部消费 smoke，验证业务项目可导入能力 manifest 并完成类型检查和构建。

## 4. 不做什么

- 不新增或修改后端菜单、权限、租户数据库数据。
- 不声明所有内置页面业务链路已完成浏览器联调。
- 不把后端 resource manifest 复制为前端最终菜单数据。
- 不实现 starter、CLI 或真实 registry 发布。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/admin-pages/src/*`
- `mango-ui/packages/auth/src/*`
- `mango-ui/packages/rbac/src/*`
- `mango-ui/packages/system/src/*`
- `mango-ui/packages/file/src/*`
- `mango-ui/packages/workflow/src/*`
- `mango-ui/packages/notice/src/*`
- `mango-ui/packages/template/src/*`
- `mango-ui/packages/numgen/src/*`
- `mango-ui/packages/calendar/src/*`
- `mango-docs/plans`

### 5.2 接口变化

- `@mango/admin-pages/core` 增加能力 manifest 类型和按能力注册函数。
- 各内置能力包根入口或明确子入口导出 `mangoXxxCapability` 和 `mangoXxxPageRegistry`。
- `@mango/admin-pages/defaults` 继续保留 `registerDefaultAdminPages(options)`，内部改为使用能力 manifest 聚合。

### 5.3 数据变化

无数据库变化。菜单和权限数据来源仍为后端 `authorization_menu`、resource manifest 同步能力和真实授权接口。

### 5.4 菜单/页面/权限变化

不新增运行态菜单和权限数据。前端只把后端菜单 `component` 可解析到的页面注册 key 固化为包内公开契约，并记录每个能力包对应的 `moduleCode`。

### 5.5 测试范围

- Admin Pages 单测：能力 manifest 聚合、重复注册保护、选择性注册、页面 loader 解析。
- 包构建：Admin Pages 和所有内置能力包构建通过。
- 外部消费 smoke：临时项目从发布物导入能力 manifest，执行 typecheck 和 build。
- 回归测试：复跑 Sprint A 包契约检查、Sprint B Admin Shell 单测和核心包构建。

## 6. 完成标准

- 每个内置能力包都公开 page registry 与 capability manifest。
- `@mango/admin-pages` 默认注册不再维护能力包页面 loader 的唯一私有清单。
- 新特性测试和回归测试全部通过。
- 交付台账无未完成项；未完成真实浏览器联调必须记录为风险，不得声明已完成。

## 7. 风险与限制

- 本 Sprint 只收敛前端物料组合契约，不验证每个内置页面的新增、编辑、搜索、权限不足等完整真实业务链路。
- 当前外部 smoke 仍使用本地 `link:` 发布物，不等价于真实 npm registry pack/install。
- 部分内置能力包的后端菜单 component 历史上使用 `@/views/...` 写法，本 Sprint 通过前端归一化兼容，不改数据库历史数据。
