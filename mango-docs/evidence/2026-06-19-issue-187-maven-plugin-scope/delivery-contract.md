# Issue 187 Maven Plugin Scope Warning Delivery Contract

## 1. Goal

Fix the Maven Plugin dependency scope warning reported in GitHub issue #187 for `mango-maven-plugin`.

## 2. Scope

- Update `mango/mango-tools/mango-maven-plugin/pom.xml`.
- Keep Maven runtime and plugin annotation dependencies in `provided` scope.
- Verify the plugin descriptor build and the issue reproduction command after merging latest `origin/main`.

## 3. Out Of Scope

- No Java source changes.
- No API, database, menu, page, permission, or runtime service changes.
- No version release or dependency version bump.

## 4. Design Input

- GitHub issue #187: `mango-maven-plugin` descriptor build prints Maven Plugin dependency scope warnings.
- Maven Plugin descriptor generation expects Maven runtime dependencies to be provided by Maven, not packaged as plugin runtime dependencies.

## 5. Design Notes

### 5.1 Impacted Modules

- `mango-tools/mango-maven-plugin`

### 5.2 API Changes

None.

### 5.3 Data Changes

None.

### 5.4 Menu, Page, And Permission Changes

None.

### 5.5 Test Scope

- `mango-tools/mango-maven-plugin` module test build.
- `mango-tools/mango-maven-plugin` module package build.
- Issue #187 targeted Maven command after latest `origin/main` is merged.
- Dependency tree and generated plugin descriptor dependency list.

## 6. Risks And Limits

No known runtime risk. The changed dependencies are Maven/plugin-tooling APIs supplied by the Maven execution environment or compile-time annotation processing.

## 7. Delivery Ledger

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | GitHub issue #187 | Remove Maven Plugin dependency scope warning for Maven runtime dependencies. | Set `maven-plugin-api`, `maven-core`, and `maven-plugin-annotations` to `provided`; leave real plugin runtime dependencies as compile. | `mango/mango-tools/mango-maven-plugin/pom.xml` | Maven module test/package, issue reproduction command, dependency tree, generated `plugin.xml` dependency list. | DONE | This file and PR checks |

## 8. Acceptance Evidence

| Ledger ID | Object | Check Point | Test Data | Key Assertion | UI Check | Console/Network Result | Evidence | Result |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | `mango-maven-plugin` | Maven Plugin descriptor dependency scopes | Maven reactor build | Original scope warning is absent; Maven runtime dependencies are `provided`; plugin descriptor lists only real runtime dependencies. | N/A | N/A | Commands listed in PR body | PASS |
