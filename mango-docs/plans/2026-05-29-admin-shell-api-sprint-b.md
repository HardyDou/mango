# Mango Admin Shell API 产品化 Sprint B

## 1. 背景

Sprint A 已证明 Mango 前端包可以通过 `dist` 发布物被外部项目消费。下一步需要让业务项目通过 `@mango/admin-shell` 的稳定 API 完成差异化，而不是复制或修改 shell 源码。

## 2. 目标

业务项目调用 `createMangoAdminApp(options)` 后，可以配置标题、API 地址、登录、主题、菜单、运行模式、运行配置加载策略和关键页面扩展点，并保留单体、微前端、iframe、外链的自由组合部署能力。

## 3. 范围

- 固化 `MangoAdminShellOptions` 的公开配置面。
- 提供 Shell、Layout、Login、Error、Profile 等组件替换扩展点。
- 支持 options 初始化主题、布局、运行配置和菜单加载策略。
- 暴露运行时决策与运行配置诊断读取能力。
- 补充 Admin Shell 单测和外部消费方 smoke。

## 4. 不做什么

- 不重写 Admin Shell 视觉布局。
- 不调整后端菜单、权限、租户数据结构。
- 不新增远程微前端适配器类型。
- 不实现 starter 或 CLI 初始化能力。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/admin-shell/src/config.ts`
- `mango-ui/packages/admin-shell/src/index.ts`
- `mango-ui/packages/admin-shell/src/appBootstrap.ts`
- `mango-ui/packages/admin-shell/src/router.ts`
- `mango-ui/packages/admin-shell/src/runtime/*`
- `mango-ui/packages/admin-shell/src/__tests__/*`
- `mango-docs/plans`

### 5.2 接口变化

- `MangoAdminShellOptions` 增加组件扩展点、主题、布局、菜单和 runtime 调试配置。
- `createMangoAdminApp(options)` 保持主入口不变。
- `@mango/admin-shell/runtime` 暴露运行时诊断读取函数。

### 5.3 数据变化

无数据库变化。

### 5.4 菜单/页面/权限变化

无后端菜单和权限数据变化；前端菜单加载支持通过 options 覆盖 `appCode` 和加载函数。

### 5.5 测试范围

- Admin Shell 单测覆盖 options 合并、扩展点替换、菜单加载和运行时诊断。
- 外部消费方 smoke 覆盖 `createMangoAdminApp(options)` 类型检查和生产构建。
- 回归复跑 Sprint A 包契约检查和核心包构建。

## 6. 完成标准

- 业务项目可通过 options 替换关键 Shell 组件，不修改 `@mango/admin-shell` 源码。
- 菜单 appCode、API baseUrl、runtime config URL、运行模式和主题可通过 options 配置。
- 运行时决策和 runtime config 诊断有稳定读取入口。
- 新特性测试和回归测试全部通过后，才能进入 Sprint C。

## 7. 风险与限制

- 本 Sprint 只稳定前端 Shell API，不保证所有内置能力页面的业务链路已完成真实接口验收。
- 外部 smoke 仍使用 `link:` 验证本地发布物；真实 npm registry 的 pack/install 验证留到发布前链路。
