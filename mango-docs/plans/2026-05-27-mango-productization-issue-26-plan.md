# Mango 产品化 Issue #26 交叉方案

## 1. 目标

把 GitHub Issue #26 的产品化诉求与上一轮发布物阻断修复台账中的 `EXCEPTION 4` 合并为一个正式产品化路线图，明确范围、边界、依赖顺序、验收口径和后续 Sprint 拆分。

本方案的直接目标不是一次性实现所有产品化能力，而是消除“历史例外项没有归属”的状态，让这些能力进入可开发、可验证、可评审的产品化计划。

## 2. 范围

- 逐项映射 Issue #26 的 15 项要求。
- 逐项映射上一轮台账 `EXT-005` 到 `EXT-008` 的 4 个例外项。
- 明确已经由 PR #27 覆盖的基线能力。
- 明确产品化能力的模块边界、依赖方向、接口变化、数据变化、菜单/页面/权限变化和测试范围。
- 拆分后续可执行 Sprint，并给出每个 Sprint 的完成标准。
- 明确 Issue #26 的更新和关闭口径。

## 3. 不做什么

- 本轮不修改历史发布物阻断修复台账。该台账描述的是 PR #27 的当轮范围，历史结论保持有效。
- 本轮不直接实现 `@mango/admin-shell` 产品化包。
- 本轮不直接实现 Mango Initializr。
- 本轮产品化总方案不一次性实现初始化种子数据和业务模块模板生成器；菜单资源同步按 Sprint 2 独立交付。
- 本轮不直接修复 CodeEditor 和构建 warning。

## 4. 设计输入

- GitHub Issue #26：`产品化`。
- GitHub Issue #26 评论中的 4 个上一轮例外项。
- `mango-docs/plans/2026-05-27-mango-external-artifact-consumption-fixes.md`。
- `mango-docs/plans/2026-05-27-mango-external-artifact-consumption-fixes-ledger.md`。
- `mango-docs/plans/2026-05-27-business-project-development-model-review.md`。
- `mango-docs/plans/2026-05-27-business-project-development-model-review-record.md`。
- `mango-pmo/rules/00-dev-flow.md`。
- `mango-pmo/rules/01-delivery-contract.md`。
- `mango-pmo/rules/backend/03-api.md`。
- `mango-pmo/rules/backend/05-module.md`。
- `mango-pmo/rules/backend/08-test.md`。
- `mango-pmo/rules/frontend/01-vue-code.md`。
- `mango-pmo/rules/frontend/06-monorepo-architecture.md`。
- 当前代码事实：后端已有 `mango-authorization-resource-sync-starter`、`R<T>`；前端已有 `apps/mango-admin-shell` 应用和 `@mango/admin-pages` 稳定导出基线。

## 5. 交叉结论

`EXCEPTION 4` 不是遗漏项，也不是永久不做项。它们是 PR #27 阻断修复 Sprint 的范围外能力，现在应归并到 Issue #26 产品化主线中。

| 历史例外 | 对应 Issue #26 | 结论 |
|---|---|---|
| `EXT-005`：产品化 `@mango/admin-shell` | #2、#12、#13、#14 | 进入产品化 Sprint，作为前端壳包与模块注册体系交付 |
| `EXT-006`：Mango Initializr | #15 | 进入产品化 Sprint，但依赖模板、admin starter、admin shell、资源同步能力 |
| `EXT-007`：可选初始化数据能力 | Issue #26 未显式列出，和 #1、#5、#7、#15 交叉 | 作为产品化缺口补入 Issue #26 方案，不再散落在历史例外中 |
| `EXT-008`：业务模块菜单/权限/资源同步规范 | #5、#6、#7、#9、#13 | 进入产品化 Sprint，作为后端资源 manifest、前端 page registry 和生成器的共同契约 |

## 6. Issue #26 覆盖矩阵

| Issue 项 | 要求 | 本方案处理 |
|---|---|---|
| #1 | 提供 `mango-admin-starter` 聚合后端 starter | 拆为 Sprint 3，先做依赖白名单和聚合边界，再提供 starter |
| #2 | 发布可复用前端壳 `@mango/admin-shell` | 拆为 Sprint 4，先从 `apps/mango-admin-shell` 抽出包，再让 app 反向消费包 |
| #3 | 修复 `@mango/admin-pages` 公开 API | 已由 PR #27 完成，作为基线，不重复实现 |
| #4 | 修复 `@mango/common` CodeEditor 白屏 | 拆为 Sprint 1，作为独立前端质量修复 |
| #5 | 模块菜单自动注册机制 | 拆为 Sprint 2，进入资源 manifest 和同步机制 |
| #6 | 统一菜单 `component` 规范 | 拆为 Sprint 2，作为后端菜单和前端 page registry 的共同契约 |
| #7 | 菜单初始化/同步命令 | 拆为 Sprint 2，先提供应用启动同步，再评估 Maven goal 或独立 CLI |
| #8 | 明确标准响应契约 `R<T>` | 现有 PMO 已要求，Sprint 5 在业务模板和生成器中固化 |
| #9 | 提供业务模块模板 | 拆为 Sprint 5，生成后端分层和前端页面包 |
| #10 | 提供微单体/微服务双模式模板 | 拆为 Sprint 5，和业务模块模板一起交付 |
| #11 | 统一业务请求封装 | 拆为 Sprint 5，在前端模板 API client 中固化 `@mango/common` 请求工具 |
| #12 | 提供 admin app 配置入口 | 拆为 Sprint 4，作为 `createMangoAdminApp` 核心 API |
| #13 | 模块前端 npm 包规范化 | 拆为 Sprint 4 和 Sprint 5，先定义注册 API，再生成模块包 |
| #14 | 解决构建 warning | 拆为 Sprint 1，和注册方式收敛相关 |
| #15 | 做 Mango Initializr | 拆为 Sprint 6，依赖前置产品化资产 |

