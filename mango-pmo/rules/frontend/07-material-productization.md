# 前端物料产品化规范

## 1. 目标

- Mango 前端必须支持完整基座复用和能力包按需组合。
- 业务项目默认可以使用完整 Mango Admin，也可以按能力裁剪。
- 业务开发只扩展业务模块，不重写 Mango 主框架、菜单、顶栏、通知、用户区、主题和权限上下文。

## 2. 物料分层

Mango 前端物料分为三类：

- `@mango/admin`：完整管理后台基座，默认 full preset。
- `@mango/*-admin`：管理后台能力包，提供页面、菜单、路由、权限、局部样式和后台组件。
- `@mango/*-api`：API/SDK 能力包，提供接口方法、类型、枚举和可选 hooks，允许非管理后台 UI 使用。

过渡期允许现有 `@mango/system`、`@mango/rbac`、`@mango/file` 等混合包存在，但新增能力必须优先按 `*-api` 和 `*-admin` 拆分。

## 3. 默认复用规则

- `full` 模式默认启用完整 Mango Admin 能力。
- `custom` 模式按能力组合，但必须自动补齐必需依赖。
- 内置能力菜单、权限、页面和局部样式必须由能力包声明，禁止业务 starter 手写 Mango 内置菜单。
- 业务 starter 只能声明业务模块菜单和业务运行时配置。
- 菜单来源必须支持后端菜单优先，并能合并能力包菜单和业务菜单。

## 4. API 包规则

`@mango/*-api` 必须满足：

- 不包含 `views`、后台页面、菜单声明和后台布局组件。
- 不依赖 `@mango/admin`、`@mango/admin-shell`、`@mango/admin-pages`。
- 不依赖 Element Plus 作为运行时依赖。
- 对外导出 API 方法、类型、枚举和必要 hooks。
- API 类型不得依赖后台页面组件类型。

## 5. Admin 能力包规则

`@mango/*-admin` 必须满足：

- 依赖对应 `@mango/*-api`，不得在页面内重复维护 API 请求。
- 必须导出 `./capability`。
- capability 必须声明 `moduleCode`、`packageName`、`capabilityCode`、`capabilityName` 和 `pages`。
- 每个页面必须声明 `component` 和 `loader`。
- 菜单页面必须声明 `menuCode`，受权限控制的菜单必须声明 `permissions`。
- 包含局部样式时必须通过包导出提供稳定样式入口。
- 页面、菜单、权限和局部样式必须能被 `@mango/admin` 自动集成。

过渡期混合包如果包含 `src/capability.ts`，必须按 Admin 能力包的 capability 规则检查。

## 6. 基座规则

完整 Admin 基座必须负责：

- 原 Mango 主框架布局。
- 顶栏、通知、小铃铛、用户区、设置、主题和标签页。
- 登录、租户、权限、菜单和运行时上下文。
- 单体和微前端运行模式。
- 能力包注册、依赖补齐、菜单合并和权限过滤。

业务模块禁止复制或仿写这些基座能力。

## 7. 依赖规则

- 能力包必须声明必需依赖和可选依赖。
- CLI 和基座必须在 `custom` 模式下校验依赖缺失。
- 依赖缺失时必须失败并给出明确提示，禁止静默降级成残缺页面。
- 系统管理、权限、租户、菜单等基础能力不得被业务模块私自复制。

## 8. 验收规则

涉及前端物料产品化的交付必须验证：

- `full` 模式启动后与原 Mango Admin 主框架一致。
- `custom` 模式按能力组合后菜单、页面、权限和样式自动集成。
- `*-api` 包可被非管理后台 UI 安装、类型检查和调用。
- `*-admin` 包可被 Admin 基座安装、构建和浏览器访问。
- E2E 必须保存截图，并检查布局、颜色、菜单、样式、数据和功能。

## 9. 程序化检查

前端物料改动必须执行：

```bash
cd mango-ui
pnpm package:check
```

检查至少覆盖：

- 发布包入口只指向 `dist`。
- 不暴露源码路径和 app 私有路径。
- `*-api` 包不包含后台 UI 和 Admin 运行时依赖。
- `*-admin` 包导出 capability 并依赖对应 API 包。
- capability 声明完整，`packageName` 与包名一致。
- 有 capability 的包必须导出 `./capability`。

## 10. 禁止事项

- 禁止把“能打开一个仿 Mango 页面”当成 Mango 基座复用完成。
- 禁止业务 starter 手写 Mango 内置菜单作为最终方案。
- 禁止 Admin 页面绕过 API 包直接散写请求逻辑。
- 禁止 `*-api` 包依赖后台主框架、菜单、Element Plus 页面组件或 Admin store。
- 禁止新增能力包没有 capability、菜单、权限、样式和 E2E 验证就声明可复用。
