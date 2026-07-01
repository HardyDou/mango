# 网址导航 Link

## 1. 概览

`mango-link` 提供公司网址、公开网址、个人网址和收藏夹能力。它适合放在内部管理后台，也可以通过公开 Open API 给门户、首页、小组件或独立导航页展示网址。

模块提供三类入口：

| 入口 | 使用者 | 说明 |
|------|--------|------|
| 后台管理 | 管理员 | 维护网址分类、网址列表、可见范围和状态。 |
| 用户侧导航 | 登录用户 | 查看公司网址、我的收藏、我的网址。 |
| Open API | 匿名或登录用户 | 未登录返回公开网址；已登录返回当前用户可见的公司网址、收藏和个人网址。 |

访问网址默认直接打开原始 URL。需要访问统计时，在系统配置中开启 `mango.link.open.jump.enabled=true`，Open API 才返回系统跳转地址 `/link/open/jump?url=...`，后端会校验 URL 格式并写入 `link_access_record` 访问记录。

## 2. 功能清单

| 能力 | 入口 |
|------|------|
| 网址分类管理 | `/link/categories/**`，管理页面 `/data/link/categories` |
| 网址列表管理 | `/link/items/**`，管理页面 `/data/link/items` |
| 公司网址查看 | `/link/company-links/list`，用户页面 `/link/company` |
| 我的收藏 | `/link/favorites/**`，用户页面 `/link/favorites` |
| 我的网址 | `/link/personal-links/**`，用户页面 `/link/my-links` |
| 我的分组 | `/link/personal-categories/**`，供独立面板创建个人分组 |
| 公开导航数据 | `/link/open/public-links/list` |
| 系统跳转与访问统计 | `/link/open/jump?url=...`，访问记录表 `link_access_record` |

## 3. 后端接入

提供本地能力的应用引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.link</groupId>
    <artifactId>mango-link-starter</artifactId>
</dependency>
```

只面向契约编码时引入 API：

```xml
<dependency>
    <groupId>io.mango.platform.link</groupId>
    <artifactId>mango-link-api</artifactId>
</dependency>
```

微服务远程消费时引入 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.link</groupId>
    <artifactId>mango-link-starter-remote</artifactId>
</dependency>
```

## 4. 前端接入

管理后台页面包是 `@mango/link`：

```ts
import { registerMangoLinkAdminPages } from '@mango/link/admin-pages';
import '@mango/link/style.css';

registerMangoLinkAdminPages();
```

非管理后台或门户页面使用：

| 包 | 用途 |
|----|------|
| `@mango/link-openapi` | TypeScript API client，调用 `/link/open/**` 和登录用户个人操作接口。 |
| `@mango/link-page` | 独立网址导航页面，包含 Logo、搜索、登录/退出、分组、网址和个人操作。 |
| `@mango/link-panel` | 兼容包，继续导出 `@mango/link-page` 能力。 |

详见：

- [@mango/link](../../../mango-ui/packages/link/README.md)
- [@mango/link-openapi](../../../mango-ui/packages/link-openapi/README.md)
- [@mango/link-page](../../../mango-ui/packages/link-page/README.md)
- [@mango/link-panel](../../../mango-ui/packages/link-panel/README.md)

## 5. 数据与初始化

| 数据 | 初始化来源 | 说明 |
|------|------------|------|
| 表结构 | `mango-link-core/src/main/resources/db/migration/link` | 创建 `link_category`、`link_item`、`link_visibility_target`、`link_favorite`、`link_access_record`。 |
| 默认导航数据 | `mango-link-core/src/main/resources/db/migration/link/V4__seed_default_navigation.sql` | 固化 `企业导航` 分组，以及 Mango 管理后台、百度、GitHub、Maven Central 默认网址。 |
| 菜单与权限 | `mango-link-starter/src/main/resources/META-INF/mango/resources/link-common-menu.json` | 通过 resource 注入到授权模块。 |
| 前端页面 | `@mango/link/admin-pages` | 菜单 `component` 通过 admin-pages 映射到 Vue 页面。 |
| 系统配置 | `mango-link-starter/src/main/resources/META-INF/mango/resources/link-common-config.yml` | 初始化 `mango.link.open.jump.enabled`，控制 Open API 是否返回系统跳转地址。 |

