---
name: mango-retro
distribution: project
description: Produce an evidence-based Mango delivery retrospective after implementation, verification, review, or release. Use to capture what helped, what confused people, which gates or rules failed, and one or two owned follow-up actions; do not modify product code or rewrite history.
---

# Mango Retro

## Purpose

Turn one completed delivery into a small, actionable learning record. A retro is not a second design document and not a blame report. It should explain where understanding, routing, implementation, verification, review, or release lost time and what will change next time.

## Resolve and load

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, return `STOP`. Read `$PMO_ROOT/rules/06-document-assets.md`, `$PMO_ROOT/rules/11-delivery-assurance.md`, and `$PMO_ROOT/templates/retrospective.md`.

Read the actual delivery evidence before writing:

- the approved design or plan, when applicable;
- the delivery record and acceptance evidence;
- review findings and resolved conversations, when a PR was involved;
- exact verification commands and results;
- the comprehension or clarification evidence when the task used `$mango-explain` or `$mango-ask`.

## Execute

1. Identify one delivery, its tested commit/version, owner, and evidence paths. If the delivery cannot be identified, return `ASK`.
2. Separate observed facts from interpretation. Every claim about a failure, delay, gate, or comprehension gap must cite a path, command, review item, or dated record.
3. Record at most three items in each of: **keep**, **change**, and **stop**. Prefer one concrete example over a long narrative.
4. Include a short understanding/routing check when the task involved unfamiliar terms, a document, or a cross-module flow: what users misunderstood, which source clarified it, and whether the first source entry was correct.
5. Create or update only the retrospective record under `mango-docs/evidence/retrospectives/`; do not modify product code, formal requirements, design, plan, test results, or approval state.
6. Validate the record with `node "$PMO_ROOT/tools/check-retrospective.mjs" --document <path>`.
7. For every follow-up, name an owner, target path or Skill, completion condition, and due milestone. Do not create a new permanent rule from one anecdote; route a repeated issue to PMO governance for separate review.

## Text and decision boundary

- Follow `mango-pmo/rules/13-agent-text-output.md`: put evidence-backed conclusions first and keep facts separate from proposals.
- If a follow-up becomes a scope, design, implementation, verification, Issue, PR, or release decision, follow `mango-pmo/rules/14-decision-expert-review.md` before presenting it as final.

## Actions

- `ASK`: delivery identity or evidence boundary is missing.
- `WRITE`: the record is being drafted or has an unverified follow-up.
- `NEXT`: the retrospective checker passes, facts and evidence are present, and follow-ups are explicitly owned.
- `STOP`: the request would rewrite prior results, fabricate evidence, or modify product artifacts during the retro.

## Required response shape

```markdown
## 这次交付

## 保留

## 改变

## 停止

## 理解与路由复盘

## 后续动作

## 证据
```

A retro is complete only when it makes the next delivery easier to understand or safer to execute. It is not a release approval and it never substitutes for QA, PR review, lifecycle handoff, or human approval.
