# Issue #888 测试与验收物料清理治理记录

- Issue: [#888](https://github.com/HardyDou/mango/issues/888)
- 初始分析基线：`5aea90563` (`origin/main`, 2026-08-29)；提交前目标基线：`ee42edab0` (Mango 1.0.43)
- 需求影响：L2；方案风险：L3；最终风险：L3；模式：FULL 治理记录；工作区：`CREATE` (`chore/test-asset-cleanup`)

## 范围

- 删除 evidence 中 Sprint 4/5 `consumer-app`、Sprint 6 `generated-full-app` 生成项目及其一次性 consumer 验证脚本。
- 删除明确的构建/安装/启动/同步过程 `.log`，保留结构化报告、SQL/HTTP/JSON 结果、截图及 `baselines/**/latest`。
- 不删除 `mango-ui/**` 正式测试、后端 `src/test`、当前基线或产品运行代码。

## 证据与计划

候选均已通过 `git ls-files` 和全仓引用搜索确认：生成项目只被历史摘要/台账引用，正式入口在 `mango-ui`；日志仅被历史台账引用，结果可由结构化摘要承载。删除通过 Git PR，可从 PR/历史恢复。

1. 删除候选并更新历史索引，消除失效路径。
2. 执行 `git diff --check`、PMO/测试资产检查及受影响包测试。
3. 使用 Mango CLI 初始化隔离 workspace，从零启动并验收菜单、所有页面及每个模块随机一个功能；记录真实服务、数据库、账号、UI 与 console/network 证据。
4. 提交 PR，等待 required checks，合并后回读远端状态并清理任务 worktree。

## 实施结果

- 已删除 Sprint 4/5 `consumer-app`、Sprint 6 `generated-full-app`、对应一次性 consumer 脚本，以及 Issue #64/#252 明确属于过程产物的构建、安装、启动、同步日志。
- 已更新历史摘要、台账和索引；Issue #252 以结构化 `verification-summary.md` 替代原始过程日志，不改变原验收结论。
- 未修改 `mango-ui/**` 正式测试、后端 `src/test`、产品运行代码或 `baselines/**/latest`。

## 验证结果

| 类别 | 命令/场景 | 结果 |
|---|---|---|
| Diff | `git diff --check` | PASS |
| 测试资产 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | PASS，0 个正式测试文件变更 |
| 工作区布局 | `node mango-pmo/tools/workspace-layout-check.mjs --root .` | PASS |
| Issue #252 证据 | `acceptance-evidence-check` | PASS，6 行 |
| Issue #252 交付契约 | `delivery-contract-check` | PASS，5 DONE，0 exception/incomplete |
| 前端样式 | `pnpm admin:styles:check`、`pnpm admin:module-styles:check` | PASS |
| 全仓测试 | `pnpm -r test` | 部分通过；`mango-cli` 65 个 Node 测试、`@mango/file` 7 文件 41 测试通过；`@mango/ai` 3 个套件因工作区包入口未预构建、`@mango/common` 与 `@mango/ai-api` 无法解析而失败，与本次 evidence-only diff 无关 |
| 冷启动 | 项目源码 CLI，workspace `mango_009`，Resource Bootstrap generation 1 | PASS；后端 `18009`、前端 `30009` 及 8 个子应用启动，Actuator/DB 均为 `UP` |
| 全菜单 UI | 真实 `default / admin` 登录，遍历实时菜单接口返回的 76 个叶子页 | 73 PASS、3 FAIL；失败均为现存跨模块权限 403，已登记 [#890](https://github.com/HardyDou/mango/issues/890) |
| 模块功能 | 14 个模块各执行一个只读查询、刷新、重置或筛选动作 | 14/14 PASS，无动作级 console/network 错误 |

全菜单验收原始 JSON、四轮诊断结果、28 张截图和临时脚本保存在任务工作区 `.runtime/issue-888/`，不纳入 Git。最终失败页为：

- `/workflow/manage/template`：`GET /api/system/tenant/list?status=1` 返回 403。
- `/home-management/list`：`GET /api/identity/users/page?page=1&size=200&status=1` 返回 403。
- `/home-management/user`：`GET /api/identity/users/page?page=1&size=50` 返回 403。

源码 CLI 可正确跳过历史 Maven install；`pnpm exec mango` 解析到全局 CLI 的版本/解析漂移已单独登记 [#889](https://github.com/HardyDou/mango/issues/889)，不进入本清理 PR。

## 验收映射

| ID | 目标 | 证据 |
|---|---|---|
| VA-001 | 删除清单与引用核对 | DONE：Git 清单、`rg` 输出、PR diff |
| VA-002 | PMO/测试资产边界 | DONE：检查全部通过 |
| VA-003 | 正式测试入口和包构建 | DONE_WITH_EXCEPTION：正式测试未变；全仓测试存在与 diff 无关的 AI 包预构建/解析失败 |
| VA-004 | 菜单/页面/随机功能真实验收 | DONE_WITH_EXCEPTION：73/76 页面通过、14/14 模块动作通过；3 个现存权限缺口转 #890 |

## 专家复核

- 复核视角：测试资产治理、历史证据可追溯性、正式测试入口保留、运行态回归。
- 阻断问题：无。删除对象均为 Git 可恢复的生成项目、一次性脚本或过程日志；历史最终报告、截图、结构化数据和正式测试入口仍保留。
- 非阻断建议：Issue #888 后续批次继续按引用、正式入口和可复核结果三项事实逐批清理，不扩大到 `baselines/**/latest`。
- 结论：本次范围可提交；#889、#890 为独立现存问题，不混入清理 PR。

## 风险

旧历史目录中仍可能存在其他一次性脚本副本，本轮仅处理规则冲突且无当前入口引用的项目和过程日志，其他候选留在 Issue #888 后续批次。

本次清理没有引入产品行为变更。残余验证风险为全仓 AI 测试依赖预构建失败，以及 #890 的 3 个页面权限缺口；两者均已明确隔离，不作为清理结果的通过项。
