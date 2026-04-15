# Sprint 09 Delivery Record

- Sprint: `sprint-09`
- Topic: `mango-common` boundary cleanup
- Delivery date: 2026-04-15
- Branch: `sprint-09-fix`
- Commit: `f3993a14`

---

## 1. Delivery Scope

This delivery completes Sprint 09 as a **single-module cleanup** of `mango-common`.

It does **not** introduce new Maven modules such as `mango-common-kernel` or `mango-common-contract`.

Implemented scope:

- clarified Sprint 09 scope and removed module-splitting ambiguity
- reduced `mango-common` to common contracts and low-coupling models
- removed stale crypto compatibility code from `mango-common`
- narrowed `mango-common` dependencies
- fixed downstream code generation references
- added focused tests and module README

---

## 2. Key Changes

### `mango-common`

- added [mango-common README](../../mango/mango-common/README.md)
- removed deprecated `io.mango.common.crypto.base.Base64Utils`
- narrowed `pom.xml` dependencies by removing Web, Jackson, BouncyCastle, Hutool Crypto, and MyBatis annotation dependencies
- clarified `BizCode`, `CommonCode`, `BizException`, `R`, `Require`, `PageQuery`, and `PageResult`
- replaced scattered default `400` handling in `Require` with centralized `CommonCode.BAD_REQUEST`
- added `PageQueryTest` and `PageResultTest`

### `mango-tools`

- fixed `GenCrudMojo` to import `io.mango.common.vo.PageResult`
- fixed generated controller class-level `@RequestMapping`

### `mango-docs`

- updated Sprint 09 plan to explicitly state that this sprint does not require splitting `mango-common` into new Maven submodules

---

## 3. Validation Evidence

Executed in `mango/` from worktree `sprint-09-fix`:

```bash
mvn -pl mango-common test -q
mvn -pl mango-common verify -q
mvn -pl mango-common clean checkstyle:check -q
mvn -pl mango-tools/mango-maven-plugin -Dtest=GenCrudMojoTest test -q
```

Result:

- all commands passed
- `mango-common` builds and validates in isolation
- generated CRUD code now uses the corrected paging import and controller mapping

---

## 4. Acceptance Notes

Sprint 09 acceptance is satisfied by code and command evidence, not UI screenshots.

Reason:

- this sprint is a backend boundary and contract cleanup
- there is no user-facing page or browser interaction introduced by this change
- the most relevant evidence is source diff, tests, and build/check results

---

## 5. Final Interpretation

The phrase “common kernel and contracts” in Sprint 09 is treated as a **responsibility target**, not a required Maven module split.

If the project later wants:

- `mango-common-kernel`
- `mango-common-contract`
- other `mango-common-*` submodules

that work must be planned as a separate sprint with its own design and migration steps.
