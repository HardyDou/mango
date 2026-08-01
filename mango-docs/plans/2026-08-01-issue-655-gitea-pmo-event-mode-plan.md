# Issue #655 Gitea PMO 事件模式实施计划

## 基线

- Issue：https://github.com/HardyDou/mango/issues/655
- 设计：`mango-docs/designs/2026-08-01-issue-655-gitea-pmo-event-mode.md`
- 最终风险：L3；模式：FULL 治理。
- 工作区：M01=CREATE，`fix/issue-655-gitea-pmo-edited-events`。

## 实施与验证

| ID       | 交付项                                                                 | 验证                                                                                                                                             | 状态 |
| -------- | ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ---- |
| TASK-001 | 新增事件模式解析器，按 PR 生命周期分流 diff 与正文合同检查             | `node --test mango-pmo/tests/pmo-check-event-mode.test.mjs`                                                                                      | DONE |
| TASK-002 | Gitea 模板仅在 `change-validation` 执行 scope、文档、前端和 Maven 检查 | `node --test mango-pmo/tests/pmo-check-scope.test.mjs`                                                                                           | DONE |
| TASK-003 | 将工具纳入 PMO bundle、生成项目 CLI 校验和使用说明                     | `pnpm --dir mango-ui --filter @mango/pmo build && pnpm --dir mango-ui --filter @mango/pmo check`; `pnpm --dir mango-ui --filter @mango/cli test` | DONE |
| TASK-004 | 复核治理约束、工作区和变更范围                                         | `node mango-pmo/tools/check-governance-intent.mjs`; `node mango-pmo/tools/workspace-layout-check.mjs --root .`; `git diff --check`               | DONE |

## 保障措施与剩余风险

- M07=ENABLE：本设计和计划记录 Required Check 语义与不处理范围。
- M08=ENABLE：更新 PMO 与生成业务项目 README，以及能力地图。
- M09=ENABLE：执行 workflow 静态契约、治理意图和工作区检查。
- M10=ENABLE：覆盖事件模式解析和模板守卫。
- M14=ENABLE：提交前独立复核终态分流、开放 PR 语义和 diff fail-closed 保持不变。
- M15=ENABLE：创建 PR 后回读 PR、Required Check 和合并状态。
- M11/M12/M13/M16=DISABLE：本次不改变数据库、服务 API 或浏览器界面；实际 Gitea runner 需在业务仓升级并触发终态正文编辑后观察。
