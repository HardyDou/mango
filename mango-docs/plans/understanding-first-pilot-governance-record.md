# Understanding-first Pilot Governance Record

## 1. Decision

- Status: `PILOT_READY_WITH_MANUAL_GATE`
- Decision source: Five-seat peer review in the current task conversation; the user confirmed implementation with “开始处理吧”.
- Objective: Reduce semantic misunderstanding between Mango users and Agent responses without adding a second permanent rule source.
- Delivery mode: `FULL`
- Requirement impact: `L2` — changes the way users consume Mango capabilities and PMO Skills.
- Solution risk: `L3` — changes the Mango PMO Skill distribution and its evaluation contract.
- Final risk: `L3`
- Workspace: `CREATE`, external sibling worktree `/Users/hardy/Work/mango-understanding-pilot`.

## 2. Pilot scope

### Included

1. Add the on-demand `$mango-ask` and `$mango-explain` project Skills.
2. Add `$mango-retro` for evidence-based delivery learning without code mutation.
3. Add eight blank-context comprehension cases and a structured comprehension dataset.
4. Add a reader-facing pilot index and quick-use guides for Resource Registry, File upload/preview, and Workflow approval.
5. Add a human reading entry checker for BRD/SRS/TDD/Plan, a Mermaid task-graph renderer for Implementation Plan, and a point-of-use rule action-card renderer derived from the canonical rule.
6. Link the pilot from the Mango documentation and capability-map entry points.
7. Add static Skill-evaluation coverage for trigger, non-trigger, blank context, boundary, gate, and confirmed handoff behavior.

### Excluded

- No repository-wide README rewrite.
- No global permanent glossary.
- No runtime Java, frontend, database, API, permission, tenant, or release behavior change.
- No automatic explanation injection into every conversation.
- No claim that the pilot has passed human comprehension until the dataset is executed by a user/evaluator.
- No claim that a simulated blank Agent is a real expert or human result.

## 3. Active assurance capabilities

| ID | Decision | Evidence target |
|---|---|---|
| M01 | `CREATE` | External task worktree and workspace-layout check |
| M07 | `ENABLE` | This governance record and explicit non-goals |
| M08 | `ENABLE` | Capability map, documentation index, and three quick-use guides |
| M09 | `ENABLE` | Skill/eval schema checks and package checks |
| M14 | `ENABLE` | Peer-review decision, PR review boundary, and retro record |
| M16 | `ENABLE` | Human comprehension score and first-correct-entry observation |
| M02/M10-M13/M15 | `DISABLE` | No database, runtime code, UI behavior, external state, or algorithm change in this pilot |

## 4. Observable requirements

| ID | Requirement | Failure meaning |
|---|---|---|
| COMP-001 | An explanation starts with one concrete scenario and the user's problem. | A structure-only answer is not accepted as understandable. |
| COMP-002 | New terms include plain meaning, current evidence, excluded meaning, and confirmation status. | An unexplained or invented term is a comprehension defect. |
| COMP-003 | The answer identifies owner, input, state/data change, output, failure behavior, and next source file. | The user cannot decide what to inspect or change next. |
| COMP-004 | Explanation mode does not modify repository files or create formal delivery documents. | Any unauthorized mutation is a safety failure. |
| DOC-001 | Each pilot guide gives one basic path, prerequisites, expected result, failure entry, and authoritative README link. | A user must browse unrelated history or source code for the basic use. |
| DOC-002 | New BRD/SRS/TDD/Plan documents start with a filled scenario-first reading entry; Plans expose a task graph command. | A formally valid document still hides its goal and failure boundary in tables. |
| RULE-001 | A user can render a short execution card from the canonical rule without creating a second rule source. | The Agent loads a large unrelated rule set or follows a stale copied checklist. |
| RETRO-001 | A completed delivery can record evidence-backed keep/change/stop items and owned follow-ups without rewriting history. | A retro fabricates evidence, changes product artifacts, or leaves actions ownerless. |
| ROUTE-001 | Ambiguous “workflow” is not routed to Mango approval guidance without explicit evidence. | A generic workflow, CI workflow, or PMO flow is misclassified. |

## 5. Implementation order

