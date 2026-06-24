# @mango/pmo

`@mango/pmo` publishes the Mango PMO baseline as a versioned npm package. The source of truth remains the repository root `mango-pmo`; this package only builds a distributable snapshot for business projects and `@mango/cli`.

Build before publishing:

```bash
pnpm -F @mango/pmo build
pnpm -F @mango/pmo check
```

Published assets:

| Path | Purpose |
|------|---------|
| `dist/baseline` | Executable PMO baseline copied from `mango-pmo` |
| `dist/baseline.json` | SHA-256 manifest used by `mango pmo status/check/sync/upgrade` |

Business projects should consume this package through `@mango/cli` instead of editing baseline files manually.
