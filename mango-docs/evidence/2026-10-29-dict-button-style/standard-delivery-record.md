# 标准交付记录

> 任务：字典组件按钮样式

## 0. 决策复核

- 事实与用户视角：现有 `DictSelect` 只有下拉/树形选择；本次目标是让 FLAT 字典支持单选按钮和多选按钮录入样式。依据：`mango-ui/packages/common/components/DictSelect/index.vue`、用户需求。
- 技术与风险视角：新增公共 `variant` prop，不改变字典接口、数据结构和默认 `select` 行为；`TREE` 继续使用树形选择器。共享组件和公共 API 变化使方案风险为 L2。
- 验证与交付视角：组件测试覆盖两种按钮样式，开发中心展示真实组件，Playwright 覆盖浏览器可见入口，Common 包和 Admin 应用构建通过。
- 评审选择：`AGENT_ONLY`。本任务未指定外部同行评审；保留后续 Review 入口。

## 1. 元数据

- 任务 ID：dict-button-style
- 交付模式：STANDARD
- 需求影响：L2 - 影响 `@mango/common` 公共选择组件及其浏览器录入方式。
- 方案风险：L2 - 新增公共 prop 和两条 Element Plus 组件渲染路径。
- 最终风险：L2
- 工作区决策：CREATE
- 工作区：`/Users/hardy/Work/mango-dict-button-style`
- 分支：`feat/dict-button-style`

## 2. 目标与范围

