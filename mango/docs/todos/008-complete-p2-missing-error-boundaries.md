---
status: pending
priority: p2
issue_id: 008
tags: [code-review, vue, error-handling]
dependencies: []
---

# Missing Error Boundaries for Async Components

## Problem Statement

Layout components use `defineAsyncComponent` without error boundaries. If an async component fails to load, there's no fallback UI or error display, leading to broken layouts.

## Findings

- **Source**: kieran-typescript-reviewer
- **Evidence**: `src/layout/components/columnsAside.vue` and similar layout files using async components
- **Severity**: P2 - reliability issue

## Proposed Solutions

### Solution A: Add error boundary component (Recommended)
**Pros**: Graceful degradation when components fail
**Cons**: Slight code complexity
**Effort**: Small
**Risk**: Low

### Solution B: Add loading/error slots for async components
**Pros**: Built-in Vue async component handling
**Cons**: Requires per-component setup
**Effort**: Small
**Risk**: Low

## Recommended Action

Wrap async component usage with proper error handling using Vue's `Suspense` or custom error boundary component.

## Technical Details

- **Affected files**: Layout components using `defineAsyncComponent`
- **Component**: Layout/routing

## Acceptance Criteria

- [ ] Failed async components show error state, not blank
- [ ] Loading states display during component load
- [ ] User can retry failed loads

## Work Log

- 2026-03-30: Created issue during mango-web code review

## Resources

- [Vue Async Components](https://vuejs.org/guide/components/async.html)
