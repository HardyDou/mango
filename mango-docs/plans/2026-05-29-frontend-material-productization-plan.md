# Mango 前端物料依赖与自由组合部署产品化升级计划

## 1. 背景

`mango-sample` 暴露出 Mango 前端产品化链路没有闭环：业务项目可以手工接入 `@mango/admin-shell`，但不能证明“基于 Mango 初始化即可快速开发”。当前主要问题是前端发布包仍以源码作为入口、业务 starter 只生成骨架、单体与微前端部署组合缺少统一验收口径。

本计划目标是把 Mango 前端能力从“仓内可用”升级为“外部业务项目可通过发布物稳定消费”。

## 2. 目标

业务项目通过 Mango 初始化入口生成后，可以只依赖 Mango 发布物进行开发，并按需要选择单体、本地模块、微前端或混合部署。

完成后应满足：

- 新项目由 `create-mango-app` 或后续 Initializr 生成。
- 前端主框架通过 `@mango/admin-shell` 依赖，不复制 `mango-ui/apps/*` 源码。
- 内置能力页面通过 `@mango/*` 发布包组合，不要求业务项目理解 Mango monorepo 内部路径。
- 业务模块通过 `registerModulePages` 本地注册，或通过运行时配置作为微前端挂载。
- 生成项目默认可安装、类型检查、构建、启动和访问基础后台能力。
- 单体与微前端两种部署形态使用同一菜单、权限、资源和页面运行协议。

## 3. 范围

- 前端 npm 发布契约：`dist`、类型声明、`exports`、peer 依赖、样式入口、资源入口。
- `@mango/admin-shell` 主框架稳定 API：应用创建、登录、菜单、权限、主题、运行时、扩展点。
- `@mango/admin-pages` 页面注册协议：本地页面、内置页面、业务页面、缺省错误页。
- `@mango/app-runtime` 运行时协议：本地模块、微前端、iframe、外链统一决策和诊断。
- Mango 内置能力包组合：系统、RBAC、认证、工作流、通知、文件、模板、编号、日历等前端页面包。
- `mango-business-starter` 模板升级：默认 registry、环境变量、启动脚本、运行配置、基础能力开关。
- `create-mango-app` 升级：能力选择、部署模式选择、生成后验证提示和自测。
- 外部消费验收：生成干净业务项目并执行 install、typecheck、build、dev、浏览器冒烟。

## 4. 不做什么

- 不重写 Mango Admin Shell 的视觉布局。
- 不把业务项目代码放回 Mango 主仓长期维护。
- 不用手工样例替代初始化器验收。
- 不绕过权限、租户、菜单、资源同步链路声明完成。
- 不在业务项目中复制 Mango 内置能力源码。

## 5. 设计输入

- `mango-sample` 失败验证结论。
- `mango-docs/plans/2026-05-27-mango-productization-sprint-4-admin-shell.md`
- `mango-docs/plans/2026-05-27-mango-productization-sprint-5-business-template.md`
- `mango-docs/plans/2026-05-27-mango-productization-sprint-6-initializr.md`
- `mango-docs/plans/2026-05-27-mango-external-artifact-consumption-fixes.md`
- `mango-pmo/rules/frontend/06-monorepo-architecture.md`
- `mango-pmo/rules/frontend/01-vue-code.md`

## 6. 目标架构

```text
业务项目
├── frontend/apps/<project>-admin
│   └── 只调用 createMangoAdminApp
├── frontend/packages/<module>
│   └── 注册业务页面或导出微前端入口
├── frontend/packages/<module>-api
│   └── 业务 API 类型和请求封装
└── runtime-config
    └── 描述哪些模块本地加载、哪些模块微前端加载

Mango 发布物
├── @mango/admin-shell       主框架壳
├── @mango/admin-pages       页面注册协议和内置页面聚合
├── @mango/app-runtime       本地/微前端/iframe/外链运行时协议
├── @mango/common            请求、主题、通用组件
├── @mango/auth              登录和认证页面能力
├── @mango/rbac              权限和资源页面能力
├── @mango/system            系统管理页面能力
├── @mango/workflow          工作流页面能力
├── @mango/notice            通知页面能力
└── @mango/*                 其他可选能力包
```

### 6.1 依赖方向

- `apps/*` 可以依赖 `@mango/admin-shell` 和业务包。
- 业务包可以依赖 `@mango/admin-pages/core`、`@mango/common` 和自己的 API 包。
- `@mango/admin-shell` 可以依赖 `@mango/admin-pages`、`@mango/app-runtime` 和基础能力包。
- `packages/*` 禁止依赖 `apps/*`。
- 发布包禁止把 `src` 作为对外入口。

### 6.2 部署组合

