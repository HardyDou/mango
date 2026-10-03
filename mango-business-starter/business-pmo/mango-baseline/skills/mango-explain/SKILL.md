---
name: mango-explain
distribution: project
description: Explain Mango capabilities, Agent decisions, documents, and source flows in user language before design or implementation. Use for explicit explanation requests, unfamiliar terms, unclear PRD emphasis, or a request to understand a feature; do not modify code or create formal design during explanation.
---

# Mango Explain

## Purpose

Use this Skill to establish a shared language between the user and Mango before the Agent proposes design, code, or a formal delivery document.

The goal is not to make an answer shorter or add another permanent rule. The goal is that the user can explain the current behavior in their own words and find the next relevant file.

## Trigger boundary

Use this Skill when the user:

- says “解释一下”“我看不懂”“这是什么”“重点是什么” or equivalent;
- asks why a Mango capability, PRD, design, rule, or Agent answer is hard to understand;
- asks for the basic usage of a Mango capability;
- encounters an unfamiliar term, cross-module flow, or ambiguous word before choosing a design or implementation path;
- asks to compare two concepts that look similar, such as department removal versus tenant-member removal.

Do not use it as the primary Skill when the user has already confirmed the meaning and asks to:

- implement an approved design;
- run verification or review;
- submit or release a change;
- apply a clear coding or documentation fix without an explanation request.

For a bare ambiguous word such as “workflow”, keep the current task domain. Ask one short classification question only when classification changes the next action; do not assume Mango approval workflow.

## Resolve current facts

Set `PMO_ROOT` to the first available source: `<repo>/business-pmo/mango-baseline`, `<repo>/mango-pmo`, or `<plugin-root>/dist/baseline`. If none exists, `STOP`.

Before explaining a Mango capability, read only the current facts needed for the question:

1. `$PMO_ROOT/rules/08-capability-docs.md` for source priority and capability boundaries;
2. `$PMO_ROOT/rules/06-document-assets.md` when the question is about document type or location;
3. `$PMO_ROOT/rules/00-dev-flow.md` when the explanation concerns a delivery step;
4. `mango-docs/capabilities/README.md` for the current capability entry;
5. the relevant current module README or developer guide;
6. the exact source, test, or checker needed to prove a disputed fact.

Current README and capability-map facts outrank remembered context, historical designs, old migrations, and previous Agent messages.

## Execute

1. Restate the user's concrete problem in their words. Do not rename it into a new domain term.
2. Select one real scenario that demonstrates the behavior end to end.
3. Identify only the new words needed for that scenario. For each word, record its plain meaning, current evidence, excluded meaning, and a possible user-approved replacement.
4. Separate `事实`, `推断`, and `待确认决策`.
5. Explain the trigger, input, owner, data/state change, output, and failure behavior.
6. Cite current evidence as `path:line-range` and section heading. Use `rg -n` or `nl -ba` when line evidence is needed; never invent line numbers. For a rule-execution question, use `node "$PMO_ROOT/tools/render-rule-action-card.mjs" --id <rule-id>` to show a concise view of the current rule before the full source.
7. Show a small text flow only after the scenario and terms are clear.
8. State which files the user should read or change next and which details can be ignored for now.
9. Ask for confirmation of the meaning and terminology before routing to design, implementation, or a formal document Skill.

During this Skill:

- follow `mango-pmo/rules/13-agent-text-output.md`: put the user's problem and the next useful fact first, use stable terms, and state failure boundaries;
- if the explanation recommends a scope, design, implementation, verification, Issue, PR, or release decision, follow `mango-pmo/rules/14-decision-expert-review.md` before presenting it as a final decision;
- do not modify code, configuration, rules, or product documents;
- do not create a formal BRD, SRS, TDD, Plan, or delivery record;
- do not present an unverified guess as an architecture decision;
- do not create a second permanent glossary; persist only user-confirmed, reusable terms in the appropriate capability documentation;
- do not expand a local question into a full-repository explanation.

## Required response shape

```markdown
## 你要解决的问题

## 先看一个具体场景

## 本次出现的新词

| 词 | 直白含义 | 当前证据 | 不代表什么 | 可替代名称 |
|---|---|---|---|---|

## 谁负责什么

## 输入、状态变化和输出

## 失败时怎么办

## 再看最小结构图

## 你真正需要看或改哪里

## 暂时不要关心的内容

## 请确认
```

Use at most one primary scenario and a small number of secondary examples. Put the answer to “what should I do next?” before optional reference details.

## Comprehension gate

Before returning `NEXT`, the user or evaluator must be able to answer:

1. What problem does this solve?
2. What triggers it?
3. What is the input?
4. Which concrete participant owns the behavior in this scenario?
5. What exact state, data, or message changes?
6. What happens when it fails, and where is the next source file?

Write questions with the nouns from the scenario, not abstract terms such as “which layer owns it?”. For example, ask “who supplies `OrderCode.ORDER_NOT_FOUND` in this call?” and “which message is shown first?” instead of asking the user to recite a responsibility matrix. If the user cannot answer a question, do not count it as a pass: explain only that missing point with the same scenario and ask again.

If the target, current facts, or user intent is missing, return `ASK`. If the user asks to explain and modify in the same turn, explain first and defer mutation until the user confirms.

Return `NEXT` only after confirmation and route to the smallest applicable next Skill, such as `$mango-design-technical`, `$mango-engineering`, `$mango-qa-verification`, or a documentation task. The explanation itself is never evidence that implementation or acceptance passed.

With an empty context, return `ASK` for the target capability or document, the user's concrete question, and the current repository or version if it affects the answer.
