# Mango Authorization

> Authorization 模块 - 统一授权、角色、权限、菜单与公共路径管理

## 模块职责

| 职责 | 说明 |
|------|------|
| 菜单管理 | 树形菜单、菜单分组、前端路由配置 |
| 角色管理 | 角色CRUD、角色权限分配 |
| 权限码 | 权限码 `{model}:{module}:{action}` |
| 授权快照 | 通过 `IAuthorizationProvider` 汇总角色、权限与 Spring Security authorities |
| 安全适配 | 本地 starter 为 `mango-infra-security` 提供 `IPermissionService` adapter；远程聚合由 `mango-security-starter-remote` 适配 |
| 主体角色绑定 | 保存 subject 到 role 的授权关系，不保存账号资料 |

## 子模块

```
mango-authorization/
├── mango-authorization-api/            # API 定义（接口、PO、VO、授权快照）
├── mango-authorization-core/           # 核心业务（Service、Mapper）
├── mango-authorization-starter/        # 本地调用启动器
└── mango-authorization-starter-remote/ # 远程调用启动器（Feign）
```

## 授权 API

| 类型 | 说明 |
|------|------|
| `AuthorizationQuery` | 授权查询入参，当前支持 user subject |
| `AuthorizationSnapshot` | 授权快照，包含 roles、permissions、authorities |
| `AuthorityContributor` | 授权数据贡献者，按 subject 追加授权事实 |
| `AuthorizationApi` | HTTP / Feign 授权快照契约 |
| `IAuthorizationProvider` | 统一授权 provider，聚合 contributor 输出快照 |

`mango-auth` 登录成功后调用 `IAuthorizationProvider` 获取角色与权限，不再维护 auth 内部权限检查器。账号资料与认证用户事实已抽离到 `mango-identity`。`mango-infra-security` 仍只认识基础安全契约，authorization 本地 starter 负责把本地授权快照适配成 `IPermissionService`；远程调用方通过 `mango-security-starter-remote` 完成同样适配。

## 核心实体

### SysMenu

系统菜单实体：
- `menuId` - 菜单ID
- `groupId` - **菜单分组ID**
- `parentId` - 父菜单ID（0为根）
- `menuType` - 菜单类型（1-目录，2-菜单，3-按钮）
- `menuName` - 菜单名称
- `menuCode` - 权限标识（如 `system:user:view`）
- `path` - 前端路由路径
- `component` - 前端组件路径
- `icon` - 菜单图标
- `sort` - 排序号
- `status` - 状态（0-禁用，1-启用）
- `visible` - 是否显示
- `keepAlive` - 路由缓存
- `embedded` - 内嵌模式
- `redirect` - 重定向路径
- `permissions` - 权限标识列表
- `meta` - 前端Meta信息
- `createBy` / `updateBy` - 审计字段
- `createTime` / `updateTime` - 时间戳
- `delFlag` - 删除标记

### SysMenuGroup

菜单分组实体：
- `groupId` - 分组ID
- `groupName` - 分组名称
- `groupCode` - 分组编码（唯一标识）
- `icon` - 分组图标
- `sort` - 排序号
- `status` - 状态（0-禁用，1-启用）
- `createBy` / `updateBy` - 审计字段
- `createTime` / `updateTime` - 时间戳
- `delFlag` - 删除标记

## 菜单分组

菜单通过 `groupId` 关联到分组，支持：
- 一级菜单分组（如：系统管理、业务管理、运维管理）
- 分组内菜单排序
- 分组级别的图标和状态控制

### 分组 API

| 接口 | 说明 |
|------|------|
| `GET /authorization/sys/menu/group` | 获取所有菜单分组（含菜单树） |
| `GET /authorization/sys/menu/group/{groupId}` | 获取单个分组详情 |
| `POST /authorization/sys/menu/group` | 创建菜单分组 |
| `PUT /authorization/sys/menu/group` | 更新菜单分组 |
| `DELETE /authorization/sys/menu/group/{groupId}` | 删除菜单分组（级联删除菜单） |

### 菜单 API

| 接口 | 说明 |
|------|------|
| `GET /authorization/sys/menu` | 获取菜单树 |
| `GET /authorization/sys/menu/tree` | 获取完整菜单树 |

## 菜单类型

| 类型 | 值 | 说明 |
|------|----|------|
| 目录 | 1 | 菜单分组，不对应具体页面 |
| 菜单 | 2 | 具体页面菜单 |
| 按钮 | 3 | 页面内操作按钮 |

## 权限码格式

`{model}:{module}:{action}`

示例：
- `system:user:view` - 查看用户
- `system:user:create` - 创建用户
- `system:user:update` - 更新用户
- `system:user:delete` - 删除用户

## 数据库表

| 表名 | 说明 |
|------|------|
| `authorization_menu_group` | 菜单分组表 |
| `sys_menu` | 菜单表 |
| `sys_role` | 角色表 |
| `sys_role_menu` | 角色菜单关联表 |
| `sys_user_role` | 主体角色绑定表（后续数据库 Phase 可物理改名） |

## 依赖关系

```
authorization-api
├── mango-common
├── swagger-annotations
└── mybatis-plus-annotation

authorization-core
├── authorization-api
└── mango-infra-context-core

authorization-starter
├── authorization-core
├── mango-infra-security-api
├── mango-infra-web-starter
└── spring-boot-starter-web

authorization-starter-remote
├── authorization-api
└── spring-cloud-starter-openfeign
```
