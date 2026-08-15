---
name: mango-submit-pr
description: Submit a task pull request from either the Mango source repository or a Mango business repository. Use for the explicitly authorized local commit, task-branch push, PR creation or update, and remote readback sequence after implementation checks pass. Do not use to implement, review, approve, merge, release Mango components, or deploy a business application.
---

# Mango PR Submission

## Resolve And Load

Set `PMO_ROOT` to the first existing directory: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Read `$PMO_ROOT/rules/00-dev-flow.md`, `$PMO_ROOT/rules/05-ai-delivery-quality.md`, `$PMO_ROOT/rules/08-capability-docs.md`, and `$PMO_ROOT/rules/12-pr-submission.md`.

Run PMO preflight with role `dev`, phase `verify`, the actual task, and every changed path. Read every returned `Must read` file. Read `.github/branch-protection-policy.json`, the PR template, the resolved delivery baseline, and the task's validation evidence when present.

## Classify Authorization

1. “提交 PR” or “创建 PR” authorizes the necessary local task Commit, Push of the current task branch, and creation or update of its PR as one operation.
2. “提交代码” authorizes only the local Commit. “Push” authorizes only the named current task branch Push.
3. Never infer merge, approval, force push, registry publication, Tag, GitHub Release, business deployment, traffic change, or rollback authorization.
4. Return `ASK` when repository, task scope, base, head, remote, or authorization is materially ambiguous.

## Prepare The Exact Change

1. Prove the current directory, Git root, task worktree, branch, target base, remote repository and linked Issue. Stop on `main`, the primary worktree, a detached head, or a branch belonging to another task.
2. Inspect tracked, untracked, staged and unstaged files. Attribute every submitted file to the current task and preserve unrelated user changes. Never use `git add -A`, `git add .`, an unresolved glob, or an all-repository staging shortcut.
3. Resolve every applicable required Runner check to its repository-provided local entry and run the same checker, configuration, and lock inputs before submission. Treat a claimed pass without commands, environment versions, and current-head evidence as unverified. Do not use Push and Runner feedback as the development loop.
4. Reject credentials, tokens, passwords, private keys, local runtime files, dependency caches, build products and unrelated generated files before staging.
5. Stage explicit paths, inspect `git diff --cached`, and create a scoped task Commit. Fetch the remote base, merge it non-destructively into the task branch when behind, resolve conflicts in the same task worktree, then rerun every applicable local Runner-equivalent entry on the final head. Push is forbidden until the recorded final-head suite is fully green. Do not rewrite a published branch without exact authorization.

## Submit And Read Back

1. Push only the current task branch to the confirmed remote without force.
2. Reuse an existing PR with the same base/head; otherwise create one from the repository template. Fill scope, risk, capability docs, validation, exceptions and linked Issue with actual evidence and no placeholders.
3. For a Mango Release PR, accept the machine plan and prepare evidence from repository-local `$mango-release`, but keep Commit, Push and PR creation here. A Changeset in an ordinary Mango PR does not activate `$mango-release`.
4. Read back remote head SHA and require it to equal local HEAD. Read back PR URL, number, repository, base, head, head SHA, changed files and check state.
5. Report `SUBMITTED_CHECKS_PENDING` while required checks run, `SUBMITTED_CHECKS_FAILED` on failure, or `SUBMITTED` when the PR and remote head are confirmed. Never translate submission into reviewed, mergeable, merged, released or deployed.

If a Runner exposes a deterministic failure that the local entry did not reproduce, return to the same task worktree, add or repair the local reproduction, and rerun the full affected local suite before another Push. Do not drip-feed speculative commits, blindly rerun the Runner, or weaken the remote check.

## Stop Conditions

`STOP` on unexplained changes, failed or missing required local checks, a required Runner check without a local equivalent, base conflicts, sensitive files, remote mismatch, failed Push, incorrect PR base/head, stale remote head, or an attempt to combine PR submission with merge/release/deployment.

With an empty context, return `ASK` for the repository/task, target base, and exact requested submission action. Never guess a branch or stage the whole repository.
