---
name: mango-issue-register
description: Register a newly discovered Mango framework, starter, CLI, template, frontend package, release-material defect, or improvement as a GitHub Issue with evidence and task back-reference. Do not use to process an existing Issue number, repair a generic product defect, create a feature document, or review a PR.
---

# Mango New Issue Registration

## Resolve And Load

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`.

Read `$PMO_ROOT/rules/07-mango-issue-runbook.md` and `$PMO_ROOT/rules/05-ai-delivery-quality.md`. Run PMO preflight only if the task also requires repository changes or a formal delivery conclusion.

## Execute

1. Confirm the suspected problem belongs to Mango rather than the current business implementation, using the baseline and evidence required by the runbook.
2. Return `ASK` when attribution, version, reproduction, evidence, expected behavior, impact, or current business task reference is missing.
3. Return `STOP` when sensitive data cannot be removed, repository authorization is unavailable, or attribution shows the problem belongs to the current task and must be fixed there.
4. Create the Issue with the canonical repository, title, priority, label, and body fields defined by the runbook.
5. Write the created Issue URL and blocking state back to the current business task record.
6. Return `NEXT` only with the created URL and back-reference evidence. Do not implement the Issue unless the user starts a separate handling task.

When already in a non-`main` task worktree and the user explicitly asks to solve the current problem, do not create another worktree and do not replace the fix with Issue registration. Register only when the user chooses registration or the runbook proves the problem belongs outside the current task; if that choice is unclear, return `ASK`.

With an empty context, return `ASK` for the observed behavior and evidence; do not create a speculative Issue.
