# 开发环境规范

## 1. 定位

- 本文件约束 Mango 本地开发环境、工作区配置和前后端启动方式。
- 本地开发环境的长期规则只放在 `mango-pmo`。
- 配套脚本放在仓库根目录 `scripts/`。

## 2. 工作区配置

- 每个工作区必须使用 `mango workspace init` 初始化本地配置。
- `scripts/dev-workspace.sh` 只保留为历史兼容入口，正式开发、验证和交付命令必须使用 Mango CLI。
- 所有代码、接口、数据库、测试、前端页面或构建配置改动必须在任务专用 Git worktree 中进行。
- 任务 worktree 必须从最新 `main` 新建，并使用独立任务分支。
- 一个任务或一个 PR 在本地只能对应一个开发 worktree。
- 验收返工、Review 修改、CI 修复和 PR 门禁修复必须复用该任务或 PR 的既有 worktree。
- 新建 worktree 前，Agent 必须先执行 `git worktree list`，确认是否已有同一任务分支或 PR 分支对应的 worktree。
- 只有新独立任务、用户明确拆分任务、原 worktree 丢失或用户明确要求重建时，才允许创建新的 worktree。
- 原 worktree 丢失或损坏时，必须基于原任务分支重建 worktree，不得另起无关联分支。
- 主工作区只用于拉取 `main`、创建 worktree、查看状态和执行清理，不承载任务改动。
- 只修改 PMO 规范、流程、Agent 入口、设计文档、Sprint 计划、交付记录或历史材料，且不影响服务代码、接口、数据库、测试、前端页面或构建配置时，可按 preflight 的 `main-direct-allowed` 结果在主工作区直接提交。
- 本地工作区配置文件固定为 `.mango/workspace.json` 和 `.mango/dev-workspace.env`。
- `.mango/workspace.json` 记录当前 worktree 的稳定 slot、端口、数据库名和 workspace id，禁止提交到 Git。
- `.mango/dev-workspace.env` 只属于当前工作区，禁止提交到 Git。
- Mango CLI 应维护本机工作区注册表，避免同一台机器上不同 worktree 分配到相同端口或数据库名。
- Mango CLI 首次初始化时按稳定工作区号 `NNN` 分配端口和数据库名。
- 工作区号 `NNN` 必须同时体现在后端端口、前端端口、子前端端口和数据库名中。
- 后端端口固定为 `18NNN`，前端主端口固定为 `30NNN`，子前端端口固定为 `31NNN`、`32NNN`、`33NNN` 递增表达子应用类型。
- 数据库名固定为 `mango_dev_<projectSlug>_<NNN>`。
- Mango CLI 分配工作区号时必须跳过本机已监听端口、注册表已占用端口、注册表已占用数据库名，以及本机 MySQL 中已存在的目标 `MANGO_DB_NAME`。
- 已存在 `.mango/dev-workspace.env` 时，脚本禁止覆盖本机数据库连接、插件开关等人工配置；但 `MANGO_WORKSPACE_ID`、`MANGO_BACKEND_PORT`、`MANGO_FRONTEND_PORT`、子前端端口和 `MANGO_DB_NAME` 必须同步为 `.mango/workspace.json` 的当前值。
- 同一个工作区必须复用同一组端口和数据库名。
- 不同工作区应使用不同的端口和数据库名。
- 新工作区首次生成 `.mango/dev-workspace.env` 时，禁止选择本机已存在的 `MANGO_DB_NAME`，避免误复用旧库数据。
- 新建 Git worktree 后必须执行 `mango workspace init`。

## 3. 必需配置项

`.mango/workspace.json` 至少包含：

- `workspaceId`：当前 worktree 标识。
- `slot`：当前 worktree 的稳定端口槽位。
- `backendPort`：后端端口。
- `frontendPort`：前端主端口。
- `frontendApps`：前端子应用端口映射。
- `dbName`：当前工作区本地数据库名。

`.mango/dev-workspace.env` 至少包含：

- `MANGO_BACKEND_PORT`：后端本地端口。
- `MANGO_FRONTEND_PORT`：前端本地端口。
- `MANGO_DB_NAME`：当前工作区本地数据库名。
- `MANGO_DB_HOST`：数据库主机。
- `MANGO_DB_PORT`：数据库端口。
- `MANGO_DB_USERNAME`：数据库用户名。
- `MANGO_DB_PASSWORD`：数据库密码。
- `MANGO_OFFICE_PLUGIN_ENABLED`：是否启用本地 Office 转换插件，开发默认 `false`。

## 4. 启动规则

- 默认使用 `mango dev start` 启动本地前后端。
- 只启动后端使用 `mango dev start backend`。
- 只启动前端使用 `mango dev start frontend`。
- 停止当前工作区服务使用 `mango dev stop`。
- 查看当前工作区配置使用 `mango workspace status`。
- 诊断当前工作区使用 `mango dev doctor`。
- 前端 source 模式准备使用 `mango frontend prepare`。
- Mango CLI 必须从 `.mango/workspace.json` 和 `.mango/dev-workspace.env` 读取端口、数据库名和数据库连接信息。
- 开发环境默认关闭 Office 转换插件；需要本地调试文件预览转换时，显式将 `MANGO_OFFICE_PLUGIN_ENABLED=true` 并配置本机 Office 组件。
- Mango CLI 必须在启动前检查端口占用和工作区归属。
- 端口冲突时必须失败并提示占用进程或 owner worktree，禁止静默随机换端口。
- 前端 Vite 端口必须由 Mango CLI 注入；前端 app 不得在 `dev` 脚本中写死本地端口。

