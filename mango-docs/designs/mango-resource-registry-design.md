# Mango Resource Registry 设计方案

## 1. 定位

`mango-resource` 是 Mango 的 Resource Registry，中文名称为资源注册中心。

它不是资源存储中心，不保存消息模板、工作流定义、编码规则、打印模板、AI Prompt、字典项、菜单权限或其它真实业务资源内容。真实资源仍归各目标模块管理，`mango-resource` 只负责注册、同步、比对、覆盖控制、审计、日志和变更追踪。

核心原则：

- Flyway 只维护结构。
- 谁的表谁维护。
- 谁的数据谁负责。
- 业务模块只声明资源。
- 目标模块负责校验、合并、落库和逻辑删除。
- Registry 不把目标表结构暴露为业务扩展协议。

## 2. 目标与范围

本设计解决业务模块向公共模块注入配置时的长期治理问题：

- 资源声明。
- 配置升级。
- 配置覆盖。
- 配置删除。
- 人工接管。
- 变更追踪。
- 版本兼容。
- 多实例启动同步。
- 冲突检测。

本设计不定义各目标模块的业务资源内容语义。例如消息模板字段、工作流 DSL、编码规则表达式、打印模板格式、AI Prompt 结构，都由对应目标模块定义。

当前可直接使用的资源类型以 `mango/mango-platform/mango-resource/README.md` 的“当前已开放资源类型”为准。本文中的目标模块接入策略和示例包含早期规划项，不能单独作为已实现 handler 清单。

## 3. 模块结构

新增平台能力模块：

```text
mango-resource
├── mango-resource-api
├── mango-resource-support
├── mango-resource-core
├── mango-resource-starter
├── mango-resource-sync-starter
└── mango-resource-starter-remote
```

职责：

| 子模块 | 职责 |
|---|---|
| `mango-resource-api` | 定义 `ResourceProvider`、`ResourceHandler`、`ResourceDeclaration`、`ResourceType`、`ResourceSpec`、`ResourceRegistryApi` 等契约。 |
| `mango-resource-support` | 提供声明文件加载、声明采集、配置绑定等本域共享实现，供 `core` 和同步 starter 复用。 |
| `mango-resource-core` | 负责资源比对、覆盖控制、同步编排、锁、日志、变更记录和持久化。 |
| `mango-resource-starter` | 平台本地实现 starter，负责本地自动装配、Mapper 扫描，并提供资源管理后台接口、同步记录、变更记录、资源类型和同步配置入口。 |
| `mango-resource-sync-starter` | 资源声明同步 runner，负责汇总声明文件 Provider、Java Provider、自定义 Provider，并调用 `ResourceRegistryApi` 注册。 |
| `mango-resource-starter-remote` | 微服务远程适配 starter，只提供 `ResourceRegistryApi` 的 Feign 实现，不扫描、不落库、不声明 `module.properties`。 |

依赖方向：

```text
业务模块 -> mango-resource-api
目标模块 -> mango-resource-api
mango-resource-core -> mango-resource-api + mango-resource-support
mango-resource-starter -> mango-resource-core
mango-resource-sync-starter -> mango-resource-api + mango-resource-support
mango-resource-starter-remote -> mango-resource-api + mango-resource-support + mango-infra-feign-starter
本地部署 app -> mango-resource-starter + mango-resource-sync-starter
微服务业务 app -> mango-resource-starter-remote + mango-resource-sync-starter
```

`mango-resource` 不依赖 `mango-notice`、`mango-workflow`、`mango-numgen` 等具体业务模块。目标模块通过 `ResourceHandler` 反向注册能力。

接口权限现有的 `mango-authorization-resource-sync-starter` 与本设计模式一致：API 放契约，sync starter 扫描运行时资源并调用 API 注册。后续可将接口权限、菜单权限等声明能力作为 `mango-resource` 的资源类型接入，由自定义 `ResourceProvider` 生成声明，再由 `mango-resource-sync-starter` 统一注册。

## 4. 资源声明来源

资源声明至少支持四种来源：

1. classpath JSON。
2. classpath YAML。
3. Java Provider。
4. 自定义 `ResourceProvider`。

配置文件只用于适合显式声明的资源，例如字典、消息模板、编码规则、工作流模板、AI Prompt、打印模板。

