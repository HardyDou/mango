# Mango Common

`mango-common` is the single-module common kernel and contract package for Mango.

## Scope

This module keeps only stable, low-coupling, cross-module contracts:

- business error contract: `BizCode`, `CommonCode`, `BizException`
- response contract: `R<T>`
- assertion helpers: `Require`
- paging models: `PageQuery`, `PageResult`
- validation annotations and validators: `@Phone`, `@IdCard`

## Out Of Scope

This module must not keep implementation-heavy runtime concerns:

- web utilities
- security context implementations
- cryptography implementations
- duplicate compatibility shims after migration

## Sprint 09 Decision

Sprint 09 does not split `mango-common` into new Maven submodules.

The delivery target is:

1. keep the existing `mango-common` module
2. reduce it to common kernel and contracts
3. move implementation-heavy code to infra modules
4. fix downstream references

## Migration Notes

- `Base64Utils` lives in `mango-infra-crypto`
- `JacksonUtils` lives in `mango-infra-web`
- `TokenContextHolder` lives in `mango-infra-security`

## Validation

Run focused checks from `mango/`:

```bash
mvn -pl mango-common test
mvn -pl mango-common verify
mvn -pl mango-common checkstyle:check
```
