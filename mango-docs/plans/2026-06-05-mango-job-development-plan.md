# Mango Job 任务调度开发计划

## 1. 背景

Mango 任务调度能力采用 Mango 原生 Job 契约和统一 UI，底层优先集成 PowerJob。多数据源底座已经完成，`mango-job` 按 `mango_{module}` 规则使用独立 `mango_job` 数据库；PowerJob 内部表可与 `mango_job` 同库共置，也可使用独立 `powerjob` 数据库或 schema。

设计说明：`mango-docs/designs/mango-job-design.md`

交付台账：`mango-docs/plans/2026-06-05-mango-job-delivery-ledger.md`

## 2. 目标

- 新增 `mango-platform/mango-job` 平台模块。
- 提供 Mango Job 原生契约和治理表。
- 接入 PowerJob 作为第一调度引擎。
- 提供后台 API、菜单、权限和统一 UI。
- 支持单体部署 `Job Center + Worker`、独立 Job Center、远程 Worker 和共享 PowerJob Server 部署。
- 给出可验证、可发布、可回归的分阶段交付。
- PowerJob 实现基线使用最新稳定版本，当前锁定 `5.1.2`；进入编码前复核 Maven 坐标、Server 镜像、安全公告和兼容 API。

## 3. 范围

### 3.1 本轮做

- `mango-job-api`、`mango-job-core`、`mango-job-starter`、`mango-job-starter-remote` 模块设计和实现。
- `MangoJobEngine` SPI。
- `PowerJobEngineAdapter`。
- 业务处理器注册契约和触发上下文。
- `mango_job` 数据库 migration，物理库名遵循 `mango-pmo/rules/backend/07-persistence.md`。
- Job 定义、实例、日志索引、Worker 快照、告警规则和引擎映射。
- 后端 REST API。
- 后台菜单、权限种子和前端页面。
- 单测、集成测试、前端 E2E。
- 发布 `mango-job` 相关 Maven 模块和前端包。

### 3.2 本轮不做

- 不自研调度执行引擎。
- 不复制 PowerJob 或 XXL-JOB 源码。
- 不改 PowerJob 内部表结构。
- 不实现 XXL-JOB Adapter。
- 不实现 Quartz Adapter。
- 不提供 PowerJob Console iframe 作为正式页面。
- 不提供跨库强一致事务。
- 不默认启用脚本任务；HTTP 任务只开放白名单内受控调用。

## 4. 影响模块

| 模块 | 改动 |
|---|---|
| `mango-platform/mango-job` | 新增 Job 平台模块。 |
| `mango-infra-persistence-starter` | 消费已发布多数据源能力，原则上不改底座。 |
| `mango-platform/mango-authorization` | 新增菜单和权限种子接入。 |
| `mango-platform/mango-notice` | 告警通知接入，Job 只配置消息模板和启用策略。 |
| `mango-ui/packages/admin-pages` | 新增任务调度页面注册。 |
| `mango-ui/apps/mango-admin-shell` | 验证菜单和运行态，不改 Shell 主框架。 |
| `mango-docs` | 更新设计、计划和交付记录。 |

## 5. Sprint 拆分

### Sprint 0：设计、契约和台账

目标：完成 Job 研发资料，锁定范围和验收。

交付物：

- `mango-docs/designs/mango-job-design.md`
- `mango-docs/plans/2026-06-05-mango-job-development-plan.md`
- `mango-docs/plans/2026-06-05-mango-job-delivery-ledger.md`

验收：

- 设计文档包含目标、范围、不做范围、组件调研、模块边界、接口、数据、菜单、权限、测试。
- 计划按 Sprint 拆分。
- 交付台账原子项完整。

验证：

```bash
node mango-pmo/tools/delivery-contract-check.mjs \
  --design mango-docs/designs/mango-job-design.md \
  --ledger mango-docs/plans/2026-06-05-mango-job-delivery-ledger.md \
  --mode verify
```

### Sprint 1：模块骨架和数据库模型

目标：建立 `mango-job` 模块、数据源声明和治理表。

改动项：

