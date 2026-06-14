# PMO 与能力说明治理目的验证矩阵

日期：2026-06-13

本文件记录本轮治理改动的预期目的、落地机制和验证方式。它是验收证据，不是规范源。

| 预期目的 | 落地机制 | 自动验证 |
|----------|----------|----------|
| 长会话中防止 Agent 忘记 Mango 规范 | `AGENTS.md` 说明 PMO preflight 是规范记忆召回；完整边界收口到 `mango-pmo/rules/00-dev-flow.md` | `node mango-pmo/tools/check-governance-intent.mjs` |
| 简单问答、只读定位、纯同步和本地运维不进入 PMO | `AGENTS.md` 和 `rules/00-dev-flow.md` 明确低风险豁免；`check-pmo-preflight.mjs` 固化关键词回归 | `node mango-pmo/tools/check-pmo-preflight.mjs` |
| 低风险操作一旦需要改文件、解决冲突、修复问题或形成交付结论，立即升级 PMO | `AGENTS.md` 自检提示词和 `rules/00-dev-flow.md` 触发边界 | `node mango-pmo/tools/check-governance-intent.mjs` |
| 代码、接口、配置、数据库、前端、CLI、starter 等改动能召回能力说明规范 | `mango-pmo/rules/index.json` 的 `capabilityDocs` bundle；`pmo-preflight.mjs` 修复嵌套 glob 匹配 | `node mango-pmo/tools/check-pmo-preflight.mjs` |
| 改公开能力后不能漏更新模块 README 或能力地图 | `check-capability-docs.mjs` 检查能力影响文件对应模块 README 或能力地图 | `node mango-pmo/tools/check-capability-docs.mjs --self-test` |
| 不能用无关 README 冒充模块 README | `check-capability-docs.mjs` 将能力影响路径映射到对应模块 README | `node mango-pmo/tools/check-capability-docs.mjs --self-test` |
| 顶层后端模块不能被错误映射到 `src/README.md` | `check-capability-docs.mjs` 对 `mango-common`、`mango-app` 等顶层模块有显式映射 | `node mango-pmo/tools/check-capability-docs.mjs --self-test` |
| 确实不需要更新能力说明时必须写清楚原因 | PR 模板要求 `Not applicable reason`；检查脚本要求非占位、非空，并包含 public API、配置、菜单、权限、租户、页面、启动、验收、运行时行为或能力影响判断 | `node mango-pmo/tools/check-capability-docs.mjs --self-test` |
| 空 PR、模板占位 PR、无验证命令 PR 不能通过 | PR 模板字段和 `check-capability-docs.mjs` PR body 校验 | `PR_BODY_FILE=.github/pull_request_template.md node mango-pmo/tools/check-capability-docs.mjs --base main --head HEAD` 预期失败 |
| CI 不能因为 diff 失败或 0 changed files 静默通过 | workflow 使用 PR base/head SHA；脚本在 GitHub Actions 下遇到 diff 失败或 0 changed files 直接失败 | `GITHUB_ACTIONS=true node mango-pmo/tools/check-capability-docs.mjs --base main --head HEAD` 本地 0 diff 预期失败 |
| 能力地图不成为第二套规范源 | 能力地图只做索引，规则放 `mango-pmo/rules/**`；目的级脚本检查能力地图不含长期强规则词 | `node mango-pmo/tools/check-governance-intent.mjs` |
| 模块 README 不复制长期 PMO 规则 | 模块 README 模板要求只写模块事实，长期规则只链接 `mango-pmo/rules/**` | `node mango-pmo/tools/check-governance-intent.mjs` |
| 现有 README 参差不齐有可执行补齐基线 | `audit-module-readmes.mjs` 输出 A/B/C 优先级审计；审计报告归档到 evidence | `node mango-pmo/tools/audit-module-readmes.mjs` |
| 前端关键组件和页面入口不再只依赖 package README | `frontend-entry-readme.md` 模板和 `audit-module-readmes.mjs` 首批入口清单覆盖 auth/file/job/rbac/system/workflow | `node mango-pmo/tools/audit-module-readmes.mjs --self-test` |
| README 验证命令不能引用不存在的前端 package script | `audit-module-readmes.mjs` 校验 `pnpm -F @mango/* <script>` 对应 package.json scripts | `node mango-pmo/tools/audit-module-readmes.mjs --self-test` |
| README 和能力地图链接不能只检查文件、漏掉锚点 | `audit-module-readmes.mjs` 与 `check-capability-docs.mjs` 校验 Markdown 文件链接和 `#anchor` | `node mango-pmo/tools/audit-module-readmes.mjs` |

## 人工仍需判断

- 具体代码改动是否真的改变公开能力，仍需要开发者和 reviewer 结合业务语义判断。
- `Not applicable reason` 是否充分，CI 已检查非空、非占位和影响判断关键词，但不能完全替代 reviewer 对业务语义的判断。
- 未纳入首批清单的前端内部私有组件和页面局部拆分组件，仍需要在改变公开导出、页面 key、props、事件、API、权限、租户或验收方式时由 reviewer 判断是否升级为关键入口 README。
