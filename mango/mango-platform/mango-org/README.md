# Mango Org

## 1. 概览

`mango-org` 是 Mango 的组织和岗位能力，负责租户内组织树、岗位、组织成员关系和组织负责人。业务模块需要按部门、岗位或负责人选人时，使用这里的组织和岗位数据。

它不负责账号、租户、登录和角色授权。账号和成员来自 `mango-identity`，租户来自 `mango-system`，菜单和按钮权限来自 `mango-authorization`。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 组织树 | 查询根组织、子组织、组织详情，支持按父级、组织类型和启用状态过滤 |
| 组织维护 | 新增、修改、删除租户内组织 |
| 组织成员 | 给租户成员绑定组织、岗位、主组织和组织负责人标记 |
| 负责人查询 | 按组织查询负责人 `memberId` |
| 岗位维护 | 岗位分页、详情、新增、修改和删除 |
| 租户初始化 | 新租户创建时初始化根组织和默认岗位 |
| 删除保护 | 租户删除前检查当前租户是否仍有组织或岗位数据 |
| 资源声明 | 通过 Resource Registry 的 `ORG_UNIT` 和 `ORG_POST` 注入组织、岗位基线 |

## 3. 后端接入

业务模块只引用组织、岗位 API 契约时依赖 API 包：

```xml
<dependency>
    <groupId>io.mango.platform.org</groupId>
    <artifactId>mango-org-api</artifactId>
</dependency>
```

部署组织和岗位接口时依赖 starter：

```xml
<dependency>
    <groupId>io.mango.platform.org</groupId>
    <artifactId>mango-org-starter</artifactId>
</dependency>
```

微服务远程调用组织服务时依赖 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.org</groupId>
    <artifactId>mango-org-starter-remote</artifactId>
