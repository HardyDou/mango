# Mango Admin Runtime 完整产品化升级计划

## 1. 目标

把 Mango 前端从“散包可消费、简化 Shell 可启动”升级为“完整 Admin 基座可复用、能力包可组合、API SDK 可独立使用”的产品化形态。

最终交付必须满足：

- 业务项目默认使用 `@mango/admin` full preset，启动后就是完整原 Mango Admin。
- 业务项目可以使用 custom preset，按需选择 Mango 能力，并自动补齐必需依赖。
- Mango 内置能力拆成 `@mango/*-api` 和 `@mango/*-admin` 两类物料。
- `*-api` 可被非管理后台 UI 使用，不引入 Admin Shell 和后台页面。
- `*-admin` 一依赖就自动集成页面、菜单、路由、权限、局部样式和能力依赖。
- 菜单默认复用 Mango 后端菜单体系，并能合并能力包菜单和业务菜单。
- 单体、本地模块、微前端、混合部署使用同一运行时协议。
- E2E 必须用截图和布局报告验证 UI、样式、颜色、菜单、数据和功能，不允许只看构建通过。

## 2. 范围

### 2.1 必做范围

- 新增完整基座包 `@mango/admin`。
- 保留 `@mango/admin-shell` 作为底层运行时，不再让业务 starter 直接拼装简化 Shell。
- 定义并实现 `full`、`standard`、`minimal`、`custom` preset。
- 拆分 Mango 内置能力为 API SDK 包和 Admin UI 包。
- 定义 capability manifest 2.0：菜单、路由、权限、依赖、样式、运行时模式、后端能力要求。
- 实现能力依赖解析和缺失依赖失败机制。
- 实现后端菜单、能力包菜单和业务菜单合并。
- 改造 create-mango-app 和 mango-business-starter。
- 建立原 Mango 基准截图，并做 full/custom E2E 截图验收。
- 建立程序化检查，阻止新能力继续走半成品混合包路线。

### 2.2 不允许缩小的范围

- 不允许把“能打开一个业务页面”替代完整 Mango Admin。
- 不允许把“简化 Shell + 静态 starter 菜单”替代原 Mango 主框架。
- 不允许只拆页面，不拆 API/Admin 边界。
- 不允许只支持 full，不支持 custom。
- 不允许只支持本地模块，不支持微前端和混合部署。
- 不允许以 mock、静态菜单、静态权限、假数据作为完成依据。
- 不允许未做截图识别和真实数据联调就声明 UI 可用。

## 3. 设计输入

- `mango-pmo/rules/frontend/07-material-productization.md`
- `mango-pmo/rules/frontend/06-monorepo-architecture.md`
- `mango-pmo/rules/frontend/03-component-development.md`
- `mango-docs/plans/2026-05-29-frontend-material-productization-plan.md`
- `mango-docs/evidence/2026-05-29-frontend-reverify/reverification-report.md`
- 用户确认的目标：默认完整 Mango，可配置裁剪，可业务扩展，API 包可给非管理系统 UI 使用。

## 4. 目标架构

```text
业务项目
├── frontend/apps/<project>-admin
│   └── import { createMangoAdmin } from '@mango/admin'
├── frontend/packages/<module>-api
│   └── 业务 API SDK
├── frontend/packages/<module>-admin
│   └── 业务 Admin capability
└── runtime-config
    └── 本地模块、微前端、混合部署配置

Mango 发布物
├── @mango/admin                 完整 Admin Runtime
├── @mango/admin-shell           底层 Shell Runtime
├── @mango/admin-pages           页面注册和解析协议
├── @mango/app-runtime           本地/微前端/iframe/外链运行协议
├── @mango/common                通用工具、主题、基础组件
├── @mango/system-api            系统 API SDK
├── @mango/system-admin          系统管理 Admin 能力
├── @mango/rbac-api              权限 API SDK
├── @mango/rbac-admin            权限 Admin 能力
├── @mango/file-api              文件 API SDK
├── @mango/file-admin            文件 Admin 能力
├── @mango/workflow-api          工作流 API SDK
├── @mango/workflow-admin        工作流 Admin 能力
├── @mango/notice-api            通知 API SDK
├── @mango/notice-admin          通知 Admin 能力
└── @mango/*-api / *-admin       其他能力
```

## 5. 核心契约

### 5.1 Admin 入口

