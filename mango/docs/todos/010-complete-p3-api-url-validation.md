---
status: pending
priority: p3
issue_id: 010
tags: [code-review, security, api]
dependencies: []
---

# API URL Path Parameter Not Validated

## Problem Statement

The i18n API (`src/api/admin/i18n.ts`) accepts path parameters without validation. Malformed or malicious path parameters could cause unexpected behavior.

## Findings

- **Source**: security-sentinel
- **Evidence**: `src/api/admin/i18n.ts` - path parameters passed to API without validation
- **Severity**: P3 - low risk but good practice

## Proposed Solutions

### Solution A: Add path parameter validation (Recommended)
**Pros**: Defense in depth; clear error messages for bad input
**Cons**: Slight code overhead
**Effort**: Small
**Risk**: Low

### Solution B: Use typed route parameters
**Pros**: Compile-time safety for route params
**Cons**: May require router changes
**Effort**: Medium
**Risk**: Low

## Recommended Action

Add validation for path parameters before making API calls. Ensure path parameters match expected format (e.g., numeric IDs are actually numeric).

## Technical Details

- **Affected files**: `src/api/admin/i18n.ts` and similar API files
- **Component**: API layer

## Acceptance Criteria

- [ ] Invalid path params rejected before API call
- [ ] Clear error message shown for invalid params
- [ ] API not called with malformed data

## Work Log

- 2026-03-30: Created issue during mango-web code review

## Resources

- [Input Validation Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)
