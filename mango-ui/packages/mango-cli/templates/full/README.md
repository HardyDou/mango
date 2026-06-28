# {{projectPascal}}

## 1. 概览
这是 `@mango/cli` 生成的 Mango 业务项目模板说明。生成项目消费已发布的 Mango Maven 包和 NPM 包，不依赖 Mango 源仓源码路径。

生成项目包含：

- `backend`：Spring Boot 后端应用，full preset 默认依赖 `io.mango:mango-admin-starter`。
- `frontend`：Vue 管理后台，full preset 默认使用 `@mango/admin/full` 和 `@mango/admin/style-full.css`。
- `business-pmo`：业务仓库内置 PMO baseline、Agent 规则和检查工具。
- `business-docs`：业务交付契约和台账示例。
- `topologies`：单体和微服务接入说明。
- `mango.dev.json`：本地开发工作区 manifest。
- `scripts`：历史兼容入口，会把旧命令转发到 Mango CLI。

## 2. 功能清单

| 能力 | 使用入口 | 说明 |
|------|----------|------|
| 后端业务应用 | `backend/app` | full preset 默认依赖 `io.mango:mango-admin-starter`。 |
| 前端管理后台 | `frontend` | full preset 默认使用 `@mango/admin/full` 和 `@mango/admin/style-full.css`。 |
| 本地开发编排 | `mango.dev.json`、`.mango/workspace.json`、Mango CLI | 后端、前端启动、日志、状态和停止。 |
| 平台能力接入 | 后端 starter、前端 `@mango/*` 包 | 认证、授权、系统、文件、模板、通知、编号、日历、工作流、job 等。 |
| 业务模块生成 | `mango module add` | 在业务项目内生成 backend module 和 frontend package。 |
| PMO baseline | `business-pmo`、`AGENTS.md` | 脱离 Mango 源码后执行 preflight 和交付检查。 |

## 3. 能力边界
- 不作为生产部署脚本；`mango dev start` 只面向本地开发。
- 不替代业务项目的数据库初始化 runbook、部署 runbook、E2E 和性能基准。
- 不提供独立的运行时种子初始化；平台默认数据由 Flyway 和 Resource Registry 写入，生产业务数据由业务流程维护。
- 不允许前端 dev proxy 指向任意主机；模板只允许本机后端代理。
- 不自动生成业务领域代码；业务模块需通过 `mango module add` 或人工开发。

## 4. 模块入口
模板负责生成可运行的业务项目骨架和默认 Mango 平台依赖。生成后：

- 平台能力由 Mango starter 和 `@mango/*` 包提供。
- 本地启动由 `@mango/cli` 读取 `mango.dev.json`、`.mango/workspace.json` 和 `.mango/dev-workspace.env` 执行。
- 业务模块由业务仓库维护，CLI 只更新 managed block。
- 生产部署、密钥、数据库、对象存储、网关域名和权限授权由业务项目自己治理。

## 5. 接入方式
生成后进入项目：

业务开发前先确认本次会用到的 Mango 能力说明。在线阅读走 Mango 文档站和能力地图；离线开发使用与依赖版本匹配的文档快照。业务模块生成后，模块根 `README.md` 会登记 Persistence、Authorization、Admin Pages 和 PMO baseline 等常用入口。

```bash
cd {{projectKebab}}
cd frontend && pnpm install && cd ..
mango workspace init
mango validate
mango dev doctor
mango dev plan
mango dev start
```

CLI 执行来源需要区分：首次创建、历史项目升级和临时诊断可以使用全局 `@mango/cli`；业务项目日常开发正式入口是 `mango workspace`、`mango dev` 和 `mango frontend` 命令。`scripts/dev-workspace.sh` 只作为旧命令兼容 shim。

需要直接执行 `mango ...` 时，先确认本机全局 CLI 已安装且版本符合当前项目依赖：

```bash
npm install -g @mango/cli --registry {{npmRegistry}}
```

后端健康检查：

```bash
curl http://127.0.0.1:5555/actuator/health
```

前端独立构建：

```bash
npm --prefix frontend install --registry={{npmRegistry}}
npm --prefix frontend run build
```

