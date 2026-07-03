# Home 用户首页工作台使用说明

## 1. 概览

`mango-home` 提供当前登录用户的多首页工作台管理能力，配合前端 `@mango/home` 和 `@mango/admin-shell` 首页宿主使用。它面向后台首页、个人工作台、业务看板等需要用户自定义多个页面并设置默认首页的场景。

模块只负责用户首页元数据、默认首页偏好和布局 JSON 持久化，不维护业务小组件数据、不提供角色或租户级默认首页模板，也不接管小组件权限判断。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 查询我的首页 | 获取当前登录用户的首页列表 | `GET /home/pages` |
| 解析首页 | `/home` 解析默认首页，`/home/:homeId` 解析指定首页 | `GET /home/pages/resolve` |
| 创建首页 | 新建个人首页，可设置为默认首页 | `POST /home/pages` |
| 重命名首页 | 修改个人首页名称 | `PUT /home/pages/name` |
| 复制首页 | 基于当前布局复制一个首页 | `POST /home/pages/duplicate` |
| 保存布局 | 保存首页布局 JSON | `PUT /home/pages/layout` |
| 首页排序 | 调整个人首页顺序 | `PUT /home/pages/sort` |
| 设置默认首页 | 设置登录后默认打开的首页 | `PUT /home/pages/default` |
| 删除首页 | 删除个人首页，删除默认首页时自动回退 | `DELETE /home/pages` |

## 3. 接入方式

后端应用引入 starter：

```xml
<dependency>
  <groupId>io.mango.platform.home</groupId>
  <artifactId>mango-home-starter</artifactId>
</dependency>
```

`mango-admin-starter` 已引入该 starter。独立能力验证可启动 `mango-home-capability-app`。

前端管理端引入：

```ts
import { homePageApi } from '@mango/home';
```

`@mango/admin-shell` 的 `/home` 宿主已接入 `@mango/home`：

- `/home` 加载当前用户默认首页。
- `/home/:homeId` 加载当前用户拥有的指定首页。
- 用户可创建、重命名、复制、排序、删除、设置默认首页，并保存布局 JSON。

## 4. 配置说明

| 配置入口 | 字段 / Key | 默认值 | 含义 | 影响行为 |
|----------|------------|--------|------|----------|
| Maven dependency | `mango-home-starter` | 未引入 | 启用首页 HTTP 接口和服务 | 引入后注册 Controller、Service 和 Mapper |
| Spring property | `mango.home.enabled` | `true` | 是否启用自动装配 | 为 `false` 时不注册 home starter |
| 请求体 | `layoutJson` | 无 | 首页布局 JSON 字符串 | 保存用户工作台布局 |

## 5. API 与扩展

HTTP 接口：

| 方法 | 地址 | 说明 |
|------|------|------|
| `GET` | `/home/pages` | 查询当前用户首页列表 |
| `GET` | `/home/pages/resolve?homeId=...` | 解析默认或指定首页 |
| `POST` | `/home/pages` | 创建首页 |
| `PUT` | `/home/pages/name` | 重命名首页 |
| `POST` | `/home/pages/duplicate` | 复制首页 |
| `PUT` | `/home/pages/layout` | 保存布局 |
| `PUT` | `/home/pages/sort` | 保存排序 |
| `PUT` | `/home/pages/default` | 设置默认首页 |
| `DELETE` | `/home/pages` | 删除首页 |

Java API：

| API | 说明 |
|-----|------|
| `HomePageApi#listMyPages` | 查询当前用户首页列表 |
| `HomePageApi#resolve` | 解析默认或指定首页 |
| `HomePageApi#create` | 创建首页 |
| `HomePageApi#rename` | 重命名首页 |
| `HomePageApi#duplicate` | 复制首页 |
| `HomePageApi#saveLayout` | 保存布局 |
| `HomePageApi#sort` | 保存排序 |
| `HomePageApi#setDefault` | 设置默认首页 |
| `HomePageApi#delete` | 删除首页 |

布局 JSON 当前结构：

```json
{
  "schemaVersion": 1,
  "items": []
}
```

后端会校验 `schemaVersion`、`items` 数组、组件数量、组件 ID、`widgetType` 和 12 栅格坐标范围。业务小组件注册、菜单权限过滤和业务数据查询由前端运行时及各业务模块负责。

## 6. 数据与初始化

本模块使用 Flyway migration 初始化数据表。

| 类型 | 位置 | 初始化内容 | 幂等键 / 唯一键 | 生效时机 |
|------|------|------------|-----------------|----------|
| Flyway migration | `mango-home-core/src/main/resources/db/migration/home/V1__init_home.sql` | 创建 `sys_user_home_page`、`sys_user_home_preference` | `tenant_id + user_id` 偏好唯一 | 应用启动时由 Flyway 执行 |

数据表：

| 表名 | 说明 |
|------|------|
| `sys_user_home_page` | 当前租户、用户下的个人首页和布局 JSON |
| `sys_user_home_preference` | 当前租户、用户的默认首页偏好 |

关键字段：

| 字段 | 说明 |
|------|------|
| `tenant_id` | 当前登录租户 |
| `org_id` | 当前组织上下文，非组织主体时为空 |
| `user_id` | 当前登录用户 |
| `default_home_page_id` | 默认首页 ID |
| `layout_json` | 前端布局 JSON 字符串 |

## 7. 管理入口

本模块不新增独立后台菜单页面，不新增按钮权限码，也不新增默认套餐或角色授权数据。管理端首页仍使用已有 `首页` 菜单和 `/home` 路由。

接口使用登录访问模式，租户和用户来自 `MangoContextHolder`。前端不传 `tenantId`、`orgId` 和 `userId`。指定 `homeId` 时，后端只允许访问当前用户拥有且启用的首页。

## 8. 快速开始

业务接入最小闭环：

1. 后端应用引入 `mango-home-starter`。
2. 数据库执行 `home` Flyway migration。
3. 前端使用 `@mango/home` 或直接使用 `@mango/admin-shell` 的 `/home` 宿主。
4. 业务模块通过 grid widget 注册机制提供小组件。
5. 首页加载时调用 `resolve`，没有个人首页时使用内置系统工作台。
6. 用户创建首页、设置默认首页并保存布局后，`/home` 自动解析到默认首页。

后端核心测试：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-home/mango-home-core -am test
```

后端 starter 测试：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-home/mango-home-starter -am test
```

前端包构建：

```bash
pnpm -F @mango/home build
pnpm -F @mango/admin-shell build
```
