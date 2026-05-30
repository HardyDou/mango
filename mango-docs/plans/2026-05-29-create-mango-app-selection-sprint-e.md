# create-mango-app 能力选择 Sprint E

## 1. 背景

Sprint D 已让 starter 生成项目具备前端 workspace、运行时配置和可构建 admin app。下一步需要让 `create-mango-app` 在初始化阶段表达业务项目要启用哪些 Mango 能力，以及前端采用本地、微前端或混合部署模式。

## 2. 目标

CLI 支持 `--features` 和 `--frontend-mode`，生成项目的依赖、默认能力注册、runtime config、环境变量和 `mango.config.json` 与选择一致。生成后提示包含自检命令。

## 3. 范围

- `create-mango-app` 增加 features 和 frontend-mode 参数解析。
- starter 模板增加按能力渲染的依赖、能力注册、runtime modules 和 env。
- CLI 自测覆盖能力选择、部署模式选择、变量渲染和生成后提示。
- 生成项目 smoke 执行 install、typecheck、build。

## 4. 不做什么

- 不实现 Web Initializr。
- 不在本 Sprint 执行真实 npm registry 发布。
- 不做浏览器端到端登录和真实权限链路验收。

## 5. 设计说明

### 5.1 影响模块

- `mango-ui/packages/create-mango-app/src/index.mjs`
- `mango-ui/packages/create-mango-app/scripts/check-cli.mjs`
- `mango-business-starter`
- `mango-ui/packages/create-mango-app/templates/mango-business-starter`

### 5.2 接口变化

- CLI 新增：
  - `--features <list>`：逗号分隔，支持 `base,system,rbac,workflow,notice,file,template,numgen,calendar`。
  - `--frontend-mode <mode>`：支持 `local`、`micro`、`mixed`。

### 5.3 数据变化

无数据库变化。能力选择只影响前端依赖和运行配置，后端菜单、权限、租户数据仍由后端 resource manifest 和真实授权链路负责。

### 5.4 测试范围

- CLI 自测：生成 micro/mixed/local 不同配置并检查文件内容。
- 生成项目 smoke：安装依赖、类型检查、生产构建。
- 回归：复跑 starter 模板自检、包契约、Admin Shell/Admin Pages 单测。

## 6. 完成标准

- CLI 参数解析和生成配置覆盖 features/frontend-mode。
- 生成项目能按选择注册默认能力、业务页面和 runtime modules。
- 新特性测试与回归测试全部通过。
- 交付台账无未完成项。

## 7. 风险与限制

- `base` 默认包含认证和权限基础能力，后续可在 Web Initializr 里提供更细粒度说明。
- `micro`/`mixed` 只生成运行时配置，不启动远程微前端服务。
- 真实 registry、浏览器登录和权限链路仍归 Sprint F 验收。
