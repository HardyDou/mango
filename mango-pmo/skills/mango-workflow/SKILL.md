---
name: mango-workflow
description: Use only for explicit Mango approval-flow work evidenced by @mango/workflow, mango-workflow-api or mango-workflow-starter, /workflow APIs, workflow definitions, designerJson/formJson, process publish/start, task approve/reject/claim, business approval components, events, or workflow data permissions. Do not use for frontend standards, lint/typecheck or directory governance, CI/GitHub Actions workflows, PMO/development/release processes, generic state machines or orchestration, or a bare ambiguous “workflow/工作流”.
---

# Mango Approval Workflow

## Classify Before Loading

`Mango Workflow` in this Skill means Mango's approval-flow capability. It does not mean every use of “workflow” as an English word.

Invoke this Skill implicitly only when at least one explicit domain signal exists:

- package/module coordinates: `@mango/workflow`, `mango-workflow-api`, `mango-workflow-starter`, or another exact `mango-workflow-*` source module;
- approval contracts: `/workflow/` APIs, `WorkflowProcessApi`, `WorkflowTaskApi`, `WorkflowBusinessApplyApi`, or workflow events/data permissions;
- approval definition models: `designerJson`, `formJson`, definition publish/deploy, node catalog, approval form configuration;
- approval actions or UI: apply/start, approve/complete, reject, claim, todo tasks, workflow definition designer, business approval page.

Do not invoke this Skill for:

- Vue/frontend code standards, ESLint, Prettier, Stylelint, typecheck, tests, directory or monorepo governance;
- GitHub Actions, CI/CD, build, release, deployment, automation, or job workflows;
- PMO, requirements, design, development, verification, release, review, or documentation processes;
- generic state machines, business status transitions, orchestration, data pipelines, agent workflows, or product UX flows;
- a bare “workflow/工作流/流程” with no Mango approval evidence.

When the user explicitly names `$mango-workflow` but supplies no objective or evidence, return `ASK` for the exact package/module/API/page and intended approval behavior. When only an ambiguous word appears, stay in the current task domain; ask one short classification question only if progress truly depends on it. Never introduce `@mango/workflow` into an unrelated proposal merely to explain that it is unrelated.

## Resolve And Load

Set `PMO_ROOT` to the first available source: the installed business baseline, Mango source PMO, or `<plugin-root>/dist/baseline`. Read `$PMO_ROOT/rules/08-capability-docs.md`.

For implementation or diagnosis, locate the current Mango source root from the active repository/worktree. Read only the relevant current facts:

- backend capability: `mango/mango-platform/mango-workflow/README.md`;
- business integration: `mango-docs/guides/business-integration/workflow-business-approval.md`;
- frontend package: `mango-ui/packages/workflow/README.md`;
- business component example: `mango-ui/packages/workflow-business-example/README.md`;
- exact API/model/E2E sources identified in `references/source-map.md`.

Do not use hardcoded user paths, decompile installed packages when source is available, or invent workflow schemas. Run PMO preflight before changing version-controlled files and read every returned `Must read` file.

## Execute

1. Confirm the task is Mango approval-flow work using an explicit signal above.
2. Confirm the public API, model, UI registration, permission and tenant behavior from current source.
3. Keep business status, permission checks, snapshots, idempotency and event handling owned by the business module. Mango approval flow does not replace a business state machine.
4. Business code depends on `mango-workflow-api`; only a runtime host includes `mango-workflow-starter`.
5. Use public definition, apply, process and task APIs. Do not seed operational definitions in business code unless an approved design explicitly requires a built-in definition.
6. Validate the published definition, process instance, task visibility/action, business progress, permissions and browser evidence appropriate to the change.

Return `STOP` when asked to claim completion without current source evidence, bypass public contracts, or replace the business state machine with workflow runtime state. Return `NEXT` only when the requested integration or diagnosis has current source mapping, validation evidence and no unresolved boundary.

With an empty context reached by explicitly naming this Skill, return `ASK` for the exact Mango approval objective and affected package/module/API/page.
