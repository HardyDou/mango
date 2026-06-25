# @mango/cms

`@mango/cms` is the Mango CMS admin package. It provides management pages, a small advertisement component, and typed HTTP API helpers for the backend `mango-cms` module.

## 1. 概览

This package is for Mango Admin and business admin applications. It registers CMS management pages through `@mango/admin-pages`, calls `/cms/**` management APIs, and ships its own `style.css` for package-level styles.

The package does not implement the public site frontend. Public site applications should use `@mango/site-shell` for `/cms/open/**` consumption and build their own visual pages.

## 2. 功能清单

| Capability | Entry |
|------------|-------|
| Site management | `CmsSitesView`, `cmsApi.pageSites()` |
| Site category management | `CmsSiteCategoriesView`, `cmsApi.treeSiteCategories()` |
| Content category management | `CmsContentCategoriesView`, `cmsApi.treeContentCategories()` |
| Content tag management | `CmsContentTagsView`, `cmsApi.pageContentTags()` |
| Content pool management | `CmsContentsView`, `cmsApi.pageContents()` |
| Content publish management | `CmsContentPublishesView`, `cmsApi.pagePublishes()` |
| Navigation management | `CmsNavigationsView`, `cmsApi.pageNavigations()` |
| Advertisement slot management | `CmsAdvertisementsView`, `cmsApi.pageAdvertisements()` |
| Advertisement delivery management | `CmsAdDeliveriesView`, `cmsApi.pageAdDeliveries()` |
| Inline advertisement rendering | `MAd` |

## 3. 接入方式

Install the package in an admin application:

```bash
pnpm add @mango/cms
```

Register CMS admin pages during application bootstrap:

```ts
import { registerMangoCmsAdminPages } from '@mango/cms/admin-pages';
import '@mango/cms/style.css';

registerMangoCmsAdminPages();
```

Use API helpers in admin pages or integration code:

```ts
import { cmsApi } from '@mango/cms';

const sites = await cmsApi.pageSites({ pageNum: 1, pageSize: 10 });
```

Render an advertisement block from already loaded delivery data:

```vue
<script setup lang="ts">
import { MAd, type MAdItem } from '@mango/cms';

defineProps<{ items: MAdItem[] }>();
</script>

<template>
  <MAd :items="items" />
</template>
```

## 4. 配置说明

This package has no standalone environment file. Runtime behavior comes from page registration, the backend `mango-cms` APIs, and the host application's request base URL.

| Configuration | Default | Meaning |
|---------------|---------|---------|
| `registerMangoCmsAdminPages()` | Not called | Registers CMS page keys into the admin page registry. |
| `@mango/cms/style.css` | Not imported by this package consumer | Loads CMS page and component styles. |
| Host request base URL | Host-defined | Determines where `/cms/**` API requests are sent. |

For official Mango Admin full integration, `@mango/cms/style.css` is declared in `mango-ui/packages/admin/admin-packages.json` and generated into the admin style aggregation chain.

## 5. API 与扩展

Page registration entry:

| Export | Source | Purpose |
|--------|--------|---------|
| `registerMangoCmsAdminPages()` | `@mango/cms/admin-pages` | Registers CMS admin page keys under module code `mango-cms`. |

Registered page keys:

| Page key | Export |
|----------|--------|
| `cms/sites/index` | `CmsSitesView` |
| `cms/site-categories/index` | `CmsSiteCategoriesView` |
| `cms/contents/index` | `CmsContentsView` |
| `cms/content-categories/index` | `CmsContentCategoriesView` |
| `cms/content-tags/index` | `CmsContentTagsView` |
| `cms/content-publishes/index` | `CmsContentPublishesView` |
| `cms/navigations/index` | `CmsNavigationsView` |
| `cms/advertisements/index` | `CmsAdvertisementsView` |
| `cms/ad-deliveries/index` | `CmsAdDeliveriesView` |

Main API helper groups:

