# Sprint 5 NPM Package Audit

## 1. Conclusion

Current Mango frontend admin consumer packages passed clean tarball consumption and Nexus consumption validation.

The audit result is:

- Core admin and capability packages have public entries, type entries, peer dependencies and style entries.
- `@mango/admin-pages` intentionally has no style entry because it is a page registry package, not a UI style package.
- `@mango/api-schema`, `@mango/app-runtime` and `create-mango-app` are not yet proven as standalone direct-consumer packages; Sprint 5 proved them only through the `@mango/admin` consumer closure where applicable.
- No `workspace:*` dependency was found in checked frontend package manifests.
- Test-only config files still reference app-local node_modules or package source aliases; these are not runtime package blockers, but should be cleaned before final release readiness.

## 2. Package Status

| Package | Public entry | Type entry | Style entry | Peer deps | Workspace deps | Status | Notes |
|---|---:|---:|---:|---:|---:|---|---|
| `@mango/admin` | yes | yes | yes | yes | no | PASS | Admin consumer entry. |
| `@mango/admin-shell` | yes | yes | yes | yes | no | PASS | Shell package. |
| `@mango/admin-pages` | yes | yes | n/a | yes | no | PASS | Registry package; no runtime style ownership. |
| `@mango/common` | yes | yes | yes | yes | no | PASS | Common package. |
| `@mango/auth` | yes | yes | yes | yes | no | PASS | Auth package. |
| `@mango/file` | yes | yes | yes | yes | no | PASS | File UI/API package. |
| `@mango/workflow` | yes | yes | yes | yes | no | PASS | Workflow package. |
| `@mango/workflow-business-example` | yes | yes | yes | yes | no | PASS | Example package; release policy still needs confirmation. |
| `@mango/rbac` | yes | yes | yes | yes | no | PASS | RBAC package. |
| `@mango/system` | yes | yes | yes | yes | no | PASS | System package; should remain required in admin preset. |
| `@mango/template` | yes | yes | yes | yes | no | PASS | Template package. |
| `@mango/notice` | yes | yes | yes | yes | no | PASS | Notice package. |
| `@mango/calendar` | yes | yes | yes | yes | no | PASS | Calendar package. |
| `@mango/numgen` | yes | yes | yes | yes | no | PASS | Number generation package. |
| `@mango/api-schema` | no | no | n/a | no | no | BLOCKED | Needs explicit package contract or must be marked internal. |
| `@mango/app-runtime` | yes | no | n/a | yes | no | BLOCKED | Needs type entry and consumer validation. |
| `create-mango-app` | no | no | n/a | no | no | BLOCKED | CLI package; should be handled by `mango-cli`/create flow Sprint. |

## 3. Nexus Publishing Rule

Maven and npm should use the same Nexus account system.

Allowed:

- Maven credentials in user-level Maven settings or CI Secret.
- npm credentials in user-level npm config or CI Secret.
- Repository docs may record registry URLs, scopes and commands.

Forbidden:

- Commit npm token, username or password.
- Commit Maven server password.
- Put credentials in `.npmrc`, `package.json`, generated starter templates or documentation examples.

## 4. Final Verification

Sprint 5 completed:

1. Pack install verification from clean consumer.
2. Nexus npm install verification on company intranet.
3. Consumer typecheck/build/runtime smoke.
4. E2E screenshots with UI/layout inspection.
5. Data/API checks for representative admin capability pages.
6. Feature-selection verification for core, workflow-only and full consumers.
7. Workflow custom apply activation verification through explicit `@mango/workflow-business-example` registration.

## 5. Integration Contract

Admin consumers integrate packages through one place: `createMangoAdminApp`.

Core usage:

```ts
import { createMangoAdminApp } from '@mango/admin';
import '@mango/admin/style.css';

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: '/api',
  features: 'core',
}).mount();
```

Selective optional usage:

```ts
import { createMangoAdminApp } from '@mango/admin';
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';
import { registerMangoWorkflowBusinessExampleAdminPages } from '@mango/workflow-business-example/admin-pages';
import '@mango/admin/style.css';
import '@mango/workflow/style.css';
import '@mango/workflow-business-example/style.css';

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: '/api',
  features: ['workflow'],
  featureRegistrars: [
    registerMangoWorkflowAdminPages,
    registerMangoWorkflowBusinessExampleAdminPages,
  ],
}).mount();
```

Full usage:

```ts
import { createMangoAdminApp } from '@mango/admin';
import { mangoFullAdminFeatureRegistrars } from '@mango/admin/full';
import '@mango/admin/style-full.css';

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: '/api',
  features: 'full',
  featureRegistrars: mangoFullAdminFeatureRegistrars,
}).mount();
```

Rules:

- `@mango/admin/style.css` is core style only.
- Optional package styles follow their package and must be imported by the consumer or by `@mango/admin/style-full.css`.
- Optional package pages follow their package and must be registered through that package registrar.
- Hidden local routes, such as `/workflow/custom-apply`, are package-owned and registered through `@mango/admin-pages`.
- `@mango/workflow` must not secretly import `@mango/workflow-business-example`.

Remaining package governance for later Sprint:

- Decide whether `@mango/api-schema` and `@mango/app-runtime` are direct-consumer public packages or internal dependency packages.
- Replace `create-mango-app` with the planned `mango-cli` scope after the verified Nexus package path.