分类有两种归属：

| scope | 含义 |
|-------|------|
| `COMPANY` | 管理员维护的公司分类。 |
| `PERSONAL` | 用户自己的网址分组。 |

网址可见范围：

| visibilityScope | 含义 |
|-----------------|------|
| `PUBLIC` | 公开可见，匿名可通过 Open API 查询。 |
| `COMPANY` | 登录且属于当前公司/租户的用户可见。 |
| `DEPARTMENT` | 指定部门用户可见。 |
| `USER` | 指定用户可见。 |
| `PERSONAL` | 个人网址，仅 owner 用户可见。 |

内置默认数据：

| 分组 | 网址 | 可见范围 | 说明 |
|------|------|----------|------|
| 企业导航 | Mango 管理后台 | `COMPANY` | 登录且属于当前租户的用户可见。 |
| 企业导航 | 百度 | `PUBLIC` | 匿名和登录用户均可见。 |
| 企业导航 | GitHub | `PUBLIC` | 匿名和登录用户均可见。 |
| 企业导航 | Maven Central | `PUBLIC` | 匿名和登录用户均可见。 |

## 6. 菜单与权限

| 菜单 | 路由 | 页面 key | 权限码 |
|------|------|----------|--------|
| 网址导航 / 公司网址 | `/link/company` | `link/company/index` | `link:navigation:view` |
| 网址导航 / 我的收藏 | `/link/favorites` | `link/favorites/index` | `link:favorite:view` |
| 网址导航 / 我的网址 | `/link/my-links` | `link/my-links/index` | `link:personal:view` |
| 平台能力 / 网址管理 / 网址分类 | `/data/link/categories` | `link/categories/index` | `link:category:view`、`link:category:create`、`link:category:update`、`link:category:status`、`link:category:delete` |
| 平台能力 / 网址管理 / 网址列表 | `/data/link/items` | `link/items/index` | `link:item:view`、`link:item:create`、`link:item:update`、`link:item:status`、`link:item:delete` |

菜单来自 resource 声明，初始化或重建工作区后应通过授权菜单接口确认能看到 `网址导航` 和 `网址管理`。

## 7. 接口入口

### 7.1 Open API

| 方法 | 路径 | 登录态 | 说明 |
|------|------|--------|------|
| GET | `/link/open/public-links/list` | 可选 | 未登录只返回公开网址；已登录返回当前用户可见的公司网址、收藏和个人网址。 |
| GET | `/link/open/jump?url=https%3A%2F%2Fexample.com&uid=user-001&source=COMPANY` | 可选 | 按 URL 跳转到真实网址，写入访问记录；只允许 `http/https`。 |

`/link/open/public-links/list` 返回项包含 `source`。当系统配置 `mango.link.open.jump.enabled=true` 时，返回项还包含 `redirectUrl`，形式为 `/link/open/jump?url=...`；前端打开网址优先使用 `redirectUrl`。配置关闭时不返回 `redirectUrl`，前端直接打开原始 `url`。

### 7.1.1 系统配置

| 配置 key | 默认值 | 说明 |
|----------|--------|------|
| `mango.link.open.jump.enabled` | `false` | `true`：Open API 返回 `redirectUrl`，点击网址经过 `/link/open/jump` 并写访问记录；`false`：Open API 不返回 `redirectUrl`，前端直连原始网址，不记录跳转访问。 |

### 7.2 用户侧接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/link/company-links/list` | 查询当前用户可见的公司网址。 |
| GET | `/link/favorites/list` | 查询我的收藏。 |
| POST | `/link/favorites/create` | 收藏网址。 |
| DELETE | `/link/favorites/delete` | 取消收藏。 |
| GET | `/link/personal-links/page` | 分页查询我的网址。 |
| POST | `/link/personal-links/create` | 新增我的网址。 |
| PUT | `/link/personal-links/update` | 编辑我的网址。 |
| DELETE | `/link/personal-links/delete` | 删除我的网址。 |
| GET | `/link/personal-categories/list` | 查询我的网址分组。 |
| POST | `/link/personal-categories/create` | 新增我的网址分组。 |

