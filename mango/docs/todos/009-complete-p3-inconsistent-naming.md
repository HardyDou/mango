---
status: pending
priority: p3
issue_id: 009
tags: [code-review, naming, quality]
dependencies: []
---

# Inconsistent Component Naming

## Problem Statement

Component naming in mango-web doesn't consistently follow Vue/TypeScript conventions, making the codebase harder to navigate.

## Findings

- **Source**: kieran-typescript-reviewer
- **Evidence**: Mixed naming patterns (PascalCase vs kebab-case) in different files
- **Severity**: P3 - nice to have

## Proposed Solutions

### Solution A: Adopt consistent naming convention
**Pros**: Easier to find files; follows Vue ecosystem standard
**Cons**: Requires renaming across codebase
**Effort**: Medium
**Risk**: Low - IDE refactoring handles this well

### Solution B: Document current convention and enforce with ESLint
**Pros**: Low effort; prevents drift
**Cons**: Doesn't fix existing inconsistencies
**Effort**: Small
**Risk**: None

## Recommended Action

Establish and document naming conventions. Use IDE bulk rename for existing files.

## Technical Details

- **Affected files**: Various Vue components
- **Component**: Code style

## Acceptance Criteria

- [ ] Naming convention documented in CONTRIBUTING.md
- [ ] ESLint/vue-component-name-style enabled
- [ ] All components follow consistent naming

## Work Log

- 2026-03-30: Created issue during mango-web code review

## Resources

- [Vue Style Guide](https://vuejs.org/style-guide/)
