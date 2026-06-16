# 开发环境规范

## 1. 定位

- 本文件约束 Mango 本地开发环境、工作区配置和前后端启动方式。
- 本地开发环境的长期规则只放在 `mango-pmo`。
- 配套脚本放在仓库根目录 `scripts/`。

## 2. 工作区配置

- 每个 clone 首次使用必须执行 `scripts/dev-workspace.sh install-hooks`。
- 每个工作区必须使用 `scripts/dev-workspace.sh init` 初始化本地配置。
- 所有代码、接口、数据库、测试、前端页面或构建配置改动必须在任务专用 Git worktree 中进行。
- 任务 worktree 必须从最新 `main` 新建，并使用独立任务分支。
- 一个任务或一个 PR 在本地只能对应一个开发 worktree。
- 验收返工、Review 修改、CI 修复和 PR 门禁修复必须复用该任务或 PR 的既有 worktree。
- 新建 worktree 前，Agent 必须先执行 `git worktree list`，确认是否已有同一任务分支或 PR 分支对应的 worktree。
- 只有新独立任务、用户明确拆分任务、原 worktree 丢失或用户明确要求重建时，才允许创建新的 worktree。
- 原 worktree 丢失或损坏时，必须基于原任务分支重建 worktree，不得另起无关联分支。
- 主工作区只用于拉取 `main`、创建 worktree、查看状态和执行清理，不承载任务改动。
- 只修改 PMO 规范、流程、Agent 入口、设计文档、Sprint 计划、交付记录或历史材料，且不影响服务代码、接口、数据库、测试、前端页面或构建配置时，可按 preflight 的 `main-direct-allowed` 结果在主工作区直接提交。
- 本地配置文件固定为 `.mango/dev-workspace.env`。
- `.mango/dev-workspace.env` 只属于当前工作区，禁止提交到 Git。
- 脚本应维护本机工作区注册表，避免同一台机器上不同工作区分配到相同端口或数据库名。
- 脚本首次初始化时按工作区绝对路径生成默认值。
- 已存在 `.mango/dev-workspace.env` 时，脚本禁止覆盖。
- 同一个工作区必须复用同一组端口和数据库名。
- 不同工作区应使用不同的端口和数据库名。
- 新工作区首次生成 `.mango/dev-workspace.env` 时，禁止选择本机已存在的 `MANGO_DB_NAME`，避免误复用旧库数据。
- 新建 Git worktree 时，`post-checkout` hook 必须自动执行 `scripts/dev-workspace.sh init`。

## 3. 必需配置项

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

- 默认使用 `scripts/dev-workspace.sh start` 启动本地前后端。
- 只启动后端使用 `scripts/dev-workspace.sh backend`。
- 只启动前端使用 `scripts/dev-workspace.sh frontend`。
- 停止当前工作区服务使用 `scripts/dev-workspace.sh stop`。
- 查看当前工作区配置使用 `scripts/dev-workspace.sh print`。
- 启动脚本必须从 `.mango/dev-workspace.env` 读取端口、数据库名和数据库连接信息。
- 开发环境默认关闭 Office 转换插件；需要本地调试文件预览转换时，显式将 `MANGO_OFFICE_PLUGIN_ENABLED=true` 并配置本机 Office 组件。
- 启动脚本必须在启动前检查端口占用。
- 端口冲突时必须失败并提示修改 `.mango/dev-workspace.env`，禁止静默随机换端口。

## 4.1 Worktree 删除规则

- 原生 `git worktree remove` 不负责停止服务，也不负责删除数据库。
- 删除开发 worktree 必须使用 `scripts/dev-workspace.sh worktree-remove <path>`。
- 任务 PR 合并前禁止删除对应 worktree，除非用户明确取消该任务或要求重建 worktree。
- PR 合并后必须清理对应 worktree；需要删除本地开发数据库时显式追加 `--drop-db`。
- `worktree-remove` 必须先按目标 worktree 的 `.mango/dev-workspace.env` 停止对应端口上的前后端服务。
- `worktree-remove` 默认保留数据库。
- 只有显式传入 `--drop-db` 时才允许删除数据库。
- 脚本只允许删除名称匹配 `mango_dev_*` 的本地工作区数据库。
- 需要强制移除 dirty worktree 时，必须显式传入 `--force`。

## 5. 数据库规则

- 本地开发数据库名必须来自 `MANGO_DB_NAME`。
- 脚本可以在 `MANGO_DB_AUTO_CREATE=true` 时创建本地数据库。
- 删除 worktree 时默认不删除数据库。
- 删除数据库必须显式使用 `scripts/dev-workspace.sh worktree-remove <path> --drop-db`。
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

- 需要启动本地服务时，Agent 必须优先使用 `scripts/dev-workspace.sh`。
- 需要修复验收、Review、CI 或 PR 门禁发现的问题时，Agent 必须先复用当前任务或 PR 的既有 worktree。
- 需要删除本地开发 worktree 时，Agent 必须优先使用 `scripts/dev-workspace.sh worktree-remove`。
- 启动前必须说明本次使用的后端端口、前端端口和数据库名。
- 启动失败时必须先检查 `.mango/dev-workspace.env`、端口占用、数据库连接和启动日志。
- 只有调试脚本本身或用户明确要求时，才允许绕过该脚本手写启动命令。
