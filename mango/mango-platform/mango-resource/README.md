# Mango Resource

## 1. 概览

`mango-resource` 是 Mango 的资源注册中心，只负责资源声明采集、注册、同步、覆盖控制、审计和变更追踪。

它不保存目标资源内容。字典、系统参数、消息模板、工作流配置、编号规则、打印模板、文件配置等真实数据仍由各目标模块自己的表维护。

## 2. 模块结构

| 模块 | 职责 |
|------|------|
| `mango-resource-api` | 对外契约，包含 `ResourceProvider`、`ResourceHandler`、`ResourceDeclaration`、`ResourceRegistryApi`、资源类型和字段模型。 |
| `mango-resource-support` | 声明文件加载、声明采集和配置绑定。 |
| `mango-resource-core` | 本地注册中心核心逻辑，负责同步编排、hash 比对、锁、注册表、同步日志和变更日志。 |
| `mango-resource-starter` | 本地资源注册中心 starter，装配 core、Mapper 和 `/resource/**` 管理接口。 |
| `mango-resource-sync-starter` | 资源声明扫描和上报 runner，汇总文件声明和 Java Provider 后调用 `ResourceRegistryApi`。 |
| `mango-resource-starter-remote` | 微服务远程适配 starter，提供 `ResourceRegistryApi` Feign 实现。 |

## 3. 功能清单

- 采集 classpath JSON/YAML 资源声明和 Java `ResourceProvider` 声明。
- 将声明写入 `resource_registry`，并记录同步日志和变更日志。
- 按资源类型调用目标模块 `ResourceHandler` 完成创建、更新、禁用和删除。
- 支持 `AUTO`、`MANUAL`、`LOCKED` 同步模式和强制同步。
- 支持本地单体注册中心和微服务远程上报两种拓扑。
- 提供后台管理接口查询注册资源、同步日志、变更日志和处理器字段契约。

## 4. 接入方式

开发期只依赖 API：

```xml
<dependency>
    <groupId>io.mango.platform.resource</groupId>
    <artifactId>mango-resource-api</artifactId>
</dependency>
```

本地提供资源注册中心的应用依赖：

官方单体聚合入口 `mango-admin-starter` 已包含以下本地 Resource Registry runtime；只有自定义单体聚合或平台服务入口需要显式声明。

```xml
<dependency>
    <groupId>io.mango.platform.resource</groupId>
    <artifactId>mango-resource-starter</artifactId>
</dependency>
<dependency>
    <groupId>io.mango.platform.resource</groupId>
    <artifactId>mango-resource-sync-starter</artifactId>
</dependency>
```

微服务中只上报资源声明的业务应用依赖：

```xml
<dependency>
    <groupId>io.mango.platform.resource</groupId>
    <artifactId>mango-resource-starter-remote</artifactId>
</dependency>
<dependency>
    <groupId>io.mango.platform.resource</groupId>
    <artifactId>mango-resource-sync-starter</artifactId>
</dependency>
```

普通业务模块和公共能力模块不得依赖 `mango-resource-core`、`mango-resource-starter`、`mango-resource-sync-starter` 或 `mango-resource-starter-remote`；部署应用按拓扑选择 starter。

### 4.1 业务部署拓扑

| 部署方式 | 依赖选择 | 说明 |
|----------|----------|------|
| 官方单体 | `mango-admin-starter` | 已包含本地 Resource Registry runtime 和声明同步入口。 |
| 自定义单体 | `mango-resource-starter` + `mango-resource-sync-starter` | 同一 JVM 内采集声明、写 registry、调用本地 handler。 |
| Resource 能力服务 | `mango-resource-starter` | 作为远程注册中心和 target dispatch 入口。 |
| 普通微服务或能力 app | `mango-resource-starter-remote` + `mango-resource-sync-starter` | 只采集本服务声明并远程上报。 |
| 目标资源服务 | 本模块 starter + 远程 target controller | 消费跨服务分发过来的目标资源批次。 |

业务 `core`、`support`、普通 starter 模块只声明资源或实现 handler，不直接选择部署拓扑。是否本地同步或远程上报由最终 app 决定。

## 5. 配置说明