## 7. 总体设计决策

### 7.1 产品化分层

Mango 产品化能力按四层组织：

```text
业务项目
  依赖 Maven starter / npm package / PMO baseline / Initializr 输出

产品化入口
  Mango Initializr
  business template
  mango add module/page/api/contract

可复用框架资产
  mango-admin-starter
  @mango/admin-shell
  @mango/admin-pages
  @mango/common
  业务模块模板

平台运行契约
  R<T>
  module.properties
  META-INF/mango/resource-manifest.json
  META-INF/mango/resource-manifests/*.json
  page registry key
  seed data manifest
```

### 7.2 后端边界

- `mango-admin-starter` 只做后台基础能力聚合，不承载业务实现。
- 聚合 starter 只依赖可对外装配的 starter，不依赖 `core`。
- 菜单、权限、种子数据的同步能力归属 `mango-authorization` 和对应平台模块的 starter 扩展点。
- 业务模块通过 `META-INF/mango/resource-manifest.json` 或 `META-INF/mango/resource-manifests/*.json` 声明资源，不直接写平台表。
- 对外业务 API 继续使用 `R<T>`、`Command`、`Query`、`VO`。

### 7.3 前端边界

- `@mango/admin-shell` 是 npm 包，不是 app 私有源码复制模板。
- `apps/mango-admin-shell` 保留为运行和验证用应用，必须依赖 `@mango/admin-shell`，不能继续作为业务项目复制源。
- `@mango/admin-pages` 继续只负责页面注册和 Mango 内置页面集合，不承载 shell 布局、store、router、i18n、theme。
- 业务模块前端包必须导出页面注册函数、API client、类型和可选 remote entry。
- 后端菜单 `component` 字段必须等于前端 page registry key。

### 7.4 初始化数据边界

- 初始化数据不是业务项目手写 SQL 的责任。
- 默认租户、`internal-admin` 应用、默认账号、角色、菜单、菜单套餐和模块运行策略必须由 Mango 提供可选 seed 能力。
- seed 能力必须具备幂等性、租户边界、环境开关和安全默认值。
- 默认口令只能用于本地开发模板，生产模板必须要求首次启动改密或禁用默认口令。

### 7.5 Initializr 边界

- Initializr 不生成框架源码副本。
- Initializr 只生成业务项目结构、依赖声明、PMO 入口、环境配置、启动脚本、示例业务模块和验证脚本。
- Initializr 的能力选择必须对应真实已发布 Maven/npm 资产。
- Initializr 必须支持 monolith 和 microservice 两种拓扑。

## 8. Sprint 拆分

### Sprint 0：产品化交叉设计

目标：完成 Issue #26 与 `EXCEPTION 4` 的合并、范围界定和后续 Sprint 拆分。

交付物：

- 本方案文档。
- 本方案交付台账。
- Issue #26 更新口径。

完成标准：

- 交付台账检查通过。
- 每个 Issue #26 条目都有归属。
- 每个历史例外项都有归属。

### Sprint 1：前端质量与构建阻断收敛

目标：先清理不依赖大架构的前端阻断点。

交付物：

- 修复 `@mango/common` CodeEditor 在 Vite dev 下的白屏问题。
- 解决 `@mango/workflow-business-example` 静态与动态导入混用导致的构建 warning。
- 补充对应组件测试和构建验证。

完成标准：

- `pnpm -C mango-ui test` 覆盖 CodeEditor 回归。
- `pnpm -C mango-ui build` 无相关构建 warning。
- 开发态和生产构建态都可访问相关页面。

### Sprint 2：资源 manifest 与菜单权限同步

目标：建立业务模块资源同步的后端和前端共同契约。

交付物：

- `META-INF/mango/resource-manifest.json` 统一清单契约。
- `META-INF/mango/resource-manifests/*.json` 多清单扩展路径。
- 菜单 `component` 与前端 page registry key 约定。
- 应用启动资源同步能力。
- `mode=read/write`、幂等覆盖、状态、排序和按钮权限绑定策略。
- 资源同步测试。

