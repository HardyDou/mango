---
name: mango-ask
distribution: project
description: Classify an unclear Mango request and route it to the smallest next Skill without guessing the domain, risk, or implementation scope. Use when the user is unsure what to ask for, mixes explanation with change, or uses an ambiguous term; do not implement or invent missing facts.
---

# Mango Ask

## Purpose

Use this Skill as the short front door when the user does not yet know whether they need an explanation, a document, a design, implementation, verification, review, or release. It reduces routing questions to one useful classification instead of sending the user through the full PMO process.

## Resolve current facts

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, return `STOP`. Never reconstruct the route from memory.

Read only the current Skill metadata and the specific source needed to classify the request. Load `$PMO_ROOT/rules/00-dev-flow.md` and `$PMO_ROOT/rules/11-delivery-assurance.md` when the request could change versioned files or delivery mode. Load `$PMO_ROOT/rules/08-capability-docs.md` when the request names a capability or ambiguous domain term. When the user asks “这条规范我该怎么执行”, render a point-of-use card with `node "$PMO_ROOT/tools/render-rule-action-card.mjs" --id <rule-id>`; the card is a view of the canonical rule, not a second rule source.

## Classify

1. Restate the user's goal in their words. Do not convert “我看不懂” into an implementation request.
2. Classify the smallest next action:
   - **Explain**: the user needs behavior, terms, a basic usage path, or document focus clarified. Route to `$mango-explain`.
   - **Requirements**: the user has business facts and explicitly wants BRD/SRS content. Route to the matching requirements Skill.
   - **Design**: the user has confirmed requirements and asks for architecture, API, data, UI, or technical decisions. Route to `$mango-design-technical` or `$mango-design-delivery-assurance` as applicable.
   - **Implement**: the scope and approved source are known and the user asks to change code or configuration. Route to `$mango-engineering`.
   - **Verify / Review / Retro**: route to `$mango-qa-verification`, `$mango-review-pr`, or `$mango-retro` only when the artifact and evidence boundary are explicit.
   - **Release**: route to `$mango-release` only for an actual Mango source-repository platform release; otherwise use the consuming repository's delivery process.
3. If the request contains both “explain” and “change”, explain first and defer mutation. Do not write code or a formal document before confirmation.
4. If one missing fact changes the route, ask exactly one short classification question. Prefer a concrete noun, for example: “你要先看上传预览怎么工作，还是已经确认方案、要我修改代码？”
5. Do not ask a questionnaire. Once the route is clear, return the next Skill and the one fact it will need.

## Ambiguous terms

A bare “workflow”, “权限”, “同步” or similar word is not enough to choose a specialized capability. Do not load every related rule. Ask one question only when the domain changes the next action; for “workflow”, distinguish Mango approval (`@mango/workflow`, `mango-workflow-api`, `/workflow/`, process definition, approve/reject/task actions) from CI/CD, PMO flow, generic state machines, and agent workflows.

## Text and decision boundary

- Follow `mango-pmo/rules/13-agent-text-output.md`: put the smallest useful classification first and avoid a questionnaire.
- If classification becomes a scope, design, implementation, verification, Issue, PR, or release decision, follow `mango-pmo/rules/14-decision-expert-review.md`; do not present an unreviewed recommendation as final.

## Actions

- `ASK`: the user's goal or domain is genuinely ambiguous.
- `NEXT`: the smallest next Skill is clear and the required source or artifact is identified.
- `STOP`: the request asks for an unsupported shortcut, an invented fact, or a mutation before explanation/approval.

This Skill is read-only. It does not modify code, configuration, rules, product documents, delivery records, or release state.

## Required response shape

```markdown
## 你现在要完成什么

## 最小分类

- 类型：Explain / Requirements / Design / Implement / Verify / Review / Retro / Release
- 下一步：`$skill-name`
- 依据：用户原话中的具体目标

## 还缺一个事实（仅在需要时）

## 下一步会读取或验证什么
```

With empty context, return `ASK` for the user's goal, target capability or document, and whether they want explanation, change, verification, review, retro, or release. Do not guess a delivery mode.
