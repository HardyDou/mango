# @mango/cli

## 1. 概览
`@mango/cli` 是 Mango 面向业务项目的命令行入口，用来生成业务项目、追加 Mango 可选能力、生成业务模块骨架、同步 PMO baseline，并编排本地后端和前端开发进程。

代码入口和发布事实：

| 项目 | 值 |
|------|----|
| NPM 包 | `@mango/cli` |
| 当前版本 | `1.0.82` |
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
| 检查和同步业务 PMO bundle | `mango pmo status`、`mango pmo check`、`mango pmo sync`、`mango pmo upgrade`、`mango pmo rollback` | `business-pmo`、`.agents/skills`、部分 `business-docs`、`AGENTS.md`、兼容脚本 |
| 初始化和启动本地开发工作区 | `mango workspace init`、`mango workspace status`、`mango workspace doctor`、`mango dev doctor`、`mango dev start` | `.mango/workspace.json`、`.mango/dev-workspace.env`、`.mango/run` |
| 拉取当前 Mango 版本文档包 | `mango docs pull`、`mango docs status`、`mango docs path` | `.mango/docs/<mango.version>` |
| 编排可审计发布状态机 | `mango release publish/status/verify/repair`、`mango release registry doctor` | `.mango/releases/<version>/manifest.json` 或项目配置的证据目录 |
| 查看发布说明 | `mango changelog` | 不改文件 |

## 3. 能力边界
- 不作为业务前端运行时依赖安装到 `dependencies`；业务前端使用 `@mango/admin`、`@mango/file` 等运行时包。
- 不替代 Maven、NPM、Vite、数据库迁移和浏览器验收；CLI 只做模板生成、静态契约校验和本地进程编排。
- 不在 full preset 项目里追加 Mango 可选能力；full preset 已包含全部可选能力，`mango add` 会拒绝执行。
- 不负责生产部署。`mango dev start` 面向本地开发，生产运行应使用业务项目自己的部署脚本、镜像或进程管理。
- 不自动覆盖业务已经接管的 PMO 文档、`AGENTS.md`、`mango.dev.json` 和业务代码。

## 4. 模块入口
CLI 负责：

- 从 `templates/full` 渲染业务项目。
- 根据 `release-versions.json` 锁定 Mango 后端 Maven 版本和 NPM 包版本。
- 根据随包发布的 `admin-modules.json`、preset 和 module code 生成前端依赖、页面注册、样式入口、运行时模块配置和后端 Maven 依赖。
- 为生成项目写入最后执行的 `backend/architecture-verification` 模块和后端 CI；`mvn verify` 在完整 Reactor、完整代码范围内同时执行 Mango 架构规则、P3C/PMD、Checkstyle 和 SpotBugs，并拒绝缩小检查范围的命令行覆盖。
- 生成的业务 Checkstyle 默认允许三元表达式且不限制单行字符数；需要更严格排版约束的项目可以维护 `backend/config/quality/checkstyle.xml`。
- 生成项目的 `backend/config/quality/pmd-p3c.xml` 与 `mango-maven-plugin` canonical 规则保持一致；Mango Service 实现类允许使用 `XxxService` 或 `XxxServiceImpl`，不强制 `Impl` 后缀。
- 读取 `mango.dev.json`、`.mango/workspace.json`、`.mango/dev-workspace.env`、`.mango/dev-workspace.local.json`，启动本地开发应用。
- 维护受 `mango-cli` marker 保护的代码块，例如 `backend/pom.xml`、`backend/app/pom.xml`、`frontend/src/main.ts`、`application.yml` 中的 managed block。
- 同步业务 PMO baseline、项目级 Skill、兼容脚本和 Agent 入口。
- 通过精确依赖的 `@mango/pmo` 安装版本化 PMO bundle，并用 `baseline.json`、`pmo-lock.json` 和逐文件 hash 校验业务仓是否漂移。
- 原子升级或回滚 baseline、项目锁和 bundle-owned Skill；项目级 Skill 同步不修改用户级 Codex plugin 配置。
- 按业务项目锁定的 Mango 后端版本，从 Maven 仓库拉取 `io.mango:mango-docs-bundle:<version>`，解包到 `.mango/docs/<version>`，供业务开发和 AI 优先读取同版本 README、能力文档、规则和示例。

CLI 不负责：

- 业务模块内部领域设计。
- Mango 平台模块的运行时逻辑。
- 数据库表结构设计和迁移执行本身。
- 业务项目已有文件的语义合并；没有 managed block 的文件不会被 CLI 猜测修改。

## 5. 接入方式
全局安装只用于创建项目、历史项目升级和跨仓库临时诊断：

