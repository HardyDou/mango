# Mango RBAC

> RBAC 模块 - 菜单、角色、权限管理

## 模块职责

| 职责 | 说明 |
|------|------|
| 菜单管理 | 树形菜单、菜单分组、前端路由配置 |
| 角色管理 | 角色CRUD、角色权限分配 |
| 权限码 | RBAC 权限码 `{model}:{module}:{action}` |

## 子模块

```
mango-rbac/
├── mango-rbac-api/            # API 定义（接口、PO、VO）
├── mango-rbac-core/           # 核心业务（Service、Mapper）
├── mango-rbac-starter/        # 本地调用启动器
└── mango-rbac-starter-remote/ # 远程调用启动器（Feign）
```

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
| `GET /sys/menu/group` | 获取所有菜单分组（含菜单树） |
| `GET /sys/menu/group/{groupId}` | 获取单个分组详情 |
| `POST /sys/menu/group` | 创建菜单分组 |
| `PUT /sys/menu/group` | 更新菜单分组 |
| `DELETE /sys/menu/group/{groupId}` | 删除菜单分组（级联删除菜单） |

### 菜单 API

| 接口 | 说明 |
|------|------|
| `GET /sys/menu` | 获取菜单树 |
| `GET /sys/menu/tree` | 获取完整菜单树 |

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
| `rbac_menu_group` | 菜单分组表 |
| `sys_menu` | 菜单表 |
| `sys_role` | 角色表 |
| `sys_user` | 用户表 |
| `sys_role_menu` | 角色菜单关联表 |
| `sys_user_role` | 用户角色关联表 |

## 依赖关系

```
rbac-api
├── mango-common
├── swagger-annotations
└── mybatis-plus-annotation

rbac-core
├── rbac-api
├── user-api (跨域API)
└── org-api (跨域API)

rbac-starter
├── rbac-core
└── spring-boot-starter-web
```
