# @mango/admin-shell

Reusable Mango Admin shell package for enterprise admin applications.

## Install

```bash
npm install @mango/admin-shell @mango/admin-pages @mango/app-runtime @mango/auth @mango/common @mango/file @mango/notice @mango/rbac @mango/workflow element-plus pinia vue vue-i18n vue-router
```

Import the shell styles once in the host entry:

```ts
import '@mango/admin-shell/style.css';
```

## Create An App

```ts
import { createMangoAdminApp } from '@mango/admin-shell';
import '@mango/admin-shell/style.css';

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: '/api',
  title: 'Acme Admin',
  features: ['authorization', 'system'],
  runtimeConfigUrl: '/runtime-config.json',
}).mount();
```

`createMangoAdminApp` installs Vue, router, Pinia, Element Plus, Mango auth, i18n, auth directives and runtime hosting.

## Options

| Option | Purpose |
|---|---|
| `mountTarget` | DOM selector or element used by `mount()`. Default: `#app`. |
| `apiBaseUrl` | Base URL for Mango HTTP requests. Default: `/api`. |
| `title` | Shell title and login brand title. |
| `contentMode` | Shell content mode. Product apps normally use the default `runtime-outlet`. |
| `login` | Mango auth login configuration. Used for brand, defaults and slots. |
| `features` | Enabled Mango platform features. Use `full` only through `@mango/admin/full`. |
| `featureRegistrars` | Functions that register page providers and shell integrations before menus/runtime load. |
| `modules` | Inline runtime module configuration merged with defaults. |
| `localApps` | Local runtime applications registered into `@mango/app-runtime`. |
| `runtimeConfigUrl` | Remote or relative runtime config URL. |
| `runtimeConfigLoadOptions` | Runtime config safety options, including allowed entry origins and fail-closed behavior. |
| `devCenter` | Development center visibility, deploy environment, page registrars and page provider. |

## Extension Points

### Feature Registrars

Feature registrars are the normal way for a business package to register pages or shell integrations:

```ts
import { createMangoAdminApp, type MangoAdminFeatureRegistrar } from '@mango/admin-shell';
import { registerProcurementPages } from '@acme/procurement';

const featureRegistrars: MangoAdminFeatureRegistrar[] = [
  registerProcurementPages,
];

createMangoAdminApp({ featureRegistrars }).mount();
```

Registrars run once before shell consumers load menus and runtime providers.

### Runtime Modules

Runtime modules describe whether a module is mounted locally or through a micro frontend entry:

```ts
createMangoAdminApp({
  modules: {
    'acme-procurement': {
      mode: 'local',
      runtimeCode: 'acme-procurement-local',
      appType: 'LOCAL',
      framework: 'vue3',
    },
  },
});
```

For production-like environments, keep `failClosed: true` and configure `allowedEntryOrigins` for micro frontend entries.

### Menu Contract

Backend menus are loaded from `/authorization/menus/user?fmt=tree&appCode=internal-admin`.

- Directory menus use `menuType = 1` and should declare `redirect` or have a runnable child menu.
- Runnable menus use `menuType = 2` and must have a registered page `component` key.
- Button permissions use `menuType = 3` and are filtered out of route menus.
- Hidden package routes can be registered by page providers and are not shown as visible menu items.

### Theme

The shell exposes Mango and Element Plus CSS variables through `@mango/admin-shell/style.css`.

Important variables:

- `--mango-color-primary`
- `--el-color-primary`
- `--mango-bg-top-bar`
- `--mango-bg-menu-bar`
- `--mango-bg-columns-menu-bar`

Micro frontend apps receive theme data through the Mango runtime object and should listen for runtime `theme-change` events instead of reading shell stores directly.

### I18n

The shell installs `vue-i18n` in composition mode with `zh-cn` defaults for login and common Mango components. Business packages should provide their own i18n keys or pass localized text through component props.

### Directives

The shell installs three permission directives:

- `v-auth`
- `v-auths`
- `v-auth-all`

These directives read Mango auth permission state. They are display guards only; backend APIs must still enforce authorization.

## Public Entrypoints

Stable product entrypoints:

- `@mango/admin-shell`
- `@mango/admin-shell/runtime`
- `@mango/admin-shell/menu`
- `@mango/admin-shell/stores`
- `@mango/admin-shell/router`
- `@mango/admin-shell/home`
- `@mango/admin-shell/style.css`

Development-center entrypoints are for Mango development and verification:

- `@mango/admin-shell/dev-pages`
- `@mango/admin-shell/dev-base-pages`
- `@mango/admin-shell/dev-upload-page`
- `@mango/admin-shell/dev-workflow-page`

Enterprise business projects should normally consume `@mango/admin` or `@mango/admin/full`; use these lower-level shell entrypoints only when building a custom host.

## Migration From App-Local Shell Code

1. Install `@mango/admin-shell` and peer dependencies.
2. Replace app-local layout, router and store imports with `createMangoAdminApp`.
3. Move business pages into package page registrars.
4. Move runtime module entries into `runtime-config.json` or the `modules` option.
5. Import `@mango/admin-shell/style.css` once.
6. Run typecheck, production build and browser acceptance against real menus and permissions.

## Compatibility Matrix

| Mode | Expected check |
|---|---|
| Full preset | `@mango/admin/full` imports shell and full feature registrars. |
| Custom preset | `@mango/admin` imports shell and selected feature registrars. |
| Local module | Registered page provider mounts through `LOCAL_ROUTE`. |
| Micro module | Runtime config entry is allowlisted and mounted through app runtime. |
| Published npm package | `npm pack --dry-run --json` includes `dist`, `src`, `style.css` and `README.md`. |

## Verification

```bash
pnpm -F @mango/admin-shell test
pnpm -F @mango/admin-shell build
npm pack --dry-run --json
```
