# Sprint 7 Delivery Ledger

| Item | Status | Evidence |
| --- | --- | --- |
| Custom preset supports selected optional modules | DONE | `check-cli.mjs` validates `workflow-example,template` includes workflow/template/example only |
| Full preset remains full mode | DONE | `check-cli.mjs` validates `@mango/admin/full`, `style-full.css` and `mango-admin-starter` |
| `mango add` adds selected optional module | DONE | `check-cli.mjs` validates adding `notice` updates config, package, frontend entry and backend pom |
| `mango add` does not overwrite business-owned files | DONE | `check-cli.mjs` validates generated project `README.md` remains unchanged after add |
| PMO page regression checks require page details | DONE | `mango-pmo/rules/frontend/04-test.md` and generated baseline copy updated |
| Browser screenshots for generated project | EXCEPTION | Not a runtime UI Sprint; no generated service was started. Rule is added for future UI acceptance. |

## Commands

```bash
node mango-ui/packages/mango-cli/scripts/check-cli.mjs
pnpm --dir mango-ui -F mango-cli test
git diff --check
```