接口权限、菜单、前端路由、模块能力、注解声明等可从代码、路由、模块清单或运行时上下文推导的资源，必须通过自定义 Provider 生成声明。Provider 内部可以使用扫描工具或运行时上下文，不要求也不建议改成 YAML/JSON 配置文件。

运行时禁止生成随机资源 ID。资源 ID 必须保持稳定：显式声明资源使用开发期雪花 ID，扫描型资源必须由扫描器提供稳定 ID 或稳定 ID 映射策略。

声明文件扫描路径：

```text
classpath*:META-INF/mango/resources/*.json
classpath*:META-INF/mango/resources/*.yml
classpath*:META-INF/mango/resources/*.yaml
```

Java Provider：

```java
public interface ResourceProvider {

    List<ResourceDeclaration> provide();
}
```

业务模块可以同时提供 Java Provider 和声明文件，但同一个资源 ID 或同一个 `resourceType + bizKey` 只能出现一次。

声明文件 Provider：

```java
public class FileResourceProvider implements ResourceProvider {

    @Override
    public List<ResourceDeclaration> provide() {
        return loader.load();
    }
}
```

典型自定义 Provider：

| Provider | 来源 | 适用资源 |
|---|---|---|
| `ApiAccessResourceProvider` | Controller、`@ApiAccess`、MVC mapping | 接口权限资源。 |
| `MenuResourceProvider` | 前端路由、模块菜单清单、注解 | 菜单和按钮资源。 |
| `GatewayRouteResourceProvider` | Gateway route definition | 网关接口资源。 |
| 业务自定义 Provider | 业务模块运行时上下文 | 无法用配置文件表达或不适合配置文件维护的资源。 |

## 5. YAML / JSON Schema

声明文件统一使用 `mango.resource` 前缀，按模块划分。一个模块声明文件可以包含多个资源类型，每个资源类型可以包含多个资源。

YAML 示例：

```yaml
mango:
  resource:
    schemaVersion: 1
    moduleCode: guarantee
    moduleName: 担保业务
    declarations:
      MESSAGE_TEMPLATE:
        - id: "739201839201839104"
          version: 1
          bizKey: guarantee.apply.submit
          name: 担保申请提交消息
          targetModule: mango-notice
          syncMode: AUTO
          status: ACTIVE
          fields:
            templateCode:
              type: string
              value: guarantee_apply_submit
            title:
              type: string
              value: 担保申请已提交
            content:
              type: file
              location: classpath:/mango/resources/guarantee/message/apply-submit.html
              encoding: UTF-8
              mediaType: text/html
            sort:
              type: int
              value: 10
      WORKFLOW_DEFINITION:
        - id: "739201839201839105"
          version: 3
          bizKey: guarantee.apply.approve
          name: 担保申请审批流
          targetModule: mango-workflow
          syncMode: AUTO
          status: ACTIVE
          fields:
            definitionCode:
              type: string
              value: guarantee_apply_approve
            definition:
              type: file
              location: classpath:/mango/resources/guarantee/workflow/apply-approve.json
              encoding: UTF-8
              mediaType: application/json
```

JSON 使用同一结构：

```json
{
  "mango": {
    "resource": {
      "schemaVersion": 1,
      "moduleCode": "guarantee",
      "moduleName": "担保业务",
      "declarations": {
        "MESSAGE_TEMPLATE": [
          {
            "id": "739201839201839104",
            "version": 1,
            "bizKey": "guarantee.apply.submit",
            "name": "担保申请提交消息",
            "targetModule": "mango-notice",
            "syncMode": "AUTO",
            "status": "ACTIVE",
            "fields": {
              "templateCode": {
                "type": "string",
                "value": "guarantee_apply_submit"
              }
            }
          }
        ]
      }
    }
  }
}
```

资源必填字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | string | 雪花算法生成的资源 ID，永久稳定。声明中用字符串保存，避免 JSON 或前端工具丢精度。 |
| `version` | int | 资源声明版本，只增不减。 |
| `bizKey` | string | 业务稳定键，格式为 `业务域.对象.动作`。 |
| `name` | string | 资源展示名。 |
| `targetModule` | string | 目标消费者模块。 |
| `syncMode` | enum | `AUTO`、`MANUAL`、`LOCKED`。 |
| `status` | enum | `ACTIVE`、`DISABLED`、`DEPRECATED`、`REMOVED`。 |
| `fields` | map | 字段由目标模块定义语义。 |

## 6. 字段类型