- 目标：让 `DictSelect` 支持 FLAT 字典的下拉、单选按钮和多选按钮样式，并允许消费者自定义按钮组样式。
- 成功条件：`variant="radio-button"` 使用标量 value；`variant="checkbox-button"` 使用数组 value；`buttonClass`/`buttonStyle` 可传递到按钮组；默认下拉和 TREE 行为保持兼容；文档、示例和测试同步。
- 处理范围：Common 公共组件、类型导出、组件测试、字典开发中心示例、字典组件 README、组件 E2E。
- 不处理范围：后端字典 API、字典数据模型、TREE 的按钮样式、发布版本和 PR 创建。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-001 | 业务表单使用 `DictSelect` | FLAT 字典、`variant="radio-button"` | 显示 `el-radio-button` 单选按钮，v-model 为单值 | 字典接口失败仍显示组件错误态并触发 `error` | 组件测试和浏览器示例通过 |
| AC-002 | 业务表单使用 `DictSelect` | FLAT 字典、`variant="checkbox-button"` | 显示 `el-checkbox-button` 多选按钮，v-model 为数组 | 清空通过 v-model 设为 `[]` 或调用 `clear()`；按钮样式无下拉清空图标 | 组件测试和浏览器示例通过 |
| AC-003 | 既有 FLAT/TREE 消费者 | 不传 `variant`，或使用 TREE | 默认下拉和树形选择行为不变 | 不支持的 TREE 按钮样式忽略 `variant`，仍使用树形选择器 | Common 测试、Admin 构建和既有字典 E2E 通过 |
| AC-004 | 业务页面自定义样式 | 按钮样式传入 `buttonClass`/`buttonStyle` | class 和内联样式可用于按钮组自定义 | 不传时使用 Element Plus 默认样式 | 文档和开发中心示例提供接入方式；不增加样式布局断言 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | AC-001/AC-002 | 新增 `DictSelectVariant = 'select' | 'radio-button' | 'checkbox-button'` 和 `variant` prop；默认值为 `select`。 | `packages/common/components/DictSelect`、Common 主入口类型导出 | 移除按钮分支和 prop，默认下拉路径可独立恢复 |
| TD-002 | AC-001/AC-002 | 按钮样式只用于 FLAT；radio 固定单选，checkbox 固定多选；仍复用 `/system/dict/data/options` 和现有 value 过滤。 | `DictSelect` 模板和选择值归一化 | 回退到 `el-select`，不涉及后端数据恢复 |
| TD-003 | AC-003 | TREE 始终使用现有 `el-tree-select`；不新增 TREE 按钮语义。 | `DictSelect` TREE 分支 | 删除 `variant` 不影响 TREE |
| TD-004 | AC-001/AC-002 | 公开能力变更同步更新组件 README、Common README、开发中心示例和 E2E。 | Common 文档与 Admin 字典示例 | 代码行为仍可运行，但文档需随 API 一并回退 |
| TD-005 | AC-004 | 通过 `buttonClass` 和 Vue `StyleValue` 类型的 `buttonStyle` 配置按钮组外层；不复制 Element Plus 按钮实现。 | `DictSelect` props、按钮组 class/style 绑定、组件 README | 移除两个 props 即恢复默认样式 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| IM-001 | TD-001/TD-002 | 1 | `mango-ui/packages/common/components/DictSelect/index.vue`、`types.ts`、`mango-ui/packages/common/index.ts` | 公共 prop、类型和渲染路径完成 |
| IM-002 | TD-001/TD-002 | 2 | `mango-ui/packages/common/components/DictSelect/__tests__/DictSelect.spec.ts` | 两种按钮样式、value 形态和事件有自动化覆盖 |
| IM-003 | TD-004 | 3 | Common README、DictSelect README、Admin 字典示例 | 使用方式、限制和参数表同步 |
| IM-004 | TD-004 | 4 | `mango-ui/apps/mango-admin/e2e/specs/dict-component-demo.spec.ts` | 浏览器示例断言 radio/checkbox 入口 |
| IM-005 | TD-005 | 5 | `DictSelect` README、Admin 字典示例 | 自定义 class/style 有接入示例；不增加样式布局断言 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-001/AC-002 | 组件测试 | `cd mango-ui && pnpm --filter @mango/common test -- components/DictSelect/__tests__/DictSelect.spec.ts` | PASS：39 个文件、406 个测试通过 | Vitest 命令输出 |
| AC-001/AC-002 | Common 类型与构建 | `pnpm exec vue-tsc --noEmit -p packages/common/tsconfig.json`；`pnpm --filter @mango/common build` | PASS | vue-tsc/Vite 输出 |
| AC-001/AC-002/AC-003 | Admin 生产构建 | `pnpm --filter mango-admin build` | PASS | Vite production build 输出 |
| AC-001/AC-002 | 浏览器 UI/E2E | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm exec playwright test --config apps/mango-admin/playwright.config.ts apps/mango-admin/e2e/specs/dict-component-demo.spec.ts --project=chromium` | PASS：1 passed | Playwright 输出 |
| AC-001/AC-002 | 静态与聚合门禁 | `pnpm exec stylelint ...`；`pnpm admin:styles:check`；`pnpm admin:module-styles:check` | PASS | Stylelint、Admin style governance 输出 |
| AC-004 | 样式自定义 API | 不增加样式或布局断言；保留 `buttonClass`/`buttonStyle` 文档和构建检查 | EXCEPTION：按用户要求不增加样式断言 | 用户明确要求 |
| AC-001/AC-002/AC-003 | 项目级 Admin TypeScript | `pnpm exec vue-tsc --noEmit -p apps/mango-admin/tsconfig.json` | BLOCKED：仓库现有类型/构建产物错误，包括多个未生成的内部包类型和既有 API 类型错误；未将该结果作为本次通过依据 | 命令输出；Common 类型检查和 Admin 生产构建已通过 |

## 7. 例外与剩余风险

- 未创建 PR、未提交代码、未发布 `@mango/common` 版本；本记录只覆盖当前任务 worktree 的实现与验证。
- 按钮样式没有下拉清空图标，消费者需要用 v-model 或 `clear()` 清空；该限制已写入 DictSelect README。
- 未对自定义 CSS 做 computed style 或截图断言，符合用户要求的“只为功能增加断言”。
- 未执行 Firefox/WebKit；本次组件 E2E 使用 Chromium，浏览器兼容性仍由既有夜间/发布套件覆盖。