```ts
import { createMangoAdmin } from '@mango/admin';
import { guaranteeAdminCapability } from '@company/guarantee-admin';

createMangoAdmin({
  preset: 'full',
  modules: [guaranteeAdminCapability],
}).mount('#app');
```

custom 模式：

```ts
createMangoAdmin({
  preset: 'custom',
  capabilities: [
    mangoSystemAdminCapability,
    mangoRbacAdminCapability,
    mangoFileAdminCapability,
    guaranteeAdminCapability,
  ],
}).mount('#app');
```

### 5.2 API SDK 包

`@mango/*-api` 只允许包含：

- API 请求方法。
- TypeScript 类型。
- 枚举、常量、错误码。
- 不依赖 Admin Shell 的 hooks/composables。

禁止包含：

- `views`。
- 菜单和后台路由。
- Admin store。
- Element Plus 页面组件。
- Admin Shell 运行时依赖。

### 5.3 Admin 能力包

`@mango/*-admin` 必须包含：

- capability manifest。
- 页面 registry。
- 菜单声明。
- 权限声明。
- 局部样式入口。
- 后端能力要求。
- 依赖声明。
- E2E 覆盖清单。

### 5.4 capability manifest 2.0

manifest 必须能描述：

- `moduleCode`
- `packageName`
- `capabilityCode`
- `capabilityName`
- `requires`
- `optional`
- `backend`
- `pages`
- `menus`
- `permissions`
- `styles`
- `runtime`
- `e2e`

## 6. 阶段计划

### Sprint 0：基准冻结和防线加固

目标：先冻结“原 Mango 完整框架”的验收基准，防止后续再次把仿版当成完成。

改动项：

- 建立原 Mango Admin 基准截图集。
- 建立 baseline E2E：登录、首页、顶栏、小铃铛、用户区、设置、标签页、侧栏、系统菜单、权限菜单、文件/通知/工作流入口。
- 提前定义 capability manifest 2.0 类型和检查器骨架，字段至少覆盖依赖、菜单、权限、样式、运行时、后端能力和 E2E 清单。
- 扩展 `package:check`，阻止新增混合包、静态内置菜单和缺 capability 包。
- 扩展 starter/template 检查，禁止 starter 手写 Mango 内置菜单作为最终方案。
- 明确后端菜单字段与 capability 字段映射：`menuCode`、`component`、`permission`、`moduleCode`、排序、父子关系和冲突策略。

新特性测试：

- 原 Mango 基准截图采集脚本通过。
- capability manifest 2.0 类型检查和字段完整性检查通过。
- `package:check` 能识别违规 `*-api`、违规 `*-admin`、缺 capability、缺权限声明。
- template check 能识别 starter 静态内置菜单。

回归测试：

- `pnpm package:check`
- `pnpm package:build`
- 当前 mode matrix E2E
- registry consumption E2E
- dev-start E2E

完成标准：

- 基准截图和布局报告保存到 `mango-docs/evidence`。
- capability manifest 2.0 类型和检查器骨架已落地，后续拆包不得继续按旧 manifest 扩散。
- 违规样例检查可失败，正常仓库检查可通过。
- 无未解释的 E2E 失败。

### Sprint 1：`@mango/admin` 完整基座

目标：发布完整 Admin Runtime，让业务项目不再直接拼 `@mango/admin-shell`。

改动项：

- 新增 `mango-ui/packages/admin`。
- `@mango/admin` 默认组合原 Mango 主框架能力。
- `createMangoAdmin` 支持 full/standard/minimal/custom preset。
- 从原 Mango 应用抽取完整顶栏、通知、小铃铛、用户区、设置、主题、标签页和布局能力。
- `@mango/admin-shell` 下沉为底层运行时。
- 业务 starter 入口改为 `createMangoAdmin`。

新特性测试：

- full preset 启动截图与原 Mango 基准一致。
- 顶栏、通知、小铃铛、用户区、设置、主题、标签页均存在并可交互。
- starter 不再直接导入 `@mango/admin-shell`。

回归测试：

- Admin Shell 单测。
- Admin Pages 单测。
- package check/build。
- full preset 浏览器 E2E。
- 旧 mode matrix E2E 迁移到 `@mango/admin` 后通过。

完成标准：

