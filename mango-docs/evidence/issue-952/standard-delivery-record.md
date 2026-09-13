# Issue #952 消息业务配置分页交付记录

## 目标与范围

- 目标：修复 `@mango/notice` 消息业务配置列表只显示默认分页部分记录的问题。
- 范围：业务配置列表的分页请求参数、总数展示、翻页刷新和查询/业务域切换时的页码重置。
- 不处理：后端分页接口、业务域分组组件、消息配置数据和权限契约。

## 可观察要求

1. 列表请求 `GET /notice/business-types` 明确携带 `pageNum` 和 `pageSize`。
2. 页面显示分页组件并使用接口返回的 `total`。
3. 翻页可加载下一页；执行查询或切换业务域从第 1 页开始。
4. 现有 `DomainSideTree` 业务域分组/切换保持不变。

## 技术决定与影响

- 后端 `NoticeBusinessTypePageQuery` 使用 `pageNum/pageSize`（默认 1/10），前端直接遵循该契约，不转换为 `page/size`。
- 仅修改通知管理前端页面及回归测试；不改变公共 API 路径、权限、租户或数据结构。
- M08：不触发能力说明更新。该变更是局部缺陷修复，模块 README 已覆盖业务配置页面和接口使用方式，公开能力与接入方式不变。

## 实施清单

- [x] 在业务配置页面增加分页状态、请求参数、总数和分页控件。
- [x] 查询与业务域切换重置页码。
- [x] 增加分页契约回归测试。
- [x] 执行包测试、构建和质量门禁并记录结果。

## 验收映射

| 验收项 | 验证方式 | 结果 |
| --- | --- | --- |
| 请求带页码/每页数量 | `adminPages.spec.ts` 源码契约回归 | 通过；12 个测试文件、52 个测试通过 |
| 总数驱动分页控件 | `adminPages.spec.ts` 源码契约回归 | 通过 |
| 包测试与构建 | Node 22.23.1 下 `pnpm --filter @mango/notice test`、`build` | 通过 |
| 变更格式与范围 | `git diff --check`、Prettier、工作树审查 | 通过 |
| 前端样式门禁 | `pnpm admin:styles:check`、`pnpm admin:module-styles:check` | 通过 |
| PMO 测试质量门禁 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | 通过 |
| 通知包类型检查 | 生成 workspace 声明后 `pnpm exec vue-tsc --noEmit -p packages/notice/tsconfig.json` | 未通过；仅报告既有 realtime 导出缺失和 `retry` 页隐式 `any`，未涉及本次分页代码 |
| 前端 PR 质量门禁 | `pnpm check:pr` | 未通过；全仓既有 22 个 workspace、570 条 typecheck 诊断，未涉及本次变更文件 |
| 浏览器人工验收 | `http://127.0.0.1:30007/notice/business-config/index` | 通过；用户于 2026-09-10 确认测试通过 |

## 未验证项与回滚

- 当前工作树后端 `http://127.0.0.1:18007` 健康状态为 `UP`，前端页面由用户完成浏览器人工验收并确认通过。
- 本地新库仅包含 Mango 冷启动数据，不等同于 Issue 报告中 Baohan 消费项目的 22 条业务配置数据；消费项目大数据量复验仍由原业务环境承担。
- 验证环境：macOS，Node `22.23.1`，pnpm `11.14.0`；Node 26 未用于正式结果。
- 回滚方式：恢复本次页面、测试和证据文件改动，不涉及数据库或接口迁移。

## Mango 1.0.53 发布衔接

- 发布候选 PR #955 已合并为 `6ced753469174630377b2e47790df03bf97d2e8a`。
- 因 GitHub squash merge 不保留候选计划源提交 `da5f19e0849b47109cbb3a32e517a415d6c760b7` 的祖先关系，后续发布衔接 PR 仅增加不改变文件树的 ancestry 记录，以满足 sealed prepare 的来源校验。
- 该衔接不改变通知分页实现、接口、数据、权限或租户行为。
