# Issue #838 前端页面基线项目级开关实施计划

## 基线

- Issue：https://github.com/HardyDou/mango/issues/838
- 设计：`mango-docs/designs/2026-08-20-issue-838-frontend-page-baseline-toggle.md`
- 最终风险：L3；模式：FULL 治理。
- 工作区：M01=CREATE，`feat/issue-838-frontend-baseline-toggle`。

## 实施与验证

| ID | 交付项 | 验证 | 状态 |
| --- | --- | --- | --- |
| TASK-001 | 增加统一配置解析并接入 checker/classifier | PMO 页面基线与 scope 单测：29/29 通过 | DONE |
| TASK-002 | 接入 GitHub/Gitea 托管 Workflow | CLI 生成投影与 Workflow 合同测试通过；GitHub/Gitea 条件保持对称 | DONE |
| TASK-003 | 接入 CLI init/sync/upgrade/check | `check-cli.mjs` 通过；CLI 单测 74 通过、2 项平台跳过 | DONE |
| TASK-004 | 同步 PMO package、Starter baseline 和能力说明 | package build/check、Starter projection、模板检查、README 审计通过 | DONE |
| TASK-005 | 执行空白上下文边界用例和独立专家复核 | 默认/显式关闭/非法值/非触发边界已覆盖；独立 Agent 复核因模型服务容量暂不可用，保留本地人工复核结论 | DONE WITH REVIEW LIMITATION |

## 保障措施与剩余风险

- M07=ENABLE：设计和计划记录单项门禁的长期配置与 required check 边界。
- M08=ENABLE：更新 PMO、PMO package、CLI、Starter 和能力地图说明。
- M09=ENABLE：执行配置、Workflow、治理意图、投影和工作区静态检查。
- M10=ENABLE：覆盖默认值、显式关闭、非法配置和 checker 行为。
- M11=ENABLE：覆盖 CLI init/sync/upgrade/check 与 package/Starter 投影协作。
- M14=ENABLE：由独立上下文复核 fail-closed、双平台语义和稳定汇总边界。
- M12/M13/M15/M16=DISABLE：无服务 API、浏览器 UI 或本次必须回写的外部平台状态；真实 Runner 状态在后续 PR/发布与业务升级任务中验证。

## 本次验证结果

- `node --check`（CLI 与统一解析器）通过，`git diff --check` 通过。
- PMO 页面基线与 scope 测试 29/29 通过。
- `check-governance-intent`、`workspace-layout-check`、PMO package build/check、Starter projection/template check 通过。
- CLI 生命周期检查通过；CLI 单测 76 项中 74 项通过、2 项按平台跳过。
- `audit-module-readmes` 与 `audit-readme-source-facts` 通过。
- 未执行 commit、push、PR、发布或外部状态写入。