</dependency>
```

常用 Java API：

| API | 用途 |
|-----|------|
| `SysOrgApi` | 组织树、子组织、详情、组织维护、组织成员和负责人查询 |
| `PostApi` | 岗位分页、详情和维护 |

## 4. 前端接入

组织和岗位页面在 `@mango/rbac` 中：

| 前端包 | 形态 | 相关导出 |
|--------|------|----------|
| `@mango/rbac` | `admin-pages` 管理页面插件 | `OrgView`、`PostView`、`orgApi`、`postApi` |

`orgApi` 调用 `/org/**`，`postApi` 调用 `/post/**`。这些页面适合 Mango Admin 或业务管理后台，不是官网或 C 端站点的通用组件。

## 5. 快速开始

新增一个部门并给成员设置主组织：

1. 确认当前租户存在根组织。新租户创建时 `OrgTenantProvisioner` 会自动创建。
2. 调用 `POST /org` 创建组织，填写 `pid`、`orgName`、`orgCode`、`orgType`。
3. 调用 `POST /post` 创建岗位，填写 `postName`、`postCode`。
4. 调用 `POST /org/{orgId}/members`，传入 identity 的 `memberId` 和岗位 `postId`。
5. 需要部门负责人时，把 `leaderFlag` 设为 `true`。
6. 审批或选人场景通过 `GET /org/leader/{orgId}` 查询负责人。

也可以用 Resource Registry 做基线注入：

| 资源类型 | 关键字段 |
|----------|----------|
| `ORG_UNIT` | `tenantId`、`orgCode`、`orgName`、`orgType`，可用 `parentOrgCode` 解析父组织。 |
| `ORG_POST` | `tenantId`、`postCode`、`postName`，可声明排序、状态和备注。 |

## 6. 配置说明

`mango-org` 当前没有独立 `@ConfigurationProperties` 前缀。引入 starter 后会通过自动配置注册 mapper、service、组织 controller、岗位 controller、租户初始化扩展和租户删除依赖检查扩展。

模块声明：

```properties
module-name=mango-org
module-path=/org,/post
```

## 7. API 与扩展

组织接口前缀是 `/org`。

| 方法 | 路径 | 权限 | 用途 |
|------|------|------|------|
| GET | `/org/tree` | `system:org:list` | 查询组织树 |
| GET | `/org/children` | `system:org:list` | 查询子组织 |
| GET | `/org/detail` | `system:org:query` | 查询组织详情 |
| POST | `/org` | `system:org:add` | 新增组织 |
| PUT | `/org` | `system:org:edit` | 修改组织 |
| DELETE | `/org` | `system:org:delete` | 删除组织 |
| GET | `/org/{orgId}/members` | `system:org:list` | 查询组织成员 |
| POST | `/org/{orgId}/members` | `system:org:edit` | 增加组织成员 |
| PUT | `/org/members` | `system:org:edit` | 修改组织成员关系 |
| DELETE | `/org/members` | `system:org:edit` | 删除组织成员关系 |
| GET | `/org/leader/{orgId}` | LOGIN | 查询组织负责人 |

岗位接口前缀是 `/post`。

| 方法 | 路径 | 权限 | 用途 |
|------|------|------|------|
| GET | `/post/page` | `system:post:list` | 分页查询岗位 |
| GET | `/post/detail` | `system:post:query` | 查询岗位详情 |
| POST | `/post` | `system:post:add` | 新增岗位 |
| PUT | `/post` | `system:post:edit` | 修改岗位 |
| DELETE | `/post` | `system:post:delete` | 删除岗位 |

主要入参：

| 对象 | 关键字段 |
|------|----------|
| `CreateOrgCommand` | `pid`、`orgName`、`orgCode`、`orgType` 必填；可传 `orgSort`、`orgStatus` |
| `UpdateOrgCommand` | `id`、`pid`、`orgName`、`orgCode`、`orgType` 必填 |
| `SysOrgTreeQuery` | `parentId`、`type`、`includeDisabled` |
| `AddOrgMemberCommand` | `memberId` 必填；可传 `postId`、`primaryFlag`、`leaderFlag` |
| `UpdateOrgMemberCommand` | `relationId` 必填；可改岗位、主组织和负责人标记 |
| `CreatePostCommand` | `postName`、`postCode` 必填；可传 `postSort`、`postStatus`、`remark` |
| `UpdatePostCommand` | `id`、`postName`、`postCode` 必填 |
| `PostPageQuery` | `page`、`size`、`postName`、`postCode`、`postStatus` |

扩展点：

| 扩展 | 作用 |
|------|------|
| `OrgTenantProvisioner` | 新租户创建后初始化根组织和默认岗位 |
| `TenantDependencyChecker` | 租户删除前检查组织和岗位数据，返回阻断原因 |

## 8. 返回字段

| 对象 | 关键字段 |
|------|----------|
| `SysOrg` | `id`、`pid`、`orgName`、`orgCode`、`orgType`、`orgSort`、`orgStatus`、`tenantId`、`children` |
| `OrgMemberVO` | `relationId`、`memberId`、`userId`、`username`、`nickname`、`memberName`、`memberType`、`status`、`orgId`、`postId`、`postName`、`postCode`、`primaryFlag`、`leaderFlag` |
| `PostVO` | `id`、`postName`、`postCode`、`postSort`、`postStatus`、`remark`、`tenantId`、`createTime`、`updateTime` |

## 9. 数据与初始化

Flyway 路径：

```text
mango-org-core/src/main/resources/db/migration/org
```

核心表：

| 表 | 用途 | 关键约束 |
|----|------|----------|
| `sys_org` | 组织树 | `uk_sys_org_tenant_code(tenant_id, org_code)` |
| `org_post` | 岗位 | `uk_org_post_tenant_code(tenant_id, post_code)` |

初始化数据：

| 来源 | 内容 |
|------|------|
| `V1__init_org.sql` | 租户 `1` 的芒果集团组织树、租户 `2/3/4` 的公司根组织、默认岗位 |
| `OrgTenantProvisioner` | 新租户创建时生成根组织和默认岗位 |

新租户默认编码规则：

| 数据 | 编码 |
|------|------|
| 根组织 | `<TENANT_CODE>_ROOT` |
| 机构管理员岗位 | `<TENANT_CODE>_INSTITUTION_ADMIN` |
| 组织管理岗位 | `<TENANT_CODE>_DEPT_MANAGER` |
| 员工岗位 | `<TENANT_CODE>_EMPLOYEE` |

## 10. 管理入口

组织和岗位接口使用这些权限码：

| 权限码 | 用途 |
|--------|------|
| `system:org:list` | 组织树、子组织和组织成员查询 |
| `system:org:query` | 组织详情 |
| `system:org:add` | 新增组织 |
| `system:org:edit` | 修改组织和组织成员 |
| `system:org:delete` | 删除组织 |
| `system:post:list` | 岗位列表 |
| `system:post:query` | 岗位详情 |
| `system:post:add` | 新增岗位 |
| `system:post:edit` | 修改岗位 |
| `system:post:delete` | 删除岗位 |

菜单通常由 `mango-authorization` 资源同步或系统管理菜单初始化提供。前端 `@mango/rbac` 的组织、岗位页面需要与这些权限码配套。

## 11. 问题排查

| 现象 | 排查点 |
|------|--------|
| 组织树为空 | 检查当前租户上下文、`sys_org` 数据、组织状态和新租户初始化 |
| 岗位保存提示编码冲突 | `post_code` 在租户内唯一 |
| 成员加不到组织 | 检查 `memberId` 是否来自当前租户的 `tenant_member` |
| 查询不到负责人 | 检查组织成员关系中是否有 `leaderFlag=true` |
| 删除租户失败 | 组织模块会因已有组织或岗位数据阻止租户删除 |

## 12. 相关文档

- [Mango Identity](../mango-identity/README.md)
- [Mango System](../mango-system/README.md)
- [Mango Authorization](../mango-authorization/README.md)
- [@mango/rbac 前端包](../../../mango-ui/packages/rbac/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