## 4.1 Worktree 删除规则

- 原生 `git worktree remove` 不负责停止服务，也不负责删除数据库。
- 删除开发 worktree 必须先使用 `mango dev stop` 停止服务，再使用 `mango workspace release --workspace <path>` 释放本机注册。
- 任务 PR 合并前禁止删除对应 worktree，除非用户明确取消该任务或要求重建 worktree。
- PR 合并后必须清理对应 worktree；需要保留本地开发数据库时显式追加 `--keep-db`。
- 删除 worktree 前必须按目标 worktree 的 `.mango/workspace.json` 和 `.mango/dev-workspace.env` 停止对应端口上的前后端服务。
- `mango workspace release --workspace <path>` 默认删除当前 workspace 拥有的本地开发数据库。
- 只有显式传入 `--keep-db` 时才允许保留数据库。
- 脚本只允许删除 `.mango/workspace.json` 或本机 workspace 注册表记录中归属于目标 worktree 且名称匹配 `mango_dev_*` 的本地工作区数据库。
- 需要强制移除 dirty worktree 时，必须显式传入 `--force`。

## 5. 数据库规则

- 本地开发数据库名必须来自 `MANGO_DB_NAME`。
- `MANGO_DB_AUTO_CREATE=true` 只表示允许 Mango CLI 在启动 Spring Boot app 前创建本地工作区数据库，不表示绕过本机 MySQL 连接前置条件。
- 自动创建数据库依赖本机 `mysql` 命令可用，并且 `MANGO_DB_HOST`、`MANGO_DB_PORT`、`MANGO_DB_USERNAME`、`MANGO_DB_PASSWORD` 能连接到目标 MySQL。
- Mango CLI 只能自动创建名称匹配 `mango_dev_*` 的工作区数据库；数据库名不匹配时必须拒绝启动。
- 自动建库失败必须在 CLI 输出和应用日志中暴露具体失败原因，禁止继续静默等待 health 超时。
- 释放 workspace 时默认删除当前 workspace 拥有的本地开发数据库；保留数据库必须显式传入 `--keep-db`。
- 删除数据库必须执行受控清理命令，且只能删除目标 worktree 记录的、名称匹配 `mango_dev_*` 的本地工作区数据库。
- 数据库结构和初始数据仍由 Flyway migration 管理。
- 禁止用本地启动脚本替代正式 migration。
- 禁止把本地数据库名写死到应用配置或代码中。

## 6. 运行时临时目录

- 仓库内运行时临时文件统一放到 `<repo>/.runtime/`。
- `.runtime/` 必须被 Git 忽略，禁止提交。
- 测试 `mango-cli` 生成的新项目必须放到 `.runtime/projects/`。
- 临时包缓存必须放到 `.runtime/package-store/`。
- 临时启动日志、浏览器脚本中间产物、下载文件和非证据截图必须放到 `.runtime/` 下的子目录。
- `mango-docs/evidence/` 只保存最终验收截图、报告和可复核脚本，禁止放生成项目、依赖缓存、构建产物和运行日志。
- 任务结束后应清理 `.runtime/` 中与本次任务相关且不再需要的内容。

## 7. Agent 执行要求

- 需要启动本地服务时，Agent 必须使用 `mango dev start`、`mango dev start backend` 或 `mango dev start frontend`。
- 需要修复验收、Review、CI 或 PR 门禁发现的问题时，Agent 必须先复用当前任务或 PR 的既有 worktree。
- 需要删除本地开发 worktree 时，Agent 必须先停止服务并释放 workspace 注册。
- 启动前必须说明本次使用的后端端口、前端端口和数据库名。
- 启动失败时必须先检查 `.mango/workspace.json`、`.mango/dev-workspace.env`、端口归属、数据库连接和启动日志。
- 只有调试脚本本身或用户明确要求时，才允许绕过该脚本手写启动命令。

## 8. 破坏性升级说明

- 从本规范生效起，正式本地开发入口从 `scripts/dev-workspace.sh` 切换为 Mango CLI。
- 旧脚本只作为兼容 shim，不再维护端口分配、数据库分配、前端准备或进程归属逻辑。
- 业务项目升级后必须执行 `mango workspace init` 生成 `.mango/workspace.json`；旧 `.mango/dev-workspace.env` 会被 CLI 补齐缺失字段。
- 旧的 `~/.mango/workspaces.tsv` 注册表不再作为正式数据源；Mango CLI 使用 `~/.mango/workspaces.json`。
- 如果旧 worktree 已占用端口，新的 CLI 启动会失败并提示 owner，开发者必须停止旧 worktree 或释放注册后重试。