使用内网 [npm-group](http://nexus.inner.yunxinbaokeji.com/repository/npm-group/) 安装：

```bash
npm view @mango/pmo@1.3.1 version --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
npm view @mango/cli@1.0.82 version --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
npm install -g @mango/cli@1.0.82 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
```

两个查询都返回精确版本后，该批次才可供业务项目安装。PMO 升级会整体同步 baseline、Agent 入口和 `.agents/skills`，不需要逐个安装 Skill。

生成 full 项目：

```bash
mango init demo-admin --preset full --topology monolith
cd demo-admin/frontend
pnpm install
pnpm exec mango workspace init
pnpm exec mango workspace status
pnpm exec mango dev doctor
pnpm exec mango dev start
```

业务仓日常开发以项目内锁定的 `@mango/cli` 为准。进入生成项目的 `frontend` 后先安装依赖，再用 `pnpm exec mango workspace ...`、`pnpm exec mango dev ...` 和 `pnpm exec mango frontend ...` 执行本地开发命令。系统 `PATH` 上的 `mango` 可能是旧全局入口，不能作为业务项目 CLI 版本依据。

生成项目中的 `scripts/dev-workspace.sh` 只保留为历史兼容 shim，会把旧命令转发到 Mango CLI。历史项目升级时，先用全局 CLI 执行 `mango pmo upgrade --project-dir . --to 1.3.1 --sync-shell`；已经锁定到该 bundle 的项目只需用 `mango pmo sync --project-dir . --sync-shell` 修复当前锁。随后进入 `frontend` 安装项目内依赖，并在每个 active worktree 执行 `pnpm exec mango workspace init` 生成 `.mango/workspace.json` 并补齐 `.mango/dev-workspace.env`。

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
mango pmo check --project-dir demo-custom --locked
mango pmo sync --project-dir demo-custom --dry-run
mango pmo sync --project-dir demo-custom
mango pmo upgrade --project-dir demo-custom --to 1.3.1
mango pmo rollback --project-dir demo-custom --dry-run
```

拉取当前项目对应版本的 Mango 文档包：

```bash
mango docs status --project-dir demo-custom
mango docs pull --project-dir demo-custom
mango docs path --project-dir demo-custom
```

`mango docs pull` 默认从项目 `mango.config.json.mavenRepository` 拉取 `io.mango:mango-docs-bundle:<mangoBackendVersion>:jar`。业务仓没有 Mango 源码时，AI 和开发者应先读取 `mango docs path` 输出目录下的同版本文档，再参考在线文档或历史上下文。需要临时验证其它版本或仓库时使用：

```bash
mango docs pull --project-dir demo-custom --version 1.0.1 --maven-repository https://nexus.inner.yunxinbaokeji.com/repository/maven-public/ --force
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
| `--maven-repository` | [maven-public](https://nexus.inner.yunxinbaokeji.com/repository/maven-public/) | 生成项目 Maven 仓库 | 后端拉取 Mango Maven 包时使用 | `parseArgs` |
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
| `mangoBackendVersion` | `release-versions.json` 的 `maven.mangoBackend` | Mango 后端固定 Maven 版本 | `writeMangoConfig` |
| `paths.backend` | `backend` | 后端 Maven 根目录；历史仓可设为 `baohan-backend` | `writeMangoConfig`，历史仓人工配置一次 |
| `paths.frontend` | `frontend` | 前端根目录 | `writeMangoConfig`，历史仓人工配置一次 |
| `paths.businessDocs` | `business-docs` | PMO 生命周期文档根目录 | `writeMangoConfig`，历史仓人工配置一次 |
| `modules.required` | `authorization`、`system` | 必选 Mango 平台能力 | `writeMangoConfig` |
| `modules.optional` | `workflow`、`template` | 已启用的 Mango 可选能力 | `writeMangoConfig`、`addModules` |
| `mangoFrontendVersions` | `@mango/admin` 等 | 前端 Mango 包版本锁 | `writeMangoConfig` |
| `npmRegistry` | NPM group URL | 项目 NPM registry | `writeMangoConfig` |
| `mavenRepository` | Maven public URL | 项目 Maven 仓库 | `writeMangoConfig` |
| `businessModules` | 业务模块列表 | `mango module add` 追加的业务模块登记 | `updateBusinessConfig` |

生成项目的后端 Mango jar 版本由 `backend/pom.xml` 中的 `<mango.version>` 统一锁定。默认值来自当前 CLI 随包发布的 `release-versions.json.maven.mangoBackend`，业务项目选择版本时优先固定 `@mango/cli` 版本；需要验证其它后端平台版本时，再通过 `mango init --mango-version <version>` 或项目内 `mango.config.json` 的 `mangoBackendVersion` 明确覆盖。

### 6.3 docs bundle

`mango docs` 解决业务仓不下载 Mango 源码时的文档可达性问题。版本解析顺序为：命令参数 `--version`、`mango.config.json.mangoBackendVersion`、`backend/pom.xml` 或 `pom.xml` 中的 `<mango.version>`、CLI 随包 `release-versions.json.maven.mangoBackend`。

文档包 Maven 坐标固定为 `io.mango:mango-docs-bundle:<version>:jar`，推荐包内目录为 `META-INF/mango-docs`。CLI 会把 jar 解包到 `.mango/docs/<version>`，并写入 `.mango/docs/current.json` 记录版本、坐标、来源 URL、SHA-256 和本地路径。

| 命令 | 作用 | 关键参数 | 修改范围 |
|------|------|----------|----------|
| `mango docs pull` | 下载并解包同版本文档包 | `--project-dir`、`--version`、`--maven-repository`、`--force` | `.mango/docs/<version>`、`.mango/docs/current.json` |
| `mango docs status` | 查看项目 Mango 版本、文档包坐标和本地安装状态 | `--project-dir` | 不改文件 |
| `mango docs path` | 输出当前项目版本的本地文档目录 | `--project-dir` | 不改文件 |

### 6.4 本地开发工作区

CLI 从当前目录向上查找 `mango.dev.json`。本地工作区分配事实来自 `.mango/workspace.json`，本地私有运行配置来自 `.mango/dev-workspace.env`，局部覆盖来自 `.mango/dev-workspace.local.json`。本机全局注册表为 `~/.mango/workspaces.json`。

`mango workspace init` 还会在路径不存在时创建 .mango/m2/repository，并把它链接到用户公共仓库 ~/.m2/repository。因此业务 `mango.dev.json` 可以继续使用 -Dmaven.repo.local=.mango/m2/repository，不同 worktree 的端口、数据库和进程仍隔离，但不会重复下载 Maven 依赖。若该路径已经是一个真实目录或指向其它位置的链接，CLI 只提示并保留现状，供明确需要独立 Maven 缓存的工作区继续使用。

新项目模板会生成固定的 `backend`、`frontend` 开发清单。历史业务项目执行 `mango pmo sync --sync-shell` 或缺少清单时执行 `mango workspace init`，CLI 会先扫描项目结构再生成 `mango.dev.json`：

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
| `.mango/workspace.json` | `workspaceId` | `mango_<slot>` | 当前 worktree 标识 | 进程归属和诊断 | `ensureWorkspaceConfig` |
| `.mango/workspace.json` | `slot` | `1..200` 稳定工作区号 `NNN` | 本机工作区分配号 | 推导端口和数据库名 | `buildWorkspaceConfig` |
| `.mango/workspace.json` | `backendPort` | `18NNN` | 后端端口 | 写入 `MANGO_BACKEND_PORT` | `workspacePorts` |
| `.mango/workspace.json` | `frontendPort` | `30NNN` | 前端主端口 | 写入 `MANGO_FRONTEND_PORT` | `workspacePorts` |
| `.mango/workspace.json` | `frontendApps` | `31NNN`、`32NNN`、`33NNN`... | 前端子应用端口 | 写入 `MANGO_ADMIN_*_PORT` | `buildFrontendAppPorts` |
| `.mango/workspace.json` | `dbName` | `mango_dev_<projectSlug>_<NNN>` | 本地数据库名 | 写入 `MANGO_DB_NAME` | `buildWorkspaceConfig` |
| .mango/m2/repository | 目录链接 | ~/.m2/repository | worktree Maven 本地仓库入口 | 默认复用用户公共 Maven 缓存；已有路径不覆盖 | `ensureWorkspaceMavenRepository` |
| `.mango/dev-workspace.env` | `MANGO_CRYPTO_SM4_SECRET_KEY` | 随机 16 字节 hex | Mango 加密密钥 | 注入后端环境变量；缺失时自动补写 | `defaultDevWorkspaceEnv`、`ensureDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_WORKSPACE_ID` | 来自 `.mango/workspace.json` | 当前本地 worktree 标识 | 用于区分同机多业务工作区 | `ensureDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_BACKEND_PORT` | 来自 `.mango/workspace.json` | 后端端口 | 后端 `server.port` 和前端代理目标；同机 registry 分配避免冲突 | `ensureDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_PORT` | 来自 `.mango/workspace.json` | 前端端口 | Vite dev server 端口；同机 registry 分配避免冲突 | `ensureDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_MODE` | `source` | 前端运行模式 | `source` 运行源码；`package` 要求已构建包产物 | `prepareFrontendWorkspace` |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_HOST` | `127.0.0.1` | 前端监听 host | Vite host | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_OPEN` | `false` | 是否自动打开浏览器 | 写入 `VITE_OPEN` | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_AUTO_INSTALL` | `true` | 预留前端自动安装开关 | 供生成脚本和后续扩展读取 | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_NAME` | 来自 `.mango/workspace.json` | 数据库名 | 拼接 Spring datasource URL；同机 registry 分配避免跨 worktree 共用库 | `ensureDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_HOST` | `127.0.0.1` | 数据库 host | 拼接 Spring datasource URL | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_PORT` | `3306` | 数据库端口 | 拼接 Spring datasource URL | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_USERNAME` | `root` | 数据库用户名 | 注入 Spring datasource | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_PASSWORD` | 空字符串 | 数据库密码 | 注入 Spring datasource | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_DB_AUTO_CREATE` | `true` | 数据库自动创建开关 | `mango dev start` 启动 Spring Boot app 前调用本机 `mysql` 创建 `mango_dev_*` 工作区数据库 | `defaultDevWorkspaceEnv`、`ensureWorkspaceDatabase` |
| `.mango/dev-workspace.env` | `MANGO_OFFICE_PLUGIN_ENABLED` | `false` | Office 插件开关 | 注入 `office.plugin.enabled` | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.env` | `MANGO_BACKEND_ADDITIONAL_ARGS` | 空字符串 | 后端额外启动参数 | 追加到 Spring Boot args | `defaultDevWorkspaceEnv` |
| `.mango/dev-workspace.local.json` | `groups`、`apps` | 空 | 本机覆盖 manifest | 与 `mango.dev.json` 深合并 | `mergeDevWorkspaceManifest` |

示例：项目目录名为 `baohan-system` 且分配到 `slot=7` 时，后端端口为 `18007`，前端主端口为 `30007`，子前端按 `31007`、`32007`、`33007` 递增，数据库名为 `mango_dev_baohan_system_007`。

本地运行文件：

| 路径 | 内容 | 生成时机 | 排查用途 |
|------|------|----------|----------|
| `.mango` 下的 `run`、`pids`、`<app>.json` | pid、pgid、启动命令、端口、URL、日志路径 | `mango dev start` | `status`、`stop` 判断进程 |
| `.mango` 下的 `run`、`logs`、`<app>.log` | 安装和启动输出 | `mango dev start` | `mango dev logs <app>` 和失败诊断 |
| `.mango/run/state.json` | 预留状态文件路径 | context 初始化 | 后续状态扩展 |

`mango dev start` 执行前置安装命令时会把 stdout/stderr 直接追加到对应 app 日志，不在 CLI 进程内缓存完整输出。大型 Maven Reactor 即使产生超过 Node.js `spawnSync` 默认缓冲上限的日志，也不会因此被误判为安装失败；诊断仍统一使用 `mango dev logs <app>` 或对应 app 日志文件。

`mango dev status`、`mango dev stop` 和 `mango dev restart` 先通过内核 PID 探测判断进程是否存在，再把 `ps` 作为可选的僵尸进程补充检查。因此业务开发镜像或精简容器没有安装 `ps` 时，本地进程状态、停止和重启仍可使用；安装了 `ps` 的环境继续保留僵尸进程识别。

### 6.5 可审计发布状态机

`mango release` 固定维护 `source`、`versions`、`changelog`、`readmes`、`tests`、`pr`、`tag`、`github-release`、`maven`、`npm`、`cli-lock`、`private-registry-publish`、`private-registry-consume-verify`、`docs-latest`、`docs-snapshot`、`post-verify`、`cleanup`。每个状态只能是 `passed`、`failed`、`pending` 或 `not_applicable`，并必须有非空原因。适用且已执行的状态还必须逐次记录命令、工作目录、开始/完成时间、退出码和非空脱敏输出；`not_applicable` 因没有执行命令，只记录不适用原因和时间。

```bash
mango release registry doctor --project-dir .
mango release status --version 1.0.16 --project-dir .
mango release verify --version 1.0.16 --project-dir .
mango release publish --version 1.0.16 --project-dir . --authorize
mango release repair --version 1.0.16 --project-dir . --authorize
```

配置优先级固定为 CLI 参数、环境变量、用户配置 `~/.config/mango/release.json`、项目配置 `.mango-release.json`。`publish` 和 `repair` 的授权只能来自本次 `--authorize` 或 `MANGO_RELEASE_AUTHORIZED=1`，不能持久化到配置。Maven/npm 必须分别显式选择 `private-registry`、`public-registry`、`artifact-only` 或带原因的 `disabled`；没有隐式发布模式和 registry。

项目配置中的 adapter 使用结构化命令，不经过 shell 字符串解释。下面示例复用已有 Maven/npm 发布与只读回查入口：

```json
{
  "schemaVersion": 1,
  "releaseKind": "mixed",
  "artifacts": {
    "maven": { "mode": "private-registry", "serverId": "mango-releases" },
    "npm": { "mode": "private-registry", "tokenEnv": "MANGO_NPM_TOKEN" }
  },
  "stateAdapters": {
    "maven": {
      "publish": { "command": "scripts/publish-maven-batch.sh", "args": ["--all-non-app", "--release-version", "{version}", "--verify-base-url", "{mavenConsumeRegistry}"] },
      "verify": { "command": "scripts/publish-maven-batch.sh", "args": ["--all-non-app", "--release-version", "{version}", "--verify-only", "--verify-base-url", "{mavenConsumeRegistry}"] }
    },
    "npm": {
      "publish": [
        { "command": "pnpm", "cwd": "mango-ui", "args": ["publish:pkg", "pmo", "--release-tag={tag}", "--publish-registry={npmPublishRegistry}", "--consume-registry={npmConsumeRegistry}"] },
        { "command": "pnpm", "cwd": "mango-ui", "args": ["publish:pkg", "cli", "--release-tag={tag}", "--publish-registry={npmPublishRegistry}", "--consume-registry={npmConsumeRegistry}"] }
      ],
      "verify": [
        { "command": "pnpm", "cwd": "mango-ui", "args": ["publish:pkg", "pmo", "--verify-only", "--release-tag={tag}", "--publish-registry={npmPublishRegistry}", "--consume-registry={npmConsumeRegistry}"] },
        { "command": "pnpm", "cwd": "mango-ui", "args": ["publish:pkg", "cli", "--verify-only", "--release-tag={tag}", "--publish-registry={npmPublishRegistry}", "--consume-registry={npmConsumeRegistry}"] }
      ]
    }
  }
}
```

| 场景 | 模式与地址示例 | 认证引用 |
|------|----------------|----------|
| Nexus | publish 指向 hosted/releases，consume 指向 group/public | Maven `settings.xml` 的 `serverId`；npm token 环境变量名 |
| Artifactory | publish 指向 local repository，consume 指向 virtual repository | Maven `settings.xml` server ID；npm token 环境变量或 `npmConfig` |
| GitHub Packages | `public-registry`，四类 URL 指向组织 package endpoint | CI secret 注入 token，配置只记录引用名 |
| 只产出文件 | `artifact-only` | 不要求 registry 或凭据，adapter 只构建和校验制品 |
| 禁用某类制品 | `disabled` + `disabledReason` | 对应状态为 `not_applicable`，禁止空原因 |

四类 registry 地址分别用 `MANGO_RELEASE_MAVEN_PUBLISH_REGISTRY`、`MANGO_RELEASE_MAVEN_CONSUME_REGISTRY`、`MANGO_RELEASE_NPM_PUBLISH_REGISTRY`、`MANGO_RELEASE_NPM_CONSUME_REGISTRY` 注入。`repair` 跳过所有已 `passed` 状态；从未尝试的 pending 不可变状态会执行一次原 `publish` adapter，不能用 verify-only 冒充已发布。Maven、npm、tag、GitHub Release、文档快照已经尝试后，只允许精确对象 `{"kind":"verify-existing"}` 调用同状态已审核的 `verify` adapter，禁止自动重发，也不接受独立 repair 命令、空数组或额外字段。adapter 输出进入 manifest 前会脱敏 token、password、Bearer、URL userinfo 和凭据环境变量值；缺任何必需 evidence 字段、applicability 与配置不一致或 completed 标记与状态不一致时，`status/verify/repair` 都拒绝读取 manifest。

## 7. API 与扩展
### 7.1 命令面

| 命令 | 作用 | 关键参数 | 主要修改范围 |
|------|------|----------|--------------|
| `mango init <project>` | 生成业务项目 | `--preset`、`--modules`、`--topology`、`--package` | 新项目目录 |
| `mango add <module...>` | custom 项目追加 Mango 可选能力 | `--project-dir` | `frontend/package.json`、`frontend/src/main.ts`、runtime config、后端 POM、`mango.config.json` |
| `mango module add <module>` | 生成业务模块骨架 | `--aggregate`、`--aggregate-name`、`--module-name`、`--project-dir`、`--force` | `backend/modules`、`frontend/packages`、POM、前端入口、Flyway 模块配置、`mango.config.json` |
| `mango docs pull` | 拉取当前 Mango 版本文档包 | `--project-dir`、`--version`、`--maven-repository`、`--force` | `.mango/docs/<version>`、`.mango/docs/current.json` |
| `mango docs status` | 查看当前 Mango 版本文档包状态 | `--project-dir` | 不改文件 |
| `mango docs path` | 输出本地文档包目录 | `--project-dir` | 不改文件 |
| `mango pmo sync` | 按 `pmo-lock.json` 修复 PMO baseline 和项目 Skill | `--project-dir`、`--dry-run`、`--write-agents`、`--sync-shell` | `business-pmo`、`.agents/skills`、部分 `business-docs`、`AGENTS.md`、兼容脚本 |
| `mango pmo status` | 查看当前可用包或项目锁对应的 PMO 状态 | `--project-dir`、`--locked` | 不改文件 |
| `mango pmo check` | 校验当前可用包或项目锁对应的 PMO baseline、manifest 和项目 Skill | `--project-dir`、`--locked` | 不改文件 |
| `mango pmo upgrade` | 原子升级到 CLI 精确依赖的 `@mango/pmo` bundle | `--project-dir`、`--to`、`--dry-run`、`--write-agents`、`--sync-shell` | `business-pmo`、`.agents/skills`、部分 `business-docs`、`AGENTS.md`、兼容脚本、项目根 `.mango` 下的 PMO 备份目录 |
| `mango pmo rollback` | 原子恢复已校验的本地 PMO 备份 | `--project-dir`、`--to`、`--dry-run` | `business-pmo`、`.agents/skills`、项目根 `.mango` 下的 PMO 备份目录 |
| `mango release publish` | 按固定状态顺序执行发布并逐状态写证据 | `--version`、`--tag`、`--pr`、`--authorize`、registry/mode 参数 | release manifest、配置声明的外部制品 |
| `mango release status` | 只读展示全部发布状态和原因 | `--version`、`--project-dir`、`--json` | 不改文件 |
| `mango release verify` | 通过只读 adapter 重新验证状态 | `--version`、registry/mode 参数 | release manifest；不发布制品 |
| `mango release repair` | 从失败/待执行状态恢复，跳过已成功不可变制品 | `--version`、`--authorize` | release manifest、缺失的发布动作 |
| `mango release registry doctor` | 校验 artifact mode、四类 registry 角色和认证引用 | registry/mode 参数、`--json` | 不改文件 |
| `mango workspace init` | 初始化本地开发工作区 | 无 | `.mango/workspace.json`、`.mango/dev-workspace.env`、.mango/m2/repository，缺失时创建 `mango.dev.json` |
| `mango workspace status` | 打印 workspace 应用和端口 | 无 | 不改文件 |
| `mango workspace list` | 查看本机 workspace registry | 无 | 不改文件 |
| `mango workspace release` | 释放 workspace registry 并默认删除该 workspace 本地开发库 | `--workspace <path>`、`--keep-db` | `~/.mango/workspaces.json`、本机 MySQL |
| `mango workspace doctor` | 校验 workspace manifest | 无 | 不改文件 |
| `mango dev doctor` | 校验工具链、POM、端口 | 无 | 不改文件 |
| `mango dev start` | 启动本地开发应用 | group 或 app | `.mango/run` |
| `mango dev start backend` | 启动后端分组 | 无 | `.mango/run` |
| `mango dev start frontend` | 启动前端分组 | 无 | `.mango/run` |
| `mango dev restart` | 按 stop + start 重启本地开发应用 | group 或 app | `.mango/run` |
| `mango dev status` | 查看进程状态 | 无 | 不改文件 |
| `mango dev logs <app>` | 查看最近 200 行日志 | app name | 不改文件 |
| `mango dev stop` | 停止本地开发应用 | group 或 app | 删除 pid file |
| `mango frontend prepare` | 准备前端 source 模式必要文件 | 无 | `packages/admin/generated-package-styles.css`、`packages/admin/style-full.css` |

`mango validate`、`mango dev plan`、`mango dev backend` 和 `mango dev frontend` 仍作为历史兼容入口保留，新文档和交付命令应使用上表公开入口。
| `mango frontend doctor` | 检查前端 source 模式准备状态 | 无 | 不改文件 |
| `mango changelog` | 打印 CLI changelog | 无 | 不改文件 |

### 7.2 可选模块矩阵

| code | 能力 | 前端包 | 后端 starter | 页面注册 / runtime 说明 |
|------|------|--------|--------------|-------------------------|
| `file` | 文件中心 | `@mango/file` | `mango-file-starter`、`mango-file-preview-starter` | 注册文件管理页面，不生成 runtime module |
| `template` | 模板管理 | `@mango/template` | `mango-template-starter` | runtime module 为 `mango-template` |
| `cms` | 内容中心 | `@mango/cms` | `mango-cms-starter` | runtime module 为 `mango-cms` |
| `notice` | 通知中心 | `@mango/notice` | `mango-notice-starter` | custom 后端基础依赖已包含 `mango-notice-starter` 以满足认证通知接口；选择本模块时额外注册 admin pages 和 admin shell |
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
| `release-versions.json` | 锁定 Mango 后端固定 Maven 版本和前端 NPM 包版本 | 修改发布版本后必须跑 release version 检查 |
| `scripts/check-cli.mjs` | CLI 生成契约自测 | 会生成 full 和 custom 项目并校验关键文件 |
| `scripts/check-business-module-template.mjs` | canonical 业务模块投影检查 | 比较路径、大小、SHA-256 和执行位，阻断 CLI 镜像漂移 |
| `scripts/check-generated-backend-gate.mjs` | 生成后端门禁验收 | 生成四层业务模块，正向执行 Maven verify，并反向验证 PathVariable、通用 Java 违规和 skip 绕过均被阻断 |
| `scripts/check-release-versions.mjs` | 版本锁自测 | 可加 registry 检查已发布包 |
| `templates/business-module` | 随 CLI 发布的业务模块模板 | 后端目录必须与 `mango-business-starter` canonical 模板完全一致 |

已发布 CLI 直接从包内 `templates/business-module` 执行 `mango module add`，不依赖 Mango 源仓路径。后端镜像以 `mango-business-starter` 为 canonical 源，发布和测试前执行 `pnpm --filter @mango/cli run check:business-module-template`；路径集合或任一文件 hash 不一致时必须先同步模板。

## 8. 数据与初始化
`@mango/cli` 自身不包含数据库 migration，也不会直接连接数据库。数据库结构和初始化数据来自生成项目引用的后端模块。

CLI 生成或更新的数据库相关入口：

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| 业务模块 Flyway 模板 | `backend/modules/<module>/<module>-core/src/main/resources/db/migration/<module>/V1__init_<module>.sql` | 业务模块示例表结构 | 由模板 SQL 定义 | 生成业务模块后，后端 Flyway 执行 | 检查后端启动日志和业务表 |
| 业务聚合列表页模板 | `frontend/packages/<module>/src/views/<module>/<aggregate>/index.vue` | 基于 `@mango/common` 标准列表页骨架的 CRUD 起点，默认启用常用搜索项折叠 | component key | `mango module add` 后写入前端页面包 | 检查页面使用 `MangoListPage`、`MangoSearchPanel`、`MangoListPanel` 和 `Pagination` |
| Flyway 模块开关 | `backend/app/src/main/resources/application.yml` 的 `business-flyway-modules` managed block | `<module>.enabled: true` | module code | `mango module add` 后写入，应用启动读取 | 检查 application.yml 中模块已登记 |
| 平台模块 migration | 生成项目后端依赖中的 Mango starter | 平台模块表、菜单、权限、字典或默认数据 | 各平台模块定义 | 应用启动 Flyway 执行 | 查模块 README 和 Flyway history |
| 本地数据库连接 | `.mango/dev-workspace.env` | DB host、port、name、username、password | 无 | `mango dev start` 注入后端启动参数 | `mango workspace status` 查看端口和数据库，`mango dev logs <backend>` 查 datasource |

`MANGO_DB_AUTO_CREATE=true` 的前置条件是本机 `mysql` 命令可用，且 `.mango/dev-workspace.env` 中的 MySQL host、port、username、password 能连接到目标实例。CLI 只会创建名称匹配 `mango_dev_*` 的工作区数据库；创建失败时应直接输出 `failed to auto-create database ...`，并把 MySQL 错误写入对应 app 的 `mango dev logs` 日志。

## 9. 管理入口
CLI 不在运行时管理菜单、权限和租户，但会生成让业务模块接入菜单权限体系的模板文件。

| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| 业务聚合列表页 | 由业务模块模板 `resource-manifest.json` 渲染 | 由业务模块模板按 module 和 aggregate 渲染 | `backend/modules/<module>/<module>-starter/src/main/resources/META-INF/mango/resource-manifest.json` | 模板资源清单定义 | `<module>-starter` Controller 和 core Service |
| Mango 平台页面 | 各 `@mango/*` 包的 admin pages | 各平台模块 README 登记 | 平台模块 migration 或 resource manifest | 各平台模块定义 | 各平台模块 Controller / Service |

业务模块生成后需要检查：

- `resource-manifest.json` 中 `moduleCode`、菜单 code、component key 与前端页面路径一致。
- `frontend/src/main.ts` 已写入 `register<Module>Pages()`。
- 后端 app POM 已加入 `<module>-starter` 依赖。
- 后端生成 `Api`、实现该契约的 Controller 和 Feign，并使用 `MangoTypedCrudService`、`Require` 与模块 `BizCode`。
- API 契约不携带 Spring MVC 传输注解，Controller 和 Feign 不生成 `PathVariable` 或 URI 模板参数。
- 如果业务有租户隔离要求，应在生成模板基础上补充租户字段、查询条件和权限校验，CLI 不会替业务自动推断数据边界。

## 10. 快速开始
新业务项目：

1. 安装 CLI。
2. 用 `mango init` 生成项目，优先按业务需要选择 `custom` 和明确模块列表；需要一次性体验全部平台能力时才使用 `full`。
3. 进入项目后执行 `mango workspace init`。端口和 `MANGO_DB_NAME` 由 `.mango/workspace.json` 派生，不手工改；只按需修改 `.mango/dev-workspace.env` 中数据库连接、文件目录和本机开关。
4. 执行 `mango workspace status`、`mango workspace doctor`、`mango dev doctor`，确认 manifest、工具链和端口。
5. 执行 `mango dev start`，或用 `mango dev start <group|app>` 启动指定分组 / 应用；需要重启时执行 `mango dev restart [group|app...]`；通过 `mango dev status`、`mango dev logs <app>` 查看状态。
6. 需要新增业务能力时执行 `mango module add`，然后补充业务领域代码、菜单权限、租户字段、测试和 README。

已有业务项目同步：

1. 在项目根目录确认有 `mango.config.json` 和 `mango.dev.json`。
2. 首次迁移或升版先执行 `mango pmo upgrade --project-dir . --to 1.3.1 --dry-run` 查看计划。
3. 确认后执行相同 upgrade 命令，并用 `mango pmo check --project-dir . --locked` 校验项目锁、baseline 和项目 Skill。
4. 已锁定项目发生文件漂移时执行 `mango pmo sync --project-dir .` 修复当前锁，不用 sync 隐式升版。
5. 需要恢复时先执行 `mango pmo rollback --project-dir . --dry-run`；只有明确要同步兼容启动脚本时才加 `--sync-shell`。
6. 如果 `AGENTS.md` 仍引用外部 `mango-pmo`，先人工确认，再用 `--write-agents` 迁移。

## 11. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| `mango.dev.json not found` | 当前目录不在 Mango workspace 内 | 进入项目根目录，或先执行 `mango workspace init` |
| `mango.dev.json` 生成后仍缺 app | 项目目录没有可识别的 Spring Boot app POM 或 Vite app | 查看 `pmo sync --sync-shell --dry-run` 的 `warn` 行，人工补充 `apps` 或 `.mango/dev-workspace.local.json` |
| 生成计划提示 `skipped aggregator POM` | CLI 发现 Maven 聚合 POM，但不会把它作为 Spring Boot app 启动 | 确认生成的 app 指向真实服务 POM，例如 `apps/xxx-api/pom.xml` |
| `target already exists` | init 目标目录已存在 | 换目录名，或确认可删除后加 `--force` |
| `full preset already includes all optional modules` | 对 full 项目执行了 `mango add` | full 不需要追加平台可选模块；新增业务模块请用 `mango module add` |
| `unknown module` | module code 不在可选模块矩阵 | 使用 `file`、`template`、`notice`、`numgen`、`calendar`、`workflow`、`workflow-example` |
| `managed block not found` | 业务项目删除了 `mango-cli` marker | 按模板恢复 marker，或人工合并依赖和入口 |
| `use explicit Spring Boot Maven plugin coordinate instead of spring-boot:run` | `mango.dev.json` 使用了简写 goal | 改成 `org.springframework.boot:spring-boot-maven-plugin:<version>:run` |
| 端口被占用 | 当前 worktree 分配的端口已被其他进程占用 | 先用 `mango dev status` 查看 owner，停止对应 worktree 或执行 `mango workspace release --workspace <path>` 后重试 |
| 后端启动卡在 health，且数据库 `mango_dev_*` 不存在 | 自动建库前置条件不满足，或 CLI 未按预期执行建库 | 先查 `command -v mysql`、`MANGO_DB_AUTO_CREATE=true`、MySQL 连接配置和 `mango dev logs <backend>`；如果没有 `ensured database` 或 `failed to auto-create database` 输出，按 Mango issue runbook 登记 |
| Vite app 启动后立即退出并提示 `vite: command not found` | 当前 worktree 前端依赖未安装，或 manifest 绕过了 package script 直接执行二进制 | 先执行 `pnpm -C <frontend-root> install --frozen-lockfile`；manifest 中 Vite app 优先使用 `dev -- --host <host> --port <port>` |
| 业务模块投影检查失败 | CLI 镜像与 canonical starter 的路径或 hash 不一致 | 从 canonical 目录完整同步后重跑 `check:business-module-template` |
| `release registry doctor` 提示 mode 或 registry 缺失 | 发布模式没有显式选择，或只配置 publish/consume 中的一侧 | 补齐 Maven/npm mode 和四类 registry 角色；禁止依赖默认地址 |
| `repair adapter required after an immutable publish attempt` | Maven/npm/tag/Release/快照执行过但结果失败，工具无法证明重发安全 | 先查 manifest 与 registry 实际状态；确认制品存在后配置精确的 `{ "kind": "verify-existing" }` 并复用该状态的 verify adapter，禁止重发 |
| `mango dev logs <app>` 找不到日志 | 应用未通过 `mango dev start` 启动 | 先执行 `mango dev start <app>` |
| pnpm 11 首次安装报 `ERR_PNPM_IGNORED_BUILDS` | 旧版 CLI 生成的前端缺少 `pnpm-workspace.yaml` 构建白名单 | 使用 `@mango/cli@1.0.81` 生成新项目；既有项目把当前 full 模板的 `allowBuilds` 映射合并到业务自有 workspace 配置 |

## 12. 相关文档

### 1.0.82 发布影响

`@mango/cli@1.0.82` 精确锁定 `@mango/pmo@1.3.1`，把仓库内 `mango-release` 设为 Mango 唯一发布 Skill，并补齐 Release 正文预检、不可变制品恢复、语义化验证、CHANGELOG 状态回填和环境清理。该版本不改变 Mango Maven、运行时前端包、API、数据库、菜单、权限、租户、路由或业务行为。

### 1.0.81 发布影响

`@mango/cli@1.0.81` 为新生成项目补齐 `frontend/pnpm-workspace.yaml`，显式允许 Mango 前端依赖使用的 pnpm 11 安装构建脚本，避免首次安装以 `ERR_PNPM_IGNORED_BUILDS` 退出。消费者回归现在直接校验生成文件，不再由测试脚本注入独立白名单。Mango Maven `1.0.22`、`@mango/pmo@1.3.0`、其它前端包锁、API、数据库、菜单、权限、租户、路由和运行时行为均不变。

### 1.0.80 发布影响

`@mango/cli@1.0.80` 将生成和升级锁更新到 Mango Maven `1.0.22`、`@mango/pmo@1.3.0` 与 `@mango/admin@1.0.49` 对应批次。Maven `1.0.22` 以 `ResourceDeclarationApi` 替换 `ResourceRegistryApi`，不保留兼容别名；业务项目必须先迁移 import、注入、实现和 Feign 引用，并将旧资源 target 依赖改为 `mango-resource-sync-starter`。已有数据库会执行 File V2 迁移，将文件访问默认值改为代理模式。

### 1.0.78 发布影响

`@mango/cli@1.0.78` 修复 [Issue #507](https://github.com/HardyDou/mango/issues/507)：`mango workspace init` 会把缺失的 .mango/m2/repository 初始化为指向 ~/.m2/repository 的目录链接，使业务清单在保留 worktree 本地路径写法的同时复用公共 Maven 缓存。已有真实目录或其它链接不会被覆盖；端口、数据库、进程和显式独立 Maven 仓库的隔离行为不变。该版本精确依赖 `@mango/pmo@1.2.6`，并将 Mango Maven 锁升级为 `1.0.21`，同时锁定本批次发布的 Notice、Payment、Admin Shell 和 Admin 包版本。

### 1.0.77 发布影响

`@mango/cli@1.0.77` 将后端 Mango Maven 锁升级为 `1.0.20`，修复 Maven 3 无法为 `architecture` goal 绑定 `java.nio.file.Path` 参数的问题。已有业务项目可直接把后端 Mango 版本升级到 `1.0.20`；本次不改变 PMO 版本、CLI 命令、模板结构、API、数据库、菜单、权限、租户或运行时配置。

### 1.0.76 发布影响

`@mango/cli@1.0.76` 继续精确依赖 `@mango/pmo@1.2.5`，后端 Mango Maven 锁仍为 `1.0.19`。本次 `release-versions.json` 同步锁定 `@mango/link-page@1.0.5`，使新生成或升级后的业务前端消费公共链接首页时拿到已发布的样式优化版本。该锁定不改变 CLI 命令、模板结构、后端 Maven 版本、页面注册方式或运行时配置；业务项目仍按 `@mango/link-page` README 引入组件和 `@mango/link-page/style.css`。

发布验证脚本同时规范 Windows release worktree 下的本地 tarball 路径写法，避免反斜杠进入临时消费者 `package.json` 后被 YAML/包管理器解析错误。该修复只影响 `package-consumer:typecheck` 使用本地 pack 产物做消费方类型检查的流程，不改变业务项目安装已发布 npm 包的方式。

### 1.0.75 发布影响

`@mango/cli@1.0.75` 精确依赖 `@mango/pmo@1.2.5`，并将业务后端锁升级为 Mango Maven `1.0.19`。该批次修复 `no-new-violations` 在 Mango 聚合前被历史 PMD/Checkstyle 直接阻断的问题，并为 PMO 合同启用前的历史生命周期文档提供逐路径、逐 SHA-256 的显式存量基线；历史文档内容变化和所有新文档仍会失败。

业务仓安装新 CLI 后执行 `mango pmo upgrade --project-dir . --to 1.2.5`，将后端 `<mango.version>` 升到 `1.0.19`，对既有历史文档登记哈希基线后只运行一次最终 required check。

### 1.0.74 发布影响

`@mango/cli@1.0.74` 精确依赖 `@mango/pmo@1.2.4`，把 PR 描述合同拆为秒级 `pr-contract-check`，并将代码 SHA 门禁拆为 PMO、文档、CLI 和 Java 并行任务。新 SHA 会取消旧代码检查；纯版本、README 和发布锁变更不再运行生成后端验收，生成后端门禁的 Maven 启动次数从 19 次降为 9 次且不再执行 `clean`。Mango Maven 继续使用 `1.0.18`。

业务仓安装新 CLI 后执行 `mango pmo upgrade --project-dir . --to 1.2.4` 并同步对应 GitHub/Gitea workflow。GitHub 仓将 `pr-contract-check` 与稳定汇总 `pmo-doc-check` 设为 required checks；无需人工 Review。

### 1.0.73 发布影响

`@mango/cli@1.0.73` 精确依赖 `@mango/pmo@1.2.3`，并把业务后端锁升级为 Mango Maven `1.0.18`。该批次修复 clean runner 缺少上游 SNAPSHOT 导致 partial 门禁失败，以及 `mango:check` 嵌套静态分析错误进入架构聚合模块的问题；质量门禁仍只扫描直接修改模块。

业务仓安装新 CLI 后执行 `mango pmo upgrade --project-dir . --to 1.2.3`，将后端 `<mango.version>` 升到 `1.0.18`，再只重跑一次 required check。无需继续重试基于旧制品的相同失败运行。

### 1.0.72 发布影响

`@mango/cli@1.0.72` 精确依赖 `@mango/pmo@1.2.2`。标准 scope classifier 从 `mango.config.json` 的 `paths` 对象读取业务仓目录；例如设置 `"backend": "baohan-backend"` 后，后端改动会进入直接模块 Maven 门禁，不会因目录名不是 `backend/` 而被误判为无后端改动。full template 同时提供 GitHub 和 Gitea 的 `pmo-doc-check`，两者使用同一 classifier 输出和稳定 check 名称。

该补丁不改变 Mango Java 运行时，业务后端继续使用 Mango Maven `1.0.17`。Mango 的正式 Maven `--all-non-app` 发布步骤已明确包含同版本 `io.mango:mango-docs-bundle`，不再依赖批次外人工补发。业务仓安装 `@mango/cli@1.0.72` 后，执行 `mango pmo upgrade --project-dir . --to 1.2.2`，再同步适用平台的标准 workflow。

### 1.0.71 发布影响

`@mango/cli@1.0.71` 精确依赖 `@mango/pmo@1.2.1`，修复 Issue #464。PMO 包在发布前使用真实 `pnpm pack` 校验 manifest 中每个文件的 hash、size 和 mode，发布后再对消费仓库下载的 tarball 执行相同权限校验，避免可执行工具被打包为 `0644` 后仍误判发布成功。`@mango/pmo@1.2.0` 与 `@mango/cli@1.0.70` 保留为历史制品，不再用于业务升级。

该补丁不改变 Mango Java 运行时，业务后端继续使用 Mango Maven `1.0.17`。升级业务仓时安装 `@mango/cli@1.0.71`，先执行 `mango pmo upgrade --project-dir . --to 1.2.1 --dry-run`，确认计划后再正式升级并运行 `mango pmo check --project-dir . --locked`。

### 1.0.70 发布影响

`@mango/cli@1.0.70` 精确依赖 `@mango/pmo@1.2.0`。新生成或升级后的业务项目使用需求影响与解决方案风险的最大值作为最终等级，PR 只从 `STATIC`、`UNIT`、`API`、`UI` 中选择能够证明验收结果的最小充分集合。`pmo-doc-check` 的稳定名称不变；普通后端质量门禁通过 Git 路径只选择直接修改的 Maven 模块，不使用 `-am` 或 `-amd` 扩大扫描范围。依赖构建和消费者兼容性由独立验证承担；PMO 同步、文档或前端局部改动不启动后端 Reactor。Mango Maven 锁定 `1.0.17`，该版本移除了插件委托静态分析时的隐藏 Reactor 扩展。

升级后先填写 `.github/pull_request_template.md` 的 `Risk / Verification`，再用 `mango pmo check --project-dir . --locked` 校验 baseline、项目 Skill 和锁。仅移动按钮位置且行为不变时可以记录 L0、选择 `STATIC, UI`，以定向截图证明位置，并说明跳过 UNIT/API 的理由。

### 1.0.69 发布影响

`@mango/cli@1.0.69` 精确依赖 `@mango/pmo@1.1.1`，修复统一发布状态机在前置状态失败后恢复时把尚未尝试的 npm/Maven 状态错误执行为 verify-only 的问题。`repair` 现在只对已经尝试过的不可变状态使用 `verify-existing`；`attempts=0` 的 pending 状态会执行首次 publish。Mango Maven 仍锁定 `1.0.16`，业务项目无需调整后端版本。

### 1.0.68 发布影响

`@mango/cli@1.0.68` 精确依赖 `@mango/pmo@1.1.0`，并把新项目的 Mango Maven 后端锁升级为 `1.0.16`。Maven `1.0.16` 必须先发布，它提供 typed CRUD 契约、global Entity manifest 新契约和生成项目默认启用的架构门禁；已存在的 Maven `1.0.15` 不能解析该 manifest，禁止与本批次模板混用。PMO baseline、文档契约、专用 Agent、项目 Skill 和 Codex plugin 投影纳入同一可复现 bundle。业务项目通过 `pmo-lock.json` 锁定 bundle；`sync` 只修复当前锁，`upgrade --to` 显式升版，`rollback` 只恢复已校验备份。项目 Skill 写入 `.agents/skills`，不会安装或修改用户级 Codex plugin。

### 1.0.67 发布影响

`@mango/cli@1.0.67` 将新生成业务项目的 Mango Maven 后端锁向前更新为 `1.0.14`，前端 npm 锁保持 `v2026.07.11-npm-readme-forward-release` 批次不变。本次不改变 CLI 命令、模板结构、workspace 管理或运行时源码；已有业务项目升级时将 `<mango.version>` 更新为 `1.0.14`。

### 1.0.66 发布影响

`@mango/cli@1.0.66` 是不回退运行时代码的前向文档发布：同步新的前端 npm 版本锁和业务 starter 依赖，使已更正的 package README 进入实际发布包。本次 CLI 不改变命令行参数、workspace 端口分配、模板生成逻辑或后端 Maven `1.0.13` 基线。业务项目应按本批次版本锁成组升级，不要混用上一批次的精确 `@mango/*` 依赖。
- [PMO 总流程](../../../mango-pmo/rules/00-dev-flow.md)
- [AI 编码红线](../../../mango-pmo/rules/03-ai-coding-redlines.md)
- [交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [文档资产边界](../../../mango-pmo/rules/06-document-assets.md)
- [能力说明维护](../../../mango-pmo/rules/08-capability-docs.md)

- [CLI CHANGELOG](./CHANGELOG.md)
- [full 模板 README](./templates/full/README.md)
- [Business Starter](../../../mango-business-starter/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
