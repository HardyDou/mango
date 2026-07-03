# Home 用户首页工作台使用说明

## 1. 概览

`mango-home` 提供当前登录用户的多首页工作台管理能力，配合前端 `@mango/home` 和 `@mango/admin-shell` 首页宿主使用。它面向后台首页、个人工作台、业务看板等需要用户自定义多个页面并设置默认首页的场景。

模块负责用户首页元数据、默认首页偏好、首页模板、模板发布版本和模板授权的持久化，不维护业务小组件数据，也不接管小组件权限判断。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 查询我的首页 | 获取当前登录用户的首页列表 | `GET /home/pages` |
| 解析首页 | 首页默认路由解析默认首页，带 `homeId` 参数的首页路由解析指定首页 | `GET /home/pages/resolve` |
| 创建首页 | 新建个人首页，可设置为默认首页 | `POST /home/pages` |
| 重命名首页 | 修改个人首页名称 | `PUT /home/pages/name` |
| 复制首页 | 基于当前布局复制一个首页 | `POST /home/pages/duplicate` |
| 保存布局 | 保存首页布局 JSON | `PUT /home/pages/layout` |
| 首页排序 | 调整个人首页顺序 | `PUT /home/pages/sort` |
| 设置默认首页 | 设置登录后默认打开的首页 | `PUT /home/pages/default` |
| 删除首页 | 删除个人首页，删除默认首页时自动回退 | `DELETE /home/pages` |
| 首页模板管理 | 管理平台级首页模板草稿、复制、发布、启停和删除 | `GET /home/templates` |
| 模板授权 | 将已发布模板授权给个人、部门或角色 | `PUT /home/templates/authorizations` |
| 用户最终视图 | 后台按用户、成员、部门查看最终可见首页集合 | `GET /home/templates/user-pages` |

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
import { homePageApi, homeTemplateApi } from '@mango/home';
```

`@mango/admin-shell` 的首页宿主已接入 `@mango/home`：

- 默认首页路由加载当前用户默认首页。
- 带 `homeId` 参数的首页路由加载当前用户拥有的指定首页。
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
| `GET` | `/home/templates` | 查询首页模板 |
| `GET` | `/home/templates/detail?id=...` | 查询模板草稿和发布版本 |
| `POST` | `/home/templates` | 创建模板草稿 |
| `PUT` | `/home/templates/draft` | 编辑模板草稿 |
| `POST` | `/home/templates/copy` | 复制模板为新草稿 |
| `PUT` | `/home/templates/publish` | 发布模板草稿 |
| `PUT` | `/home/templates/status` | 启停模板 |
| `DELETE` | `/home/templates` | 删除未授权模板 |
| `GET` | `/home/templates/authorizations?templateId=...` | 查询模板授权 |
| `PUT` | `/home/templates/authorizations` | 保存模板授权 |
| `GET` | `/home/templates/user-pages?userId=...` | 查询用户最终首页集合 |

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
| `HomeTemplateApi` | 模板草稿、复制、发布、启停、授权和用户首页视图管理 |

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
| Flyway migration | `mango-home-core/src/main/resources/db/migration/home/V2__home_template_management.sql` | 创建模板、模板版本、模板授权表，并增加 `default_home_ref` | 模板授权对象唯一 | 应用启动时由 Flyway 执行 |

数据表：

| 表名 | 说明 |
|------|------|
| `sys_user_home_page` | 当前租户、用户下的个人首页和布局 JSON |
| `sys_user_home_preference` | 当前租户、用户的默认首页偏好 |
| `sys_home_template` | 平台级首页模板主表 |
| `sys_home_template_version` | 模板草稿、发布版本、历史版本 |
| `sys_home_template_authorization` | 模板授权对象，支持个人、部门、角色 |

关键字段：

| 字段 | 说明 |
|------|------|
| `tenant_id` | 当前登录租户 |
| `org_id` | 当前组织上下文，非组织主体时为空 |
| `user_id` | 当前登录用户 |
| `default_home_page_id` | 默认首页 ID |
| `default_home_ref` | 默认首页路由标识，支持个人首页 ID 和 `template:{id}` 授权模板 |
| `layout_json` | 前端布局 JSON 字符串 |

## 7. 管理入口

本模块新增后台菜单资源 `首页管理`，位于 `平台能力` 下，包含三个页面：

| 菜单 | 路由 | 组件 key | 用途 |
|------|------|----------|------|
| 首页模板 | `/home-management/templates` | `home/templates/index` | 管理模板草稿、复制、发布、启停和授权 |
| 首页列表 | `/home-management/list` | `home/list/index` | 查询所有用户自定义首页 |
| 用户首页 | `/home-management/user` | `home/user/index` | 输入或选择用户后渲染该用户最终可见首页，并支持切换不同首页 |

模板管理规则：

- 已发布模板不可直接修改；需要复制生成新草稿后再编辑。
- 发布草稿后，已授权用户看到最新发布版本。
- 授权支持个人、部门、角色；部门授权继承到下级部门。
- 默认优先级为：用户手动默认 > 个人授权默认 > 部门授权默认 > 角色授权默认 > 系统默认 > 首个可见首页。
- 用户复制授权首页后会生成个人首页副本，不再跟随模板后续发布。

权限码：

| 权限码 | 说明 |
|--------|------|
| `home:templates:list` | 查询模板列表 |
| `home:templates:query` | 查询模板详情 |
| `home:templates:add` | 新建或复制模板 |
| `home:templates:edit` | 编辑模板草稿 |
| `home:templates:publish` | 发布模板 |
| `home:templates:status` | 启停模板 |
| `home:templates:delete` | 删除模板 |
| `home:templates:auth` | 管理模板授权 |
| `home:list:view` | 查看用户自定义首页列表 |
| `home:user:view` | 查看并渲染指定用户最终首页 |

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

## 9. 问题排查

| 现象 | 排查项 |
|------|--------|
| 首页列表为空 | 确认当前用户是否已创建个人首页；未创建时前端首页宿主使用内置系统工作台 |
| 默认首页未生效 | 确认 `sys_user_home_preference.default_home_page_id` 指向当前用户拥有且启用的首页 |
| 保存布局失败 | 检查 `layoutJson` 是否包含 `schemaVersion` 和 `items`，并满足组件数量、组件 ID 和 12 栅格坐标校验 |
| 指定首页无法打开 | 确认请求中的 `homeId` 属于当前登录租户和用户 |
| 接口返回未登录或无上下文 | 确认请求经过后台登录态、租户上下文和 `MangoContextHolder` 初始化链路 |

## 10. 相关文档

- [@mango/home 前端 README](../../../mango-ui/packages/home/README.md)
- [admin-shell README](../../../mango-ui/packages/admin-shell/README.md)
- [Mango 能力索引](../../../mango-docs/capabilities/README.md)
