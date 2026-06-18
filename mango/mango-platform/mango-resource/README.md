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

## 5. 配置说明

`mango-resource` 配置前缀为 `mango.resource.registry`。

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enabled` | `true` | 是否启用资源注册和资源声明同步。 |
| `fail-on-conflict` | `true` | 资源 ID 或 `resourceType + bizKey` 冲突时是否启动失败。 |
| `instance-id` | 空 | 同步锁持有者标识，为空时使用当前 JVM 进程名。 |
| `lock-ttl-seconds` | `300` | 多实例同步锁 TTL。 |
| `locations` | `classpath*:META-INF/mango/resources/*.{json,yml,yaml}` | 声明文件扫描路径。 |
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
| YAML | classpath 下声明文件，适合字典、模板、编号规则等显式配置。 |
| JSON | 与 YAML 相同结构。 |
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
META-INF/mango/resources/{module}-common-{resource}.yml
```

示例：

```text
payment-common-domain.yml
payment-common-numgen.yml
job-common-message.yml
template-common-dict.yml
```

## 7. YAML 示例

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
| `sync-mode` | `ENUM` | 否 | `AUTO`、`MANUAL`、`LOCKED`，默认 `AUTO`。 |
| `status` | `ENUM` | 否 | `ACTIVE`、`DISABLED`、`DEPRECATED`、`REMOVED`，默认 `ACTIVE`。 |
| `fields` | `MAP` | 是 | 目标模块定义的字段。 |

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
| hash 或 version 变化 | `AUTO` 允许覆盖目标资源；`MANUAL`、`LOCKED` 跳过。 |
| 声明状态为 `DISABLED`、`DEPRECATED` | 调用目标模块 `disable`，目标模块负责逻辑禁用。 |
| 声明状态为 `REMOVED` | 调用目标模块 `delete`；目标模块不支持物理删除时降级为 `disable`。 |
| 强制同步 | 后台 `/resource/sync/force` 触发，跳过 hash 未变化限制。 |

多实例启动时通过 `mango-infra-kv` 的 `ILocker` 抢占 `mango-resource-sync` 锁，抢到锁的实例执行同步，其它实例跳过。

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
2. 在声明来源模块中加入 `mango-resource-api`，并提供 YAML/JSON 声明或 `ResourceProvider`。
3. 在目标模块中实现对应资源类型的 `ResourceHandler`。
4. 启动应用后查看 `/resource/registries/page`、`/resource/sync-logs/page` 和目标模块数据。
5. 声明内容变更后递增 `version`，再重启应用或调用 `/resource/sync/force`。

## 13. 已接入资源类型

| 资源类型 | 消费模块 | 字段契约 |
|----------|----------|----------|
| `SYSTEM_DICT` | `mango-system` | 见 `mango-system` README。 |
| `SYSTEM_CONFIG` | `mango-system` | 见 `mango-system` README。 |
| `BUSINESS_DOMAIN` | `mango-domain` | 见 `mango-domain` README。 |
| `API_RESOURCE` | `mango-authorization` | 由接口权限 Provider 扫描生成，见 `mango-authorization` README。 |
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
