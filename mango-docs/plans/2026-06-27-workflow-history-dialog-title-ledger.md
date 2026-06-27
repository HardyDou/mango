# 工作流历史申请弹窗标题修复交付契约

## 1. 目标

修复工作流侧边栏按钮菜单打开的“历史申请”弹窗中标题重复显示的问题。

## 2. 范围

- `@mango/workflow` 公共组件 `WorkflowInstanceHistory`。
- `@mango/workflow` 弹窗组件 `WorkflowInstanceHistoryDialog`。
- `@mango/workflow` 组件 README 的历史申请组件 props 说明。
- 业务审批接入指南的变更影响记录。

## 3. 不做什么

- 不修改工作流接口、数据库、菜单、权限和路由。
- 不调整历史申请列表的数据结构和加载逻辑。
- 不修改业务示例页的历史申请页签布局。

## 4. 设计输入

- 用户截图指出“历史申请”弹窗顶部重复出现同名标题。
- 弹窗已有 `ElDialog` 标题，内容组件不应在弹窗场景再次显示同级标题。

## 5. 设计说明

### 5.1 影响模块

- 前端包：`mango-ui/packages/workflow`。
- 组件：`WorkflowInstanceHistory.vue`、`WorkflowInstanceHistoryDialog.vue`。

### 5.2 接口变化

- 无后端接口变化。
- `WorkflowInstanceHistory` 新增可选 prop `showTitle?: boolean`，默认 `true`，保持直接使用该组件时的原有标题展示。

### 5.3 数据变化

- 无数据库、持久化数据、接口 payload 或响应字段变化。

### 5.4 菜单/页面/权限变化

- 无菜单、路由和权限变化。
- `WorkflowInstanceHistoryDialog` 内部传入 `show-title=false`，弹窗场景只保留 `ElDialog` 标题。

### 5.5 测试范围

- 包构建。
- 组件弹窗渲染截图。
- 标题 DOM 断言：弹窗标题存在，内部 `h3` 不存在。

## 6. 风险与限制

- 本地完整后台链路未启动成功，原因是任务 worktree 数据库 `mango_dev_bffd82` 不存在。
- 使用 `.runtime/workflow-history-dialog-preview` 临时页注入组件 props 进行视觉验收，不作为正式代码提交。
- `pnpm --filter @mango/workflow test` 受现有 Vitest 解析 `@mango/file` 失败阻断，非本次改动引入。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | 用户截图 | 去掉历史申请弹窗重复标题 | 在历史列表组件保留默认标题能力，弹窗调用时关闭内部标题 | `WorkflowInstanceHistory.vue`、`WorkflowInstanceHistoryDialog.vue`、组件 README、业务审批接入指南 | 构建、截图、DOM 断言 | DONE | 本文件第 8 节 |

## 8. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | `http://127.0.0.1:8126/` 临时组件预览页 | 历史申请弹窗标题 | 1 条历史申请记录 | `el-dialog__title` 为“历史申请”；`.workflow-instance-history__header h3` 数量为 0 | 副标题和“1 条”保留，列表记录正常展示 | Playwright 截图成功；完整后端链路因本地数据库缺失未验证 | 对话中已输出截图；临时页位于 `.runtime/workflow-history-dialog-preview` | DONE |

## 9. 验证命令

```bash
pnpm install --frozen-lockfile
pnpm --filter @mango/workflow build
pnpm --filter @mango/workflow test
pnpm --filter @mango/workflow exec vue-tsc --noEmit --skipLibCheck
git diff --check
```

## 10. 验证结果

- `pnpm --filter @mango/workflow build`：通过。
- `git diff --check`：通过。
- `pnpm --filter @mango/workflow test`：未通过，失败点为既有测试环境无法解析 `@mango/file`，9 个已收集测试通过。
- `pnpm --filter @mango/workflow exec vue-tsc --noEmit --skipLibCheck`：未通过，失败点为既有 `@mango/*` 类型解析、测试全局类型和历史类型问题；本次改动文件未出现在报错中。
