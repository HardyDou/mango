# Mango Business Starter 可启动模板 Sprint D

## 1. 背景

Sprint A-C 已完成前端发布物契约、Admin Shell 扩展点和内置能力包组合。下一步需要让 `mango-business-starter` 生成后的业务项目具备可安装、可类型检查、可构建的前端工作区，并保留单体、微前端和混合部署的运行时配置入口。

## 2. 目标

业务项目生成后，不复制 Mango app 源码，只依赖 Mango 发布包和业务本地包即可启动 admin app。模板必须自带 pnpm workspace、前端脚本、环境变量、运行时配置、基础能力注册和业务页面注册。

## 3. 范围

- 为 starter 增加根级 `package.json`、`pnpm-workspace.yaml`、`tsconfig`、`.npmrc`、`.gitignore`。
- 为 admin app 增加环境变量、runtime config、菜单 fallback 和类型检查脚本。
- 将 admin app 启动逻辑改为注册 Mango 默认能力、注册业务页面，并通过 runtime config 描述本地/微前端部署。
- 增加模板验证脚本覆盖新增可启动资产。
- 同步更新 create-mango-app 内置 starter 模板。

## 4. 不做什么

- 不在本 Sprint 实现真实 npm registry 发布。
- 不声明生成项目已经完成真实登录、菜单权限、数据库初始化和浏览器业务链路验收。
- 不改后端模块真实业务逻辑。
- 不实现 CLI 能力选择；能力选择归 Sprint E。

## 5. 设计说明

### 5.1 影响模块

- `mango-business-starter`
- `mango-ui/packages/create-mango-app/templates/mango-business-starter`
- `mango-ui/packages/create-mango-app/src/index.mjs`
- `mango-docs/plans`

### 5.2 接口变化

- 无后端 HTTP API 变化。
- starter 前端增加 `src/runtimeConfig.ts` 和 `src/starterMenus.ts`，作为生成项目内的公开启动资产。
- create-mango-app 生成配置增加前端模式信息。

### 5.3 数据变化

无数据库结构变化。模板继续通过业务模块 Flyway migration 和 resource manifest 表达初始化数据来源。

### 5.4 测试范围

- 模板自检：新增文件、脚本、runtime config、菜单 fallback、workspace 和 starter 包边界。
- CLI 自检：生成项目后运行模板自检和 baseline preflight。
- 生成项目前端 smoke：在临时项目中安装依赖、执行 typecheck、build。
- 回归测试：复跑 Sprint A/B/C 关键检查。

## 6. 完成标准

- `node mango-business-starter/scripts/check-template.mjs` 通过。
- create-mango-app 生成项目后模板检查通过。
- 临时生成项目 `pnpm install`、`pnpm typecheck`、`pnpm build` 通过。
- Sprint A/B/C 回归检查通过。
- 交付台账无未完成项；真实 registry、真实后端联调和浏览器验收未覆盖项必须记录为风险。

## 7. 风险与限制

- 本地验证使用 workspace/link 方式消费本仓 Mango 包，不等同于真实 npm registry。
- 生成项目的 starter menus 只作为后端菜单接口未就绪时的开发降级入口，不替代后端授权菜单。
- 完整浏览器登录、权限不足、CRUD 和真实数据链路留到 Sprint F 端到端验收。