`fields` 是 typed map。字段名由消费者模块定义，字段值必须声明 `type`。

第一版支持：

| 类型 | 说明 |
|---|---|
| `string` | 字符串。 |
| `int` | 32 位整数。 |
| `long` | 64 位整数。 |
| `decimal` | 十进制数，建议声明值使用字符串。 |
| `boolean` | 布尔值。 |
| `date` | 日期，格式 `yyyy-MM-dd`。 |
| `datetime` | 日期时间，格式 `yyyy-MM-dd'T'HH:mm:ss` 或目标模块明确支持的格式。 |
| `json` | JSON 值。 |
| `object` | 对象结构。 |
| `list` | 列表结构。 |
| `file` | classpath 文件引用。 |

示例：

```yaml
amountLimit:
  type: decimal
  value: "100000.00"
enabled:
  type: boolean
  value: true
effectiveDate:
  type: date
  value: "2026-06-18"
extra:
  type: json
  value:
    channel: SITE
    priority: HIGH
template:
  type: file
  location: classpath:/mango/resources/notice/template.html
  encoding: UTF-8
  mediaType: text/html
```

`file` 类型第一版只支持 `classpath:` 地址。hash 计算必须包含文件内容、encoding、mediaType 和字段元信息，不能只 hash 文件路径。

## 7. ID、BizKey 与 Version

### 7.1 Resource ID

每个资源必须有稳定 `resourceId`，声明字段名为 `id`。

要求：

- ID 由雪花算法生成。
- ID 必须在开发期生成。
- ID 必须写入声明文件或 Java Provider 常量。
- ID 不能由数据库自增生成。
- ID 不能随环境变化。
- ID 不能因资源升级变化。
- ID 不携带目标表语义。

提供工具：

```bash
mango resource id
mango resource validate
```

或 Maven 插件目标：

```bash
mvn mango:resource-id
mvn mango:resource-validate
```

### 7.2 BizKey

`bizKey` 统一格式：

```text
业务域.对象.动作
```

示例：

```text
guarantee.apply.submit
guarantee.apply.approve
guarantee.issue.success
guarantee.apply.no
```

唯一约束：

```text
resource_type + biz_key
```

`bizKey` 用于人读、排障、跨资源引用和兼容迁移。禁止使用数据库 ID 作为业务引用。

### 7.3 Version

每个资源必须有 `version`。

规则：

- `version` 是资源声明版本，不是目标表版本。
- `version` 只增不减。
- 兼容性变更或目标模块要求的升级必须递增 `version`。
- 相同 `version` 但 hash 变化允许同步，但记录为 same-version content change。
- `version` 回退默认拒绝同步。

## 8. ResourceType 与消费者注册

每个消费者模块自行公开支持的资源类型、字段内容、字段类型、必填规则、同步模式和状态处理。

支持两种注册方式。

### 8.1 代码注册

```java
public interface ResourceHandler {

    String resourceType();

    ResourceSpec spec();

    ResourceSyncResult upsert(ResourceDeclaration resource);

    default boolean requiresCompleteBatch() {
        return false;
    }

    default Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> resources) {
        ...
    }

    ResourceSyncResult disable(ResourceDeclaration resource);
}
```

`upsertBatch` 是为了保持目标模块既有批量语义。普通资源可以使用默认逐条处理；如果目标模块会按“本次完整扫描结果”判断缺失资源并禁用，例如接口权限资源，Handler 必须返回 `requiresCompleteBatch() = true` 并覆盖批量方法。

完整批次处理规则：

- Registry 只对 hash 变化、新增或状态变化的资源写同步日志和变更日志。
- Handler 如声明需要完整批次，调用时会收到当前类型全部 active 声明。
- Registry 中 `MANUAL`、`LOCKED` 的资源必须传递其实际同步模式，Handler 不得覆盖人工接管内容。
- 目标模块已有批量注册 API 时，应继续复用该 API，禁止绕过原有落库和缺失资源处理逻辑。

示例目标模块：

```text
mango-notice        -> MESSAGE_EVENT, MESSAGE_TEMPLATE
mango-workflow      -> WORKFLOW_DEFINITION
mango-numgen        -> SEQUENCE_RULE
mango-template      -> PRINT_TEMPLATE
mango-ai            -> AI_PROMPT
mango-system        -> SYSTEM_DICT, SYSTEM_CONFIG, SYSTEM_I18N
mango-authorization -> AUTH_RESOURCE, AUTH_PACKAGE_BINDING
mango-job           -> JOB_DEFINITION, JOB_ALARM_RULE
```

