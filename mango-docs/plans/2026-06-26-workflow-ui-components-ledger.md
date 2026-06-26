# 工作流业务 UI 组件交付台账

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| 需求确认 | DONE | 已确认组件用于业务模块复用，流程图弹窗展示流程定义图 |
| 设计文档 | DONE | `mango-docs/designs/2026-06-26-workflow-ui-components-design.md` |
| Issue 登记 | DONE | https://github.com/HardyDou/mango/issues/266 |
| 公共组件实现 | DONE | `@mango/workflow` 新增 `components/business-ui` 组件组 |
| 任务详情页复用 | DONE | 任务详情页已复用 `WorkflowLayout`、`WorkflowSidebar`，保留业务自定义表单和记录面板扩展 |
| README 更新 | DONE | workflow 包 README 和组件 README 已补充业务 UI 组件用法 |
| 测试验证 | DONE | 构建、Vitest、Playwright 视觉 E2E 均已执行 |

## 验证记录

- `pnpm --filter @mango/workflow build`：通过。
- `node_modules/.pnpm/node_modules/.bin/vitest run --config vitest.workflow.config.ts packages/workflow/src/components/__tests__/workflowBusinessUi.spec.ts packages/workflow/src/views/task-detail/__tests__/taskDetail.spec.ts`：通过，14 个测试通过。测试环境存在 Element Plus 组件未全局注册 warning，不影响断言和退出码。
- 收尾修正后重新执行上述 build 与 Vitest：通过。历史申请组件已补充接口失败降级处理，历史申请弹窗已补充稳定选择器类名。
- Playwright 视觉 E2E：通过，验证主面板、流程图弹窗、历史申请弹窗。
- 视觉截图：
  - `mango-ui/test-results/workflow-ui-panel.png`
  - `mango-ui/test-results/workflow-ui-graph-dialog.png`
  - `mango-ui/test-results/workflow-ui-history-dialog.png`
- `scripts/dev-workspace.sh frontend` 启动真实后端时受现有后端迁移资源阻塞：`notice` Flyway 模块缺少 `db/migration/notice/V15__notice_announcement.sql`，与本次前端组件改造无关。本轮改用前端 Vite 沙盒完成组件视觉 E2E。

## 风险

- 历史申请摘要存在业务差异，需要通过 slot 扩展，公共组件只提供通用字段。
- `WorkflowProgressTree` 当前名称不够准确，新增语义别名时必须保持旧导出兼容。
- 任务详情页已有自定义记录面板能力，替换时不能破坏 `recordPanelMode`。
