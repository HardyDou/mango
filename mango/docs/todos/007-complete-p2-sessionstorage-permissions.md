---
status: pending
priority: p2
issue_id: 007
tags: [code-review, security, session]
dependencies: []
---

# User Permissions Stored in sessionStorage

## Problem Statement

User permissions are stored in `sessionStorage` (`src/stores/userInfo.ts`). While sessionStorage is slightly safer than localStorage (cleared on tab close), it's still accessible to JavaScript and vulnerable to XSS.

## Findings

- **Source**: security-sentinel
- **Evidence**: `src/stores/userInfo.ts` - stores permissions in sessionStorage
- **Severity**: P2 - should fix for defense in depth

## Proposed Solutions

### Solution A: Keep permissions in memory only (Recommended)
**Pros**: Not persisted anywhere; cleared on page unload
**Cons**: Permissions lost on page refresh (but can be refetched)
**Effort**: Small
**Risk**: Low

### Solution B: Encrypt permissions if sessionStorage is required
**Pros**: Data not in plaintext
**Cons**: Key management issue
**Effort**: Medium
**Risk**: Medium

## Recommended Action

Keep permissions in reactive state (Pinia store) without persisting to storage. Refetch on page load if needed.

## Technical Details

- **Affected files**: `src/stores/userInfo.ts`
- **Component**: User state management

## Acceptance Criteria

- [ ] User permissions not in sessionStorage
- [ ] Permissions still work correctly after refresh (refetched from API)
- [ ] Permissions cleared when tab closes

## Work Log

- 2026-03-30: Created issue during mango-web code review

## Resources

- [sessionStorage vs localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage)
