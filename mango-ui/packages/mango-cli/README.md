# @mango/cli

## 1. 概览
`@mango/cli` 是 Mango 面向业务项目的命令行入口，用来生成业务项目、追加 Mango 可选能力、生成业务模块骨架、同步 PMO baseline，并编排本地后端和前端开发进程。

代码入口和发布事实：

| 项目 | 值 |
|------|----|
| NPM 包 | `@mango/cli` |
| 当前版本 | `1.0.36` |
| bin 命令 | `mango`、`mango-cli` |
| 命令入口 | `src/index.mjs` |
| 发布 registry | [npm-hosted](http://nexus.inner.yunxinbaokeji.com/repository/npm-hosted/) |
| 使用 registry | [npm-group](http://nexus.inner.yunxinbaokeji.com/repository/npm-group/) |
| 随包发布文件 | `src`、`templates`、`admin-modules.json`、`release-versions.json`、`CHANGELOG.md`、`README.md`、`package.json` |

## 2. 功能清单

| 能力 | 命令入口 | 改动范围 |
|------|----------|----------|
| 新建 Mango 业务项目 | `mango init <project> --preset full`、`mango init <project> --preset custom` | 新项目目录 |
| custom 项目追加 Mango 可选能力 | `mango add file workflow --project-dir <dir>` | 前端依赖、页面注册、runtime config、后端 POM、`mango.config.json` |
| 生成业务模块骨架 | `mango module add order --aggregate sales-order --project-dir <dir>` | `backend/modules`、`frontend/packages`、POM、Flyway 模块开关、业务配置 |
| 检查和同步业务 PMO baseline | `mango pmo status`、`mango pmo check`、`mango pmo sync`、`mango pmo upgrade` | `business-pmo`、部分 `business-docs`、`AGENTS.md`、兼容脚本 |
| 初始化和启动本地开发工作区 | `mango init-dev`、`mango validate`、`mango doctor`、`mango plan`、`mango start` | `.mango/dev-workspace.env`、`.mango/run` |
| 查看发布说明 | `mango changelog` | 不改文件 |

## 3. 能力边界
- 不作为业务前端运行时依赖安装到 `dependencies`；业务前端使用 `@mango/admin`、`@mango/file` 等运行时包。
- 不替代 Maven、NPM、Vite、数据库迁移和浏览器验收；CLI 只做模板生成、静态契约校验和本地进程编排。
- 不在 full preset 项目里追加 Mango 可选能力；full preset 已包含全部可选能力，`mango add` 会拒绝执行。
- 不负责生产部署。`mango start` 面向本地开发，生产运行应使用业务项目自己的部署脚本、镜像或进程管理。
- 不自动覆盖业务已经接管的 PMO 文档、`AGENTS.md`、`mango.dev.json` 和业务代码。

## 4. 模块入口
CLI 负责：

- 从 `templates/full` 渲染业务项目。
- 根据 `release-versions.json` 锁定 Mango Maven 和 NPM 包版本。
- 根据随包发布的 `admin-modules.json`、preset 和 module code 生成前端依赖、页面注册、样式入口、运行时模块配置和后端 Maven 依赖。
- 读取 `mango.dev.json`、`.mango/dev-workspace.env`、`.mango/dev-workspace.local.json`，启动本地开发应用。
- 维护受 `mango-cli` marker 保护的代码块，例如 `backend/pom.xml`、`backend/app/pom.xml`、`frontend/src/main.ts`、`application.yml` 中的 managed block。
- 同步业务 PMO baseline、兼容脚本和 Agent 入口。
- 通过 `@mango/pmo` 安装版本化 PMO baseline，并用 `baseline.json` 校验业务仓是否漂移。

CLI 不负责：

- 业务模块内部领域设计。
- Mango 平台模块的运行时逻辑。
- 数据库表结构设计和迁移执行本身。
- 业务项目已有文件的语义合并；没有 managed block 的文件不会被 CLI 猜测修改。

## 5. 接入方式
全局安装用于创建项目、历史项目升级和跨仓库临时诊断：

使用内网 [npm-group](http://nexus.inner.yunxinbaokeji.com/repository/npm-group/) 安装：

```bash
npm install -g @mango/cli --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
```

生成 full 项目：

```bash
mango init demo-admin --preset full --topology monolith
cd demo-admin
mango init-dev
mango validate
mango plan
mango start
```

推荐全局安装 `@mango/cli`，这样可以在任意业务仓根目录直接执行 `mango ...`。生成项目的 `scripts/dev-workspace.sh` 优先使用 `frontend` 中锁定的项目内 CLI；项目依赖未安装时回退到全局 `mango`。因此业务项目首次拉取后应先执行 `cd frontend && pnpm install`，再回到项目根目录使用 `scripts/dev-workspace.sh validate|plan|start`。

最终执行约定是：创建、升级和临时诊断可以直接用全局 CLI；业务仓日常开发命令优先走 `scripts/dev-workspace.sh`，以项目内锁定的 CLI 版本为准。历史项目如果没有脚本或项目依赖，可以先用全局 CLI 执行 `mango pmo upgrade --project-dir .`，再按升级后的项目说明切换到项目内版本。

生成 custom 项目：

```bash
mango init demo-custom --preset custom --modules workflow,template --topology monolith
```

追加 Mango 可选能力到 custom 项目：

```bash
mango add file notice --project-dir demo-custom
```

生成业务模块骨架：

```bash
mango module add order --aggregate sales-order --aggregate-name 销售订单 --module-name 订单模块 --project-dir demo-custom
```

同步 PMO baseline：

```bash
mango pmo status --project-dir demo-custom
mango pmo check --project-dir demo-custom
mango pmo sync --project-dir demo-custom --dry-run
mango pmo sync --project-dir demo-custom
mango pmo upgrade --project-dir demo-custom
```

## 6. 配置说明
### 6.1 init 命令参数

| 参数 | 默认值 | 含义 | 影响行为 | 源码入口 |
|------|--------|------|----------|----------|
| `<project>` | 无 | 目标项目名 | 会转成 kebab case 作为目录名和默认 project code | `parseArgs` |
| `--preset` | `full` | 项目预设，支持 `full`、`custom` | full 使用聚合 starter；custom 按模块选择依赖 | `validateOptions`、`renderBackendDependencies` |
| `--modules` | 空 | custom 项目的可选模块列表；支持逗号列表、`all`、`none` | 决定前端包、页面注册、后端 starter、运行时模块配置 | `resolveModuleCodes` |
| `--topology` | `monolith` | 拓扑，支持 `monolith`、`microservice` | 决定生成拓扑文档和 runtime config 的微前端入口 | `validateOptions`、`renderRuntimeModulesJson` |
| `--package` | `com.example.mango` | Java base package | 渲染后端包名和源码路径 | `buildVariables` |
| `--group-id` | 同 `--package` | Maven groupId | 渲染生成项目 Maven 坐标 | `parseArgs` |
| `--version` | `1.0.0-SNAPSHOT` | 生成项目版本 | 渲染 Maven 和前端业务包版本 | `buildVariables` |
| `--mango-version` | `release-versions.json` 的 `maven.mangoBackend` | Mango 后端 Maven 版本 | 写入生成项目 Maven 依赖版本 | `defaultVersions` |
| `--npm-registry` | [npm-group](http://nexus.inner.yunxinbaokeji.com/repository/npm-group/) | 生成项目 `.npmrc` registry | 前端安装 Mango NPM 包时使用 | `parseArgs` |
| `--maven-repository` | [maven-public](http://nexus.inner.yunxinbaokeji.com/repository/maven-public/) | 生成项目 Maven 仓库 | 后端拉取 Mango Maven 包时使用 | `parseArgs` |
| `--force` | `false` | 目标目录已存在时是否覆盖 | 为 true 时先删除目标目录再生成 | `main` |

### 6.2 mango.config.json

`mango init` 会在项目根目录生成 `mango.config.json`，`mango add` 和 `mango module add` 都依赖它判断项目状态。

| 字段 | 示例 | 含义 | 写入 / 更新入口 |
|------|------|------|-----------------|
| `project` | `demo-admin` | 项目 code | `writeMangoConfig` |
| `preset` | `custom` | 当前项目预设 | `writeMangoConfig` |
| `topology` | `monolith` | 当前拓扑 | `writeMangoConfig` |
| `basePackage` | `com.example.mango` | Java 根包名 | `writeMangoConfig` |
| `groupId` | `com.example.mango` | Maven groupId | `writeMangoConfig` |
| `projectVersion` | `1.0.0-SNAPSHOT` | 业务项目版本 | `writeMangoConfig` |
| `mangoBackendVersion` | `1.0.0-SNAPSHOT` | Mango 后端版本 | `writeMangoConfig` |
| `modules.required` | `authorization`、`system` | 必选 Mango 平台能力 | `writeMangoConfig` |
| `modules.optional` | `workflow`、`template` | 已启用的 Mango 可选能力 | `writeMangoConfig`、`addModules` |
| `mangoFrontendVersions` | `@mango/admin` 等 | 前端 Mango 包版本锁 | `writeMangoConfig` |
| `npmRegistry` | NPM group URL | 项目 NPM registry | `writeMangoConfig` |
| `mavenRepository` | Maven public URL | 项目 Maven 仓库 | `writeMangoConfig` |
| `businessModules` | 业务模块列表 | `mango module add` 追加的业务模块登记 | `updateBusinessConfig` |

### 6.3 本地开发工作区

CLI 从当前目录向上查找 `mango.dev.json`。本地私有配置来自 `.mango/dev-workspace.env`，局部覆盖来自 `.mango/dev-workspace.local.json`。

新项目模板会生成固定的 `backend`、`frontend` 开发清单。历史业务项目执行 `mango pmo sync --sync-shell` 或缺少清单时执行 `mango init-dev`，CLI 会先扫描项目结构再生成 `mango.dev.json`：

- 后端扫描 `pom.xml`，只把包含 Spring Boot app 特征的 POM 生成为 `spring-boot-maven` app；`<packaging>pom</packaging>` 且带 `<modules>` 的 aggregator POM 会被跳过并在计划中提示。
- 前端扫描 `package.json`，带 `scripts.dev` 且依赖 Vite 或存在 `vite.config.*` 的目录会生成为 `vite` app；包管理器按项目根的 `pnpm-lock.yaml`、`yarn.lock` 自动选择，默认 `npm`。
- 检测到多个后端或前端 app 时，`groups.backend`、`groups.frontend` 会包含全部 app，`groups.default` 只取第一个后端和第一个前端，并在计划中提示人工确认。
- 已存在业务自有 `mango.dev.json` 时不会覆盖；需要本机临时改路径时使用 `.mango/dev-workspace.local.json`。

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `mango.dev.json` | `version` | `1` | manifest 版本 | 非 1 时 `validate` 失败 | `validateDevWorkspace` |
| `mango.dev.json` | `groups` | `default`、`backend`、`frontend` | 启动分组 | `plan`、`start`、`stop` 展开目标 | `resolveDevWorkspaceTargets` |
| `mango.dev.json` | `apps.<name>.type` | `spring-boot-maven`、`vite` | 应用类型 | 决定命令解析方式 | `validateDevWorkspace`、`resolveDevApp` |
| `mango.dev.json` | `apps.<name>.cwd` | 模板内路径 | 应用工作目录 | 不存在时校验失败 | `validateDevWorkspace` |
| `mango.dev.json` | `apps.<name>.dependsOn` | 前端依赖后端 | 启动顺序 | 先启动依赖应用 | `resolveDevWorkspaceTargets` |
| `mango.dev.json` | `apps.<name>.health` | `/actuator/health` | 健康检查路径 | `start` 等待后端 ready | `waitForDevApp` |
| `mango.dev.json` | `apps.<name>.portEnv` | `MANGO_BACKEND_PORT` 或 `MANGO_FRONTEND_PORT` | 端口环境变量名 | 覆盖默认端口 | `resolveDevApp` |
| `.mango/dev-workspace.env` | `MANGO_CRYPTO_SM4_SECRET_KEY` | 随机 16 字节 hex | Mango 加密密钥 | 注入后端环境变量；缺失时自动补写 | `defaultDevWorkspaceEnv`、`ensureDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_WORKSPACE_ID` | `mango_<hash>` | 当前本地 worktree 标识 | 用于区分同机多业务工作区 | `allocateDevWorkspace` |
| `.mango/dev-workspace.env` | `MANGO_BACKEND_PORT` | `18080+hash` | 后端端口 | 后端 `server.port` 和前端代理目标；同机 registry 分配避免冲突 | `allocateDevWorkspace` |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_PORT` | `7770+hash` | 前端端口 | Vite dev server 端口；同机 registry 分配避免冲突 | `allocateDevWorkspace` |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_HOST` | `127.0.0.1` | 前端监听 host | Vite host | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_OPEN` | `false` | 是否自动打开浏览器 | 写入 `VITE_OPEN` | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_AUTO_INSTALL` | `true` | 预留前端自动安装开关 | 供生成脚本和后续扩展读取 | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_NAME` | `mango_dev_<hash>` | 数据库名 | 拼接 Spring datasource URL；同机 registry 分配避免跨 worktree 共用库 | `allocateDevWorkspace` |
| `.mango/dev-workspace.env` | `MANGO_DB_HOST` | `127.0.0.1` | 数据库 host | 拼接 Spring datasource URL | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_PORT` | `3306` | 数据库端口 | 拼接 Spring datasource URL | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_USERNAME` | `root` | 数据库用户名 | 注入 Spring datasource | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_PASSWORD` | 空字符串 | 数据库密码 | 注入 Spring datasource | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_AUTO_CREATE` | `true` | 数据库自动创建开关 | 当前 CLI 只生成该值，是否消费取决于业务脚本 | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_OFFICE_PLUGIN_ENABLED` | `false` | Office 插件开关 | 注入 `office.plugin.enabled` | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_BACKEND_ADDITIONAL_ARGS` | 空字符串 | 后端额外启动参数 | 追加到 Spring Boot args | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.local.json` | `groups`、`apps` | 空 | 本机覆盖 manifest | 与 `mango.dev.json` 深合并 | `mergeDevWorkspaceManifest` |

本地运行文件：

| 路径 | 内容 | 生成时机 | 排查用途 |
|------|------|----------|----------|
| `.mango` 下的 `run`、`pids`、`<app>.json` | pid、pgid、启动命令、端口、URL、日志路径 | `mango start` | `status`、`stop` 判断进程 |
| `.mango` 下的 `run`、`logs`、`<app>.log` | 安装和启动输出 | `mango start` | `mango logs <app>` 和失败诊断 |
| `.mango/run/state.json` | 预留状态文件路径 | context 初始化 | 后续状态扩展 |

## 7. API 与扩展
### 7.1 命令面

| 命令 | 作用 | 关键参数 | 主要修改范围 |
|------|------|----------|--------------|
| `mango init <project>` | 生成业务项目 | `--preset`、`--modules`、`--topology`、`--package` | 新项目目录 |
| `mango add <module...>` | custom 项目追加 Mango 可选能力 | `--project-dir` | `frontend/package.json`、`frontend/src/main.ts`、runtime config、后端 POM、`mango.config.json` |
| `mango module add <module>` | 生成业务模块骨架 | `--aggregate`、`--aggregate-name`、`--module-name`、`--project-dir`、`--force` | `backend/modules`、`frontend/packages`、POM、前端入口、Flyway 模块配置、`mango.config.json` |
| `mango pmo sync` | 同步 PMO baseline | `--project-dir`、`--dry-run`、`--write-agents`、`--sync-shell` | `business-pmo`、部分 `business-docs`、`AGENTS.md`、兼容脚本 |
| `mango pmo status` | 查看业务仓 PMO baseline 状态 | `--project-dir` | 不改文件 |
| `mango pmo check` | 校验业务仓 PMO baseline 是否等于当前 `@mango/pmo` | `--project-dir` | 不改文件 |
| `mango pmo upgrade` | 按当前 `@mango/pmo` 升级业务仓 baseline | `--project-dir`、`--dry-run`、`--write-agents`、`--sync-shell` | `business-pmo`、部分 `business-docs`、`AGENTS.md`、兼容脚本 |
| `mango init-dev` | 初始化本地开发工作区 | 无 | `.mango/dev-workspace.env`、缺失时创建 `mango.dev.json` |
| `mango print` | 打印 workspace 应用 | 无 | 不改文件 |
| `mango validate` | 校验 workspace manifest | 无 | 不改文件 |
| `mango doctor` | 校验工具链、POM、端口 | 无 | 不改文件 |
| `mango plan` | 展开启动计划 | group 或 app | 不改文件 |
| `mango start` | 启动本地开发应用 | group 或 app | `.mango/run` |
| `mango backend` | 启动后端分组 | 无 | `.mango/run` |
| `mango frontend` | 启动前端分组 | 无 | `.mango/run` |
| `mango status` | 查看进程状态 | 无 | 不改文件 |
| `mango logs <app>` | 查看最近 200 行日志 | app name | 不改文件 |
| `mango stop` | 停止本地开发应用 | group 或 app | 删除 pid file |
| `mango changelog` | 打印 CLI changelog | 无 | 不改文件 |

### 7.2 可选模块矩阵

| code | 能力 | 前端包 | 后端 starter | 页面注册 / runtime 说明 |
|------|------|--------|--------------|-------------------------|
| `file` | 文件中心 | `@mango/file` | `mango-file-starter`、`mango-file-preview-starter` | 注册文件管理页面，不生成 runtime module |
| `template` | 模板管理 | `@mango/template` | `mango-template-starter` | runtime module 为 `mango-template` |
| `cms` | 内容中心 | `@mango/cms` | `mango-cms-starter` | runtime module 为 `mango-cms` |
| `notice` | 通知中心 | `@mango/notice` | `mango-notice-starter` | 注册 admin pages 和 admin shell |
| `numgen` | 编号规则 | `@mango/numgen` | `mango-numgen-starter` | 注册编号规则页面 |
| `calendar` | 工作日历 | `@mango/calendar` | `mango-calendar-starter` | 注册工作日历页面 |
| `workflow` | 审批中心 | `@mango/workflow` | `mango-workflow-starter` | runtime module 为 `mango-workflow` |
| `workflow-example` | 审批示例 | `@mango/workflow-business-example` | 无独立后端 starter | 自动依赖 `workflow` |

必选 runtime module：

| moduleCode | monolith / local | microservice |
|------------|------------------|--------------|
| `mango-authorization` | `mango-admin-rbac-local` | `mango-admin-rbac-app`，entry `http://b.mango.io:5181/` |
| `mango-system` | `mango-admin-system-local` | `mango-admin-system-local` |
| `mango-workflow` | `mango-admin-workflow-local` | `mango-admin-workflow-app`，entry `http://c.mango.io:5182/` |
| `mango-template` | `mango-admin-template-local` | `mango-admin-template-app`，entry `http://d.mango.io:5183/` |
| `mango-cms` | `mango-admin-cms-local` | `mango-admin-cms-app`，entry `http://e.mango.io:5184/` |

### 7.3 模板和版本扩展点

| 入口 | 用途 | 注意事项 |
|------|------|----------|
| `templates/full` | `mango init` 的项目模板 | 发布包包含该目录 |
| `templates/full/mango.dev.json` | 新项目开发工作区 manifest 模板 | 历史业务项目执行 `pmo sync --sync-shell` 时优先按真实目录探测生成；业务项目可用 `.mango/dev-workspace.local.json` 本机覆盖 |
| `release-versions.json` | 锁定 Mango 后端 Maven 版本和前端 NPM 包版本 | 修改发布版本后必须跑 release version 检查 |
| `scripts/check-cli.mjs` | CLI 生成契约自测 | 会生成 full 和 custom 项目并校验关键文件 |
| `scripts/check-release-versions.mjs` | 版本锁自测 | 可加 registry 检查已发布包 |
| `templates/business-module` | 业务模块模板优先路径 | 当前包内没有该目录；源码运行时会回退到仓库根目录 `mango-business-starter` |

业务模块模板限制要特别注意：`package.json` 的发布文件包含 `templates`，但当前实际存在的是 `templates/full`。如果只从已发布 NPM 包运行，`mango module add` 需要确认包内已经带上业务模块模板；在仓库源码内运行时才会回退到 `mango-business-starter`。

## 8. 数据与初始化
`@mango/cli` 自身不包含数据库 migration，也不会直接连接数据库。数据库结构和初始化数据来自生成项目引用的后端模块。

CLI 生成或更新的数据库相关入口：

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| 业务模块 Flyway 模板 | `backend/modules/<module>/<module>-core/src/main/resources/db/migration/<module>/V1__init_<module>.sql` | 业务模块示例表结构 | 由模板 SQL 定义 | 生成业务模块后，后端 Flyway 执行 | 检查后端启动日志和业务表 |
| Flyway 模块开关 | `backend/app/src/main/resources/application.yml` 的 `business-flyway-modules` managed block | `<module>.enabled: true` | module code | `mango module add` 后写入，应用启动读取 | 检查 application.yml 中模块已登记 |
| 平台模块 migration | 生成项目后端依赖中的 Mango starter | 平台模块表、菜单、权限、字典或默认数据 | 各平台模块定义 | 应用启动 Flyway 执行 | 查模块 README 和 Flyway history |
| 本地数据库连接 | `.mango/dev-workspace.env` | DB host、port、name、username、password | 无 | `mango start` 注入后端启动参数 | `mango plan` 查看命令，`mango logs <backend>` 查 datasource |

## 9. 管理入口
CLI 不在运行时管理菜单、权限和租户，但会生成让业务模块接入菜单权限体系的模板文件。

| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 业务聚合列表页 | 由业务模块模板 `resource-manifest.json` 渲染 | 由业务模块模板按 module 和 aggregate 渲染 | `backend/modules/<module>/<module>-starter/src/main/resources/META-INF/mango/resource-manifest.json` | 模板资源清单定义 | `<module>-starter` 中生成的 Controller |
| Mango 平台页面 | 各 `@mango/*` 包的 admin pages | 各平台模块 README 登记 | 平台模块 migration 或 resource manifest | 各平台模块定义 | 各平台模块 Controller / Service |

业务模块生成后需要检查：

- `resource-manifest.json` 中 `moduleCode`、菜单 code、component key 与前端页面路径一致。
- `frontend/src/main.ts` 已写入 `register<Module>Pages()`。
- 后端 app POM 已加入 `<module>-starter` 依赖。
- 如果业务有租户隔离要求，应在生成模板基础上补充租户字段、查询条件和权限校验，CLI 不会替业务自动推断数据边界。

## 10. 快速开始
新业务项目：

1. 安装 CLI。
2. 用 `mango init` 生成项目，优先按业务需要选择 `custom` 和明确模块列表；需要一次性体验全部平台能力时才使用 `full`。
3. 进入项目后执行 `mango init-dev`，修改 `.mango/dev-workspace.env` 中数据库、端口和本机开关。
4. 执行 `mango validate`、`mango doctor`、`mango plan`，确认 manifest、工具链和端口。
5. 执行 `mango start`，通过 `mango status`、`mango logs <app>` 查看状态。
6. 需要新增业务能力时执行 `mango module add`，然后补充业务领域代码、菜单权限、租户字段、测试和 README。

已有业务项目同步：

1. 在项目根目录确认有 `mango.config.json` 和 `mango.dev.json`。
2. 先执行 `mango pmo sync --project-dir . --dry-run` 看计划。
3. 没有风险后执行 `mango pmo sync --project-dir .`。
4. 只有明确要同步兼容启动脚本时才加 `--sync-shell`。
5. 如果 `AGENTS.md` 仍引用外部 `mango-pmo`，先人工确认，再用 `--write-agents` 迁移。

## 11. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| `mango.dev.json not found` | 当前目录不在 Mango workspace 内 | 进入项目根目录，或先执行 `mango init-dev` |
| `mango.dev.json` 生成后仍缺 app | 项目目录没有可识别的 Spring Boot app POM 或 Vite app | 查看 `pmo sync --sync-shell --dry-run` 的 `warn` 行，人工补充 `apps` 或 `.mango/dev-workspace.local.json` |
| 生成计划提示 `skipped aggregator POM` | CLI 发现 Maven 聚合 POM，但不会把它作为 Spring Boot app 启动 | 确认生成的 app 指向真实服务 POM，例如 `apps/xxx-api/pom.xml` |
| `target already exists` | init 目标目录已存在 | 换目录名，或确认可删除后加 `--force` |
| `full preset already includes all optional modules` | 对 full 项目执行了 `mango add` | full 不需要追加平台可选模块；新增业务模块请用 `mango module add` |
| `unknown module` | module code 不在可选模块矩阵 | 使用 `file`、`template`、`notice`、`numgen`、`calendar`、`workflow`、`workflow-example` |
| `managed block not found` | 业务项目删除了 `mango-cli` marker | 按模板恢复 marker，或人工合并依赖和入口 |
| `use explicit Spring Boot Maven plugin coordinate instead of spring-boot:run` | `mango.dev.json` 使用了简写 goal | 改成 `org.springframework.boot:spring-boot-maven-plugin:<version>:run` |
| 端口被占用 | `MANGO_BACKEND_PORT` 或 `MANGO_FRONTEND_PORT` 已被其他进程占用 | 停止冲突进程，或修改 `.mango/dev-workspace.env` |
| 已发布 CLI 运行 `module add` 找不到业务模块模板 | 当前发布包可能只包含 `templates/full` | 在仓库源码内运行，或把业务模块模板纳入 CLI 发布包后再发布 |
| `mango logs <app>` 找不到日志 | 应用未通过 `mango start` 启动 | 先执行 `mango start <app>` |

## 12. 相关文档
- [PMO 总流程](../../../mango-pmo/rules/00-dev-flow.md)
- [AI 编码红线](../../../mango-pmo/rules/03-ai-coding-redlines.md)
- [交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [文档资产边界](../../../mango-pmo/rules/06-document-assets.md)
- [能力说明维护](../../../mango-pmo/rules/08-capability-docs.md)

- [CLI CHANGELOG](./CHANGELOG.md)
- [full 模板 README](./templates/full/README.md)
- [Business Starter](../../../mango-business-starter/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