- 新建 `mango-platform/mango-job` 聚合模块。
- 新建 `api/core/starter/starter-remote` 子模块。
- 增加 `META-INF/mango/module.properties`，声明 `persistence-datasource=job`。
- 增加 `mango_job` migration。
- 数据库名按 `mango_{module}` 规则由 `mango-job` / `job` 归一化得到，数据源 key 使用 `job`。
- 增加任务定义唯一约束、实例查询索引、引擎映射索引。
- 增加 Entity、Mapper、Service 基础结构。

验收：

- Maven reactor 能识别 `mango-job`。
- Flyway 将 `db/migration/mango-job` 迁移到 `job` 数据源。
- 表结构通过 `mango:check -Drule=persistence-schema`。
- `job` 数据源未注册时，本地开发可回退 `primary`；生产配置必须显式注册 `job` 数据源。

建议验证：

```bash
cd mango
mvn -pl mango-platform/mango-job -am test
mvn mango:check -Drule=persistence-schema
```

### Sprint 2：Job 原生契约和后端 API

目标：完成任务定义、实例、日志索引和 Worker 查询的 Mango API。

改动项：

- 增加 Command、Query、VO、枚举。
- 增加 `MangoJobHandler` 业务处理器契约。
- 增加处理器注册和查询接口。
- 增加任务定义 CRUD。
- 增加启用、禁用、暂停、恢复、删除状态流转。
- 增加手动触发入口。
- 增加实例、日志、Worker、引擎状态查询接口。
- 增加权限校验和租户过滤。

验收：

- 任务定义可创建、修改、查询、删除。
- 状态流转非法路径返回明确业务异常。
- 查询按租户隔离。
- API 不暴露 Entity。
- 业务模块不直接依赖 PowerJob Processor。

建议验证：

```bash
cd mango
mvn -pl mango-platform/mango-job/mango-job-core -am test
```

### Sprint 3：PowerJob Adapter

目标：把 Mango Job 契约映射到 PowerJob。

改动项：

- 定义 `MangoJobEngine` SPI。
- 实现 `PowerJobEngineAdapter`。
- 增加 PowerJob 配置属性。
- 增加 app、job、instance 映射。
- 增加 `sync_status` 同步状态和失败重试。
- 增加状态同步、日志拉取、终止实例。
- 增加引擎连接健康检查。
- 增加 PowerJob Server 认证配置校验。
- Mango 内部 API 复用内部调用安全机制，PowerJob Server 保留访问令牌和网络隔离。

验收：

- 创建 Mango 任务后 PowerJob 存在对应任务。
- 手动触发后 Mango 实例记录能关联 PowerJob instance。
- PowerJob 异常转换为 Mango 业务异常。
- 前端和业务 API 不出现 PowerJob 内部模型。
- 同步失败可查询并重试。
- Mango UI 和服务间接口复用 Mango 安全能力，PowerJob Server 仍使用访问令牌和网络隔离。

建议验证：

```bash
cd mango
mvn -pl mango-platform/mango-job/mango-job-starter -am test
```

集成环境验证：

```bash
docker compose -f deploy/job/docker-compose.powerjob.yml up -d
```

### Sprint 4：菜单、权限和统一 UI

目标：完成前后端同步交付。

改动项：

- 增加后台菜单种子。
- 增加权限码。
- 增加前端页面：任务定义、执行实例、执行日志、执行器、告警规则、引擎状态。
- 增加处理器页面。
- 注册页面组件。
- 增加 API client。
- 增加加载、空、错误态。

验收：

- 登录后能看到任务管理菜单。
- 菜单入口为 `平台能力 -> 任务管理`。
- 权限按钮按权限码显示。
- 单体和 Shell 菜单一致。
- 页面使用真实 API。
- 操作后列表和详情回显真实数据。
- 处理器列表展示所属应用、模块、参数 schema 和健康状态。

建议验证：

```bash
cd mango-ui
pnpm test
pnpm --filter mango-admin-shell test:e2e
```

### Sprint 5：告警、运维和可观测性

目标：补齐生产运维能力。

改动项：

