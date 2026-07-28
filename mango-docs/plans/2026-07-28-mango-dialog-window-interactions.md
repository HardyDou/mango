# MangoDialog 浏览器窗口交互标准交付记录

## 1. 元数据

- 任务 ID：MANGO-DIALOG-WINDOW-20260728
- 交付模式：STANDARD
- 需求影响：L2 - 改变 `@mango/common` 公共弹框的遮罩默认关闭语义，并新增可选拖拽、尺寸调整和多实例层级行为。
- 方案风险：L2 - 交互作用于公共组件和浏览器 Pointer Events，需要兼容现有 slot、Element Plus 生命周期、响应式尺寸及多个弹框。
- 最终风险：L2
- 工作区决策：CREATE；`codex/mango-dialog-interactions` / `D:\Project\mango-worktrees\mango-dialog-interactions`

## 2. 目标与范围

- 目标：让通用 `MangoDialog` 在不绑定任何业务内容的前提下，可选支持浏览器内拖拽、四角调整宽高和当前点击实例置顶。
- 成功条件：普通弹框默认保留遮罩且点击遮罩不关闭；拖拽默认无遮罩并可移出视口；显式配置可让拖拽与遮罩共存；尺寸调整响应视口变化；现有 slots 和关闭流程保持兼容。
- 处理范围：`@mango/common` 的 `MangoDialog` 实现、公开类型、组件测试和使用说明。
- 不处理范围：不修改 `baohan-system`、`GuaranteeFilePreviewDialog`、`GuaranteeFilePreviewPanel` 或 `FilePreviewPanel`；不发布 npm 包；不增加最大化、最小化、边缘缩放或内容缩放。

## 3. 可观察系统要求

| ID    | 参与者或入口              | 输入或前置条件               | 预期行为                                       | 失败语义                               | 验收标准                                   |
| ----- | ------------------------- | ---------------------------- | ---------------------------------------------- | -------------------------------------- | ------------------------------------------ |
| AR-01 | 普通 `MangoDialog` 消费者 | 未开启拖拽                   | 保留遮罩，点击遮罩不关闭弹框                   | 误关闭或无遮罩                         | `modal=true`、`closeOnClickModal=false`    |
| AR-02 | 可拖拽弹框消费者          | `draggable=true`             | 标题区拖动整个弹框，默认无遮罩，可移出视口     | 只能拖内容、被视口限制或遮罩仍阻塞页面 | Pointer Events 改变弹框位置，`modal=false` |
| AR-03 | 需要保留遮罩的拖拽消费者  | `draggable=true, modal=true` | 拖拽有效且遮罩保留，点击遮罩不关闭             | 配置冲突或拖拽失效                     | 显式 modal 优先，关闭语义独立              |
| AR-04 | 可调整尺寸弹框消费者      | `resizable=true`             | 四个角可自由改变宽高，每次从真实 DOM 尺寸起算  | 累计误差、错误锚点或出现边缘手柄       | 四角手柄，固定对角，满足最小/最大尺寸      |
| AR-05 | 多弹框消费者              | 同时打开多个无模态弹框       | 按下哪个弹框，哪个获得最高层级                 | 后打开实例永久遮挡其它实例             | 每次交互申请新的 Element Plus z-index      |
| AR-06 | 响应式浏览器              | 视口尺寸缩小                 | 已交互弹框宽高收缩到安全范围，位置不被强制拉回 | 内容溢出或违反允许拖出视口的约束       | 宽高不超过视口安全尺寸，`left/top` 保持    |

## 4. 技术决定

