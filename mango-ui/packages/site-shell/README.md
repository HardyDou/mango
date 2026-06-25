# @mango/site-shell

`@mango/site-shell` is the runtime package for CMS-backed public sites. It wraps `/cms/open/**` requests, site resolution, SEO application, and Vue state sharing for enterprise sites, help centers, and portal applications.

## 1. 概览

This package is a thin site runtime layer. It does not ship a visual theme or complete pages; each site App owns its own layout, navigation, content presentation, responsive design, and route structure.

Use this package when a frontend needs to consume published CMS data without joining Mango Admin. For management pages, use `@mango/cms`.

## 2. 功能清单

| Capability | Entry |
|------------|-------|
| Resolve a site by `siteCode` or domain | `cmsSiteApi.resolveSite()`, `createSiteResolveQuery()` |
| Read site detail | `cmsSiteApi.detailSite()` |
| Read public category tree | `cmsSiteApi.treeCategories()` |
| Read public navigation | `cmsSiteApi.listNavigations()` |
| Read public banners | `cmsSiteApi.listBanners()` |
| Read public advertisements | `cmsSiteApi.listAdvertisements()` |
| Read published content list | `cmsSiteApi.pageContents()` |
| Read published content detail | `cmsSiteApi.detailContent()` |
| Share site state in Vue | `createSiteShellState()`, `provideSiteShell()`, `useSiteShell()` |
| Apply SEO metadata | `applySiteSeo()` |

## 3. 接入方式

Install the package in a public site app:

```bash
pnpm add @mango/site-shell
```

Resolve the current site by browser domain:

```ts
import { cmsSiteApi, createSiteResolveQuery } from '@mango/site-shell';

const site = await cmsSiteApi.resolveSite(createSiteResolveQuery(undefined, window.location.host));
```

Provide shared state in a Vue app:

```ts
import { createSiteShellState, provideSiteShell } from '@mango/site-shell';

const siteShell = provideSiteShell(createSiteShellState());
await siteShell.resolve();
```

Use the state in child components:

```ts
import { useSiteShell } from '@mango/site-shell';

const { site, loading, error } = useSiteShell();
```

## 4. 配置说明

This package has no package-level configuration file.

| Configuration | Default | Meaning |
|---------------|---------|---------|
| `createSiteShellState(defaultQuery)` | `{}` | Default query used by `resolve()` when no query is supplied. |
| `createSiteResolveQuery(siteCode, domain)` | Browser host fallback | Builds a query using `siteCode` first, then `domain`, then `window.location.host`. |
| Host request base URL | Host-defined | Determines where `/cms/open/**` API requests are sent. |
| `ignoreToken` request option | `true` in package API helpers | Public site requests do not require an admin login token. |
| `silentError` request option | `true` in package API helpers | Host pages decide how to present public site loading errors. |

SEO values come from CMS site data. `applySiteSeo()` updates `document.title`, `meta[name="keywords"]`, and `meta[name="description"]` when running in a browser.

## 5. API 与扩展

Main API helper:

| Method | Backend API | Purpose |
|--------|-------------|---------|
| `cmsSiteApi.resolveSite(params)` | `GET /cms/open/sites/resolve` | Resolve current site metadata. |
| `cmsSiteApi.detailSite(params)` | `GET /cms/open/sites/detail` | Read detailed site configuration. |
| `cmsSiteApi.treeCategories(params)` | `GET /cms/open/site-categories/tree` | Read visible category tree. |
| `cmsSiteApi.listNavigations(params)` | `GET /cms/open/navigations/list` | Read navigation by `navType`. |
| `cmsSiteApi.listBanners(params)` | `GET /cms/open/banners/list` | Read banner records by position. |
| `cmsSiteApi.listAdvertisements(params)` | `GET /cms/open/advertisements/list` | Read advertisement deliveries by position. |
| `cmsSiteApi.pageContents(params)` | `GET /cms/open/contents/page` | Read published content pages. |
| `cmsSiteApi.detailContent(params)` | `GET /cms/open/contents/detail` | Read one published content detail. |

State helpers:

| Export | Purpose |
|--------|---------|
| `createSiteShellState(defaultQuery)` | Creates reactive site, loading, error, and resolve state. |
| `provideSiteShell(state)` | Provides a `SiteShellState` to Vue descendants. |
| `useSiteShell()` | Injects the provided site shell state. |
| `normalizeSiteQuery(query)` | Adds browser host domain when neither `siteCode` nor `domain` is provided. |
| `createSiteResolveQuery(siteCode, domain)` | Creates a normalized site resolve query. |
| `applySiteSeo(site)` | Applies title, keywords, and description to the current document. |

Shared TypeScript types include `ApiId`, `SiteResolveQuery`, `SiteResolve`, `CmsSite`, `SiteCategory`, `SiteNavigation`, `SiteBanner`, `SiteAdvertisement`, `SiteContent`, `PageResult`, and `SiteShellState`.

## 6. 数据与初始化

This frontend package does not initialize database records, menus, permissions, or static site content.

| Data | Source |
|------|--------|
| Site metadata, SEO, categories, navigation, banners, advertisements, and content | Backend `mango-cms` public APIs under `/cms/open/**`. |
| Public image and media preview URLs | Backend CMS and file capabilities derive them at runtime from file IDs. |
| Initial site query | Host app route, domain, or explicit `siteCode`. |
| Page layout and navigation rendering | Host site app. |

Public CMS APIs only return enabled, visible, published, and valid-time data according to backend `mango-cms` filtering.

## 7. 管理入口

`@mango/site-shell` has no admin pages and registers no menu component keys. Site content is managed through the `@mango/cms` admin package and backend `mango-cms` management APIs.

The public backend API prefix is `/cms/open/**`. These APIs are registered as public access by the backend CMS starter, while tenant, visibility, publish status, and validity filtering remain backend responsibilities.

## 8. 快速开始

1. Enable backend `mango-cms-starter` and run CMS migrations.
2. Create a CMS site, domain, category tree, navigation, banners, and published content in Mango Admin.
3. Install `@mango/site-shell` in the public site App.
4. Resolve the site by `siteCode` or browser domain.
5. Load navigation, categories, banners, advertisements, and published content through `cmsSiteApi`.
6. Render the visual pages in the host site App.
7. Use `applySiteSeo()` or `createSiteShellState().resolve()` to apply SEO metadata.

## 9. 问题排查

**Site resolve returns empty or duplicate-site errors**

Check whether the CMS site is enabled and whether `siteCode` or `domain` maps to exactly one site record.

**Public requests return 401**

Confirm backend `mango-cms-starter` is enabled and that `/cms/open/**` is included in the public access path registration.

**Content list is empty**

Confirm the content is approved or published, the publish relation is published, the category belongs to the resolved site, and the publish window is valid.

**SEO metadata does not change**

Confirm `applySiteSeo()` runs in a browser context after site resolution. Server-side rendering hosts should apply metadata through their own SSR head management.

## 10. 相关文档

- [Backend CMS module](../../../mango/mango-platform/mango-cms/README.md)
- [CMS admin package](../cms/README.md)
- [CMS design](../../../mango-docs/designs/2026-06-22-mango-cms-design.md)
- [Capability docs rule](../../../mango-pmo/rules/08-capability-docs.md)