| Group | Methods |
|-------|---------|
| Sites | `pageSites()`, `detailSite()`, `createSite()`, `updateSite()`, `updateSiteStatus()`, `deleteSite()` |
| Content categories | `pageContentCategories()`, `listContentCategories()`, `treeContentCategories()`, `createContentCategory()`, `updateContentCategory()`, `updateContentCategoryStatus()`, `deleteContentCategory()` |
| Content tags | `pageContentTags()`, `listContentTags()`, `createContentTag()`, `updateContentTag()`, `updateContentTagStatus()`, `deleteContentTag()` |
| Site categories | `treeSiteCategories()`, `createSiteCategory()`, `updateSiteCategory()`, `updateSiteCategoryStatus()`, `deleteSiteCategory()` |
| Contents | `pageContents()`, `detailContent()`, `createContent()`, `updateContent()`, `submitContent()`, `approveContent()`, `rejectContent()`, `offlineContent()`, `deleteContent()` |
| Publishes | `pagePublishes()`, `publishContents()`, `offlinePublish()`, `deletePublish()` |
| Navigations | `pageNavigations()`, `createNavigation()`, `updateNavigation()`, `updateNavigationStatus()`, `deleteNavigation()` |
| Advertisements | `pageAdvertisements()`, `createAdvertisement()`, `updateAdvertisement()`, `updateAdvertisementStatus()`, `deleteAdvertisement()` |
| Ad deliveries | `pageAdDeliveries()`, `createAdDelivery()`, `updateAdDelivery()`, `updateAdDeliveryStatus()`, `deleteAdDelivery()` |
| Site settings | `detailSiteSetting()`, `saveSiteSetting()` |

Shared TypeScript types include `ApiId`, `PageResult`, `CmsSite`, `CmsContent`, `CmsContentPublish`, `CmsNavigation`, `CmsAdvertisement`, `CmsAdDelivery`, `CmsStatus`, and `CmsContentStatus`.

## 6. 数据与初始化

This frontend package does not create database tables, menus, or default records.

| Data | Source |
|------|--------|
| Sites, categories, content, navigation, advertisements, and publish relations | Backend `mango-cms` module. |
| Menus and button permissions | Backend `mango-cms` resource file `META-INF/mango/resources/cms-common-menu.json`. |
| File picker, image preview, and upload records | `@mango/file` and backend `mango-file`. |
| Admin page registration | `registerMangoCmsAdminPages()` writes page loaders into `@mango/admin-pages`. |

## 7. 管理入口

The backend CMS menu components should match these page keys:

| Menu component | Frontend page key |
|----------------|-------------------|
| `@/views/cms/sites/index.vue` | `cms/sites/index` |
| `@/views/cms/site-categories/index.vue` | `cms/site-categories/index` |
| `@/views/cms/contents/index.vue` | `cms/contents/index` |
| `@/views/cms/content-categories/index.vue` | `cms/content-categories/index` |
| `@/views/cms/content-tags/index.vue` | `cms/content-tags/index` |
| `@/views/cms/content-publishes/index.vue` | `cms/content-publishes/index` |
| `@/views/cms/navigations/index.vue` | `cms/navigations/index` |
| `@/views/cms/advertisements/index.vue` | `cms/advertisements/index` |
| `@/views/cms/ad-deliveries/index.vue` | `cms/ad-deliveries/index` |

Common permission codes use the `cms:*` prefix, for example `cms:site:list`, `cms:content:list`, `cms:content:approve`, `cms:navigation:list`, and `cms:advertisement:list`. Tenant and permission checks are enforced by the backend management APIs.

## 8. 快速开始

1. Enable backend `mango-cms-starter` and run CMS Flyway migrations.
2. Install `@mango/cms` in the admin application.
3. Import `@mango/cms/style.css`.
4. Call `registerMangoCmsAdminPages()` during admin bootstrap.
5. Ensure backend menu `component` values match the page keys in this README.
6. Open the CMS menu and create site, category, navigation, content, publish, advertisement, and delivery records.

## 9. 问题排查

**CMS menu opens a 404 page**

Check whether `registerMangoCmsAdminPages()` has run before route rendering, and whether the backend menu `component` normalizes to one of the CMS page keys.

**CMS page has layout but no styles**

Confirm `@mango/cms/style.css` is imported directly by the micro app or included by the admin style aggregation chain.

**Requests return 401 or 403**

Confirm the current user has the corresponding `cms:*` menu or button permission, and that the backend application has `mango-cms-starter` enabled.

**Image upload or preview is unavailable**

Confirm the host application also integrates `@mango/file` and backend `mango-file`, because CMS stores file IDs and uses file capability APIs for upload and preview.

## 10. 相关文档

- [Backend CMS module](../../../mango/mango-platform/mango-cms/README.md)
- [Site shell package](../site-shell/README.md)
- [Admin page registry](../admin-pages/README.md)
- [CMS design](../../../mango-docs/designs/2026-06-22-mango-cms-design.md)
- [Capability docs rule](../../../mango-pmo/rules/08-capability-docs.md)
