---
status: pending
priority: p1
issue_id: 003
tags: [code-review, typescript, quality]
dependencies: []
---

# Excessive `any` Types Reduce Type Safety

## Problem Statement

Multiple files use `any` type extensively, undermining TypeScript's type safety benefits. This makes refactoring dangerous and IDE support less effective.

## Findings

- **Source**: kieran-typescript-reviewer
- **Evidence**:
  - `src/utils/tagsViewRoutes.ts` - multiple `any` types
  - `src/composables/useScrollbar.ts` - `any` type usage
  - `src/utils/mitt.ts` - `any` type usage
- **Severity**: P1 - important code quality issue

## Proposed Solutions

### Solution A: Replace `any` with proper types (Recommended)
**Pros**: Full type safety, better IDE support, self-documenting code
**Cons**: Requires understanding the actual data shapes
**Effort**: Medium
**Risk**: Low

### Solution B: Use `unknown` where type is truly unknown
**Pros**: Forces type narrowing at usage points; safer than `any`
**Cons**: May require more explicit type guards
**Effort**: Small
**Risk**: Low

### Solution C: Configure ESLint `no-explicit-any`
**Pros**: Prevents future introduction of `any`
**Cons**: Requires explicit suppression comments for legitimate cases
**Effort**: Small
**Risk**: Low

## Recommended Action

1. Replace `any` with specific types where the data shape is known
2. Use `unknown` where type is truly indeterminate, with proper type guards
3. Add ESLint rule to prevent new `any` usage

## Technical Details

- **Affected files**:
  - `src/utils/tagsViewRoutes.ts`
  - `src/composables/useScrollbar.ts`
  - `src/utils/mitt.ts`
- **Component**: TypeScript code quality

## Acceptance Criteria

- [ ] No `any` types in the three files above
- [ ] ESLint `no-explicit-any` enabled (or `strict: true` in tsconfig)
- [ ] Build passes with strict type checking

## Work Log

- 2026-03-30: Created issue during mango-web code review

## Resources

- [TypeScript Strict Mode](https://www.typescriptlang.org/tsconfig#strict)
- [ESLint no-explicit-any](https://typescript-eslint.io/rules/no-explicit-any/)
