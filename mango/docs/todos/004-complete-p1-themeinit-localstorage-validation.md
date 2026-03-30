---
status: pending
priority: p1
issue_id: 004
tags: [code-review, security, theme]
dependencies: []
---

# themeInit.ts Parses localStorage Without Validation

## Problem Statement

`themeInit.ts` reads values from localStorage and applies them as CSS without validating the data structure or sanitizing content. Malicious localStorage data could inject unexpected CSS values or trigger JS errors.

## Findings

- **Source**: security-sentinel
- **Evidence**: `src/layout/hooks/use-theme.ts` or `themeInit.ts` - reads localStorage and applies CSS directly
- **Severity**: P1 - security concern (CSS injection potential)

## Proposed Solutions

### Solution A: Validate localStorage data before applying (Recommended)
**Pros**: Prevents malformed data from breaking UI; adds safety
**Cons**: Slight code complexity increase
**Effort**: Small
**Risk**: Low

### Solution B: Use schema validation (Zod/Joi)
**Pros**: Type-safe validation, clear error messages
**Cons**: Adds a dependency
**Effort**: Small
**Risk**: Low

### Solution C: Apply CSS only to specific properties with allowlist
**Pros**: Defense in depth; even with bad data, only safe properties are applied
**Cons**: More restrictive
**Effort**: Small
**Risk**: Low

## Recommended Action

Add validation for localStorage theme data before applying. Use a schema to ensure expected structure.

## Technical Details

- **Affected files**: Theme initialization files
- **Component**: Theme system
- **Related**: Issue #002 (localStorage token storage)

## Acceptance Criteria

- [ ] Invalid theme data doesn't break the UI
- [ ] Console shows clear error for invalid theme data
- [ ] Only allowlisted CSS properties are applied

## Work Log

- 2026-03-30: Created issue during mango-web code review

## Resources

- [CSS Injection Prevention](https://owasp.org/www-community/attacks/CSS_injection)