常用本地命令：

| 命令 | 作用 |
|------|------|
| `mango workspace init` | 创建 `.mango/workspace.json`，并补齐 `.mango/dev-workspace.env` |
| `mango workspace status` | 打印当前 workspace、端口和应用配置 |
| `mango validate` | 校验 `mango.dev.json` |
| `mango dev doctor` | 校验工具链、POM 和端口 |
| `mango dev plan` | 展开启动命令 |
| `mango dev start` | 启动默认分组 |
| `mango dev backend` | 启动后端 |
| `mango dev frontend` | 启动前端 |
| `mango dev status` | 查看进程状态 |
| `mango dev logs {{projectKebab}}-service` | 查看后端日志 |
| `mango dev stop` | 停止默认分组 |
| `mango frontend prepare` | 准备前端 source 模式样式聚合文件 |

`scripts/backend-dev.sh` 只是兼容入口，会委托到 `mango dev backend`。

## 6. 配置说明
### 6.1 本地开发工作区

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `mango.dev.json` | `version` | `1` | manifest 版本 | 不是 1 时 CLI 校验失败 | `mango.dev.json` |
| `mango.dev.json` | `groups.default` | `{{projectKebab}}-service`、`{{projectKebab}}-admin` | 默认启动分组 | `mango dev start` 的目标 | `mango.dev.json` |
| `mango.dev.json` | `apps.{{projectKebab}}-service.type` | `spring-boot-maven` | 后端应用类型 | 使用 Maven Spring Boot plugin 启动 | `mango.dev.json` |
| `mango.dev.json` | `apps.{{projectKebab}}-service.health` | `/actuator/health` | 后端健康检查 | CLI 等待 ready 后再继续 | `mango.dev.json` |
| `mango.dev.json` | `apps.{{projectKebab}}-admin.type` | `vite` | 前端应用类型 | 使用 NPM dev script 启动 | `mango.dev.json` |
| `.mango/workspace.json` | `slot` | 本机稳定 slot | 推导端口和数据库名 | 避免多 worktree 互相串用 | `mango workspace init` |
| `.mango/workspace.json` | `backendPort` | `18000+slot` | 后端端口 | 写入 `MANGO_BACKEND_PORT` | `mango workspace init` |
| `.mango/workspace.json` | `frontendPort` | `8600+slot*20` | 前端端口 | 写入 `MANGO_FRONTEND_PORT` | `mango workspace init` |
| `.mango/workspace.json` | `dbName` | `mango_dev_<slot>` | 数据库名 | 写入 `MANGO_DB_NAME` | `mango workspace init` |
| `.mango/dev-workspace.env` | `MANGO_BACKEND_PORT` | 来自 `.mango/workspace.json` | 后端端口 | 注入 `server.port` | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_PORT` | 来自 `.mango/workspace.json` | 前端端口 | 注入 Vite `VITE_PORT` | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_HOST` | `127.0.0.1` | 前端 host | 注入 Vite `VITE_HOST` | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_FRONTEND_MODE` | `source` | 前端模式 | source 模式启动源码，package 模式要求已构建包 | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_CRYPTO_SM4_SECRET_KEY` | 随机 16 字节 hex | SM4 加密密钥 | 注入后端 `mango.crypto.sm4.secret-key` | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_DB_HOST` | `127.0.0.1` | 数据库 host | 拼接 datasource URL | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_DB_PORT` | `3306` | 数据库端口 | 拼接 datasource URL | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_DB_NAME` | 来自 `.mango/workspace.json` | 数据库名 | 拼接 datasource URL | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_DB_USERNAME` | `root` | 数据库用户名 | 注入 datasource | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_DB_PASSWORD` | 空字符串 | 数据库密码 | 注入 datasource | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_DB_AUTO_CREATE` | `true` | 数据库自动创建开关 | `mango dev start` 启动 Spring Boot app 前创建 `mango_dev_*` 工作区数据库 | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_OFFICE_PLUGIN_ENABLED` | `false` | Office 转 PDF 开关 | 影响 fileproc 和 office plugin | Mango CLI |
| `.mango/dev-workspace.env` | `MANGO_BACKEND_ADDITIONAL_ARGS` | 空字符串 | 后端额外参数 | 追加到 Spring Boot args | Mango CLI |

### 6.2 后端 application.yml

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `application.yml` | `server.port` | `${MANGO_BACKEND_PORT:5555}` | 后端端口 | 本地后端监听端口 | `backend/app/src/main/resources/application.yml` |
| `application.yml` | `spring.servlet.multipart.max-file-size` | `512MB` | 单文件上传上限 | 影响文件上传请求 | `application.yml` |
| `application.yml` | `spring.servlet.multipart.max-request-size` | `1024MB` | 单次请求上传上限 | 影响批量上传请求 | `application.yml` |
| `application.yml` | `spring.datasource.url` | MySQL 本地 URL | 数据库连接 | Flyway、MyBatis、平台模块数据 | `application.yml` |
| `application.yml` | `mango.security.jwt.secret` | 模板默认字符串 | JWT 密钥 | 生产必须覆盖 | `application.yml` |
| `application.yml` | `mango.auth.security.permit-paths` | swagger、health、realtime 等路径 | 放行路径 | 不走认证拦截 | `application.yml` |
| `application.yml` | `mango.access.ip-whitelist.enabled` | `true` | IP 白名单开关 | health 默认只允许本机和内网 CIDR | `application.yml` |
| `application.yml` | `mango.crypto.sm4.secret-key` | `${MANGO_CRYPTO_SM4_SECRET_KEY:}` | SM4 密钥 | 加解密能力依赖该值 | `application.yml` |
| `application.yml` | `mango.kv.store.type` | `jdbc` | KV 存储类型 | KV、token store、outbox 使用 JDBC | `application.yml` |
| `application.yml` | `mango.persistence.mybatis-plus.tenant.default-tenant-id` | `${MANGO_DEFAULT_TENANT_ID:1}` | 默认租户 ID | 租户字段默认值 | `application.yml` |
| `application.yml` | `mango.persistence.flyway.modules.*.enabled` | 多数平台模块为 `true` | Flyway 模块开关 | 决定系统、授权、文件、工作流等 migration 是否执行 | `application.yml` |
| `application.yml` | `mango.ip-location.xdb-location` | `file:./config/ip-location/ip2region_v4.xdb` | IP 库位置 | IP 定位能力读取该文件 | `application.yml` |
| `application.yml` | `mango.file.storage-type` | `LOCAL` | 文件存储类型 | 默认使用本地文件存储 | `application.yml` |
| `application.yml` | `mango.file.local.root-path` | `${MANGO_FILE_ROOT:./data/files}` | 本地文件根目录 | 文件上传落盘位置 | `application.yml` |
| `application.yml` | `mango.fileproc.convert.office-to-pdf-enabled` | `${MANGO_OFFICE_PLUGIN_ENABLED:false}` | Office 转 PDF | 控制预览转换能力 | `application.yml` |
| `application.yml` | `mango.workflow.enabled` | `true` | 工作流能力开关 | 启用 workflow starter 能力 | `application.yml` |
| `application.yml` | `mango.job.probe.enabled` | `${MANGO_JOB_PROBE_ENABLED:true}` | job 探针开关 | 影响 job 管理探测 | `application.yml` |
| `application.yml` | `springdoc.packages-to-scan` | `io.mango,{{basePackage}}` | OpenAPI 扫描包 | 扫描 Mango 和业务接口 | `application.yml` |

### 6.3 前端配置

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 | 源码入口 |
|----------|------------|--------|------|----------|----------|
| `frontend/package.json` | `scripts.dev` | `vite --host 0.0.0.0` | 本地前端启动 | CLI 启动 Vite 时执行 | `frontend/package.json` |
| `frontend/package.json` | `scripts.build` | `node scripts/build-with-report.mjs` | 前端构建 | 生成构建报告 | `frontend/package.json` |
| `frontend/package.json` | `scripts.test:e2e` | `playwright test` | E2E 测试 | 业务项目补充测试后执行 | `frontend/package.json` |
| `frontend/src/main.ts` | `apiBaseUrl` | `/api` | API base URL | Vite dev proxy 转发到后端 | `frontend/src/main.ts` |
| `frontend/src/main.ts` | `title` | `{{projectPascal}}` | 页面标题 | 管理后台标题 | `frontend/src/main.ts` |
| `frontend` 下的 `public`、`runtime-config.json` | `profile` | `monolith` | runtime profile | runtime module 加载策略 | `runtime-config.json` |
| `frontend` 下的 `public`、`runtime-config.microservice.json` | `modules` | 微服务 entry 配置 | 微前端模块入口 | `runtime-config.microservice.json` |
| `frontend/vite.config.ts` | `ALLOWED_PROXY_HOSTS` | `127.0.0.1`、`localhost` | dev proxy 白名单 | 防止代理到任意主机 | `vite.config.ts` |
| `frontend/vite.config.ts` | `DEV_ALLOWED_HOSTS` | `localhost`、`127.0.0.1`、`a.mango.io` 等 | dev server host 白名单 | 本地微前端域名访问 | `vite.config.ts` |

## 7. API 与扩展
### 7.1 后端依赖扩展

| 文件 | managed block | 用途 |
|------|---------------|------|
| `backend/pom.xml` | `business-modules` | `mango module add` 追加 `modules/<module>` |
| `backend/pom.xml` | `managed-dependencies` | CLI 根据 preset 和模块维护平台依赖版本 |
| `backend/app/pom.xml` | `dependencies` | CLI 根据 preset 和模块维护 app 平台依赖 |
| `backend/app/pom.xml` | `business-dependencies` | `mango module add` 追加业务 starter 依赖 |
| `application.yml` | `business-flyway-modules` | `mango module add` 追加业务 Flyway 模块开关 |

full preset 默认后端 app 依赖 `io.mango:mango-admin-starter`。平台默认资源来自各模块 Flyway、Resource Registry 和模块自身的 TenantProvisioner；租户、组织、账号等生产数据应由业务开通、后台维护或导入流程管理。

### 7.2 前端扩展

| 文件 | managed block / API | 用途 |
|------|---------------------|------|
| `frontend/src/main.ts` | `imports` | CLI 写入 Mango 包和业务页面包 import |
| `frontend/src/main.ts` | `features` | CLI 写入 full 或 custom feature registrars |
| `frontend/src/main.ts` | `business-registrars` | `mango module add` 写入业务页面注册 |
| `frontend` 下的 `public` runtime config 文件 | `modules` | runtime module 配置 |
| `frontend/package.json` | `workspaces` | 业务前端包默认放在 `packages/*` |

full preset 前端入口使用 `createMangoAdminApp` 和 `mangoFullAdminFeatureRegistrars`。custom preset 会按模块生成独立 registrar import。

## 8. 数据与初始化
模板自身不直接写生产业务数据；数据库结构、平台默认资源和业务模块基线由后端启动时的 Flyway 模块、Resource Registry 和模块自身初始化流程处理。

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 | 排查入口 |
|------|------|------------|-----------------|----------|----------|
| 平台 Flyway module | `mango.persistence.flyway.modules.*.enabled` | system、domain、authorization、identity、org、captcha、file、template、workflow、job、kv、notice、calendar、numgen 等平台表和基础数据 | 各模块 migration version | 后端启动时 | 查 Flyway history 和后端日志 |
| Resource Registry | 各平台模块和业务模块 `resource-manifest.json` | 菜单、权限、业务域、字典、系统参数、模板、编号规则等声明式资源 | resource code 和目标模块幂等键 | 模块启动同步时 | 查资源同步日志、目标表和模块 README |
| 业务 Flyway module | `business-flyway-modules` managed block | `mango module add` 生成的业务模块 migration | 业务 module code 和 Flyway version | 后端启动时 | 查业务表和 Flyway history |
| 业务开通 / 导入流程 | 业务后台、导入任务或开通 API | 租户、组织架构、账号、岗位、业务基础档案等生产数据 | 业务唯一键和租户边界 | 业务操作或任务执行时 | 查业务操作日志、导入结果和目标表 |
| 文件本地存储 | `mango.file.local.root-path` | 上传文件落盘目录 | 文件 object key | 文件上传时 | 查 `data/files` 或自定义目录 |
| IP 地址库 | `mango.ip-location.xdb-location` | ip2region xdb 文件 | 文件路径 | IP 定位能力调用时 | 确认 `config/ip-location/ip2region_v4.xdb` 存在 |

生产环境不得使用模板默认 JWT secret 或空数据库密码。

## 9. 管理入口
full preset 会启用授权、身份、组织、系统等平台模块的 migration 和资源同步能力。菜单、权限和租户事实来自各平台模块及后续业务模块的 `resource-manifest.json` 或 migration。

| 菜单 / 页面 | component key | 权限码 | 入库来源 | 默认套餐 / 角色 | 后端校验入口 |
|-------------|---------------|--------|----------|-----------------|--------------|
| Mango 平台页面 | 各 `@mango/*` 包登记 | 各平台模块定义 | 平台模块 migration 或 resource manifest | 平台模块资源声明或业务授权流程定义 | 各平台模块 Controller / Service |
| 业务模块页面 | `mango module add` 生成的 component key | 业务 resource manifest 定义 | 业务 `<module>-starter` resource manifest | 业务授权流程定义 | 业务 Controller / Service |

租户默认值来自 `mango.persistence.mybatis-plus.tenant.default-tenant-id`。业务模块如果继承 `TenantEntity`，必须验证当前租户上下文、查询条件和写入字段。

## 10. 快速开始
1. 生成项目后先执行 `mango workspace init`，修改 `.mango/dev-workspace.env` 中数据库、端口、文件目录和可选插件开关。
2. 执行 `mango validate`、`mango dev doctor`、`mango dev plan`，确认启动计划。
3. 执行 `mango dev start`，确认后端 health 和前端页面可访问。
4. 首次启动后确认 Flyway、Resource Registry 和模块初始化日志；租户、组织、账号等生产数据通过业务开通、后台维护或导入流程补齐。
5. 通过 `mango module add` 新增业务模块，然后补齐表结构、菜单权限、租户边界、页面交互和测试。
6. 每个业务能力完成后，把模块 README、交付契约、验证证据和 E2E 更新到业务仓库。

## 11. 问题排查
| 问题 | 原因 | 处理方式 |
|------|------|----------|
| `mango CLI not found in project frontend dependencies or globally` | 前端依赖未安装或缺少 `@mango/cli`，且机器没有全局 CLI | 先执行 `cd frontend && pnpm install`，或安装全局 `@mango/cli` |
| 前端请求后端失败 | `VITE_ADMIN_PROXY_PATH` 未指向本机后端，或后端未启动 | 用 `mango dev plan` 查看代理目标，用 `mango dev status` 查看后端 |
| Vite proxy 报 host 不允许 | `vite.config.ts` 只允许本机代理 | 本地开发保持代理到 `127.0.0.1` 或 `localhost` |
| 后端 health 访问失败 | 数据库、Flyway、端口或密钥配置错误 | 查 `mango dev logs {{projectKebab}}-service` |
| 文件上传大小不符合预期 | multipart 上限仍是模板默认值 | 修改 `spring.servlet.multipart.max-file-size` 和 `max-request-size` |
| Office 预览不可用 | `MANGO_OFFICE_PLUGIN_ENABLED=false` | 安装并确认 Office 插件后再启用 |
| 菜单没有初始化 | 对应模块 migration 或资源同步未执行 | 查 Flyway history、资源同步日志和授权模块配置 |
| 租户或管理员账号为空 | 业务开通、后台维护或导入流程未执行 | 执行业务初始化 runbook，并检查 identity、org、authorization 目标表 |

## 12. 相关文档
- [业务 PMO 入口](./business-pmo/README.md)
- [业务 baseline](./business-pmo/mango-baseline/README.md)
- [单体拓扑说明](./topologies/monolith/README.md)
- [微服务拓扑说明](./topologies/microservice/README.md)

- [生成项目 PMO baseline](./business-pmo/mango-baseline/README.md)
- [交付契约示例](./business-docs/plans/example-contract.md)
- [交付台账示例](./business-docs/plans/example-ledger.md)