`mango-resource` 配置前缀为 `mango.resource.registry`。

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enabled` | `true` | 是否启用资源注册和资源声明同步。 |
| `fail-on-conflict` | `true` | 资源 ID 或 `resourceType + bizKey` 冲突时是否启动失败。 |
| `instance-id` | 空 | 同步锁持有者标识，为空时使用当前 JVM 进程名。 |
| `lock-ttl-seconds` | `300` | 多实例同步锁 TTL。 |
| `locations` | `classpath*:META-INF/mango/resources/*.{json,yml,yaml}` | 声明文件扫描路径。 |
| `demo-enabled` | `false` | 是否额外扫描 demo 资源声明。 |
| `demo-locations` | `classpath*:META-INF/mango/demo/*.{json,yml,yaml}` | demo 资源声明扫描路径。 |
| `remote.enabled` | `true` | `mango-resource-sync-starter` 是否向注册中心上报声明。 |
| `remote.app-code` | 空 | 远程上报应用编码，空时取 `spring.application.name`。 |
| `remote.service-code` | 空 | 远程上报服务编码，空时取 `spring.application.name`。 |

本地注册中心应用必须提供 `mango-infra-kv` 的 `ILocker` 实现。Mango 单体和平台服务默认通过 `mango-infra-kv-starter` 提供 JDBC 或内存 KV 能力。

## 6. API 与扩展

核心扩展点：

| 类型 | 用途 |
|------|------|
| `ResourceProvider` | Java 代码提供资源声明。 |
| `ResourceHandler` | 目标模块消费资源声明并落库。 |
| `ResourceRegistryApi` | 本地或远程注册资源声明。 |
| `ResourceHandlerSpec` | 暴露资源处理器字段契约，供后台和文档查看。 |

资源声明来源支持：


| 来源 | 说明 |
|------|------|
| JSON | classpath 下声明文件，适合菜单树等数据量较大的结构化资源。 |
| YAML | 与 JSON 相同结构，适合字典、模板、编号规则等少量显式配置。 |
| Java Provider | 实现 `ResourceProvider`，直接返回资源声明。 |
| 自定义扫描 Provider | 适合接口权限、菜单、运行时路由等不适合写成配置文件的资源。 |

默认扫描路径：

```text
classpath*:META-INF/mango/resources/*.json
classpath*:META-INF/mango/resources/*.yml
classpath*:META-INF/mango/resources/*.yaml
```

推荐命名：

```text
META-INF/mango/resources/{module}-common-{resource}.{json,yml,yaml}
```

示例：

```text
workflow-common-menu.json
payment-common-domain.yml
payment-common-numgen.yml
job-common-message.yml
template-common-dict.yml
```

菜单树等大资源优先使用 JSON，减少重复缩进和视觉噪音；少量配置可使用 YAML。

声明文件必须包含 `schemaVersion`、`moduleCode`、`moduleName` 和 `declarations`。loader 会校验 schema 版本和结构完整性；错误信息包含来源路径，方便定位坏文件。

新增菜单使用 `AUTH_MENU`，例如：

```text
src/main/resources/META-INF/mango/resources/system-common-menu.json
```

新增字典、系统参数、消息模板、编号规则、打印模板、文件配置等资源使用对应目标模块 README 中列出的资源类型和字段契约。

### 6.1 当前已开放资源类型

Resource Registry 能否同步某个资源类型，以运行时是否装配对应目标模块的 `ResourceHandler` 为准。`ResourceTypes` 中的常量不等于已经开放使用。

当前代码中已有 handler 的资源类型如下：

| 目标模块 | 已开放资源类型 |
|----------|----------------|
| `mango-system` | `SYSTEM_DICT`、`SYSTEM_CONFIG`、`I18N_MESSAGE` |
| `mango-domain` | `BUSINESS_DOMAIN` |
| `mango-authorization` | `AUTH_MENU`、`AUTH_ROLE`、`AUTH_ROLE_DATA_SCOPE`、`AUTH_SUBJECT_ROLE`、`API_RESOURCE`、`FRONTEND_APP_REGISTRY`、`FRONTEND_MODULE_RUNTIME_STRATEGY` |
| `mango-org` | `ORG_UNIT`、`ORG_POST` |
| `mango-identity` | `IDENTITY_USER`、`ORG_MEMBER_BINDING` |
| `mango-notice` | `MESSAGE_CHANNEL`、`MESSAGE_TEMPLATE` |
| `mango-workflow` | `WORKFLOW_CATEGORY`、`WORKFLOW_TEMPLATE_CATEGORY`、`WORKFLOW_NODE_DEFINITION` |
| `mango-numgen` | `SEQUENCE_RULE` |
| `mango-template` | `PRINT_TEMPLATE` |
| `mango-job` | `JOB_DEFINITION` |
| `mango-file` | `FILE_STORAGE_CONFIG`、`FILE_SETTINGS` |

声明新资源前先确认：

- 目标应用已经依赖对应目标模块 starter 或 core，并能装配该类型的 `ResourceHandler`。
- 字段名和字段类型以目标 handler 暴露的 `ResourceHandlerSpec` 为准，可通过 `/resource/handler-specs` 查看当前应用实际装配结果。
- 资源声明只能证明 Resource Registry 调用了目标 handler；是否覆盖目标模块业务语义，要由目标模块自己的 README、集成测试或验收用例说明。

以下资源类型目前只是保留常量或设计预留，不能作为已支持能力使用：

| 资源类型 | 当前状态 | 现阶段使用方式 |
|----------|----------|----------------|
| `MESSAGE_EVENT` | 无目标表字段契约和 `ResourceHandler`。 | 通知资源当前使用 `MESSAGE_CHANNEL` 和 `MESSAGE_TEMPLATE`。事件、路由或触发规则需要等 notice 模块补齐 handler 后再开放。 |
| `WORKFLOW_DEFINITION` | 无 `ResourceHandler`。 | 工作流当前只开放分类、模板分类和节点定义资源；流程定义仍由 workflow 模块自身的发布、初始化或业务入口管理。 |
| `AI_PROMPT` | 无 `mango-ai` 目标模块运行时和 `ResourceHandler`。 | 暂不通过 Resource Registry 声明 AI Prompt。 |

## 7. 声明文件示例

```yaml
mango:
  resource:
    schema-version: 1
    module-code: payment
    module-name: 支付
    declarations:
      SEQUENCE_RULE:
        - id: "2026061800600000002"
          version: 1
          biz-key: payment.numgen.pay-order-no
          name: 支付订单号
          target-module: numgen
          sync-mode: AUTO
          status: ACTIVE
          fields:
            generatorId: { type: LONG, value: 900000000002 }
            genKey: { type: STRING, value: PAY_ORDER_NO }
            genName: { type: STRING, value: 支付订单号 }
            domainCode: { type: STRING, value: PAYMENT }
```

## 8. 通用字段

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，使用雪花算法生成并永久保持稳定。 |
| `version` | `INT` | 是 | 资源声明版本，声明内容升级时递增。 |
| `resource-type` | `STRING` | 由分组决定 | 资源类型，例如 `SYSTEM_DICT`、`SEQUENCE_RULE`。 |
| `module-code` | `STRING` | 是 | 声明来源模块。 |
| `module-name` | `STRING` | 否 | 声明来源模块名称。 |
| `biz-key` | `STRING` | 是 | 资源业务稳定键，推荐 `业务域.对象.动作`。 |
| `name` | `STRING` | 否 | 资源显示名。 |
| `target-module` | `STRING` | 是 | 消费资源的目标模块。 |
| `sync-mode` | `ENUM` | 否 | `AUTO`、`INIT_ONLY`、`MANUAL`、`LOCKED`，默认 `AUTO`。 |
| `status` | `ENUM` | 否 | `ACTIVE`、`DISABLED`、`DEPRECATED`、`REMOVED`，默认 `ACTIVE`。 |
| `fields` | `MAP` | 是 | 目标模块定义的字段。 |

`sync-mode` 语义：

| 模式 | 行为 | 适用场景 |
|------|------|----------|
| `AUTO` | 声明版本或内容变化时自动调用目标 handler 覆盖目标数据。 | 后续升级应由模块持续维护的数据。 |
| `INIT_ONLY` | 首次同步创建目标数据；目标已存在后只更新 registry 声明元数据，不覆盖目标业务表。 | 菜单、角色、流程模板等初始化后允许用户或业务运行时修改的数据。 |
| `MANUAL` | registry 已接管为人工维护后跳过声明同步。 | 运维或管理员临时接管的资源。 |
| `LOCKED` | registry 锁定后跳过声明同步。 | 禁止声明继续改动的保护资源。 |

声明文件中 `sync-mode` 支持 `INIT_ONLY`、`init-only` 和 `init_only` 写法。

字段类型支持：

| 类型 | 说明 |
|------|------|
| `STRING` | 字符串。 |
| `INT` | 32 位整数。 |
| `LONG` | 64 位整数。 |
| `DECIMAL` | 十进制数。 |
| `BOOLEAN` | 布尔值。 |
| `DATE` | 日期。 |
| `DATETIME` | 日期时间。 |
| `JSON` | JSON 内容。 |
| `OBJECT` | 对象。 |
| `LIST` | 列表。 |
| `FILE` | 文件引用，支持 `classpath:` 地址。 |

## 9. 数据与初始化

Flyway 路径：`mango-resource-core/src/main/resources/db/migration/resource`。

| 表 | 作用 |
|----|------|
| `resource_registry` | 记录资源 ID、类型、bizKey、目标表、目标 ID、hash、同步模式和状态。 |
| `resource_sync_log` | 记录每次同步、跳过、禁用、删除的结果。 |
| `resource_change_log` | 记录注册资源内容变化。 |

资源注册中心只保存声明索引和同步审计数据。字典、系统参数、消息模板、编号规则、工作流配置等目标资源仍由目标模块自己的 migration 和资源处理器维护。

应用启动时由 `ResourceSyncRunner` 运行时初始化器扫描声明并幂等同步；本地注册中心由 `ResourceRegistrySyncService` 完成目标资源 upsert、disable 和 delete。

## 10. 同步规则

| 场景 | 行为 |
|------|------|
| 新资源 | 写入 `resource_registry`，调用目标模块 `ResourceHandler.upsert`。 |
| hash 或 version 变化 | `AUTO` 允许覆盖目标资源；`INIT_ONLY` 已存在时只更新 registry 元数据；`MANUAL`、`LOCKED` 跳过。 |
| version 回退 | 拒绝同步，避免旧声明覆盖新资源。 |
| 声明状态为 `DISABLED` | 调用目标模块 `disable`，目标模块负责逻辑禁用。 |
| 声明状态为 `DEPRECATED` | 只更新注册中心声明状态和审计，目标资源继续可读，不调用 `upsert` 或 `disable`。 |
| 声明状态为 `REMOVED` | 调用目标模块 `delete`；目标模块不支持物理删除时降级为 `disable`。 |
| 强制同步 | 后台 `/resource/sync/force` 触发，跳过 hash 未变化限制。 |

多实例启动时通过 `mango-infra-kv` 的 `ILocker` 抢占 `mango-resource-sync` 锁，抢到锁的实例执行同步，其它实例跳过。

远程同步会携带来源 `appCode`、`serviceCode` 和 `moduleCodes`。缺失声明禁用只在同一来源服务和模块范围内计算，避免多个服务共用 `moduleCode` 时互相误禁用。批量 dispatch 会按 `targetModule` 分桶，不能把不同目标模块的同类资源发给同一个目标服务。

## 11. 管理入口

`mango-resource-starter` 提供 `/resource/**` 接口：

| 接口 | 用途 |
|------|------|
| `POST /resource/declarations/register` | 远程服务上报资源声明。 |
| `GET /resource/registries/page` | 查询注册资源。 |
| `POST /resource/sync/force` | 强制重新同步。 |
| `DELETE /resource/registries` | 删除注册资源，支持逻辑删除和物理删除。 |
| `GET /resource/sync-logs/page` | 查询同步日志。 |
| `GET /resource/change-logs/page` | 查询变更日志。 |
| `GET /resource/handler-specs` | 查询当前应用已装配的资源处理器字段契约。 |

管理接口通过 `@ApiAccess` 声明权限码，权限包括 `system:resource:registry:list`、`system:resource:sync:force`、`system:resource:registry:delete`、`system:resource:sync-log:list`、`system:resource:change-log:list` 和 `system:resource:handler:list`。对应菜单、租户授权和页面 component key 由授权资源注册链路消费。

## 12. 快速开始

1. 在提供注册中心的应用中加入 `mango-resource-starter` 和 `mango-resource-sync-starter`。
2. 在声明来源模块中加入 `mango-resource-api`，并提供 JSON/YAML 声明或 `ResourceProvider`。
3. 在目标模块中实现对应资源类型的 `ResourceHandler`。
4. 启动应用后查看 `/resource/registries/page`、`/resource/sync-logs/page` 和目标模块数据。
5. 声明内容变更后递增 `version`，再重启应用或调用 `/resource/sync/force`。

发布或升级前至少确认：

```text
resource_registry                 有当前模块声明
resource_sync_log                 当前批次 SUCCESS
目标模块表                         有 handler 写入结果
authorization_menu                AUTH_MENU 菜单无孤儿
authorization_api_resource         API_RESOURCE 访问模式正确
```

业务库从 Flyway 菜单初始化升级到 Resource Registry 菜单初始化时，需要备份并使用干净库重建验证。不要用手工 SQL 修复菜单、套餐、角色菜单或前端运行配置。

## 13. 已接入资源类型

| 资源类型 | 消费模块 | 字段契约 |
|----------|----------|----------|
| `SYSTEM_DICT` | `mango-system` | 见 `mango-system` README。 |
| `SYSTEM_CONFIG` | `mango-system` | 见 `mango-system` README。 |
| `BUSINESS_DOMAIN` | `mango-domain` | 见 `mango-domain` README。 |
| `AUTH_MENU` | `mango-authorization` | 菜单、按钮权限、菜单运行时配置、套餐授权和默认角色授权注入。 |
| `API_RESOURCE` | `mango-authorization` | 由接口权限 Provider 扫描生成，见 `mango-authorization` README。 |
| `AUTH_ROLE` | `mango-authorization` | 按租户和应用声明角色基线。 |
| `AUTH_ROLE_DATA_SCOPE` | `mango-authorization` | 按角色声明数据权限基线。 |
| `AUTH_SUBJECT_ROLE` | `mango-authorization` | 按 `subjectId`、`subjectCode`、`memberNo` 或 `username` 声明成员角色绑定基线。 |
| `ORG_UNIT` | `mango-org` | 按组织编码声明租户内组织基线。 |
| `ORG_POST` | `mango-org` | 按岗位编码声明租户内岗位基线。 |
| `ORG_MEMBER_BINDING` | `mango-identity` | 声明租户成员和组织、岗位的绑定关系。 |
| `IDENTITY_USER` | `mango-identity` | 声明 demo/bootstrap 用户和租户成员；声明中的初始密码会由 handler 加密保存。 |
| `MESSAGE_CHANNEL` | `mango-notice` | 见 `mango-notice` README。 |
| `MESSAGE_TEMPLATE` | `mango-notice` | 见 `mango-notice` README。 |
| `I18N_MESSAGE` | `mango-system` | 见 `mango-system` README。 |
| `WORKFLOW_CATEGORY` | `mango-workflow` | 见 `mango-workflow` README。 |
| `WORKFLOW_TEMPLATE_CATEGORY` | `mango-workflow` | 见 `mango-workflow` README。 |
| `WORKFLOW_NODE_DEFINITION` | `mango-workflow` | 见 `mango-workflow` README。 |
| `SEQUENCE_RULE` | `mango-numgen` | 见 `mango-numgen` README。 |
| `PRINT_TEMPLATE` | `mango-template` | 见 `mango-template` README。 |
| `JOB_DEFINITION` | `mango-job` | 见 `mango-job` README。 |
| `FILE_STORAGE_CONFIG` | `mango-file` | 见 `mango-file` README。 |
| `FILE_SETTINGS` | `mango-file` | 见 `mango-file` README。 |

## 14. 问题排查

| 现象 | 处理方式 |
|------|----------|
| 启动时报资源冲突 | 检查声明文件中的 `id` 和 `resourceType + bizKey` 是否重复。 |
| 资源未覆盖目标数据 | 检查注册表中的 `sync_mode` 是否为 `AUTO`，以及声明 `version` 或内容 hash 是否变化。 |
| 多实例重复同步 | 检查 `mango-infra-kv` 是否正常提供 `ILocker`，以及 `lock-ttl-seconds` 是否过短。 |
| 微服务未上报资源 | 检查业务服务是否引入 `mango-resource-starter-remote` 和 `mango-resource-sync-starter`，并确认注册中心接口可访问。 |
| 声明文件未加载 | 检查文件是否位于 `META-INF/mango/resources/`，扩展名是否为 `.json`、`.yml` 或 `.yaml`。 |

## 15. 相关文档

- [Resource Registry 设计方案](../../../mango-docs/designs/mango-resource-registry-design.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
