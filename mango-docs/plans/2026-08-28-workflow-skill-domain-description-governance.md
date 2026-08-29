# Mango Workflow Skill 领域描述治理记录

## 1. 元数据

- 任务 ID：WORKFLOW-SKILL-DOMAIN-DESCRIPTION-20260828
- 状态：IMPLEMENTED（未提交、未发布）
- 交付模式：FULL（官方 PMO Skill 修改自身，使用单份治理记录）
- 需求影响：L1，仅澄清 Skill 的发现描述，不改变 Mango 审批运行时行为。
- 方案风险：L3，修改可隐式调用的官方 Skill 元数据。
- 最终风险：L3
- 工作区决策：CREATE，分支 `chore/mango-workflow-skill-description`，独立 worktree `mango-workflow-skill-description`。
- 既有决定：延续 [Mango Workflow Skill 术语路由治理记录](./2026-07-18-workflow-skill-routing-governance.md)，不重新定义触发边界。

## 2. 目标与范围

- 目标：让 Skill 发现阶段直接识别 `mango-workflow` 是 Mango 审批领域指导，而不是需求、设计、开发、测试、发布或 CI/CD 工程流程。
- 成功条件：frontmatter 和 UI 短描述均明确审批领域；正文明确该 Skill 可以与适用工程 Skill 组合，但不能选择或替代交付流程。
- 处理范围：`mango-pmo/skills/mango-workflow/SKILL.md`、`agents/openai.yaml`、`@mango/pmo`/Business Starter 机械投影、patch Changeset 和当前用户级安装副本。
- 不处理范围：Skill 改名、审批 API/运行时、PMO 工程流程、规则索引、发布和部署。

## 3. 技术决定

- 保留 `mango-workflow` 名称，因为它与 `@mango/workflow`、`mango-workflow-api` 和 `mango-workflow-starter` 的公开坐标一致。
- 不改名为 `mango-flowable`，避免把 Mango 审批公共能力绑定到内部流程引擎实现。
- frontmatter 先陈述领域，再列精确信号和排除边界；正文补充与工程 Skill 的组合关系。
- 用户级副本只做当前环境投影，长期规范源仍是 `mango-pmo`。

## 4. 验收映射与结果

| ID | 验证方式 | 预期结果 | 结果 |
|---|---|---|---|
| VAL-001 | `uv run --with pyyaml python .../quick_validate.py mango-pmo/skills/mango-workflow` | frontmatter、目录名和 UI 元数据合法 | PASS，`Skill is valid!` |
| VAL-002 | `node mango-pmo/tests/skills/check-skill-evals.mjs` | 审批正例触发，工程流程、CI/CD、通用编排和裸词反例不触发 | PASS，17 个 Skill、149 个用例 |
| VAL-003 | Workspace layout、`git diff --check` 与文件清单 | 独立 worktree 合规且无任务外改动 | PASS |
| VAL-004 | 用户级副本 SHA-256 比较 | 当前安装副本与官方源一致 | PASS，两个文件哈希分别一致 |
| VAL-005 | `@mango/pmo` build/check 与 `sync-pmo-baseline.mjs --check` | 发布包可构建且受管 baseline 无漂移 | PASS，145 个受管文件 |
| VAL-006 | `release:change-check --include-working-tree` | 变更声明覆盖未来发布闭包 | PASS，`@mango/pmo -> @mango/cli` |

## 5. 剩余风险

- 本任务不发布 `@mango/pmo`；其他环境要在后续正式 PMO 发布并升级后获得新描述。
- `release:impact` 在未升版状态下按预期阻断 `@mango/pmo` 和 `@mango/cli`；未来发布任务必须完成版本提升和回查，本任务不越权处理。
- 不修改既有触发用例内容，因为本次只澄清已由正反例覆盖的 Skill 关系。