1. Add `$mango-ask`, `$mango-explain`, and `$mango-retro` with invocation metadata.
2. Add Skill-evaluation cases and the comprehension dataset.
3. Add the three pilot guides and index.
4. Add scenario-first document templates, their checker, the task graph renderer, the rule action-card renderer, and the retro checker.
5. Link the index, rule-card tool, and current BRD/SRS/TDD/Plan entry points from the documentation site.
6. Sync and validate the business starter PMO baseline.
7. Run static checks, package build/check, documentation build, and inspect the final diff.
8. Execute the human comprehension suite separately; record results as evidence only after actual execution.

## 6. Acceptance mapping

| Requirement | Verification | Evidence |
|---|---|---|
| COMP-001..004 | Eight-case comprehension dataset plus manual six-question scoring | Pending human execution; dataset path is `mango-pmo/tests/skills/comprehension-evals.json` |
| DOC-001 | Manual fresh-reader task using the three guides and source-link inspection | Pending manual execution |
| DOC-002 | Four valid contract fixtures pass the reading-entry checker; Plan task graph renders; document contract tests remain green | `mango-pmo/tools/check-document-reading-entry.mjs`; `mango-pmo/tools/render-implementation-plan-graph.mjs`; `mango-pmo/tests/document-reading-entry.test.mjs`; `mango-pmo/tests/implementation-plan-graph.test.mjs` |
| COMP-005 | All comprehension cases resolve to current regular source files and their cited headings/symbols | `mango-pmo/tools/check-comprehension-sources.mjs`; `mango-pmo/tests/comprehension-sources.test.mjs` |
| COMP-006 | The manual gate has a blind evaluation packet so participants see prompts and sources before the rubric | `mango-pmo/tools/create-comprehension-evaluation-packet.mjs`; `mango-pmo/tests/comprehension-evaluation-packet.test.mjs` |
| COMP-007 | The evaluation result cannot pass mechanically until all eight human records contain six scores, evidence, source entry, and summary | `mango-pmo/tools/check-comprehension-evaluation.mjs`; `mango-pmo/tests/comprehension-evaluation.test.mjs` |
| TEXT-001 | All Agent text outputs use a canonical, always-loaded rule for priority, controlled language, evidence, failure boundaries, and code comments | `mango-pmo/rules/13-agent-text-output.md`; `mango-pmo/tests/agent-text-output-rule.test.mjs` |
| DEC-001 | Material decisions record three independent views, a synthesized option, and peer review; simulated Agent opinions cannot close the gate | `mango-pmo/rules/14-decision-expert-review.md`; `mango-pmo/templates/decision-review.md`; `mango-pmo/tools/check-decision-review.mjs` |
| README-001 | Six representative backend, frontend, and shared-component READMEs use the new scenario-first reading entry without rewriting the whole repository | `mango-pmo/tools/check-readable-module-readmes.mjs`; `mango-pmo/tests/readable-module-readmes.test.mjs` |
| RETRO-001 | Evidence-backed retro checker and read-only Skill boundary tests | `mango-pmo/tools/check-retrospective.mjs`; `mango-pmo/tests/retrospective.test.mjs`; `mango-pmo/skills/mango-retro/SKILL.md` |
| RULE-001 | Canonical rule ID and explicit rule path both render action cards from the current source. | `mango-pmo/tools/render-rule-action-card.mjs`; `mango-pmo/tests/rule-action-card.test.mjs` |
| ROUTE-001 | Existing capability routing rules and new Skill boundary evals | `mango-pmo/tests/skills/evals.json`; `node mango-pmo/tests/skills/check-skill-evals.mjs` |
| Skill package integrity | PMO package build/check and business baseline projection | `node mango-ui/packages/mango-pmo/scripts/build-package.mjs`; `node mango-ui/packages/mango-pmo/scripts/check-package.mjs`; `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check` |

## 7. Verification log