- 接入 `mango-notice` 发送失败、超时、连续失败和恢复通知。
- 配置通知场景、消息模板编码、模板参数和启用策略；短信、邮件、企业微信等通道由 `mango-notice` 负责。
- 增加任务指标。
- 增加 Worker 健康快照。
- 增加执行耗时和失败率统计。
- 增加日志保留策略配置。

验收：

- 告警规则触发后生成通知。
- 引擎状态页面显示连接和 Worker 状态。
- 指标可被测试读取。
- 日志查询不输出敏感参数。

建议验证：

```bash
cd mango
mvn -pl mango-platform/mango-job -am test
```

### Sprint 6：部署、回归和发布

目标：完成发布闭环。

改动项：

- 增加部署说明。
- 增加 PowerJob Server 配置样例。
- 增加单体 `Job Center + Worker`、独立 Job Center、远程 Worker、共享 PowerJob Server 配置样例。
- 执行后端、前端、集成和 E2E 回归。
- 发布 Maven 模块和前端包。

验收：

- 同库共置、单库降级和独立库部署均可启动。
- `mango_job` 和 PowerJob 内部表逻辑边界清晰。
- Maven 模块发布成功。
- 前端构建通过。
- 交付台账全部完成或有明确例外。

建议验证：

```bash
cd mango
mvn -pl mango-platform/mango-job -am verify
mvn mango:check -Drule=all
cd ../mango-ui
pnpm build
```

## 6. 配置草案

```yaml
mango:
  persistence:
    datasources:
      primary:
        primary: true
        url: jdbc:mysql://localhost:3306/mango
        username: mango
        password: ${MANGO_DB_PASSWORD}
      job:
        url: jdbc:mysql://localhost:3306/mango_job
        username: mango_job
        password: ${MANGO_JOB_DB_PASSWORD}
      powerjob:
        url: jdbc:mysql://localhost:3306/mango_job
        username: powerjob
        password: ${POWERJOB_DB_PASSWORD}
    modules:
      mango-job:
        datasource: job
  job:
    enabled: true
    engine:
      type: powerjob
      powerjob:
        server-address: http://powerjob-server:7700
        app-name: mango-job
        datasource: job
        access-token: ${POWERJOB_ACCESS_TOKEN}
    worker:
      enabled: true
      app-code: internal-admin
```

## 7. 测试矩阵

| 层级 | 覆盖 |
|---|---|
| 单元测试 | 状态机、参数校验、权限校验、处理器注册、Adapter 映射。 |
| Repository 测试 | MyBatis-Plus 真实路由到 `job` 数据源。 |
| 集成测试 | Mango Job + PowerJob Server + `mango_job`，覆盖 PowerJob 同库共置和同步失败重试；独立库场景额外覆盖 `powerjob`。 |
| API 测试 | CRUD、启停、触发、实例、日志、Worker。 |
| 前端测试 | 页面、表单、按钮权限、空态、错误态。 |
| E2E | 登录、菜单、创建任务、触发、查看实例和日志。 |
| 发布验证 | Maven deploy、前端 build、业务消费项目依赖解析。 |

## 8. 风险

| 风险 | 处理 |
|---|---|
| PowerJob 版本或 API 变化 | Adapter 隔离，引擎模型不进入 Mango API。 |
| 多数据源部署复杂 | 提供单库降级、PowerJob 同库共置和独立库生产配置样例。 |
| 页面能力与 PowerJob Console 有差异 | Mango UI 只承载 Mango 治理必需能力。 |
| 任务执行幂等误解 | 文档和 API 明确平台不替业务保证幂等，只记录触发和实例。 |
| 旧 XXL-JOB 资料误导 | 更新相关研发资料或登记治理项，统一以本设计为准。 |
| Mango 与 PowerJob 状态不一致 | 增加 `sync_status`、同步失败列表和运维重试。 |

## 9. 完成标准

- Job 设计文档已确认。
- 开发计划已确认。
- 交付台账可通过 PMO 检查。
- 每个 Sprint 都有可运行验证命令。
- 进入开发前从 `main` 创建独立 worktree 和任务分支。