### 8.2 描述文件注册

路径：

```text
classpath*:META-INF/mango/resource-types/*.yml
classpath*:META-INF/mango/resource-types/*.yaml
classpath*:META-INF/mango/resource-types/*.json
```

示例：

```yaml
mango:
  resource:
    typeRegistry:
      targetModule: mango-notice
      supportedTypes:
        - resourceType: MESSAGE_TEMPLATE
          schemaVersion: 1
          requiredFields:
            templateCode: string
            title: string
            content: file|string
          optionalFields:
            channels: list
            sort: int
          supportedSyncModes:
            - AUTO
            - MANUAL
            - LOCKED
```

描述文件只声明字段契约和可见能力，真实校验与落库仍以目标模块 `ResourceHandler` 为准。

## 9. 同步模式与状态

同步模式：

| 模式 | 含义 |
|---|---|
| `AUTO` | 模块托管，允许自动覆盖。 |
| `MANUAL` | 后台人工接管，禁止自动覆盖。 |
| `LOCKED` | 系统锁定，禁止自动修改。 |

资源状态：

| 状态 | 含义 |
|---|---|
| `ACTIVE` | 当前有效。 |
| `DISABLED` | 禁用，但保留历史引用。 |
| `DEPRECATED` | 不推荐新用，老数据继续可读。 |
| `REMOVED` | 声明式移除，目标模块做逻辑删除或禁用。 |

删除采用逻辑删除。目标模块按自身语义执行：

| 资源 | `REMOVED` 处理 |
|---|---|
| 消息模板 | 禁用模板，历史消息不动。 |
| 工作流定义 | 停止新实例使用，历史实例继续。 |
| 编码规则 | 禁用规则，历史编号不动。 |
| 打印模板 | 禁用模板，历史打印记录不动。 |
| AI Prompt | 禁用 Prompt，历史调用记录不动。 |
| 字典项 | 禁用。 |
| 菜单权限 | 隐藏或禁用。 |
| 任务定义 | 暂停任务。 |

## 10. 同步规则

同步基于声明当前态、Registry 现状和目标模块现状进行。

新增：

```text
Provider 存在
Registry 不存在
=> 调用 Handler.upsert / Handler.upsertBatch
```

修改：

```text
Hash 变化
AUTO   => 调用 Handler.upsert / Handler.upsertBatch
MANUAL => 跳过并记录
LOCKED => 跳过并记录
```

删除：

```text
Provider 不存在
Registry 存在
=> 调用 Handler.disable
```

批量语义：

```text
Handler.requiresCompleteBatch = false
=> 只把新增/变化资源传给 Handler.upsertBatch

Handler.requiresCompleteBatch = true
=> 把当前资源类型全部 active 声明传给 Handler.upsertBatch
=> Registry 只保存新增/变化资源的同步结果
```

该规则用于兼容接口权限等已有“全量扫描注册”逻辑，避免 resource 按单条调用时改变原模块数据语义。

禁止默认删除重建。只有开发或测试环境显式配置允许时，才可以支持强制重建类策略；生产默认不支持物理删除和重建。

## 11. Hash 机制

hash 用于检测声明内容变化。

第一版使用 MD5，输入必须采用规范化内容：

- `id`。
- `resourceType`。
- `bizKey`。
- `version`。
- `targetModule`。
- `syncMode`。
- `status`。
- 规范化 `fields`。
- `file` 类型文件内容、encoding、mediaType。

字段顺序不得影响 hash。YAML 与 JSON 表达同一内容时应计算出相同 hash。

## 12. 冲突处理

冲突直接失败，不能自动猜测、覆盖或合并。

以下情况必须失败：

- 同一个 `resourceId` 被多个声明使用。
- 同一个 `resourceType + bizKey` 被多个声明使用。
- 同一个 `resourceId` 对应不同 `resourceType` 或 `bizKey`。
- `version` 回退。
- `resourceType` 没有 `ResourceHandler`。
- 字段不符合消费者 `ResourceSpec`。
- `file` 路径不存在或无法读取。
- 声明文件 schemaVersion 不支持。

错误信息必须包含：

- `resourceId`。
- `resourceType`。
- `bizKey`。
- `moduleCode`。
- `sourcePath`。
- 冲突的另一个 `sourcePath`。
- 具体失败原因。