| 模式 | 适用场景 | 前端形态 | 关键要求 |
|---|---|---|---|
| 单体本地模块 | 中小业务系统、统一发布 | 业务页面随 admin app 构建 | page registry 与后端菜单 component 一致 |
| 微前端模块 | 多团队、多模块独立发布 | shell 运行时加载远程模块 | runtime config 提供 runtimeCode、entry、appType |
| 混合部署 | 平台能力本地，业务能力远程 | 同一个 shell 同时挂本地包和微应用 | 菜单、权限、资源协议一致 |
| iframe/外链 | 第三方系统接入 | shell 只做入口聚合 | 明确安全策略和打开方式 |

## 7. 升级 Sprint 拆分

### 7.0 阶段推进门禁

每个 Sprint 完成后，先执行本阶段新特性测试，再执行受影响范围回归测试。两类测试全部通过并更新交付台账后，才能进入下一 Sprint。

阶段门禁要求：

- 新特性测试覆盖本 Sprint 新增或变更的初始化、依赖、构建、运行时、页面注册、部署模式和诊断能力。
- 回归测试覆盖上一阶段已通过的核心链路，至少保留包契约检查、核心包构建、生成项目安装/类型检查/构建、后端测试和浏览器冒烟中受影响的部分。
- 任一测试失败时，本阶段状态不得标记完成；必须修复后重测，或在交付台账中标记 `EXCEPTION` 并写明用户确认依据。
- 下一 Sprint 开始前必须复核上一 Sprint 的台账：完成项、验证命令、失败项、例外项和风险必须清楚。

### Sprint A：前端发布物契约收口

目标：所有对外 npm 包不再暴露源码入口，外部项目只消费构建产物和类型声明。

范围：

- 为 `@mango/admin-shell`、`@mango/admin-pages`、`@mango/app-runtime`、`@mango/common` 和主要业务能力包统一构建配置。
- package `main`、`module`、`types`、`exports` 指向 `dist`。
- 输出 `.d.ts`，并校验外部项目 `vue-tsc` 不检查 Mango 包源码。
- 收敛 Vue、Vue Router、Pinia、Element Plus、form-create 等 peer 依赖。
- 提供样式入口和资产入口，不要求业务项目导入包内私有路径。

验收：

- `pnpm -r build` 通过。
- 扫描 `mango-ui/packages/*/package.json`，对外包不再以 `src` 作为入口。
- 生成临时外部消费项目，执行 `pnpm install`、`pnpm typecheck`、`pnpm build` 通过。
- 新特性测试：验证发布包 `dist` 入口、类型声明、子路径 exports 和 peer 依赖检查。
- 回归测试：复跑已通过的核心包构建与包契约检查，确认 Admin Shell、App Runtime、Admin Pages 和 Common 仍可生成发布物。

### Sprint B：Admin Shell 扩展点与运行时协议稳定

目标：业务项目通过配置、插件、扩展点完成差异化，不修改 shell 源码。

范围：

- 固化 `createMangoAdminApp(options)` 参数：标题、API 地址、登录、主题、菜单、模块运行模式、运行配置加载策略。
- 提供 header、home、login、error、profile 等可替换扩展点。
- 明确本地页面、微前端、iframe、外链的运行时决策规则。
- 运行时错误诊断可见：缺菜单、缺页面、缺 runtime config、远程加载失败。
- `apps/mango-admin-shell` 保持为薄验证应用。

验收：

- `@mango/admin-shell` 单测覆盖边界。
- 本地模块和微前端模块在同一 shell 下可切换。
- 不存在 `@mango/admin-shell` 引用 `apps/*` 私有路径。
- 新特性测试：覆盖 shell options、扩展点替换、运行时决策、错误诊断和本地/微前端切换。
- 回归测试：复跑 Sprint A 包契约检查、核心包构建，以及最小 shell 应用启动检查。

### Sprint C：内置 Mango 能力包组合

目标：系统管理、权限、认证、工作流、通知、文件等能力可以作为发布物被业务项目选择启用。

范围：

- 为每个内置能力定义 page registry、菜单 component、资源权限和前端包导出。
- `@mango/admin-pages` 只负责聚合和默认注册，不承载 app 私有实现。
- 能力包支持本地构建进 shell，也支持作为微前端远程入口。
- 页面使用真实 API、字典、权限和租户上下文，不以静态数据作为完成依据。

验收：

- 选择基础能力时，生成项目可访问登录、首页、系统管理、菜单权限页面。
- 选择工作流/通知/文件等能力时，页面入口和资源权限可同步。
- 能力包构建、类型声明和页面注册扫描通过。
- 新特性测试：覆盖每个能力包的 page registry、菜单 component、资源权限、真实 API 调用和本地/远程运行模式。
- 回归测试：复跑 Sprint A/B 的发布契约、shell 扩展点和运行时切换测试。

