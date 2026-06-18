# 角色数据权限设计

## 1. 目标

为 Mango 增加基于角色的数据权限能力。权限中心负责配置、解析和合并角色数据范围；业务模块只声明自己的数据归属字段，并通过平台提供的工具把解析结果应用到查询条件。

## 2. 范围

- 支持按角色配置数据权限。
- 支持按资源编码配置不同数据范围。
- 支持 `ALL`、`SELF`、`SELF_ORG`、`SELF_ORG_AND_CHILDREN`、`ORG` 五类运营配置。
- 支持多角色数据权限合并。
- 支持业务查询通过 `DataScopeApplier` 显式接入。
- 在 RBAC 角色页增加数据权限配置入口。
- 成员主部门驱动“本人部门”类动态数据范围，避免按部门复制角色。

不包含：

- 个人数据权限。
- 第一版全局 SQL 拦截器。
- `CUSTOM` 运营配置。
- 跨租户数据权限放行。

## 3. 影响模块

| 模块 | 改动 |
|------|------|
| `mango-authorization-api` | 新增数据权限 API、Command、Query、VO 和枚举 |
| `mango-authorization-core` | 新增角色数据权限表、实体、mapper、service 和解析逻辑 |
| `mango-authorization-starter` | 新增数据权限 Controller 和 `DataScopeProvider` 实现 |
| `mango-infra-persistence-api` | 新增数据权限字段映射和应用器契约 |
| `mango-infra-persistence-starter` | 新增 MyBatis-Plus `QueryWrapper` 数据权限应用器 |
| `mango-workflow-core` | 用流程定义管理验证业务显式接入数据权限 |
| `@mango/rbac` | 角色页新增数据权限配置弹窗和 API 封装 |
| README / 能力地图 | 更新数据权限能力说明 |

## 4. 数据模型

新增表 `authorization_role_data_scope`：

| 字段 | 含义 |
|------|------|
| `id` | 主键 |
| `tenant_id` | 租户 ID |
| `app_code` | 应用编码 |
| `role_id` | 角色 ID |
| `resource_code` | 资源编码，通常使用列表或查询权限码 |
| `scope_mode` | `ALL`、`SELF`、`SELF_ORG`、`SELF_ORG_AND_CHILDREN`、`ORG` |
| `scope_values` | JSON 数组，`ORG` 时保存组织 ID；动态本人部门类模式通常为空 |
| `include_children` | 兼容字段，`ORG` 可表达固定组织包含下级；`SELF_ORG_AND_CHILDREN` 按当前成员主部门固定包含下级 |
| `status` | 状态 |
| 审计字段 | `created_by`、`created_at`、`updated_by`、`updated_at` |

唯一约束：`tenant_id + app_code + role_id + resource_code`。

## 5. 解析规则

解析输入为当前登录上下文和 `resourceCode`：

1. 根据 `memberId + tenantId + appCode + realm + actorType + partyType + partyId` 查询当前成员角色。
2. 查询这些角色在目标资源下启用的数据权限。
3. 多角色合并：
   - 任一角色为 `ALL`，最终为 `ALL`。
   - 否则合并所有 `ORG` 指定组织 ID。
   - 如果存在 `SELF_ORG`，最终按当前成员主部门解析。
   - 如果存在 `SELF_ORG_AND_CHILDREN`，最终按当前成员主部门及下级部门解析。
   - 如果存在 `SELF`，最终保留本人范围。
   - 无任何配置时默认 `SELF`。
4. `ALL` 只表示当前租户内全部数据；租户过滤仍由 persistence tenant 插件或业务租户条件处理。

## 6. 业务接入

租户业务表统一保留 `tenant_id`、`org_id`、`created_by`、`created_at`、`updated_by`、`updated_at` 标准字段。`mango-infra-persistence` 在 insert/update 时自动填充租户、组织和审计字段；全局配置表、历史表、基础设施表和第三方表不纳入租户业务表基线，通过 schema validation 排除清单管理。

业务查询显式调用平台应用器：

```java
dataScopeApplier.apply(
        wrapper,
        "payment:order:list",
        DataScopeMapping.builder()
                .tableName("payment_order")
                .selfField("created_by")
                .orgField("org_id")
                .tenantField("tenant_id")
                .build());
```

应用器行为：

- `ALL`：不追加本人或组织限制。
- `SELF`：追加本人字段等于当前用户。
- `SELF_ORG`：按当前成员主部门追加组织字段。
- `SELF_ORG_AND_CHILDREN`：按当前成员主部门及下级部门追加组织字段。
- `ORG`：追加组织字段在授权组织集合内。
- `SELF + ORG`：追加本人或组织的组合条件。
- 组织集合为空时返回空结果条件。

字段缺失时 fail closed：

- 需要 `SELF` 但未声明本人字段，抛出明确异常。
- 需要 `ORG` 但未声明组织字段，抛出明确异常。
- 声明了 `tableName` 时，应用器会校验本次规则需要的字段在表中存在；缺列直接抛出明确异常。
- 未登录上下文或缺少必要主体时，抛出明确异常。

### 6.1 业务验证：流程定义

`mango-workflow-core` 已将流程定义管理作为首个业务接入样例：

| 入口 | 数据权限资源码 | 字段映射 |
|------|----------------|----------|
| 流程定义分页、已发布分页、详情、版本列表、版本详情，以及依赖详情读取的编辑、删除、状态调整、撤回和发布 | `workflow:definition:list` | `created_by`、`org_id`、`tenant_id` |

workflow 通过可选 `DataScopeApplier` 接入数据权限。部署应用未安装授权数据权限能力时保持原行为；安装后由平台应用器追加本人、指定组织、本人主部门或本人主部门及下级范围条件。租户隔离仍由 persistence 租户插件处理。

## 7. 前端入口

在 `@mango/rbac` 角色页增加“数据权限”操作：

- 按角色打开配置弹窗。
- 以表格行内新增、编辑、保存和删除数据权限配置。
- 数据资源使用树形选择器，只展示 list 类资源，避免手工输入资源编码。
- 范围支持“全部”“本人”“本人部门”“本人部门及下级”“指定组织”。
- 指定组织复用组织树选择；本人部门类范围按成员主部门动态解析。

用户配置路径：

1. 成员管理中把用户加入部门，并设置主部门。
2. 成员管理中给用户分配角色。
3. 角色的数据权限如果选择“本人部门”或“本人部门及下级”，不同用户会按自己的主部门看到不同数据。

## 8. 测试范围

- 角色数据权限保存、查询和删除。
- 多角色合并规则。
- 无配置默认 `SELF`。
- `ALL`、`SELF`、`SELF_ORG`、`SELF_ORG_AND_CHILDREN`、`ORG` 应用到 `QueryWrapper` 的条件。
- 字段缺失 fail closed。
- 前端角色数据权限弹窗的主要保存流程。
- 成员主部门配置对部门类数据权限的动态生效。

## 9. 风险

- 业务查询不接入应用器时仍不会自动生效。
- XML SQL、JOIN、统计报表需要业务显式传入带别名的字段。
- `SELF_ORG_AND_CHILDREN` 依赖组织 API 递归查询下级部门；部署应用缺少组织能力时会 fail-fast。
