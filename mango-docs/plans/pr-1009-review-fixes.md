---
decisionId: DEC-PR-1009-REVIEW-FIXES
documentType: decision-review
status: REVIEWED
---

# PR #1009 复核缺陷修复记录

## 1. 决定摘要

- **状态：** REVIEWED
- **来源：** [合并后复核评论](https://github.com/HardyDou/mango/pull/1009#issuecomment-6031945934)。
- **目标：** 拒绝未完成的决策复核；保留询问无即时答复时的 AGENT_ONLY；恢复三个历史 FAQ 公开地址。
- **范围：** PMO checker、回归测试、模块使用说明、业务 starter baseline 投影、文档站 staging 和构建检查。
- **风险与模式：** 沿用原任务治理自修改的 L3 / FULL；这是 PR #1009 返工，不重定义评审选择策略，不新增业务需求或运行时 API。
- **工作区：** 复用 `mango-docs-restructure` / `docs/restructure-business-integration`。同步 `origin/main` 的合并提交在修复前与 main 树一致，没有覆盖上游 Java 21 验证说明。

## 2. 第一轮：事实与用户视角

| 参与者/角色 | 当前来源 | 结论与依据 | 不确定项/反对意见 | 下一步 |
|---|---|---|---|---|
| 当前 Agent，事实与用户视角 | 复核评论、checker、原测试、文档构建产物 | 原实现把方式列当结论、把标签当询问内容；构建成功未覆盖历史地址 | 没有访问线上站点，不声称线上已观察到 404 | 先添加负向用例和产物测试，复现后定向修复 |

## 3. 第二轮：技术与风险视角

| 参与者/角色 | 当前来源 | 结论与依据 | 不确定项/反对意见 | 下一步 |
|---|---|---|---|---|
| 当前 Agent，技术与风险视角 | Markdown AST、决策复核模板、VitePress staging | 复用现有 AST 按表头读取每行；静态旧地址页相对跳转至新 FAQ，不重复正文 | 结构校验不能证明询问确实发生或真实评审人身份；不声称 VM 脚本测试是浏览器验收 | 同步 baseline，并验证产物、查询参数和锚点 |

## 4. 第三轮：验证与交付视角

| 参与者/角色 | 当前来源 | 结论与依据 | 不确定项/反对意见 | 下一步 |
|---|---|---|---|---|
| 当前 Agent，验证与交付视角 | Node 测试输出、VitePress dist、baseline 校验 | 以负向测试拒绝误通过，以旧 URL 产物证明兼容入口存在 | 本地验证不包含远端 CI、PMO 发布或 Pages 部署 | 用户已授权提交代码和 PR；最终 head 复验后提交，发布仍需独立授权 |

## 5. 方案整理

### Checker

- 复用 `document-contract/markdown-ast.mjs`，按字段和表头解析，不再匹配整行关键词。
- 评审选择与询问记录只从目标章节读取；拒绝缺失、空白、重复字段和代码示例中的伪记录。
- 逐行核对方式、参与者、阻断问题、结论；拒绝缺列、重复表头、畸形行、模式不一致、开放阻断和空结论。
- `AGENT_ONLY` 接受 `PASS` / `RECORDED`；外部评审接受 `PASS`，只对参与者列检查 Agent 冒充，不因建议提到 Agent 而误拒。
- 无开放阻断明确填写 `None` / `无`；已解决问题的历史叙述放在建议或来源中。询问真实但未即时答复仍可记录继续。

### 历史 FAQ 地址

- 仅在站点构建的 `public/` 生成三个兼容 HTML，不恢复旧 Markdown 正文、不重加导航、不恢复版本快照。
- 相对链接兼容不同站点 base；脚本用 `location.replace` 保留查询参数和锚点；无脚本时提供 meta refresh 和手动链接。
- `docs:build` 自动运行旧路径输出检查；测试独立固定三个历史 slug，避免实现和断言同时漏删同一路径。

### 取舍与恢复

- 不恢复强制人工评审；不通过放宽状态、忽略阻断或修改 fixture 掩盖失败。
- 不复制 FAQ 全文，避免双份事实源；不依赖 GitHub Pages 不支持的服务端重定向配置。
- 回退时同时恢复 checker、测试和生成的 baseline；站点需要重新构建部署。回退可能重现本次误通过与旧地址失效，不能当作长期方案。
- 不改变业务 API、权限、租户、数据库或运行时行为；没有正式审批或人工理解评测结果。

## 6. 评审选择与记录

- **评审选择：** AGENT_ONLY
- **询问记录：** 沿用原任务用户“询问并记录，不强制人工”的选择及复核评论中的询问记录；本轮未指定新增外部评审，按 AGENT_ONLY 继续。不把三个视角称为三个独立 Agent 或真实同行。

| 评审方式 | 评审人/Agent 角色 | 检查范围 | 阻断问题 | 非阻断建议 | 结论 |
|---|---|---|---|---|---|
| AGENT_ONLY | 当前 Agent 三视角分析 | 三项复核意见、负向校验、公开 URL 兼容 | None | 合并后分别回读 CI 与 Pages，结构检查不替代事实审批 | RECORDED |

## 7. 审批与后续动作

- **审批人：** 不适用，本记录不声称完成人工审批。
- **提交授权：** 用户明确要求“提交代码 pr”，授权本任务 Commit、Push 和创建后续修复 PR，不包含合并、发布或部署。
- **后续动作：** 使用项目级提交流程核对最终 head 并回读远端 PR。最终 SHA、命令和 CI 状态以 PR 提交记录为准；发布后另行验证旧地址线上可达。
- **版本结论：** 本次提交源码与 starter baseline 投影，不发布新包，PMO 包版本仍为 `1.4.5`。既有业务项目不会因本 PR 自动取得修复；版本化发布与消费验证另行执行。

## 8. 本地验证证据

环境为 Node `v22.23.1`、pnpm `11.14.0`、VitePress `1.6.4`。测试使用临时 Markdown fixture 和本地构建产物，不连接业务数据库、不使用业务账号或租户数据。

原始日志位于任务 worktree 的 `.runtime/pr-1009-fixes/`，不作为业务长期测试资产。

| 验证 | 修复前 | 修复后 |
|---|---|---|
| `node --test mango-pmo/tests/decision-review.test.mjs` | 46 项中 38 项失败，确认负向缺口 | 46 项通过 |
| 决策复核、Agent 文本和 document-contract 联合测试 | 原复核的旧测试为 54 项通过 | 97 项通过，无 skipped |
| 三个旧 FAQ 的 HTML 和跳转脚本测试 | 文档构建成功，但 6 项地址测试全部失败 | 6 项通过，覆盖静态目标、无脚本入口、clean/.html URL、不同 base、查询参数与锚点 |
| `npm --prefix mango-docs run docs:build` | 179 份公开文档，缺少旧地址 | 179 份公开文档与 3 个跳转入口，无版本快照复制；仍有 bundle size 警告 |
| PMO 广泛回归，日志 `pmo-regressions.tap` | 不适用 | 215 项通过，无 skipped；覆盖文档合同、风险、工作区、preflight、架构预算和 checker |
| Skills、历史交付模式与保障合同 | 不适用 | 20 Skills / 168 eval cases、32 历史案例、100 保障案例通过 |
| baseline 同步、PMO package、starter 和业务模块投影 | 不适用 | 165 managed files、package check、84 required files / 39 contract checks 通过 |
| canonical 与 starter checker 正反例 smoke | 不适用 | 两份 checker 字节一致，8 项正反例通过 |
| 治理、README、业务指南、能力文档、测试质量和 diff | 不适用 | 检查通过；无运行时改动 |

以上不包含远端 CI、业务运行态测试、真实浏览器验收或线上 Pages 部署验证。
