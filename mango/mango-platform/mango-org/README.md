# 组织 Org

## 1. 概览
`mango-org` 提供租户内组织和岗位能力：组织树、组织详情、组织成员、岗位分页、岗位详情、岗位维护，以及新租户组织和默认岗位初始化。

主要使用者是用户归属、审批人选择、数据权限、岗位授权和组织树展示相关业务。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 业务需要按租户维护集团、公司、部门、小组等组织层级 | Maven 依赖 / HTTP API / Java API |
| 用户、租户成员或审批流需要绑定主组织、岗位或部门负责人 | Maven 依赖 / HTTP API / Java API |
| 前端需要组织树、懒加载子节点、岗位列表和组织成员列表 | Maven 依赖 / HTTP API / Java API |
| 新租户创建后需要自动生成根组织和默认岗位 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 业务需要按租户维护集团、公司、部门、小组等组织层级。
- 用户、租户成员或审批流需要绑定主组织、岗位或部门负责人。
- 前端需要组织树、懒加载子节点、岗位列表和组织成员列表。
- 新租户创建后需要自动生成根组织和默认岗位。

## 4. 边界说明
- 不负责账号认证、登录态、角色授权和菜单权限本身。
- 不负责复杂人事主数据，例如职级、汇报线、编制、考勤和薪酬。
- 不替代租户；组织是租户内结构，租户生命周期由 `mango-system` 管理。

## 5. 模块组成
- `mango-org-api`：`SysOrgApi`、`PostApi`、组织和岗位命令、查询、VO。
- `mango-org-core`：`sys_org`、`org_post` Mapper、组织服务、岗位服务、组织成员服务和 `OrgTenantProvisioner`。
- `mango-org-starter`：注册 `MangoOrgAutoConfiguration` 和岗位 Controller。
- `mango-org-starter-remote`：注册 `OrgFeignClient`、`PostFeignClient`，供微服务远程调用。

组织 Controller 位于 core，岗位 Controller 位于 starter；引入 starter 后两者一起可用。

## 6. 接入方式
提供组织岗位接口的服务引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.org</groupId>
    <artifactId>mango-org-starter</artifactId>
</dependency>
```

只做远程消费的服务引入 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.org</groupId>
    <artifactId>mango-org-starter-remote</artifactId>
</dependency>
```

业务代码注入 `SysOrgApi` 查询组织树和详情，注入 `PostApi` 查询岗位。

## 7. 配置说明
当前模块没有专属 `@ConfigurationProperties`。引入 starter 后通过自动配置注册 Mapper、服务、Controller 和租户初始化扩展。

新租户初始化依赖 `mango-system` 的 `TenantProvisioner` 编排；删除租户前依赖 `TenantDependencyChecker` 汇总阻断原因。

## 8. API 与扩展
组织接口根路径：`/org`。

| 方法 | 路径 | 权限 | 用途 |
|------|------|------|------|
| GET | `/org/tree` | `system:org:list` | 查询组织树，支持父级和类型过滤。 |
| GET | `/org/children` | `system:org:list` | 查询子组织。 |
| GET | `/org/detail` | `system:org:query` | 查询组织详情。 |
| POST | `/org` | `system:org:add` | 新增组织。 |
| PUT | `/org` | `system:org:edit` | 修改组织。 |
| DELETE | `/org` | `system:org:delete` | 删除组织。 |
| GET | `/org/{orgId}/members` | `system:org:list` | 查询组织成员。 |
| POST | `/org/{orgId}/members` | `system:org:edit` | 增加组织成员。 |
| PUT | `/org/members` | `system:org:edit` | 修改成员组织关系。 |
| DELETE | `/org/members` | `system:org:edit` | 删除成员组织关系。 |
| GET | `/org/leader/{orgId}` | 登录可访问 | 查询组织负责人。 |

岗位接口根路径：`/post`。

| 方法 | 路径 | 权限 | 用途 |
|------|------|------|------|
| GET | `/post/page` | `system:post:list` | 分页查询岗位。 |
| GET | `/post/detail` | `system:post:query` | 查询岗位详情。 |
| POST | `/post` | `system:post:add` | 新增岗位。 |
| PUT | `/post` | `system:post:edit` | 修改岗位。 |
| DELETE | `/post` | `system:post:delete` | 删除岗位。 |

扩展点：

- `OrgTenantProvisioner`：新租户初始化根组织和默认岗位。
- `TenantDependencyChecker`：租户删除前检查是否存在组织或岗位数据。

## 9. 数据与初始化
Flyway 路径：`mango-org-core/src/main/resources/db/migration/org`。

`V1__init_org.sql` 创建并初始化：

| 表 | 用途 |
|----|------|
| `sys_org` | 组织树，`tenant_id + org_code` 唯一。 |
| `org_post` | 岗位，`tenant_id + post_code` 唯一。 |

内置数据包含：

- 租户 `1` 的芒果集团组织树，含 A 公司、B 公司、C 公司、技术研发部、产品设计部等示例组织。
- 租户 `1` 的集团管理员、部门负责人、研发工程师岗位。
- 租户 `2`、`3`、`4` 的公司根组织和默认机构管理员、组织管理、员工岗位。

新租户运行期初始化：

- 根组织编码：`租户编码大写 + _ROOT`。
- 默认岗位：`租户编码大写 + _INSTITUTION_ADMIN`、`租户编码大写 + _DEPT_MANAGER`、`租户编码大写 + _EMPLOYEE`。
- 初始化是幂等的，存在根组织或岗位时不会重复插入。

## 10. 管理入口
组织和岗位接口使用 `system:org:*`、`system:post:*` 权限码。菜单通常归在系统管理或组织管理下，由 authorization 模块初始化或业务菜单配置接入。

`sys_org` 和 `org_post` 都按 `tenant_id` 隔离。业务查询组织树、岗位、组织成员时必须携带当前租户上下文，不能跨租户读取。

## 11. 快速开始
1. 引入 `mango-org-starter`。
2. 确保当前租户已有根组织和岗位。
3. 给用户或租户成员绑定主组织、岗位或负责人关系。
4. 审批、数据权限或页面筛选通过 `SysOrgApi` 查询组织树。
5. 保存业务数据时记录组织 ID 或岗位 ID，并校验它们属于当前租户。

## 12. 问题排查
- 组织树为空：检查当前租户、migration、新租户初始化和组织状态。
- 岗位编码冲突：`post_code` 在租户内唯一。
- 删除租户失败：组织模块会阻止删除已有组织或岗位数据的租户。
- 审批找不到部门负责人：先确认组织成员关系和负责人设置是否已维护。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
