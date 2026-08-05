# 标准交付记录

任务：Issue #721 个人中心扩展菜单装配修复

## 1. 元数据

- 任务 ID：Issue #721
- 交付模式：STANDARD
- 需求影响：L2 - 最新发布包的业务管理端无法从个人中心进入消息、公告、通知设置和个人登录日志，影响公开消费入口，但不改变后端数据、权限或租户边界。
- 方案风险：L2 - 扩展共享 `featureRegistrar` 装配契约并联动 Admin Shell、Notice、System 和示例应用，影响多个前端发布包，但改动可通过回退注册字段恢复。
- 最终风险：L2
- 工作区决策：CREATE - `fix/issue-721-profile-feature-sections`

## 2. 目标与范围

- 目标：让使用最新 Mango 发布包并通过 `createMangoAdminApp()` 启动的业务管理端，在注册 Notice/System feature registrars 后自动获得对应个人中心扩展菜单。
- 成功条件：Notice 自动提供“我的消息、系统公告、通知设置”，System 自动提供“登录日志”；未注册模块时不出现其扩展菜单；示例应用不再维护重复配置。
- 处理范围：`@mango/admin-shell` feature registration 契约与应用逻辑、`@mango/notice` 和 `@mango/system` registrar、`mango-admin` 示例入口、包级测试、受影响能力 README。
- 不处理范围：后端菜单资源、权限码、租户逻辑、Notice/System API、页面内部业务行为、npm 发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 业务管理端 `/profile` | 使用 `createMangoAdminApp()`，并注册 Notice registrar | 个人中心显示“我的消息、系统公告、通知设置” | 任何一项缺失即为失败 | 三个语义菜单锚点存在且可切换到对应组件 |
| SR-002 | 业务管理端 `/profile` | 使用 `createMangoAdminApp()`，并注册 System registrar | 安全设置下显示“登录日志” | 菜单缺失或组件不可渲染即为失败 | 登录日志语义菜单锚点存在且对应组件可加载 |
| SR-003 | 按需模块业务端 | 未注册 Notice 或 System registrar | 不注入未集成模块的个人中心菜单 | Shell 无条件硬编码可选模块菜单即为失败 | 包级测试证明 registrar 结果决定 section 集合 |
| SR-004 | Mango 示例应用 | 使用完整模块注册链 | 展示结果与业务消费链一致，且不维护第二份 section 清单 | app 私有硬编码仍存在即为失败 | 搜索无 app-local 四项重复配置，现有个人中心 E2E 继续通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001~SR-003 | 为 `MangoAdminFeatureRegistration` 增加可选 `profileSections`；旧 registrar 返回值保持兼容 | `packages/admin-shell/src/config.ts` | 删除可选字段及其消费逻辑 |
| TD-002 | SR-001~SR-003 | Shell 在应用 feature registration 时统一调用 Auth 的 section 注册能力；模块只声明自身 section，不依赖宿主私有代码 | `packages/admin-shell/src/runtime/featureRegistrars.ts` | 回退 Shell 聚合逻辑 |
| TD-003 | SR-001 | Notice registrar 声明三个异步个人中心页面及图标，保持模块按需装配 | `packages/notice/src/admin-pages.ts` | 删除 Notice 的 `profileSections` 返回值 |
| TD-004 | SR-002 | System registrar 声明个人登录日志异步页面及图标 | `packages/system/src/admin-pages.ts` | 删除 System 的 `profileSections` 返回值 |
| TD-005 | SR-004 | 删除 `apps/mango-admin/src/main.ts` 的四项 `profile.sections` 和只为其服务的 imports，避免双源 | `apps/mango-admin/src/main.ts` | 仅在整体回退 TD-001~TD-004 时恢复 |
| TD-006 | SR-001~SR-004 | 权限、路由 key、API 和后端数据契约不变；本次只改变前端装配来源 | 全部影响路径 | 不涉及数据恢复 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IM-001 | TD-001、TD-002 | 1 | `mango-ui/packages/admin-shell/src/config.ts`、`runtime/featureRegistrars.ts` | 类型与运行时支持聚合个人中心 sections |
| IM-002 | TD-003、TD-004 | 2 | `mango-ui/packages/notice/src/admin-pages.ts`、`packages/system/src/admin-pages.ts` | 两个模块声明自身 sections |
| IM-003 | TD-005 | 3 | `mango-ui/apps/mango-admin/src/main.ts` | 删除 app-local 重复清单 |
| IM-004 | TD-001~TD-005 | 4 | 对应包 `*.spec.ts` 与现有 E2E | 覆盖聚合、按需、去重和用户可见菜单 |
| IM-005 | TD-001~TD-006 | 5 | Admin Shell、Admin、Auth、Notice、System README | 公开装配方式与排障说明和实现一致 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001~SR-003 | M10 单元/组件测试 | `pnpm --filter @mango/auth test`；`pnpm --filter @mango/system test`；`pnpm --filter @mango/notice test`；`pnpm --filter @mango/admin-shell test` | PASS | Auth 3 files/10 tests、System 1 file/1 test、Notice 10 files/37 tests、Admin Shell 11 files/53 tests 全部通过 |
| SR-001~SR-004 | M09 受影响包构建 | `pnpm --filter @mango/auth build`；`pnpm --filter @mango/system build`；`pnpm --filter @mango/notice build`；`pnpm --filter @mango/admin-shell build`；`pnpm --filter @mango/admin build` | PASS | 五个受影响发布包均成功生成产物和类型；Admin Shell 补充 external 后无未解析依赖 |
| SR-001~SR-004 | M09 样式与 diff 静态验证 | `pnpm admin:styles:check`；`pnpm admin:module-styles:check`；`git diff --check` | PASS | Admin 聚合样式为最新、12 个官方模块样式治理通过、diff 无空白错误 |
| SR-001~SR-004 | M09 包导出验证 | `pnpm package-exports:check` | BLOCKED（非本改动） | 当前工作区未构建的 `@mango/link-page`、`@mango/site-shell` 产物及未物化的 `@mango/pmo` 发布资产导致全仓门禁失败；本次新增 `@mango/auth/config` 已在 Auth 成功构建并生成导出产物 |
| SR-001、SR-002、SR-004 | M13 UI/E2E | `pnpm exec playwright test e2e/specs/notice-message-center-menu.spec.ts --project chromium` | PASS | 1/1 通过；实际登录后个人中心三个消息入口、列表交互及登录日志入口均完成断言 |
| SR-001、SR-002、SR-004 | M13 跨浏览器环境检查 | `pnpm exec playwright test e2e/specs/notice-message-center-menu.spec.ts` | BLOCKED（环境） | Chromium 通过；本机未安装 Playwright Firefox 1532 与 WebKit 2251 可执行文件，两个项目在启动浏览器前终止，未形成产品失败证据 |

## 7. 例外与剩余风险

- 不执行 npm 发布；修复要进入业务项目仍需后续独立发布批次与业务依赖升级。
- 不改后端权限或菜单资源；若业务环境缺少 Notice/System 后端能力，对应页面仍会按既有错误语义反馈。
- 全仓 `package-exports:check` 与 Firefox/WebKit E2E 的阻断均来自本地构建/浏览器环境，不影响已通过的受影响包构建、单元测试和 Chromium 用户路径；正式发布前仍需在完整 CI 环境复验。
