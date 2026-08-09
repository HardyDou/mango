---
name: mango-issue-handle
description: Handle an existing Mango GitHub Issue identified by number or URL by reading its current state, reproducing and attributing it, then routing to clarification, fix, validation, PR, or closure. Do not use to register a newly discovered Issue, fix an untracked generic defect, create a feature, or review a PR.
---

# Mango Existing Issue Handler

## Resolve And Load

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`.

1. Require an Issue number or URL. With no identifier, return `ASK` and do nothing else.
2. Read the Issue title, body, labels, state, comments, linked PRs, and latest relevant commits before running preflight or changing files.
3. Read `$PMO_ROOT/rules/07-mango-issue-runbook.md`, `$PMO_ROOT/rules/00-dev-flow.md`, and `$PMO_ROOT/rules/05-ai-delivery-quality.md`.
4. If a repository change or formal conclusion is required, run PMO preflight with the role, phase, task, and paths established from the Issue; consult individual references only when attribution or scope remains unresolved.

## Execute

1. Follow the runbook to classify current state, reproduce or document missing conditions, compare the required baseline, establish attribution, and choose one supported disposition.
2. Use `STOP` when evidence, environment, authorization, worktree policy, or an upstream decision blocks the selected disposition.
3. Use `ASK` for one concrete missing fact. Never infer Issue state or claim a fix from memory.
4. When fixing, route implementation through `$mango-defect-fix` or the governed document chain as appropriate; this Skill does not bypass their gates.
5. Return `NEXT` only with the selected disposition, evidence, affected artifacts, and required follow-up recorded according to the runbook.

If the Issue is being handled from an existing non-`main` task worktree, reuse that worktree for the requested fix. Do not create a second worktree unless the user explicitly changes the workspace strategy after confirmation.

Never close an Issue merely because a related commit exists; verify the current behavior first.