| ID    | 对应要求            | 接口/数据/权限/兼容性决定                                                                                                                    | 影响路径                                      | 回滚方式                                      |
| ----- | ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------- | --------------------------------------------- |
| TD-01 | AR-01~AR-03         | 新增 `modal`、`closeOnClickModal`、`lockScroll` 显式 props；默认普通弹框 `modal=true`、点击遮罩不关闭；拖拽时未显式指定 modal 才默认关闭遮罩 | `MangoDialog/index.vue`、`types.ts`           | 移除新 props 解析并恢复 Element Plus 默认透传 |
| TD-02 | AR-02、AR-04、AR-06 | 使用内部 `useDialogWindow` 和 Pointer Events；首次移动时把自适应 DOM 矩形转成 fixed 布局，关闭后清除                                         | `MangoDialog/useDialogWindow.ts`              | 删除组合逻辑和交互手柄                        |
| TD-03 | AR-05               | 复用 Element Plus `useZIndex()`，标题、内容、footer 或缩放角被按下时提升当前实例；显式 `zIndex` 作为动态层级最低基线                         | `MangoDialog/index.vue`、`useDialogWindow.ts` | 恢复 Element Plus 单次打开层级                |
| TD-04 | AR-04               | 只渲染四角手柄；西侧/北侧缩放同步修改位置，保持对角固定                                                                                      | `MangoDialog/index.vue`                       | 删除 `resizable` 与手柄样式                   |

## 5. 实施清单

| ID    | 对应决定    | 顺序 | 改动路径                                                                          | 完成条件                         |
| ----- | ----------- | ---- | --------------------------------------------------------------------------------- | -------------------------------- |
| IM-01 | TD-01       | 1    | `mango-ui/packages/common/components/MangoDialog/types.ts`、`index.vue`           | props 默认值和组合关系实现       |
| IM-02 | TD-02~TD-04 | 2    | `mango-ui/packages/common/components/MangoDialog/useDialogWindow.ts`、`index.vue` | 拖拽、四角缩放、层级和响应式实现 |
| IM-03 | AR-01~AR-06 | 3    | `MangoDialog.spec.ts`                                                             | 关键组合和交互测试通过           |
| IM-04 | TD-01~TD-04 | 4    | `mango-ui/packages/common/README.md`                                              | 公共 API、默认行为和示例同步     |

## 6. 验收映射与结果

| 要求 ID     | 验证方式           | 命令或步骤                                                                                              | 结果 | 证据                                                                                                             |
| ----------- | ------------------ | ------------------------------------------------------------------------------------------------------- | ---- | ---------------------------------------------------------------------------------------------------------------- |
| AR-01~AR-06 | M10 组件测试       | `pnpm --filter @mango/common test`                                                                      | 通过 | 22 个测试文件、284 条用例全部通过；包含默认遮罩、拖拽、四角缩放、响应式、层级基线、样式透传和卸载清理测试        |
| AR-01~AR-06 | M09 类型与构建     | `pnpm --filter @mango/common build`；`pnpm build`；`pnpm package-exports:check`；消费端 typecheck/build | 通过 | common 和 38 个 workspace 构建通过；包导出检查通过；临时消费项目 `vue-tsc` 与生产构建通过                        |
| AR-01~AR-06 | M09 静态与契约检查 | 定向 ESLint、Stylelint；`pnpm component-contracts:check`                                                | 通过 | ESLint 0 error（测试 stub 保留 3 个 warning）；Stylelint 通过；195 个公开 Vue 导出契约通过                       |
| AR-02~AR-06 | M13 定向浏览器验证 | 独立组件页打开多个弹框，使用真实鼠标验证拖拽、四角缩放、遮罩组合、层级，并以 700×500 视口复验响应式     | 通过 | 拖动至 `-290,-18`；右下与左上缩放对角固定；点击实例置顶；显式 modal 保留且点击不关闭；`902×690` 收缩至 `676×476` |

## 7. 例外与剩余风险

- `baohan-system` 消费适配不在本次 Mango 改动范围；Mango 交付后提供独立修改提示词。
- npm 发布需要用户独立授权，本任务不执行发布。
- `pnpm check:affected` 在当前 Windows 环境中无法由 Node 子进程解析 `pnpm`；等价的 `pnpm check:full` 已完成全部 workspace 构建，但随后同样停在 `check-toolchain-versions.mjs` 的 `spawnSync('pnpm')` 兼容问题。相关构建、包导出、消费端类型检查、组件契约及本次改动的定向静态检查均已独立通过。
- `typecheck:raw` 受同一 Windows 子进程问题影响，将 34 个目标记为 `FAIL (0 diagnostics)` 且错误返回退出码 0；本次公开类型已由 common 构建产物生成和真实 tarball 消费端 `vue-tsc` 验证覆盖。