默认启动策略为 fail-fast，避免带着不一致资源进入 ready 状态。

## 13. 启动时序

最佳同步时机是服务启动成功、应用准备就绪之前。

Spring Boot 时序：

```text
Flyway 完成
Spring Context 初始化完成
所有 ResourceHandler / ResourceProvider Bean 就绪
ApplicationRunner 执行资源同步
ApplicationReadyEvent 之前
```

第一版采用 `ApplicationRunner + Ordered` 实现。

流程：

```text
数据库结构 migration 完成
↓
Spring Bean 初始化完成
↓
ResourceRegistryStartupRunner 抢锁
↓
汇总 JSON / YAML / Provider / Scanner 声明
↓
校验声明与消费者 schema
↓
计算 hash
↓
查询 Registry
↓
比较差异
↓
调用 Handler
↓
写 Registry / sync log / change log
↓
释放锁
↓
应用进入 ready
```

资源同步完成前，应用 readiness 应保持拒绝流量。同步完成后再允许接流量。

配置：

```yaml
mango:
  resource:
    sync:
      enabled: true
      fail-fast: true
```

默认建议：

- 生产 `fail-fast=true`。
- 本地开发可配置 `fail-fast=false`。

## 14. 多实例并发锁

多实例启动时使用数据库锁。谁抢到锁，谁执行同步；其他实例等待或跳过。

锁表：

```sql
create table resource_sync_lock (
    lock_name varchar(128) primary key,
    owner varchar(128) not null,
    locked_until datetime not null,
    locked_at datetime not null,
    updated_at datetime not null
);
```

锁名：

```text
RESOURCE_REGISTRY_SYNC
```

配置：

```yaml
mango:
  resource:
    sync:
      lock:
        wait-timeout: 30s
        lease-time: 300s
```

行为：

- 抢锁成功的实例执行同步。
- 抢锁失败的实例等待 `wait-timeout`。
- 等待期间如果同步完成，则继续启动。
- 锁超时后允许新实例抢锁，避免宕机后永久卡住。
- 同步批次必须记录锁 owner 和 batchId。

## 15. 数据库设计

### 15.1 resource_registry

记录资源当前注册状态。

```sql
create table resource_registry (
    id bigint primary key,

    resource_id bigint not null,

    resource_type varchar(64) not null,
    module_code varchar(64) not null,
    biz_key varchar(128) not null,
    resource_version int not null,

    target_module varchar(64) not null,
    target_resource_code varchar(128),
    target_id bigint,

    source_format varchar(16),
    source_path varchar(256),
    source_hash varchar(64),

    sync_mode varchar(32) not null,
    status varchar(32) not null,

    last_sync_time datetime,
    created_at datetime,
    updated_at datetime,

    unique(resource_id),
    unique(resource_type, biz_key)
);
```

说明：

- `id` 是 Registry 行 ID。
- `resource_id` 是声明中的稳定资源 ID。
- `target_id` 只用于排障辅助，不作为跨模块引用契约。
- `target_resource_code` 由目标模块返回，例如模板编码、流程编码、规则编码。

### 15.2 resource_sync_log

记录每次同步批次中的资源同步结果。

```sql
create table resource_sync_log (
    id bigint primary key,

    resource_id bigint not null,
    batch_id varchar(64) not null,

    sync_type varchar(32) not null,
    from_version int,
    to_version int,

    old_hash varchar(64),
    new_hash varchar(64),

    result varchar(32) not null,
    message text,

    lock_owner varchar(128),
    started_at datetime,
    finished_at datetime,
    created_at datetime
);
```

### 15.3 resource_change_log

记录自动同步、人工接管和状态变化。

```sql
create table resource_change_log (
    id bigint primary key,

    resource_id bigint not null,

    change_type varchar(32) not null,
    operator_id bigint,

    before_content json,
    after_content json,

    created_at datetime
);
```

### 15.4 resource_sync_lock

见第 14 节。

## 16. 后台能力

菜单：

```text
系统管理
└── 资源管理
    ├── 注册资源
    ├── 同步记录
    ├── 变更记录
    ├── 资源类型
    └── 同步配置
```

后台允许：

- 查看资源当前状态。
- 查看 `resourceId`、`resourceType`、`bizKey`、`version`、hash 和 source path。
- 查看目标模块和目标记录。
- 查看消费者支持的资源类型和字段。
- 手动触发同步。
- 切换 `AUTO` / `MANUAL`。
- 查看失败原因。
- 查看变更 diff。

