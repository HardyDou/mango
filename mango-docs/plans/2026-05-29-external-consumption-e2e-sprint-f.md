# 外部消费端到端验收 Sprint F

## 1. 背景

Sprint A-E 已完成前端发布物契约、Admin Shell 扩展点、内置能力包组合、可启动业务 starter 和 CLI 能力选择。Sprint F 用干净生成项目验证业务方是否可以通过 Mango 初始化入口快速开始前端系统开发。

## 2. 目标

使用 `create-mango-app` 生成临时业务项目，验证生成项目不复制 Mango app 源码，能够安装依赖、类型检查、生产构建、启动前端并通过浏览器冒烟。真实 npm registry、后端可运行 app 和真实登录权限链路必须单独验收，无法证明时不得声明通过。

## 3. 范围

- 通过 CLI 生成业务项目，启用 `system,rbac,workflow,notice,file` 能力，前端模式使用 `mixed`。
- 生成项目执行模板检查、baseline preflight、依赖安装、类型检查、生产构建。
- 启动生成项目 admin app，浏览器验证 shell 可加载、业务本地页面 registry 可访问、微前端远程入口缺失时有诊断。
- 回归执行 starter、CLI、包契约、Admin Pages/Admin Shell 单测和核心包构建。
- 记录无法在当前模板证明的后端 app、真实登录、真实菜单权限、真实 npm registry 消费风险。

## 4. 不做什么

- 不把本地 `link:` 验证声明为真实 npm registry 发布验收。
- 不用 starter menu fallback 替代后端授权菜单验收。
- 不用前端页面可打开替代真实登录、权限、租户、菜单和 CRUD 链路。
- 不在本 Sprint 新增后端业务 app 模板或数据库初始化能力。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/create-mango-app`
- `mango-business-starter`
- `mango-docs/plans`

### 5.2 接口变化

无新的后端 HTTP API 或 CLI 参数变化。Sprint F 只验证 Sprint A-E 已交付的初始化、能力选择、部署模式和运行时配置链路。

### 5.3 数据变化

无数据库结构变化。后端真实菜单、权限、租户和用户数据初始化未在当前 starter 中形成可运行 app，作为验收例外记录。

### 5.4 测试范围

- 新特性测试：生成干净业务项目，验证 feature/frontend-mode 渲染、模板检查、baseline preflight、install、typecheck、build、前端启动和浏览器冒烟。
- 回归测试：复跑 starter 模板检查、CLI 自测、前端包契约、Admin Pages/Admin Shell 单测和核心包构建。
- 例外测试：检查当前生成项目是否具备 Maven 根 POM、后端 app 启动脚本和真实后端登录链路；不存在时记录为 `EXCEPTION`。

## 6. 完成标准

- 可证明项均已执行并记录命令和结果。
- 不可证明项在台账中标记 `EXCEPTION`，证据写清楚，不声明 Sprint F 完整完成。
- 交付台账检查通过。

## 7. 风险与限制

- 当前外部消费安装仍依赖本地 Mango 包覆盖或 link 物料，不等同于真实 npm registry pack/install。
- 当前 starter 只有业务模块分层模板，没有可直接 `mvn test` 的生成项目根 POM 和后端 app。
- 当前浏览器冒烟只能证明前端 shell、菜单 fallback、页面 registry 和微前端诊断；不能证明真实后端登录、授权菜单、租户和 CRUD 链路。
