# 业务域 Domain

## 1. 概览

`mango-domain` 提供租户内业务域管理能力。业务域是一个稳定的业务归类编码，用于给菜单、通知类型、编号规则、任务分组、流程配置、文件和模板等平台配置标记业务归属。

业务域不是租户、组织、岗位或数据权限模型。它只回答“这条平台配置属于哪个业务域”，不负责计算用户能看哪些数据。

## 2. 功能清单

| 能力 | 说明 | 使用入口 |
|------|------|----------|
| 业务域树 | 查询全部业务域树，支持按编码、名称和状态过滤 | `DomainApi.tree` / `GET /domain/domains/tree` |
| 启用业务域树 | 给业务配置页面提供可选业务域 | `DomainApi.enabledTree` / `GET /domain/domains/enabled-tree` |
| 按编码查询 | 业务运行时校验 `domainCode` 是否存在 | `DomainApi.detailByCode` / `GET /domain/domains/code` |
| 分页查询 | 管理端列表查询业务域 | `DomainApi.page` / `GET /domain/domains/page` |
| 业务域维护 | 新增、修改、启停、逻辑删除业务域 | 管理接口 |
| 内置业务域 | 初始化 `COMMON`、`WORKFLOW`、`NOTICE`、`CMS`、`CALENDAR`、`NUMGEN`、`FILE`、`TEMPLATE`、`JOB`、`PAYMENT` | `mango-resource` 的 `BUSINESS_DOMAIN` |

## 3. 后端接入

### 3.1 开发依赖

业务模块只需要面向业务域 API 编码时，引入 `mango-domain-api`：

```xml
<dependency>
    <groupId>io.mango.platform.domain</groupId>
    <artifactId>mango-domain-api</artifactId>
</dependency>
```

常用调用是按编码查询业务域：

```java
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.vo.DomainVO;

DomainVO domain = domainApi.detailByCode("PAYMENT").getData();
```

### 3.2 部署依赖

提供业务域管理接口的应用启用 starter：

```xml
<dependency>
    <groupId>io.mango.platform.domain</groupId>
    <artifactId>mango-domain-starter</artifactId>
</dependency>
```

微服务中只远程消费业务域能力的应用启用 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.domain</groupId>
    <artifactId>mango-domain-starter-remote</artifactId>
</dependency>
```

`mango-domain-starter` 默认启用；需要关闭时配置：

```yaml
mango:
  domain:
    enabled: false
