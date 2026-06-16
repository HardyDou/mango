# 业务域 Domain

## 1. 概览
`mango-domain` 提供租户内业务域树管理能力。业务域用于给菜单、任务、通知、编号、流程、文件、模板等配置打上业务归属，避免所有平台配置堆在一个平面里。

主要使用者是平台模块开发者、业务模块开发者和后台管理员。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 新业务线需要一个稳定编码，用于归类菜单、通知业务类型、编号规则、任务分组或流程配置 | Maven 依赖 / HTTP API / Java API |
| 管理端需要展示业务域树，并按启用状态过滤可选域 | Maven 依赖 / HTTP API / Java API |
| 微服务之间需要按编码查询业务域详情 | Maven 依赖 / HTTP API / Java API |
| 初始化平台内置业务域，例如 COMMON、WORKFLOW、NOTICE、CALENDAR、NUMGEN、FILE、TEMPLATE、JOB、PAYMENT | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 新业务线需要一个稳定编码，用于归类菜单、通知业务类型、编号规则、任务分组或流程配置。
- 管理端需要展示业务域树，并按启用状态过滤可选域。
- 微服务之间需要按编码查询业务域详情。
- 初始化平台内置业务域，例如 `COMMON`、`WORKFLOW`、`NOTICE`、`CALENDAR`、`NUMGEN`、`FILE`、`TEMPLATE`、`JOB`、`PAYMENT`。

## 4. 边界说明
- 不负责组织机构、岗位、用户归属和数据权限计算。
- 不替代租户模型；业务域是租户内的业务分类，不是租户。
- 不负责菜单权限自动生成，只提供可引用的业务域编码。

## 5. 模块组成
- `mango-domain-api`：`DomainApi`、命令对象、查询对象和 `DomainVO`。
- `mango-domain-core`：`biz_domain` 实体、Mapper、树构建、启停、逻辑删除和种子数据。
- `mango-domain-starter`：`DomainAutoConfiguration` 和 `/domain/domains` Controller。
- `mango-domain-starter-remote`：`DomainFeignClient`，供其他服务远程查询。

业务模块负责决定自己的业务域编码，并在菜单、任务、流程或配置表中引用该编码。

## 6. 接入方式
提供业务域管理接口的服务引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.domain</groupId>
    <artifactId>mango-domain-starter</artifactId>
</dependency>
```

只做远程消费的服务引入 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.domain</groupId>
    <artifactId>mango-domain-starter-remote</artifactId>
</dependency>
```

业务代码优先通过 `DomainApi.detailByCode` 校验编码是否存在并启用。

## 7. 配置说明
当前模块没有专属 `@ConfigurationProperties`。引入 starter 后通过 Spring Boot 自动配置注册服务和 Controller。

运行前必须确保 Flyway 已执行 `db/migration/domain` 下的迁移，否则接口没有 `biz_domain` 表和内置业务域数据可用。

## 8. API 与扩展
HTTP 根路径：`/domain/domains`。

| 方法 | 路径 | 权限 | 用途 |
|------|------|------|------|
| GET | `/domain/domains/page` | `domain:list` | 分页查询业务域。 |
| GET | `/domain/domains/tree` | `domain:list` | 查询业务域树。 |
| GET | `/domain/domains/enabled-tree` | 登录可访问 | 查询启用业务域树。 |
| GET | `/domain/domains/detail` | `domain:list` | 按 ID 查询详情。 |
| GET | `/domain/domains/code` | 登录可访问 | 按业务域编码查询详情。 |
| POST | `/domain/domains` | `domain:add` | 新增业务域。 |
| PUT | `/domain/domains` | `domain:edit` | 修改业务域。 |
| PUT | `/domain/domains/status` | `domain:status` | 启用或停用业务域。 |
| DELETE | `/domain/domains` | `domain:delete` | 逻辑删除业务域。 |

远程调用使用 `DomainFeignClient`，Java 契约使用 `DomainApi`。

## 9. 数据与初始化
Flyway 路径：`mango-domain-core/src/main/resources/db/migration/domain`。

| 脚本 | 内容 |
|------|------|
| `V1__init_domain.sql` | 创建 `biz_domain`，初始化通用、工作流、通知、日历、编号、文件、模板等业务域。 |
| `V2__seed_job_domain.sql` | 初始化 `JOB` 定时任务业务域。 |
| `V3__seed_payment_domain.sql` | 初始化 `PAYMENT` 支付业务域。 |

`biz_domain` 关键字段：

| 字段 | 含义 |
|------|------|
| `tenant_id` | 租户标识，当前种子数据为租户 `1`。 |
| `domain_code` | 业务域编码，租户内唯一，例如 `PAYMENT`。 |
| `domain_short_code` | 业务域短编码，租户内唯一，例如 `PAY`。 |
| `domain_name` | 展示名称。 |
| `parent_id` | 父业务域 ID，`0` 表示顶级。 |
| `sort` | 排序。 |
| `status` | `1` 启用，`0` 停用。 |
| `deleted` | 逻辑删除标记。 |

种子数据使用 `ON DUPLICATE KEY UPDATE`，重复执行会更新名称、排序、状态和备注，并恢复 `deleted = 0`。

## 10. 管理入口
后端接口使用 `@ApiAccess` 绑定权限码：

- 查询：`domain:list`
- 新增：`domain:add`
- 修改：`domain:edit`
- 启停：`domain:status`
- 删除：`domain:delete`

`enabled-tree` 和 `code` 接口只要求登录，用于业务运行时读取可用业务域。

`biz_domain` 以 `tenant_id` 隔离，默认种子只写入租户 `1`。新租户如果需要内置业务域，需要通过租户初始化流程复制或重新插入。

## 11. 快速开始
1. 为业务模块确定稳定业务域编码，例如 `ORDER`。
2. 通过管理接口或迁移脚本写入 `biz_domain`。
3. 菜单、通知业务类型、编号规则、流程定义等配置引用该 `domain_code`。
4. 业务运行时通过 `DomainApi.detailByCode` 校验业务域存在且启用。
5. 新租户初始化时同步该业务域，否则租户下的配置引用会失效。

## 12. 问题排查
- 树为空：检查当前租户、`deleted`、`status` 和 migration 是否执行。
- 编码冲突：`domain_code` 和 `domain_short_code` 都要求租户内唯一。
- 停用后业务仍能读到历史配置：业务域停用只代表不再可选，已保存业务数据是否拦截由调用方决定。
- 新租户缺少内置域：默认 SQL 只写租户 `1`，需要接入租户初始化。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
