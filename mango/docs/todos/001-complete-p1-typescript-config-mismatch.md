---
status: pending
priority: p1
issue_id: 001
tags: [code-review, typescript, quality]
dependencies: []
---

# TypeScript Configuration Mismatch

## Problem Statement

The `tsconfig.json` uses TypeScript 5.x compiler options (`bundler`, `allowImportingTsExtensions`) but `package.json` specifies `"typescript": "^4.9.5"`. This mismatch will cause build failures on CI where npm installs the locked 4.9.x version.

## Findings

- **Source**: kieran-typescript-reviewer
- **Evidence**: `mango-web/tsconfig.json` uses TS5.x options; `mango-web/package.json` specifies `"typescript": "^4.9.5"`
- **Severity**: P1 - CRITICAL

## Proposed Solutions

### Solution A: Upgrade TypeScript to 5.x (Recommended)
**Pros**: Enables modern TS features, better type inference, improved error messages
**Cons**: May require minor type adjustments if using deprecated 4.x patterns
**Effort**: Small
**Risk**: Low

### Solution B: Downgrade tsconfig.json to 4.x compatible options
**Pros**: No dependency changes
**Cons**: Loses modern TS features, inconsistent with the direction of the codebase
**Effort**: Small
**Risk**: Low

## Recommended Action

Upgrade TypeScript to 5.x in package.json and run `npm install` to update lockfile.

## Technical Details

- **Affected files**:
  - `mango-web/package.json` - change `"typescript": "^4.9.5"` to `"typescript": "^5.4.0"`
  - `mango-web/tsconfig.json` - verify options are compatible with TS5.4
- **Component**: Build tooling

## Acceptance Criteria

- [ ] `npx tsc --version` shows 5.x
- [ ] `npm run build` completes without TS version errors
- [ ] CI build passes

## Work Log

- 2026-03-30: Created issue during mango-web code review

## Resources

- [TypeScript 5.4 Release Notes](https://devblogs.microsoft.com/typescript/announcing-typescript-5-4/)
