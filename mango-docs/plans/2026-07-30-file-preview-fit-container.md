# FilePreviewPanel 容器填充模式标准交付记录

## 1. 元数据

- 任务 ID：file-preview-fit-container
- 交付模式：STANDARD
- 需求影响：L2 - 新增公共组件 prop，并改变显式启用后的共享预览布局行为
- 方案风险：L1 - 使用默认关闭的局部 CSS modifier，回退时移除 prop 和样式即可
- 最终风险：L2
- 工作区决策：CREATE - `codex/file-preview-fit-container`

## 2. 目标与范围

- 目标：为 `@mango/file` 的 `FilePreviewPanel` 增加类型安全、向后兼容的容器填充模式。
- 成功条件：`fit-container` 模式下根节点、预览区域和各类内容随父容器尺寸变化；默认模式保持自然高度；消费者无需手工组合高度 CSS 变量。
- 处理范围：组件 prop 与布局样式、文件管理页真实消费者、组件测试、README、业务集成指南和能力索引。
- 不处理范围：文件 API、权限、下载行为、文件 ID 语义、文档转换接口、包版本和发布动作。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 普通页面消费者 | 未传 `fit-container` | 保持现有自然高度和 CSS 变量入口 | 旧页面高度或滚动行为变化 | 默认模式无 fit class，既有测试通过 |
| SR-002 | 固定尺寸容器 | 传入 `fit-container` | 根节点和 stage 填满可用宽高，操作栏不压缩 | 固定高度、底部空白或内容溢出 | 根节点、stage 和内容尺寸规则满足要求 |
| SR-003 | PDF/Office 预览 | fit 模式且存在 iframe URL | iframe 填满 stage，外层不产生双滚动 | iframe 被固定最小高度撑开 | PDF 和 Office 共用填充规则，外层 overflow hidden |
| SR-004 | 图片/音视频预览 | fit 模式且存在内联 URL | 内容随 stage 缩放，图片完整显示 | 图片被 cover 裁切或媒体溢出 | 图片 `contain`，媒体尺寸不超过 stage |
| SR-005 | 空状态/下载查看 | 无文件或无可用预览 URL | 状态在剩余区域水平、垂直居中 | 状态贴顶或产生无效空白 | 状态容器占满可用区域并居中 |
| SR-006 | MangoDialog 消费者 | `60vw × 65vh`，启用拖拽缩放 | 无业务 resize 监听即可同步适配 | 尺寸变化后内容保留旧尺寸 | 浏览器运行时前后尺寸断言通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001, SR-002 | 新增 `fitContainer?: boolean`，默认 `false`，通过 modifier class 启用 | `FilePreviewPanel.vue` | 删除 prop、class 和 modifier 样式 |
| TD-002 | SR-002 至 SR-006 | 使用 Flex、百分比尺寸和 `min-width/min-height: 0`，不增加 `ResizeObserver` | 组件 scoped style | 恢复原样式 |
| TD-003 | SR-001, SR-002 | 保留全部现有 CSS 变量，fit 模式只提供无需配置的默认值 | 组件样式、README | 恢复 README 和 modifier fallback |
| TD-004 | SR-002 | 文件管理页改用 `fit-container` 并删除手工变量 | 文件管理页 | 恢复消费者变量配置 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | TD-001, TD-002, TD-003 | 1 | `mango-ui/packages/file/src/components/FilePreviewPanel.vue` | prop、class 和各内容类型布局完成 |
| TASK-002 | TD-004 | 2 | `mango-ui/packages/file/src/views/files/index.vue` | 使用 prop，移除五个手工变量 |
| TASK-003 | TD-001 至 TD-004 | 3 | `FilePreviewPanel.spec.ts`、浏览器验证入口 | 默认、fit、媒体、iframe、状态和缩放有证据 |
| TASK-004 | TD-001, TD-003 | 4 | `@mango/file` 两级 README、文件上传表单业务集成指南、能力地图 | API、示例、兼容边界和验收入口已说明 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001 至 SR-005 | M10 组件测试 | `pnpm --filter @mango/file test` | PASS | 6 个测试文件、36 个测试全部通过；覆盖默认模式、fit 模式、操作栏显隐、PDF/Office、图片、音视频、空状态和下载占位 |
| SR-001 至 SR-005 | M09 静态/构建 | `pnpm --filter @mango/file build`、`pnpm lint`、`pnpm stylelint`、`pnpm component-contracts:check` | PASS | 包构建、ESLint ratchet、Stylelint ratchet和公共组件契约检查通过；生成声明导出 `FilePreviewPanelProps.fitContainer?: boolean` |
| SR-002 至 SR-006 | M11/M13 运行时 | Chromium 最小运行时 harness，组合真实 `MangoDialog` 与源码 `FilePreviewPanel` | PASS | 1280×720 视口下初始弹框 768×468（60vw×65vh），拖拽缩放至 888×548 后 stage/iframe 同步增大；弹框正文 `scrollHeight === clientHeight`；图片 `object-fit: contain`；Office、音视频、空状态和下载占位布局符合预期 |
| SR-001 | 兼容回归 | 默认模式单测；模板页消费者继续不传 prop；文件管理页真实消费者切换为 `fit-container` | PASS | 默认不生成 fit class，自然高度 CSS 默认值保持不变；文件管理页删除五个手工高度变量 |

## 7. 例外与剩余风险

- `pnpm typecheck` 仍命中实施前已存在的全仓 ratchet 基线：`failedWorkspaces: 34`、`diagnostics: 0`，没有把它声明为通过。
- 定向 `vue-tsc` 仍受未构建工作区包解析、测试 globals、`MUpload` 和重复 `PageResult` 等既有错误影响；本次新增 prop、模板和样式没有出现独立诊断。
- `pnpm package-exports:check` 及依赖它的 consumer typecheck 因当前工作区大部分包缺少 `dist` 产物而失败；`@mango/file` 自身 build 已通过，失败清单并非本次文件改动引入。
- 运行时 harness 位于被忽略的 `.runtime`，仅作本地验收证据，不进入版本控制。
