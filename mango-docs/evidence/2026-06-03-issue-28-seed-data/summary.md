# Issue 28 Seed Data Summary

## Purpose

Issue #28 adds an optional official seed contract for business projects. It helps enterprise consumers initialize the minimum Mango entry data without guessing tenant, administrator, role, application binding or menu package grants.

## Scope

- Added `io.mango.platform.seed:mango-seed-starter`.
- Seed is disabled by default through `mango.seed.enabled=false`.
- Full `mango-cli` preset depends on the seed starter but keeps it disabled unless `MANGO_SEED_ENABLED=true`.
- Initial admin creation requires an explicit `mango.seed.admin.initial-password`.
- Existing admin passwords are not overwritten.
- `prod` and `production` profiles reject known weak default passwords.
- Seed is idempotent by natural keys.

## Boundary

Historical V1 migrations already contain baseline data and were not modified. This task does not promise a completely data-free baseline migration. Menu definitions, frontend components and runtime strategies still come from official migrations and published packages; seed only grants menus from an existing official menu package.

## Verification

```bash
mvn -pl mango-platform/mango-seed/mango-seed-starter -am test -q
mvn -pl mango-platform/mango-seed/mango-seed-starter test -q
npm --prefix mango-ui/packages/mango-cli test
```

All commands passed in the issue #28 worktree.