完成标准：

- 业务 starter 引入后可自动注册菜单和按钮权限。
- 重复启动保持幂等。
- 前端 page registry key 与后端菜单 `component` 可校验。
- 同步结果可审计。

### Sprint 3：`mango-admin-starter`

目标：提供后端后台基础能力聚合入口。

交付物：

- `io.mango:mango-admin-starter`。
- 聚合依赖白名单。
- 默认自动配置边界。
- 与 monolith app 的装配示例。
- starter 依赖检查。

完成标准：

- 外部业务项目只依赖 `mango-admin-starter` 即可获得后台基础能力。
- 聚合 starter 不覆盖宿主 `server.*` 等 Spring Boot 通用配置。
- 聚合 starter 不依赖任何平台 `core` 模块。

### Sprint 4：`@mango/admin-shell`

目标：把前端后台壳从 app 私有实现产品化为 npm 包。

交付物：

- `@mango/admin-shell` 包。
- `createMangoAdminApp({ apiBaseUrl, login, modules, localMenus, title })`。
- router、layout、stores、i18n、directive、menuLoader、theme、Vite 约定。
- `apps/mango-admin-shell` 改为消费 `@mango/admin-shell`。
- 壳包构建、类型导出和 E2E 验证。

完成标准：

- 外部业务前端无需复制 `apps/mango-admin-shell/src`。
- 业务项目可通过 npm 包创建后台应用。
- 内置页面、业务页面和远程页面都通过统一注册接口进入 shell。

### Sprint 5：业务模块模板与双模式拓扑

目标：把业务模块开发规范落到可生成模板。

交付物：

- 后端 `xxx-api/core/starter/starter-remote` 模板。
- 前端 `xxx-ui/pages/api-client/types` 模板。
- monolith 和 microservice 两种部署拓扑模板。
- `@mango/common` 请求封装模板。
- `R<T>` 响应契约校验示例。
- 契约文档和交付台账模板。

完成标准：

- 新业务模块生成后可在单体模式运行。
- 新业务模块生成后可在微服务模式运行。
- 前后端接口、菜单、权限、页面注册和测试物料齐全。

### Sprint 6：Mango Initializr

目标：提供类似 Spring Initializr / Vue init 的新项目启动体验。

交付物：

- `npm create mango-business@latest`。
- `mango init <project>`。
- 项目名、包名、端口、数据库、部署模式、模块选择、Nexus 配置、worktree 规范、启动脚本。
- 可选业务模块模板。
- 初始化后冒烟验证。

完成标准：

- clone 或 init 后只改少量配置即可启动。
- 生成项目不包含 Mango 源码副本。
- 生成项目可执行后端测试、前端构建和浏览器冒烟验证。

## 9. Issue #26 更新口径

Issue #26 不应在 Sprint 0 后关闭。建议在 Issue #26 中补充产品化路线图评论，并创建子任务或后续 issue 追踪 Sprint 1 到 Sprint 6。

关闭条件：

- #1 到 #15 全部有代码或模板交付并验证。
- 历史 `EXCEPTION 4` 全部在新台账中转为 `DONE`。
- 外部业务项目可以通过 Maven/npm/Initializr 完成一次真实启动。

## 10. 接口变化

本轮无代码接口变化。

后续 Sprint 预期接口：

- `createMangoAdminApp(options)` 前端启动 API。
- 业务模块页面注册 API。
- 菜单权限资源 manifest schema。
- Initializr CLI 参数和配置文件 schema。

## 11. 数据变化

本轮无数据库变化。

后续 Sprint 预期数据变化：

- 菜单、权限、角色、菜单套餐、应用和模块运行策略的 seed 数据。
- 资源同步审计或同步状态记录，是否建表由 Sprint 2 设计决定。

## 12. 菜单/页面/权限变化

本轮无运行态菜单、页面、权限变化。

后续 Sprint 预期变化：

- 后端菜单 `component` 与前端 page registry key 建立强约束。
- 业务 starter 可声明菜单和权限资源。
- 前端模块包可声明页面注册函数。

## 13. 测试范围

本轮验证范围：

- PMO 交付台账结构检查。
- Issue #26 与历史例外项覆盖检查。

后续 Sprint 测试范围：

- 后端 starter 单元测试、集成测试和依赖边界检查。
- 前端包构建、单元测试、E2E。
- Initializr 生成项目的启动、构建和冒烟验证。
- 外部业务项目依赖 Mango 发布物的真实构建验证。

## 14. 风险与限制

- `@mango/admin-shell`、Initializr、资源同步和模板生成器是多个工程能力，不能在一个无设计 Sprint 中一次性交付。
- 种子数据涉及账号、权限和租户安全，不能以简单 SQL 文件替代正式 seed 能力。
- Initializr 必须等可复用发布物稳定后再交付，否则会生成依赖复制源码的项目。
- 单仓业务模板需要 CODEOWNERS 和 PMO preflight 配套，否则 AI 协作仍会出现 owner 边界不清。
