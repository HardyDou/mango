---
name: mango-design-delivery-assurance
description: Resolve a Mango task's workspace policy, L0-L3 risk, SIMPLE/STANDARD/FULL delivery mode, artifacts, and observable verification capabilities before implementation or when material facts change. Excludes release and publishing work.
---

# Mango Delivery Mode

## Resolve Sources

Set `PMO_ROOT` to the first existing directory: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`. Read `$PMO_ROOT/rules/11-delivery-assurance.md` and `$PMO_ROOT/contracts/delivery-assurance.json`, run PMO preflight for the actual task and paths, then read every `Must read` file.

## Keep Release Separate

Do not authorize or execute publishing, versions, registries, tags, releases, or release recovery. Route those actions to `$mango-release`.

## Resolve Workspace

1. Read-only inspection and pure fast-forward synchronization use `NO_WORKTREE`.
2. Reuse an existing non-main worktree for the same task and record `M01=REUSE`.
3. On main or the primary worktree, a tracked-file change records `M01=CREATE` and creates an external sibling task worktree without asking the user.
4. Never create a second worktree for the same task.
5. Use `M01=MAIN_EXCEPTION` only after the user explicitly requests main/primary worktree changes and confirms the warned pollution, parallel-conflict, and accidental-commit risks.

## Select The Delivery Mode

1. Establish the actual goal, success conditions, scope, exclusions, affected paths, observable behavior, data/security boundaries, coupling, rollback, existing artifacts, and material unknowns.
2. Assess requirement impact and solution risk separately. Final risk is their maximum.
3. Apply the contract mapping exactly: `L0/L1=SIMPLE`, `L2=STANDARD`, `L3=FULL`.
4. `SIMPLE`: implement directly, create no delivery document, and choose only minimum sufficient verification for the observable result.
5. `STANDARD`: create or update one file from `$PMO_ROOT/templates/standard-delivery-record.md`; do not create separate BRD/SRS/TDD/Plan files.
6. `FULL`: route the applicable complete lifecycle. Product changes use the applicable BRD/SRS/TDD/Plan chain; governance, release, migration, or other specialist work uses its complete dedicated record without fabricated product documents.
7. A user may raise the mode. Lowering below the default requires explicit confirmation and residual-risk evidence; security, tenant, money, destructive data, and irreversible release facts cannot be waived.

## Resolve Execution Capabilities

Treat M01-M16 as execution capabilities, not a questionnaire. M03-M06 follow the delivery mode. Activate M07-M16 only when their contract facts are present and they can protect or observe the actual goal. An L3 backend-only task does not activate UI verification. M02 requires human confirmation before destructive database work.

Ask only for a material unknown, main exception, downward mode override, destructive database action, external write authorization, or material scope/risk change. Do not ask users to approve a fixed M01-M16 list.

## Record The Baseline

Record goal, scope, requirement impact, solution risk, final risk, delivery mode, workspace decision, fact evidence, active capabilities, artifact paths, verification evidence, exceptions, and residual risks. Use `RESOLVED` when policy and facts determine the baseline; record human evidence only for actual exceptions.

## Re-evaluate

Recalculate the affected portion when scope, solution coupling, failure impact, rollback, database/external state, or observable acceptance changes. Escalate `SIMPLE -> STANDARD -> FULL` immediately when new facts require it. Preserve still-valid evidence and do not reconfirm equivalent implementation details or a retry.

With insufficient facts to determine the goal or risk, return `ASK` for the missing fact. Never fall back to a sixteen-item questionnaire.