| Check | Result |
|---|---|
| `node mango-pmo/tools/workspace-layout-check.mjs --root .` | PASS |
| `node mango-pmo/tests/skills/check-comprehension-evals.mjs` | PASS — 8 cases |
| `node mango-pmo/tools/check-comprehension-sources.mjs` | PASS — 8 cases and current source evidence |
| `node mango-pmo/tools/check-readable-module-readmes.mjs` | PASS — 6 representative READMEs |
| `node mango-pmo/tests/skills/check-skill-evals.mjs` | PASS — 20 Skills, 168 eval cases |
| `node --test mango-pmo/tests/document-contract/*.test.mjs mango-pmo/tests/document-reading-entry.test.mjs mango-pmo/tests/implementation-plan-graph.test.mjs mango-pmo/tests/retrospective.test.mjs mango-pmo/tests/rule-action-card.test.mjs mango-pmo/tests/comprehension-sources.test.mjs mango-pmo/tests/comprehension-evaluation-packet.test.mjs mango-pmo/tests/comprehension-evaluation.test.mjs mango-pmo/tests/decision-review.test.mjs mango-pmo/tests/agent-text-output-rule.test.mjs mango-pmo/tests/readable-module-readmes.test.mjs` | PASS — 67 tests |
| `node --test mango-pmo/tests/*.test.mjs` | PASS — 143 tests |
| `node mango-ui/packages/mango-pmo/scripts/build-package.mjs` | PASS — 164 managed files |
| `node mango-ui/packages/mango-pmo/scripts/check-package.mjs` | PASS |
| `node mango-business-starter/scripts/sync-pmo-baseline.mjs --check` | PASS |
| `corepack pnpm@11.14.0 --version` | PASS — 11.14.0 |
| Relative Markdown link check for changed documentation | PASS |
| `node mango-pmo/tools/audit-readme-source-facts.mjs --root mango-docs` | PASS |
| `node mango-pmo/tools/audit-module-readmes.mjs` | BLOCKED by 10 pre-existing component README section/related-link findings; no changed module README is among the findings |
| `npm --prefix mango-docs run docs:build` | PASS — VitePress 1.6.4; 165 public docs; npm audit reports 5 existing dependency vulnerabilities (2 moderate, 3 high) |

## 8. Manual pilot checkpoint

| Case | Observation | Decision |
|---|---|---|
| `COG-001` | Manual response not supplied in this non-interactive run. Static source refs and expected-fact rubric are present. | Do not mark human pass. |
| `COG-002` | After clarification, the user identified the concrete error-code owner, message precedence, and the no-body success boundary. | PASS — `6/6` after one clarification and retest. |
| `COG-003` | Three isolated role simulations were used only to find missing tenant-context and failure-boundary facts; they are not human or real expert results. | Static explanation contract updated; human pass remains pending. |
| `COG-004..008` | Dataset, source refs, and route/read-only boundaries are validated by static checks; no interactive participant response was available. | Do not mark human pass. |
| Agent dry run | A synthetic packet filled all rows but used `Agent dry run`; the mechanical gate rejected it. | Correctly rejected; not a human result. |

This checkpoint confirms that abstract questions such as “which layer provides the error code?” are too indirect. The Skill now requires scenario-specific questions and a retest after clarification.

## 9. Residual risks

- The pilot defines a measurement contract but does not yet contain a complete human comprehension result; this remains the only manual release gate.
- The three guides are reader-facing entry points, not a replacement for module READMEs or PMO rules.
- The document reading entry and task graph are enforced for newly written documents by the specialized Skills; historical documents remain under their existing compatibility baseline.
- The retro mechanism records learning but does not itself approve a release or close a lifecycle handoff.
- User-confirmed terms must be reviewed before being promoted into long-lived capability documentation.
- If the comprehension score does not improve against the baseline, stop expansion and do not add more rules, glossary entries, or documentation volume.

## 10. Agent text and decision review extension

本次根据新的理解性要求扩展试点：所有 Agent 文本输出统一遵循受控、重点优先、证据可追溯和失败边界明确的写作规则；需要作出范围、方案、验证、Issue、PR、发布或治理决定时，先组织三轮独立视角，再整理方案并进行同行评审。

