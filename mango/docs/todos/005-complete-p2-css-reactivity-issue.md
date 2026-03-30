---
status: pending
priority: p2
issue_id: 005
tags: [code-review, frontend, vue, css]
dependencies: []
---

# CSS Custom Property Reactivity Issue

## Problem Statement

Theme system uses `document.documentElement.style.setProperty()` to set CSS custom properties. This bypasses Vue's reactivity system - if the reactive theme state changes, the DOM won't automatically update.

## Findings

- **Source**: kieran-typescript-reviewer (related: security-sentinel themeInit concern)
- **Evidence**: Theme composable sets CSS variables via direct DOM manipulation
- **Severity**: P2 - functional issue affecting theme switching

## Proposed Solutions

### Solution A: Use CSS classes for theme switching (Recommended)
**Pros**: Vue reactivity works naturally; cleaner separation of concerns
**Cons**: May require more CSS if many theme variations
**Effort**: Medium
**Risk**: Low

### Solution B: Force re-render after theme change
**Pros**: Quick fix; keeps current structure
**Cons**: Causes unnecessary re-renders; poor UX
**Effort**: Small
**Risk**: Low

### Solution C: Use reactive CSS variable binding with Vue 3 CSS variable support
**Pros**: Keeps CSS variables; uses Vue reactivity
**Cons**: Requires Vue 3.4+ with experimental features or specific setup
**Effort**: Medium
**Risk**: Low

## Recommended Action

Refactor theme system to use CSS classes on the document root. Toggle classes based on reactive state.

## Technical Details

- **Affected files**: Theme composables, layout components
- **Component**: Theme system
- **Related**: Issue #004 (themeInit validation)

## Acceptance Criteria

- [ ] Theme changes reflect immediately in UI
- [ ] No direct DOM manipulation for theme switching
- [ ] Theme state is reactive and debuggable

## Work Log

- 2026-03-30: Created issue during mango-web code review

## Resources

- [Vue 3 Reactivity in CSS](https://vuejs.org/api/sfc-css-features.html#css-variables)
