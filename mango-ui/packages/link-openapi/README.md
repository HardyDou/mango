# @mango/link-openapi

## 1. 概览

`@mango/link-openapi` 是网址导航的前端 API client。它面向非管理后台页面、门户、小组件和独立导航页，封装后端 `mango-link` 的公开导航接口和登录用户的个人操作接口。

它只提供 TypeScript 方法，不提供 UI。

## 2. 安装

```bash
pnpm add @mango/link-openapi
```

## 3. 快速使用

```ts
import { createLinkOpenApiClient } from '@mango/link-openapi';

const linkClient = createLinkOpenApiClient({
  baseUrl: '/api',
  credentials: 'same-origin',
  headers: async () => ({
    Authorization: `Bearer ${sessionStorage.getItem('MANGO_TOKEN') || ''}`,
  }),
});

const links = await linkClient.listPublicLinks({ keyword: '办公' });
```

未登录时 `listPublicLinks` 只返回公开网址。已登录并带上登录态时，返回当前用户可见的公司网址、我的收藏和我的网址。

## 4. API

| 方法 | 后端路径 | 登录态 | 说明 |
|------|----------|--------|------|
| `listPublicLinks(query)` | `GET /link/open/public-links/list` | 可选 | 查询导航数据。 |
| `listPersonalCategories()` | `GET /link/personal-categories/list` | 需要 | 查询我的网址分组。 |
| `createPersonalCategory(input)` | `POST /link/personal-categories/create` | 需要 | 新增我的网址分组。 |
| `createPersonalLink(input)` | `POST /link/personal-links/create` | 需要 | 新增我的网址。 |
| `createFavorite(linkId)` | `POST /link/favorites/create` | 需要 | 收藏网址。 |
| `deleteFavorite(linkId)` | `DELETE /link/favorites/delete` | 需要 | 取消收藏。 |

## 5. 关键类型

```ts
interface LinkPublicItem {
  id?: string;
  categoryId?: string;
  categoryName?: string;
  name?: string;
  url?: string;
  summary?: string;
  iconUrl?: string;
  tags?: string[];
  openMode?: 'NEW_WINDOW';
  recommended?: boolean;
  favorited?: boolean;
  source?: 'PUBLIC' | 'COMPANY' | 'FAVORITE' | 'PERSONAL';
  redirectUrl?: string;
}
```

`redirectUrl` 是系统跳转地址，格式为 `/link/open/jump?url=...`。当后端配置 `mango.link.open.jump.enabled=true` 时返回它，打开网址应优先使用它并记录访问；配置关闭或字段为空时，调用方直接打开 `url`。

## 6. Client Options

| 字段 | 类型 | 说明 |
|------|------|------|
| `baseUrl` | `string` | API 前缀，例如 `/api`。 |
| `headers` | `HeadersInit \| () => HeadersInit \| Promise<HeadersInit>` | 登录态请求头。 |
| `credentials` | `RequestCredentials` | fetch credentials。 |
| `fetcher` | `typeof fetch` | 自定义 fetch 实现。 |

## 7. 错误处理

请求失败或 Mango 返回失败时会抛出 `Error`。调用方应按页面形态处理：

- 匿名页面收到 401 时引导登录。
- 登录后收到 403 时提示当前账号无权访问该网址。
- 打开网址优先使用接口返回的 `redirectUrl`；没有 `redirectUrl` 时再回退到原始 `url`。

## 8. 相关文档

- [mango-link 后端模块](../../../mango/mango-platform/mango-link/README.md)
- [@mango/link-page](../link-page/README.md)
- [@mango/link-panel 兼容包](../link-panel/README.md)