| 轮次 | 视角 | 意见 | 当前依据 |
|---|---|---|---|
| 1 | PMO / 规范 | 规则必须只有一个源；文本规则与决定评审规则分别归档到 `mango-pmo/rules/**`，不复制到业务文档。 | `mango-pmo/rules/00-dev-flow.md`; `mango-pmo/rules/06-document-assets.md` |
| 2 | 文档 / 业务开发 | 需求、技术、测试、计划、Issue、沟通和注释都先写重点；模板只链接规则，不用长篇重复规则。 | `mango-pmo/rules/13-agent-text-output.md`; 当前四阶段模板和三个快速用法 |
| 3 | QA / 交付 | 文字清晰不能替代事实、验证和审批；三轮意见、同行评审和人工责任必须可检查，模拟 Agent 不得冒充专家。 | `mango-pmo/rules/14-decision-expert-review.md`; `mango-pmo/tools/check-decision-review.mjs` |

**整理后的方案：** 新增 `13-agent-text-output.md`、`14-decision-expert-review.md`、决策评审模板和检查器；同步规则索引、Agent、生命周期模板、Issue Runbook、业务快速用法和理解评测案例。保留当前 SIMPLE / STANDARD / FULL、唯一规范源、人工审批和 worktree 隔离。

**同行评审状态：** 已完成静态规则自检和测试；真实人工同行评审尚未完成，不能将本节标记为人工审批或正式投产批准。

## 11. Module README optimization pilot

本轮不直接重写全部模块 README。先用现有审计结果和真实使用频率选择六个代表性入口：

- 后端复杂能力：`mango-resource`、`mango-file`、`mango-workflow`；
- 前端业务入口：`@mango/file`、`@mango/workflow`；
- 公共组件：`MangoDataTable`。

每个入口只补充短的“先读”段：真实场景、最小路径、不处理范围和阅读顺序。原有 API、配置、权限、租户、失败和源码事实不删改。模块 README 全量审计仍发现 10 个既有公共组件 README 结构问题；这些问题不在本轮六个样本内，不通过样本改动掩盖。

| 评审轮次 | 视角 | 结论 |
|---|---|---|
| 1 | PMO 范围 | 先做六个代表性入口，避免全仓重写和历史债务扩张。 |
| 2 | 文档/业务开发 | 先读段只表达场景、最小路径、边界和阅读顺序，不复制 API 规则。 |
| 3 | QA/交付 | 静态结构通过不等于理解通过；必须再做真实读者六问和源码入口命中率测试。 |

**整理方案：** 先提交六个样本和检查器，再依据人工结果决定是否扩展。

**静态结果：** 六个样本通过 `check-readable-module-readmes.mjs`；相关测试纳入 PMO 全量测试。

**效果门禁：** 真实读者使用六个样本完成“问题、触发、输入、责任模块、状态变化、失败入口”六问，并记录首次源码命中率、澄清次数和完成时间。样本通过后再决定是否扩展到其余模块；Agent 自读或静态结构通过不计为真实理解结果。

## 12. Synthetic blank-context evaluation

当前运行环境没有独立子 Agent 调用能力，因此以下结果是同一 Agent 按相同提示、三次独立空白上下文模拟的结果，不是三名真实开发者，也不关闭人工门禁。

评分规则：每个 README 回答六问，每题 1 分；总分为 6 个 README 的平均分。源码命中仅统计 README 内首次出现的明确 `src` 入口或组件源码入口。

| 模拟读者 | 视角 | README 总分 | 平均分 | 首次源码入口命中 |
|---|---|---:|---:|---:|
| Sim-1 | 后端业务开发 | 29/36 | 4.83/6 | 6/6 |
| Sim-2 | 前端业务开发 | 33/36 | 5.50/6 | 6/6 |
| Sim-3 | 全栈业务开发 | 35/36 | 5.83/6 | 6/6 |
| **平均** | 三次模拟 | **97/108** | **5.39/6** | **100%** |

初次模拟的源码入口命中率为 50%；随后六个 README 均补充明确源码入口，并通过检查器复测为 6/6。该改善只证明文档入口更可追溯，不是人工理解证据。

**模拟结论：** 场景、最小路径和边界表达达到试点目标的模拟分数线，源码入口命中率达到结构性目标；结果仍不能替代真实开发者评测，不能据此扩大试点或标记正式投产。
