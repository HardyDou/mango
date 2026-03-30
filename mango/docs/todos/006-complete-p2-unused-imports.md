---
status: pending
priority: p2
issue_id: 006
tags: [code-review, typescript, quality]
dependencies: []
---

# Unused Imports and Duplicate Code Issues

## Problem Statement

Several files have unused imports and other code quality issues that reduce maintainability.

## Findings

- **Source**: kieran-typescript-reviewer
- **Evidence**:
  - `src/router/index.ts` - unused imports
  - `src/utils/mitt.ts` - unused imports
  - `src/utils/request.ts` - unused imports
  - `src/layout/components/columnsAside.vue` - duplicate `onMounted`
- **Severity**: P2 - code quality issue

## Proposed Solutions

### Solution A: Clean up in bulk (Recommended)
**Pros**: Quick win; reduces noise in codebase
**Cons**: Must be careful not to remove actually-used imports
**Effort**: Small
**Risk**: Low - use IDE to verify usage before deletion

### Solution B: Enable ESLint no-unused-vars
**Pros**: Prevents future accumulation
**Cons**: Requires fixing any legitimate "unused" vars
**Effort**: Small
**Risk**: Low

## Recommended Action

1. Use IDE "optimize imports" feature to remove unused imports
2. Fix duplicate `onMounted` in columnsAside.vue
3. Enable ESLint rule to prevent future issues

## Technical Details

- **Affected files**:
  - `src/router/index.ts`
  - `src/utils/mitt.ts`
  - `src/utils/request.ts`
  - `src/layout/components/columnsAside.vue`
- **Component**: Code quality

## Acceptance Criteria

- [ ] No unused imports in listed files
- [ ] No duplicate lifecycle hooks in columnsAside.vue
- [ ] ESLint no-unused-imports passes

## Work Log

- 2026-03-30: Created issue during mango-web code review

## Resources

- [ESLint no-unused-vars](https://eslint.org/docs/latest/rules/no-unused-vars)
