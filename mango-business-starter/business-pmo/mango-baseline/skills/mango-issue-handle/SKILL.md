---
name: mango-issue-handle
description: Handle an existing Mango Issue on GitHub or Gitea identified by number or URL by reading its current state, reproducing and attributing it, routing to clarification, fix, validation, or PR, and publishing verifiable acceptance evidence before resolution or closure. Do not use to register a newly discovered Issue, fix an untracked generic defect, create a feature, or review a PR.
---

# Mango Existing Issue Handler

## Resolve And Load

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`.

1. Require an Issue number or URL. With no identifier, return `ASK` and do nothing else.
2. Resolve the actual hosting platform from the Issue URL or repository remote. Prefer `gh` for GitHub and `tea` for Gitea. Treat these CLIs as suggested platform interfaces, not mandatory tools; use another authorized platform interface when the preferred CLI is unavailable or cannot perform the required attachment or interaction.
3. Read the Issue title, body, labels, state, comments, linked PRs, and latest relevant commits before running preflight or changing files.
4. Read `$PMO_ROOT/rules/07-mango-issue-runbook.md`, `$PMO_ROOT/rules/00-dev-flow.md`, and `$PMO_ROOT/rules/05-ai-delivery-quality.md`.
5. If a repository change or formal conclusion is required, run PMO preflight with the role, phase, task, and paths established from the Issue; consult individual references only when attribution or scope remains unresolved.

## Execute

1. Follow the runbook to classify current state, reproduce or document missing conditions, compare the required baseline, establish attribution, and choose one supported disposition.
2. Use `STOP` when evidence, environment, authorization, worktree policy, or an upstream decision blocks the selected disposition.
3. Use `ASK` for one concrete missing fact. Never infer Issue state or claim a fix from memory.
4. When fixing, route implementation through `$mango-defect-fix` or the governed document chain as appropriate; this Skill does not bypass their requirements.
5. Complete evidence capture, upload, Issue commenting, and state update as the normal completion stage of the same fix task. Decide evidence dynamically from the acceptance target and observed results; do not impose a fixed sequence, tool, template, or evidence bundle. Revise the evidence choice as reproduction and regression reveal what best proves the result.
6. Require evidence to be relevant, sufficient for independent verification, accessible to reviewers, and no more elaborate than the Issue needs. When the Issue has a corresponding page or page interaction, include before/after page screenshots and a video of the key verification path. When it has no corresponding page, explain that page media is not applicable and select the process evidence that most directly proves the result; do not manufacture meaningless screenshots or video.
7. Publish an acceptance-evidence comment that records the evidence decision and enough context, artifacts, results, and risks for another person to verify the fix. Adapt its fields and order to the Issue instead of mechanically copying a template.
8. Re-read the Issue comments and verify every attachment or stable link is accessible. Local paths, pending uploads, inaccessible links, a related commit, or a passing test without the Issue evidence comment mean the completion stage is still unfinished.
9. When evidence is missing, incomplete, unsafe, unpublished, or inaccessible, continue creating, correcting, redacting, uploading, or verifying it. Return `STOP` only when an environment, authorization, or external-system blocker prevents further progress; do not stop merely because the evidence has not been created yet. Keep the Issue open while blocked.
10. When the verified evidence comment exists and its conclusion is `PASS`, continue the same workflow by closing or marking the Issue resolved. Re-read the Issue after the state change to confirm the final state.
11. Return `NEXT` only with the selected disposition, evidence-comment URL, verified attachments or links, affected artifacts, final Issue state, and required follow-up recorded according to the runbook.

If the Issue is being handled from an existing non-`main` task worktree, reuse that worktree for the requested fix. Do not create a second worktree unless the user explicitly changes the workspace strategy after confirmation.

Never close an Issue merely because a related commit exists or validation passed locally; verify the current behavior and the published Issue evidence first.