```

## 4. 前端接入

当前仓库没有独立的业务域前端 npm 包。业务域通常由后端接口提供给其他管理页面作为下拉树或筛选条件。

前端页面常用接口：

| 场景 | 接口 |
|------|------|
| 配置页面选择业务域 | `GET /domain/domains/enabled-tree` |
| 管理端维护业务域 | `/domain/domains/**` 管理接口 |
| 根据编码展示业务域信息 | `GET /domain/domains/code?domainCode=...` |

如果后续新增业务域管理页面，应登记对应菜单、权限和页面 key，并在本 README 补充前端包或页面入口。

## 5. 快速开始

1. 为业务模块确定稳定编码，例如 `ORDER`、`CONTRACT`。
2. 部署应用启用 `mango-domain-starter`，执行 domain migration。
3. 通过 `mango-resource` 的 `BUSINESS_DOMAIN` 声明或管理接口写入 `biz_domain`。
4. 在编号规则、通知类型、任务配置、流程定义等表中引用 `domain_code`。
5. 业务运行时用 `DomainApi.detailByCode` 校验业务域存在且可用。
6. 新租户初始化时同步需要的业务域数据。

## 6. 配置说明

`mango-domain` 没有专属配置项对象。只有 starter 启停开关：

```yaml
mango:
  domain:
    enabled: true
```

## 7. YAML 配置字段

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `mango.domain.enabled` | `true` | 是否启用业务域 starter，启用后注册 Mapper、Service 和 `/domain/domains/**` Controller。 |

## 8. 运行时配置字段

业务域运行时数据存储在 `biz_domain`。

| 字段 | 含义 | 约束 |
|------|------|------|
| `tenant_id` | 租户标识 | 租户内隔离 |
| `domain_code` | 业务域完整编码 | 租户内唯一，最长 64 字符 |
| `domain_short_code` | 业务域短编码 | 租户内唯一，最长 64 字符 |
| `domain_name` | 业务域名称 | 最长 128 字符 |
| `parent_id` | 父业务域 ID | `0` 表示顶级 |
| `sort` | 排序号 | 数字越小越靠前 |
| `status` | 状态 | `1` 启用，`0` 停用 |
| `remark` | 备注 | 最长 512 字符 |
| `deleted` | 逻辑删除标记 | `0` 正常，`1` 删除 |

新增业务域请求字段：

| 字段 | 含义 |
|------|------|
| `domainCode` | 本层业务域编码。顶级域直接作为完整编码，子域会拼接父级编码。 |
| `domainShortCode` | 业务域短编码。 |
| `domainName` | 业务域名称。 |
| `parentId` | 父业务域 ID，`0` 表示顶级。 |
| `sort` | 排序。 |
| `status` | `1` 启用，`0` 停用。 |
| `remark` | 备注。 |

修改业务域不修改完整 `domainCode`，只维护短编码、名称、排序、状态和备注。

## 9. 请求与返回字段

HTTP 根路径：`/domain/domains`。

| 方法 | 路径 | 权限 | 用途 |
|------|------|------|------|
| `GET` | `/domain/domains/page` | `domain:list` | 分页查询业务域。 |
| `GET` | `/domain/domains/tree` | `domain:list` | 查询业务域树。 |
| `GET` | `/domain/domains/enabled-tree` | 登录可访问 | 查询启用业务域树。 |
| `GET` | `/domain/domains/detail` | `domain:list` | 按 ID 查询详情。 |
| `GET` | `/domain/domains/code` | 登录可访问 | 按 `domainCode` 查询详情。 |
| `POST` | `/domain/domains` | `domain:add` | 新增业务域。 |
| `PUT` | `/domain/domains` | `domain:edit` | 修改业务域。 |
| `PUT` | `/domain/domains/status` | `domain:status` | 启停业务域。 |
| `DELETE` | `/domain/domains` | `domain:delete` | 逻辑删除业务域。 |

分页和树查询支持字段：

| 字段 | 含义 |
|------|------|
| `domainCode` | 按业务域编码过滤。 |
| `domainName` | 按业务域名称过滤。 |
| `status` | `1` 启用，`0` 停用。 |
| `page` / `size` | 分页参数，来自 Mango 通用分页查询对象。 |

返回对象 `DomainVO`：

| 字段 | 含义 |
|------|------|
| `id` | 业务域 ID。 |
| `tenantId` | 租户标识。 |
| `domainCode` | 业务域编码。 |
| `domainShortCode` | 业务域短编码。 |
| `domainName` | 业务域名称。 |
| `parentId` / `parentName` | 父级业务域。 |
| `sort` | 排序。 |
| `status` | `1` 启用，`0` 停用。 |
| `remark` | 备注。 |
| `createTime` / `updateTime` | 创建和更新时间。 |
| `createdBy` / `createdAt` / `updatedBy` / `updatedAt` | 标准审计字段。 |
| `children` | 子业务域列表，树接口返回。 |

## 10. 管理入口

业务域管理菜单由 `mango-domain-starter` 的 `AUTH_MENU` 资源注入：

```text
mango-domain-starter/src/main/resources/META-INF/mango/resources/domain-common-menu.json
```

菜单入口挂在后台 `系统管理` 下：

| 菜单 | 路径 | 页面 key | 父菜单 |
|------|------|----------|--------|
| 业务域 | `/system/domain` | `system/domain/index` | `system` |

后端接口绑定权限码：

| 权限码 | 用途 |
|--------|------|
| `domain:list` | 分页、树和详情查询。 |
| `domain:add` | 新增业务域。 |
| `domain:edit` | 修改业务域。 |
| `domain:status` | 启停业务域。 |
| `domain:delete` | 删除业务域。 |

`enabled-tree` 和 `code` 接口只要求登录，用于其他业务页面读取可选业务域或校验业务域编码。

## 11. 资源注入

业务域默认数据通过 `mango-resource` 注入，不在 Flyway 中写业务配置数据。资源文件放在：

```text
mango-domain-starter/src/main/resources/META-INF/mango/resources/domain-common-domain.yml
```

其它模块需要登记业务域时，也在自己的 starter 中放资源声明文件，例如：

```text
mango-notice-starter/src/main/resources/META-INF/mango/resources/notice-common-domain.yml
mango-numgen-starter/src/main/resources/META-INF/mango/resources/numgen-common-domain.yml
mango-template-starter/src/main/resources/META-INF/mango/resources/template-common-domain.yml
```

`BUSINESS_DOMAIN` 落库到 `biz_domain`，按 `tenantId + domainCode` 合并更新。

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，使用雪花 ID 字符串。 |
| `version` | `INT` | 是 | 资源版本，声明内容升级时递增。 |
| `biz-key` | `STRING` | 是 | 资源业务键，例如 `domain.notice`。 |
| `target-module` | `STRING` | 是 | 固定为 `domain`。 |
| `domainId` | `LONG` | 否 | 业务域稳定 ID，不填时使用资源 ID。 |
| `tenantId` | `LONG` | 否 | 租户标识，默认 `1`。 |
| `orgId` | `LONG` | 否 | 所属组织 ID。 |
| `domainCode` | `STRING` | 是 | 业务域完整编码，租户内唯一。 |
| `domainShortCode` | `STRING` | 是 | 业务域短编码，租户内唯一。 |
| `domainName` | `STRING` | 是 | 业务域名称。 |
| `parentId` | `LONG` | 否 | 父业务域 ID，默认 `0`。 |
| `sort` | `INT` | 否 | 排序号，默认 `0`。 |
| `status` | `INT` | 否 | `1` 启用，`0` 停用，默认 `1`。 |
| `remark` | `STRING` | 否 | 备注。 |

删除规则：

| 操作 | 行为 |
|------|------|
| `disable` | 将 `biz_domain.status` 更新为 `0`。 |
| `delete` | 将 `deleted` 更新为 `1`，不物理删除历史业务域。 |

## 12. 数据与初始化

Flyway 路径：`mango-domain-core/src/main/resources/db/migration/domain`。

| 脚本 | 内容 |
|------|------|
| `V1__init_domain.sql` | 创建 `biz_domain`。 |

内置业务域默认通过资源声明写入租户 `1`。资源重复同步会更新短编码、名称、排序、状态、备注，并恢复 `deleted = 0`。

新租户如果也需要这些业务域，需要通过租户初始化流程复制或重新插入。不要假设租户 `1` 的业务域对其他租户自动可见。

## 13. 问题排查

| 问题 | 优先检查 |
|------|----------|
| 业务域树为空 | 当前租户、`deleted`、`status`、domain migration 和 `mango-resource` 同步是否执行。 |
| 按编码查不到 | `domainCode` 是否是完整编码，是否在当前租户下，是否被逻辑删除。 |
| 新增提示编码冲突 | `domain_code` 和 `domain_short_code` 都要求租户内唯一。 |
| 停用后历史配置仍能使用 | 停用只影响业务域是否可选；已保存配置是否拦截由调用方决定。 |
| 新租户缺少内置域 | 默认资源声明只写租户 `1`，需要接入租户初始化。 |
| 前端没有管理入口 | 当前仓库未登记独立前端包和默认菜单，需要补菜单与页面注册。 |

## 14. 相关文档

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