后台不直接编辑目标业务内容。真实资源内容仍在消息管理、流程管理、编码规则管理、打印模板管理、AI 管理、系统字典、菜单权限等各自模块后台维护。

## 17. 目标模块接入策略

本节是接入规划，不是当前已实现清单。实际可声明并同步的资源类型以 `mango-resource` README 和运行时 `/resource/handler-specs` 为准。

优先接入资源接收模块：

| 模块 | 资源类型 |
|---|---|
| `mango-authorization` | `AUTH_RESOURCE`、`AUTH_PACKAGE_BINDING` |
| `mango-system` | `SYSTEM_DICT`、`SYSTEM_CONFIG`、`SYSTEM_I18N` |
| `mango-notice` | `MESSAGE_EVENT`、`MESSAGE_TEMPLATE`、`NOTICE_ROUTE` |
| `mango-template` | `PRINT_TEMPLATE`、`TEMPLATE_DEFINITION` |
| `mango-workflow` | `WORKFLOW_DEFINITION`、`WORKFLOW_CATEGORY` |
| `mango-job` | `JOB_DEFINITION`、`JOB_ALARM_RULE` |
| `mango-domain` | `DOMAIN_DEFINITION` |
| `mango-numgen` | `SEQUENCE_RULE`、`NUMGEN_GENERATOR` |
| `mango-file` | `FILE_STORAGE_PROFILE`、`FILE_DIRECTORY`、`FILE_POLICY` |
| `mango-calendar` | `BUSINESS_CALENDAR`、`HOLIDAY_RULE` |
| `mango-grid-layout` | `DEFAULT_LAYOUT` |
| `mango-payment` | `PAYMENT_METHOD`、`PAYMENT_CHANNEL`、`PAYMENT_CASHIER_CONFIG`、`PAYMENT_ROUTE` |

谨慎接入：

| 模块 | 策略 |
|---|---|
| `mango-identity` | 不开放通用用户注入，只支持管理员初始化、测试 seed 或租户初始化扩展。 |
| `mango-org` | 不建议业务随包注入真实组织，组织是运营主数据，应走 API、后台或租户初始化。 |
| `mango-auth` | 登录和安全策略优先走配置或系统配置，不作为主要资源接收方。 |
| `mango-access` | 网关访问策略后续可支持，但需先归入 authorization/access policy 统一设计。 |
| `mango-captcha` | 通常是配置，不需要资源登记。 |
| `mango-file-preview` | 通常是能力开关或引擎配置，不需要业务资源注入。 |
| 原 `mango-seed` | 已由 Resource Registry、各模块 TenantProvisioner 和业务开通/导入流程承接；不再保留独立运行时种子模块。 |

## 18. Flyway 边界

Flyway 允许：

- 建表。
- 索引。
- 约束。
- 结构迁移。

Flyway 不作为业务资源注入通道。业务模块不得通过自己的 Flyway 直接写入消息模板、工作流定义、编码规则、打印模板、AI Prompt、字典、菜单权限等公共资源表。

目标模块如需初始化自身内置资源，可以使用目标模块自己的 migration 或转换为 Resource Provider；发布后仍需遵守已发布 migration 不修改、只前进的规则。

## 19. 验证范围

实现时至少覆盖：

- YAML 声明解析。
- JSON 声明解析。
- Java Provider 扫描。
- 雪花 ID 格式校验。
- `resourceId` 冲突检测。
- `resourceType + bizKey` 冲突检测。
- version 回退拒绝。
- file classpath 内容读取和 hash。
- hash 未变化跳过。
- `AUTO` 变更更新。
- `MANUAL` 变更跳过。
- `LOCKED` 变更跳过。
- Provider 移除触发 disable。
- 缺少 Handler fail-fast。
- 多实例锁抢占和锁超时。
- sync log 与 change log 写入。
- 后台查询注册资源、同步记录和变更记录。
- 批量 Handler 不改变目标模块原有数据逻辑。
- 完整批次 Handler 不误禁用其它 active 资源。
- `MANUAL` / `LOCKED` 资源在完整批次同步时不被覆盖。

## 20. 交付台账

本设计的交付契约和台账见 `mango-docs/designs/mango-resource-registry-delivery-contract.md`。