- `@mango/admin` 可发布、可安装、可类型检查、可构建。
- full preset 截图通过人工和脚本布局检查。
- 不存在业务项目仿写主框架。

### Sprint 2：API SDK 和 Admin UI 分包

目标：把现有混合能力包拆成 `*-api` 和 `*-admin`。

改动项：

- 拆分 `system` 为 `system-api`、`system-admin`。
- 拆分 `rbac` 为 `rbac-api`、`rbac-admin`。
- 拆分 `auth` 为 `auth-api`、`auth-admin`。
- 拆分 `file` 为 `file-api`、`file-admin`。
- 拆分 `notice` 为 `notice-api`、`notice-admin`。
- 拆分 `workflow` 为 `workflow-api`、`workflow-admin`。
- 拆分 `template`、`numgen`、`calendar`。
- 保留旧包兼容出口，但只能转发到新包，并标注迁移期限。

新特性测试：

- 每个 `*-api` 包可在无 Admin Shell 的临时 Vue/TS 项目中 typecheck/build。
- 每个 `*-admin` 包依赖对应 `*-api`，并能被 `@mango/admin` 注册。
- `package:check` 对所有新包通过。

回归测试：

- 全部能力页面构建。
- full preset E2E。
- custom preset E2E。
- 非管理 UI API SDK 消费 E2E。

完成标准：

- 页面请求不再绕过 API 包散写。
- `*-api` 不引入后台 UI 依赖。
- `*-admin` 一依赖即可集成页面、菜单、权限和样式。

### Sprint 3：capability manifest 2.0 和依赖解析

目标：能力包可以声明依赖，custom 模式能自动补齐或明确失败。

改动项：

- 扩展 manifest 类型。
- 实现能力依赖图解析。
- 实现缺失依赖错误。
- 实现能力冲突检测。
- CLI 和 runtime 共用同一依赖解析逻辑。
- 定义基础依赖：system、auth、rbac、tenant、menu、permission。

新特性测试：

- 选择 `file-admin` 自动补齐必需基础能力。
- 缺少不可自动补齐依赖时失败并给出明确错误。
- 循环依赖被检查脚本识别。

回归测试：

- full/custom preset E2E。
- create-mango-app 生成测试。
- package check/build。

完成标准：

- custom 模式不会静默生成残缺页面。
- 依赖解析结果可输出报告。

### Sprint 4：菜单、权限和资源自动集成

目标：菜单不再由 starter 手写，默认复用后端菜单并合并能力菜单和业务菜单。

改动项：

- 定义菜单合并策略：后端菜单优先、能力菜单补充、业务菜单追加。
- 定义菜单冲突规则。
- 定义权限过滤规则。
- 定义资源同步输入。
- starter 只声明业务菜单，不声明 Mango 内置菜单。
- 后端初始化数据和前端 capability 对齐。

新特性测试：

- full preset 显示原 Mango 完整菜单。
- custom preset 只显示所选能力菜单。
- 业务菜单追加到指定位置。
- 用户缺权限时菜单隐藏，接口仍做权限校验。

回归测试：

- 登录/租户/权限 E2E。
- 系统管理、权限管理、文件、通知、工作流菜单访问 E2E。
- 业务菜单访问 E2E。

完成标准：

- 没有 starter 静态内置菜单。
- 菜单、页面、权限三者一致。
- 截图证明菜单布局符合 Mango 基准。

### Sprint 5：运行时部署模式统一

目标：单体、本地模块、微前端、混合部署共享同一能力协议。

改动项：

- 统一 local/micro/mixed runtime decision。
- capability 支持声明本地页面和远程入口。
- runtime config 支持版本、entry、健康检查和失败诊断。
- 远程加载失败态使用 Mango 标准错误面板。
- 本地和微前端共享菜单、权限、样式和主题协议。

新特性测试：

- local 模式真实业务页面可访问。
- micro 模式远程模块可访问。
- mixed 模式平台本地、业务远程可访问。
- 远程失败时诊断准确、样式符合 Mango。

回归测试：

- mode matrix E2E。
- registry consumption E2E。
- dev-start E2E。
- full preset E2E。

完成标准：

- 三种部署模式截图和布局报告通过。
- 不存在 CSS 断链、样式丢失、原生按钮错误态。

### Sprint 6：create-mango-app 和 starter 完整改造

目标：初始化出的项目默认就是完整 Mango，可配置裁剪，可扩展业务。