### Sprint D：Business Starter 可启动模板

目标：`mango-business-starter` 从骨架模板升级为可启动业务项目模板。

范围：

- 默认生成 `.npmrc` 或 registry 配置说明。
- 生成 `frontend` dev/build/typecheck/preview 脚本。
- 生成后端单体和微服务拓扑的启动脚本。
- 生成 runtime config，支持本地模块、微前端和混合模式。
- 生成基础资源清单、菜单、权限、租户、用户和初始化数据入口。
- 业务模块仍按 `api/core/starter/starter-remote` 分层。

验收：

- `node mango-business-starter/scripts/check-template.mjs` 通过。
- 生成物不包含 Mango 仓内私有路径。
- 单体拓扑和微前端拓扑说明与脚本一致。
- 新特性测试：从模板生成业务项目，验证 registry、环境变量、runtime config、启动脚本和初始化数据入口。
- 回归测试：复跑已通过的发布包消费、shell 启动、内置能力页面访问和资源权限链路。

### Sprint E：create-mango-app 能力选择和验收闭环

目标：初始化器成为业务项目入口，而不是只复制模板。

范围：

- CLI 支持能力选择：base、system、rbac、workflow、notice、file、template、numgen、calendar。
- CLI 支持部署模式选择：monolith、microservice、local-frontend、micro-frontend、mixed。
- CLI 支持输出生成后验证命令。
- CLI 自测生成临时项目，并验证关键文件、包依赖和配置。
- 后续 Web Initializr 复用同一模板和变量模型。

验收：

- `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` 通过。
- 生成项目可执行 `pnpm install`、`pnpm typecheck`、`pnpm build`。
- 生成项目可执行后端测试或最小启动检查。
- 新特性测试：覆盖 CLI 能力选择、部署模式选择、变量渲染、生成后提示和 CLI 自测。
- 回归测试：复跑 starter 生成项目验证、发布包消费验证和单体/微前端拓扑启动检查。

### Sprint F：外部消费端到端验收

目标：用干净生成项目证明 Mango 可以被业务系统快速使用。

范围：

- 从发布物生成一个临时业务项目。
- 不引用 `/Users/hardy/Work/mango/mango-ui/packages/*/src` 或 `apps/*`。
- 启动后端和前端。
- 浏览器验证登录、菜单、基础页面、本地业务页面、微前端页面。
- 记录阻断项，阻断项不得被手工样例覆盖。

验收：

- `pnpm install` 通过。
- `pnpm typecheck` 通过。
- `pnpm build` 通过。
- 后端测试通过。
- 浏览器冒烟通过。
- 交付台账无未完成项。
- 新特性测试：覆盖从发布物初始化干净项目、启用所选能力、访问本地业务页面和微前端页面。
- 回归测试：复跑全链路 install、typecheck、build、后端测试、浏览器登录、菜单和基础能力页面访问。

## 8. 验收总口径

最终完成时必须能执行以下链路：

```bash
npm create mango-app@latest guarantee-platform -- \
  --module guarantee \
  --package com.example.guarantee \
  --topology monolith \
  --frontend-mode mixed \
  --features system,rbac,workflow,notice,file

cd guarantee-platform
pnpm install
pnpm typecheck
pnpm build
mvn test
./scripts/dev-start.sh
```

浏览器验收：

- 可以登录。
- 可以看到后端菜单。
- 可以访问系统管理和权限页面。
- 可以访问所选 Mango 能力页面。
- 可以访问业务模块页面。
- 微前端模式下远程模块加载失败时有明确诊断。

## 9. 风险与限制

- 前端包从源码入口迁移到 `dist` 会影响 monorepo 本地调试，需要保留 workspace 开发体验和发布消费体验两个清晰入口。
- 内置能力包如果依赖 shell store 或 app 私有别名，需要先拆依赖边界。
- 微前端能力需要明确远程入口缓存、版本兼容和错误诊断策略。
- 初始化数据涉及权限和租户安全，必须和后端 starter、资源同步能力一起验收。
- 当前计划先形成升级路线，具体代码改动应按 Sprint 分支逐项推进。

## 10. 推荐执行顺序

1. 先做 Sprint A，解决外部包类型检查和发布契约。
2. 再做 Sprint B，让 shell 差异化通过扩展点完成。
3. 然后做 Sprint C，把 Mango 内置能力整理为可组合物料。
4. 接着做 Sprint D/E，让 starter 和 CLI 真正生成可启动项目。
5. 最后做 Sprint F，以生成项目作为唯一验收样例。
