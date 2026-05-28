# 开发环境稳定启动规范与脚本交付契约

## 1. 目标

为 Mango 本地开发提供统一规范和脚本，确保每个工作区拥有稳定的后端端口、前端端口和数据库名，并可一条命令启动前后端。

## 2. 范围

- 新增 PMO 开发环境规范。
- 新增根目录启动脚本。
- 新增 Git hook，使新 worktree checkout 后自动初始化本地 env。
- 脚本首次生成 `.mango/dev-workspace.env`，后续复用。
- 脚本维护本机工作区注册表，避免不同工作区重复分配端口或数据库名。
- 脚本首次生成 env 时避开本机已存在数据库名，避免重建同路径 worktree 时误复用旧库。
- 脚本读取本地 env 启动单体后端和 `mango-admin` 前端。
- 脚本在启动前检查端口占用，并按本地数据库名覆盖后端数据源。
- 开发环境默认关闭 Office 转换插件，避免未安装 Office 组件导致本地启动失败。
- 新 worktree 首次启动前端时，如果缺少 `node_modules`，默认自动执行 `pnpm install --frozen-lockfile`。
- 新增受控 worktree 删除命令，删除前停止目标 worktree 的前后端服务。
- 数据库默认保留，只有显式 `--drop-db` 时删除本地工作区库。

## 3. 不做什么

- 不修改生产环境配置。
- 不修改应用默认 `application.yml`。
- 不修改前端默认 `.env.development`。
- 不提交 `.mango/dev-workspace.env`。
- 不替代 Flyway migration。
- 不让原生 `git worktree remove` 自动删除数据库。
- 不默认删除数据库。

## 4. 设计输入

- 用户要求：在 PMO 或合适位置追加开发环境规范；提供 shell 脚本启动前后端；每个工作区初始化独立后端端口、前端端口、数据库名称；同一工作区保持不变。
- PMO 规范：`mango-pmo/rules/00-dev-flow.md`、`mango-pmo/rules/01-delivery-contract.md`、`mango-pmo/agents/05-pmo-agent.md`。
- 现有启动入口：`mango/mango-app/monolith/mango-monolith-app`、`mango-ui/apps/mango-admin`。

## 5. 设计说明

### 5.1 影响模块

- `mango-pmo`：新增开发环境规范并接入规则索引。
- `.githooks`：新增 checkout hook，用于新 worktree 自动初始化。
- `scripts`：新增工作区启动、停止、hook 安装和 worktree 删除脚本。
- `mango-docs/plans`：记录本次交付契约和台账。

### 5.2 接口变化

无业务 API 变化。

### 5.3 数据变化

无业务表结构变化。脚本会生成本地忽略文件 `.mango/dev-workspace.env`，并可按 `MANGO_DB_NAME` 创建本地开发库；脚本还会维护本机工作区注册表，默认路径为 `$HOME/.mango/workspaces.tsv`。删除 worktree 时默认保留数据库，显式 `--drop-db` 时只允许删除 `mango_dev_*` 库。首次生成 env 时，如果 MySQL 中已存在候选库名，脚本会换下一个候选。脚本默认写入 `MANGO_OFFICE_PLUGIN_ENABLED=false` 和 `MANGO_FRONTEND_AUTO_INSTALL=true`。

### 5.4 菜单/页面/权限变化

无菜单、页面、权限变化。

### 5.5 测试范围

- shell 语法检查。
- 工作区 env 首次初始化。
- 脚本配置打印。
- Git hooksPath 配置。
- 真实临时 worktree 新建后自动初始化。
- 受控 worktree 删除命令执行停止服务流程并移除 worktree。
- PMO preflight 命中开发环境规范。
- 交付台账验证。

## 6. 风险与限制

- 脚本默认使用本机 MySQL；如果本机没有 `mysql` 客户端，自动建库会跳过，需要先手动创建数据库。
- 后端实际启动依赖本机 Maven、JDK、MySQL 以及项目当前可编译状态。
- 前端首次启动依赖本机可用 `pnpm` 和可访问的依赖缓存或仓库。
- 端口冲突时脚本按规范失败，需要修改 `.mango/dev-workspace.env` 后重试。
- Git 没有原生 worktree 删除 hook，直接执行 `git worktree remove` 不会自动停服务或删库；必须使用脚本提供的 `worktree-remove` 命令。
