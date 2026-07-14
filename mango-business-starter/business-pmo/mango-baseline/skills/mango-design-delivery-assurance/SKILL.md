---
name: mango-design-delivery-assurance
description: Design and obtain human confirmation for a Mango task's delivery-assurance measures from the fixed v1 catalog. Use before work that may change version-controlled files, databases, product documents, governance, capability guidance, verification, external platform state, or human acceptance, and use again when the goal, scope, facts, or confirmed measure set materially changes. Excludes release and publishing work.
---

# Mango Delivery Assurance

## Resolve Sources

Set `PMO_ROOT` to the first existing directory:

1. `<repo>/business-pmo/mango-baseline` in a Mango business project.
2. `<repo>/mango-pmo` in the Mango source repository.
3. `<plugin-root>/dist/baseline` in an installed `@mango/pmo` package.

If none exists, `STOP` and ask for PMO installation or synchronization. Never reconstruct the catalog from memory.

Read `$PMO_ROOT/rules/11-delivery-assurance.md` and `$PMO_ROOT/contracts/delivery-assurance.json`. Run PMO preflight for the actual role, phase, task, and affected paths, then read every `Must read` file it returns.

## Keep Release Out

Do not recommend, ask about, authorize, execute, or verify publishing, version bumps, registries, release batches, tags, or release recovery through this Skill. Route release work to `$mango-release` and the release rules. A task that contains both implementation and release work must confirm only the non-release measures here; keep release authorization separate.

## Build The Recommendation

1. Establish the actual goal, success conditions, scope, exclusions, affected paths and systems, existing artifacts, external state, and material unknowns from repository evidence and user input.
2. Trigger measures only for facts the current task will change, decide, or prove. A verification-only task for an already deployed change does not retroactively trigger BRD, SRS, or TDD. A simple list/status/log view with no configuration or delivery conclusion does not trigger this Skill; M15 applies only when external readback is evidence for the current configuration or goal.
3. Evaluate exactly `M01` through `M16` from the loaded contract. Do not add, rename, merge, split, or infer a seventeenth measure.
4. Mark a measure as triggered only when its contract `triggerFact` is present in the current task facts. Do not put untriggered measures in the questionnaire.
   - For M01, implementation verbs such as create, change, fix, refactor, or update repository content are sufficient tracked-file facts unless the request explicitly says read-only or no file write. Recommend `CREATE` when the current workspace is main/primary and no reusable task worktree exists; recommend `DO_NOT_CREATE` when the same task is already isolated. If workspace facts are missing, inspect them or ask instead of guessing the value.
   - For M02, recommend `REBUILD` for disposable empty-database formation/startup proof and `DO_NOT_REBUILD` for production-history or compatibility acceptance that cannot clear data. Evaluate M11 separately; database acceptance does not merge the two measures.
   - For M11, a refactor that changes dependency direction or collaboration across modules still counts as a module-integration fact even when the promised external behavior is unchanged; static dependency checks cover M09, but do not by themselves prove runtime assembly.
5. For each triggered measure, recommend one of its exact `allowedValues`. Explain:
   - the facts that triggered it;
   - the goal or failure mode it helps protect;
   - the value, cost, and execution consequence;
   - the residual risk of the non-recommended value.
6. Treat risk level as context for recommendation strength, not as a document or verification package selector. Do not select BRD, SRS, TDD, Plan, or all verification types merely because a task is L2/L3.
7. Reuse existing valid artifacts when they already provide the selected assurance. Enabling a document measure does not require creating a duplicate document.

## Ask The User Natively

Use the host Agent's native structured user-input capability. In Codex, call `request_user_input` when it is available; in hosts that expose `AskUserQuestion`, use it. Do not replace a native prompt with a prose-only confirmation.

- Ask about one measure per question and no more than three measures per tool call.
- Omit `autoResolutionMs` or any default timeout; explicit confirmation is required.
- Put the recommended exact value first and suffix its label with `(Recommended)`.
- Offer only the measure's exact allowed values. The host may add a free-form alternative; do not add another `Other` option.
- State the measure's value and the residual risk of declining it in the question or option descriptions.
- Ask prerequisite choices first: `M01`, then `M02`. Recompute the remaining triggered measures after every batch, because an answer can change the facts or implementation boundary.
- Then ask triggered document and guidance measures `M03` through `M08`, followed by triggered verification and review measures `M09` through `M16`.
- Do not repeat a measure already explicitly decided by the user for the same unchanged goal and scope; record that decision in the baseline instead.

`M01` accepts only `CREATE` or `DO_NOT_CREATE`. `M02` accepts only `REBUILD` or `DO_NOT_REBUILD`. Every other measure accepts only `ENABLE` or `DISABLE`.

If a free-form answer proposes a substitute, map it to an execution variant of the same measure or to another existing catalog measure. Record the original measure's exact value and the substitute detail. If it would create a genuinely new measure, do not invent an ID; ask for a separate catalog-governance change.

## Record The Confirmation Baseline

After all triggered measures are answered, record one confirmation baseline in the current conversation, PR body, task record, or ignored runtime state. Do not create a version-controlled document solely to store the baseline unless the user selected a document measure or explicitly requests one.

Use this shape:

```json
{
  "contractId": "delivery-assurance",
  "schemaRevision": 1,
  "goal": "...",
  "scope": ["..."],
  "factEvidence": ["..."],
  "triggeredMeasures": ["M01"],
  "selections": {"M01": "CREATE"},
  "recommendations": {"M01": "CREATE"},
  "decisionEvidence": {"M01": "native Ask User response"},
  "acceptedResidualRisks": [],
  "substitutions": [],
  "baselineId": "sha256 of the normalized goal, scope, facts, and selections",
  "status": "CONFIRMED"
}
```

The user's confirmed values override the AI recommendation. Explain the resulting residual risk without silently re-enabling a declined measure. System or platform safety restrictions that are not user-waivable remain outside this catalog.

## Execute And Reconfirm

Route enabled document, engineering, QA, expert-review, external-readback, and acceptance work to the matching specialized Skill or Agent. This Skill designs and confirms the combination; it does not draft every selected artifact or claim that a selected check passed.

Re-run fact detection and ask only the affected measure questions when any of these materially changes:

- the goal, success condition, scope, or affected system;
- the actual diff crosses the confirmed path or behavior boundary;
- a selected measure cannot be executed or must be replaced with materially different assurance;
- database, external-state, document, verification, expert-review, or human-acceptance facts newly appear or disappear;
- the cost, authority, environment impact, or accepted residual risk materially changes.

Do not reconfirm command syntax, equivalent implementation details, read-only diagnostics, or a retry of the same confirmed measure. Preserve the unaffected selections and record a new baseline linked to the previous one.

With insufficient facts to identify the goal or triggered measures, return `ASK`; do not present all sixteen measures as a fallback questionnaire.