### 7.3 管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/link/categories/page` | 分页查询网址分类。 |
| GET | `/link/categories/list` | 查询网址分类列表。 |
| POST | `/link/categories/create` | 新增网址分类。 |
| PUT | `/link/categories/update` | 编辑网址分类。 |
| POST / PUT / DELETE | `/link/categories/*` | 分类启停、状态更新、删除。 |
| GET | `/link/items/page` | 分页查询网址列表。 |
| POST | `/link/items/create` | 新增网址。 |
| PUT | `/link/items/update` | 编辑网址。 |
| POST / PUT / DELETE | `/link/items/*` | 网址启停、状态更新、删除。 |

## 8. 关键字段

| 对象 | 字段 | 说明 |
|------|------|------|
| 分类 | `name` | 分类名称，租户、scope、owner 内唯一。 |
| 分类 | `scope` | `COMPANY` 或 `PERSONAL`。 |
| 分类 | `ownerUserId` | 个人分类归属用户；公司分类为 `0`。 |
| 网址 | `name` | 网址名称。 |
| 网址 | `url` | 真实地址，后端会标准化。 |
| 网址 | `categoryId` | 所属分类。 |
| 网址 | `summary` | 说明。 |
| 网址 | `iconUrl` | 图标地址。 |
| 网址 | `tags` | 标签数组，后端以逗号存储。 |
| 网址 | `visibilityScope` | 可见范围。 |
| 网址 | `visibilityTargets` | 指定部门或指定用户时的目标列表。 |
| 网址 | `ownerUserId` | 个人网址归属用户。 |
| 网址 | `recommended` | 是否推荐。 |
| 访问记录 | `linkId`、`userId`、`source`、`clientIp`、`userAgent`、`referer`、`accessTime` | 系统跳转时写入。 |

## 9. 验收

本地默认验收地址：

| 类型 | 地址 |
|------|------|
| 管理后台 | `http://127.0.0.1:30002` |
| 后端服务 | `http://127.0.0.1:18002` |
| 公司网址页面 | `http://127.0.0.1:30002/#/link/company` |
| 网址列表页面 | `http://127.0.0.1:30002/#/data/link/items` |

建议验证：

```bash
pnpm --dir mango-ui --filter mango-admin test:e2e -- e2e/specs/link-navigation.spec.ts
pnpm --dir mango-ui --filter @mango/link-openapi build
pnpm --dir mango-ui --filter @mango/link-page build
pnpm --dir mango-ui --filter @mango/link-panel build
pnpm --dir mango-ui --filter @mango/link build
mvn -q -pl :mango-link-api,:mango-link-core,:mango-link-starter,:mango-link-starter-remote -am test -DskipTests=false
```

模块规则检查：

```bash
cd mango/mango-platform/mango-link
mvn -q -DskipTests mango:check -Drule=dependency
mvn -q -DskipTests mango:check -Drule=api-contract
mvn -q -DskipTests mango:check -Drule=persistence-schema
```

## 10. 排障

| 问题 | 优先检查 |
|------|----------|
| 看不到菜单 | resource 是否完成同步，授权菜单接口是否返回 `网址导航`、`网址管理`。 |
| 菜单能看到但页面打不开 | `@mango/link/admin-pages` 是否注册，菜单 component 是否为 `link/company/index`、`link/items/index` 等。 |
| Open API 匿名看不到数据 | 网址是否启用，`visibilityScope` 是否为 `PUBLIC`，分类是否启用。 |
| 登录后看不到公司网址 | 当前用户是否属于租户/公司，部门或用户可见目标是否匹配。 |
| 打开网址没有统计 | 系统配置 `mango.link.open.jump.enabled` 是否为 `true`，前端是否使用接口返回的 `redirectUrl`。 |
| 跳转返回不可见 | 当前用户不满足该链接可见范围，或分类/网址已停用。 |