改动项：

- CLI 支持 `--preset full|standard|minimal|custom`。
- CLI 支持 `--features` 并自动依赖补齐。
- CLI 支持 `--frontend-mode local|micro|mixed`。
- starter 入口使用 `@mango/admin`。
- 生成业务 `*-api` 和 `*-admin` 包。
- 生成后输出验证命令。
- 模板检查覆盖 API/Admin 边界、菜单、权限、样式和运行时配置。

新特性测试：

- full 初始化项目启动后显示完整 Mango。
- custom 初始化项目只显示选择能力和业务模块。
- 生成业务 API 包可独立消费。
- 生成业务 Admin 包自动接入菜单和页面。

回归测试：

- check-template。
- check-cli。
- package check/build。
- dev-start E2E。
- registry consumption E2E。

完成标准：

- 新项目无需复制 Mango app 源码。
- 新项目无需手写 Mango 内置菜单。
- 新项目可启动、登录、看完整菜单、访问业务页。

### Sprint 7：发布、升级和兼容策略

目标：业务项目能通过升级 Mango npm 版本继承平台改进。

改动项：

- 定义包版本策略。
- 定义旧混合包兼容层和迁移期限。
- 定义破坏性变更检查。
- 定义 release checklist。
- 定义 changelog 和迁移文档模板。
- 定义业务项目升级 E2E。

新特性测试：

- 从旧 starter 项目升级到新 `@mango/admin`。
- 旧 `@mango/file` 等兼容出口仍可工作。
- 新 `@mango/file-api` / `@mango/file-admin` 推荐路径可工作。

回归测试：

- tarball consumption E2E。
- registry consumption E2E。
- generated project upgrade E2E。

完成标准：

- 发布物可安装、可升级、可回滚。
- 兼容层不掩盖新边界违规。

### Sprint 8：最终验收和质量冻结

目标：用真实生成项目证明完整目标达成。

验收项目：

- full preset 项目。
- custom preset 项目。
- 非管理 UI API SDK 消费项目。
- local 模式项目。
- micro 模式项目。
- mixed 模式项目。
- 旧项目升级项目。

每个项目必须执行：

- install。
- typecheck。
- build。
- 后端测试。
- dev-start。
- 浏览器 E2E。
- 截图保存。
- DOM 布局报告。
- CSS 断链检查。
- API 数据核对。
- 权限和菜单核对。

完成标准：

- 所有交付台账项为 `DONE` 或用户确认的 `EXCEPTION`。
- 无未解释截图异常。
- 无未解释后端错误日志。
- 无 mock、伪代码、静态菜单替代真实能力。

## 7. 总体验收命令

最终至少保留以下命令：

```bash
cd mango-ui
pnpm package:check
pnpm package:build
pnpm frontend-mode:matrix-e2e
pnpm package:registry-e2e
pnpm business-starter:dev-start-e2e
node packages/create-mango-app/scripts/check-cli.mjs
```

新增命令：

```bash
pnpm admin:baseline-e2e
pnpm admin:full-e2e
pnpm admin:custom-e2e
pnpm api-sdk:consumer-e2e
pnpm generated-project:upgrade-e2e
```

## 8. 风险与处理

- 原 Mango 主框架若仍与 `apps/mango-admin` 私有状态强耦合，必须先抽取到 `@mango/admin`，不得仿写。
- 后端菜单和前端 capability 可能存在 component/menuCode 不一致，必须以检查脚本和 E2E 暴露，不允许静默兜底。
- 旧混合包兼容层只能作为迁移路径，不能成为新开发入口。
- API/Admin 拆包会影响大量 import，必须按包逐个迁移并保留回归测试。
- 微前端远程入口涉及版本兼容和缓存，需要明确失败诊断和回滚策略。

## 9. 不完成不得声明完成的事项

- 没有 `@mango/admin` full preset，不得声明完整基座复用完成。
- 没有原 Mango 基准截图对比，不得声明 UI 一致。
- 没有 `*-api` 非管理 UI 消费验证，不得声明 API SDK 可复用。
- 没有 `*-admin` 菜单/权限/页面/样式自动集成，不得声明能力包可复用。
- 没有真实后端菜单和权限链路，不得声明菜单复用完成。
- 没有 local/micro/mixed 三模式 E2E，不得声明自由组合部署完成。
