# 标准交付记录

## 1. 元数据

- 任务 ID：MANGO-COLLAPSE-DETAIL-20260920
- 交付模式：STANDARD
- 需求影响：L2 - 新增跨业务可复用的详情骨架及文件、富文本公共契约，影响多个 npm 包消费者。
- 方案风险：L2 - 涉及 `@mango/common`、`@mango/file`、新建 `@mango/detail` 的依赖、公开导出和样式/类型发布边界。
- 最终风险：L2
- 工作区决策：CREATE 后 REUSE，任务 worktree 为 `D:/Project/mango-collapse-detail`，分支为 `codex/mango-collapse-detail`。

## 2. 目标与范围

- 目标：将保函系统已经验证的折叠详情展示能力收敛为 Mango 公共组件，并保持包边界清晰、独立消费可用和现有 `MangoDetailPage` 行为不变。
- 成功条件：公开组件、类型、样式和子路径可构建、可类型检查、可由独立消费者引用；组件测试覆盖正常、空、错误、交互和组合场景；保函临时消费者可完成真实页面验证。
- 处理范围：`MangoPageBackBar`、`MangoSideDrawerShell`、`MangoDetailSummary`、`MangoDescriptionList`、`MangoFileList`、`MangoFilePreviewDialog`、`MangoCollapseDetailPage`；增强现有 `RichTextViewer`；复用 `MangoDataTable`；同步 README、公开导出、组件契约、Changeset 和验证记录。
- 不处理范围：不迁移保函工作流业务面板、项目资料业务组件、方案表格、旧 `GuaranteeCompactPageShell`/`GuaranteeDetailPageShell`；不修改或删除现有 `MangoDetailPage`；不提交保函临时验证代码；不发布 npm 包、不合并 PR、不部署。

## 3. 可观察系统要求

| ID     | 参与者或入口           | 输入或前置条件                                                        | 预期行为                                                                      | 失败语义                                                | 验收标准                                               |
| ------ | ---------------------- | --------------------------------------------------------------------- | ----------------------------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------ |
| SR-001 | 通用详情页消费者       | 传入标题、摘要、Tabs 和面板配置                                       | 展示返回栏、摘要、Tab、折叠内容、底部操作区和回到顶部能力                     | 非法面板配置明确抛错，不静默降级                        | 组件测试覆盖布局、插槽、事件和配置错误                 |
| SR-002 | 折叠面板消费者         | `autoExpandPanels=true` 或关闭后传入 `defaultActivePanelNames`        | 默认模式自动展开全部并跟随新增面板；关闭自动展开时只初始化指定 Key            | 自动展开与指定 Key 冲突、Key 为空/重复/不存在时明确抛错 | 组件测试覆盖两种模式和全部非法输入                     |
| SR-003 | 详情页用户             | 页面处于 loading、error、ready 或 empty                               | 状态优先级为 `loading -> error -> ready/empty`；错误态可重试且隐藏底部操作    | 不显示旧内容或无反馈空白                                | 组件测试覆盖四类状态及 `retry` 事件                    |
| SR-004 | 文件列表消费者         | 传入列、行、文件和可选合并配置                                        | 支持多文件、元信息、选择、预览、下载、单元格/动作插槽和相邻行合并             | 文件能力不可用时给出明确反馈，不伪造成功                | 组件测试覆盖基本列表、插槽、选择和资料分组合并         |
| SR-005 | 文件预览消费者         | 传入 `fileId`、`file`、`preview` 或 `textContent`                     | 统一弹框中展示 `FilePreviewPanel`、文本、加载或空状态                         | 缺少可预览内容时显示空状态                              | 组件测试覆盖各输入和关闭事件                           |
| SR-006 | 富文本预览消费者       | 传入普通 HTML、托管文件 token 或受保护图片                            | 安全过滤内容、解析托管文件、展示空值 `-`，并通过公开事件/解析契约支持文件预览 | 解析失败发出可观察错误且不回退到不安全地址              | 单元/组件测试覆盖过滤、受保护资源、点击预览和文本文件  |
| SR-007 | 工作流或扩展内容消费者 | 使用 `workflow`、`toolbar`、`summary`、`actions` 和 custom panel 插槽 | 骨架提供按钮、右侧抽屉和样式能力，业务内容保持外部注入                        | 未提供工作流内容时不引入业务依赖                        | 组件测试验证插槽和 `open/close/toggle` expose          |
| SR-008 | npm 独立消费者         | 仅安装公开包及 peer dependencies                                      | 可从根入口/子路径导入组件和类型，并按 `style.css` 获取运行样式                | 未导出、缺类型或隐式仓库路径依赖时构建失败              | 包构建、exports、consumer typecheck 和组件契约检查通过 |

## 4. 技术决定

| ID     | 对应要求                       | 接口/数据/权限/兼容性决定                                                                                                                                                     | 影响路径                                                                               | 回滚方式                                                   |
| ------ | ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| TD-001 | SR-001, SR-002, SR-003, SR-007 | `MangoCollapseDetailPage` 放在独立 `@mango/detail`，只通过 peer dependency 依赖 `@mango/common` 与 `@mango/file`；不依赖 `@mango/admin`、Admin Shell 或现有 `MangoDetailPage` | `mango-ui/packages/detail/**`                                                          | 删除独立包即可回滚，不影响 Admin 聚合包和现有详情骨架      |
| TD-002 | SR-001, SR-007                 | `MangoPageBackBar`、`MangoSideDrawerShell`、`MangoDetailSummary`、`MangoDescriptionList` 放在 `@mango/common`，只通过 props、emits、slots 和 expose 与宿主协作                | `mango-ui/packages/common/**`                                                          | 删除新增组件和公开导出                                     |
| TD-003 | SR-004, SR-005                 | `MangoFileList`、`MangoFilePreviewDialog` 放在 `@mango/file`，复用 `FilePreviewPanel` 与 Mango 文件 ID/API 契约                                                               | `mango-ui/packages/file/**`                                                            | 删除新增组件和导出，保留既有 `FilePreviewPanel`            |
| TD-004 | SR-006                         | `RichTextViewer` 留在 `@mango/common`，增强安全渲染、资源解析、点击事件和空状态；文件弹框由 `@mango/detail` 组合，禁止 `common -> file` 反向依赖                              | `mango-ui/packages/common/components/RichTextViewer/**`, `mango-ui/packages/detail/**` | 回退 RichTextViewer 增强和组合层适配，不改变文件包基础 API |
| TD-005 | SR-001                         | 内置 `list-table` 复用 `MangoDataTable`，固定 `card=false`、`mode='flat'`、`showModeSwitch=false` 并转发单元格 slots；包样式禁止引入 Admin/Shell 全局样式                     | `mango-ui/packages/detail/**`                                                          | 删除内置渲染分支，公共表格本身不变                         |
| TD-006 | SR-008                         | 每个包同步根导出、必要子路径、`mangoArchitecture.sourceExports`、组件契约、README 与 Changeset；样式跟随所属 package 发布                                                     | 三个 package 与 `.changeset/**`                                                        | 删除对应导出与版本说明                                     |

## 5. 实施清单

| ID       | 对应决定       | 顺序 | 改动路径                                   | 完成条件                                                          |
| -------- | -------------- | ---- | ------------------------------------------ | ----------------------------------------------------------------- |
| IMPL-001 | TD-002         | 1    | `mango-ui/packages/common/components/**`   | 四个基础组件、类型、样式和组件测试完成                            |
| IMPL-002 | TD-003         | 2    | `mango-ui/packages/file/src/components/**` | 文件列表、文件预览弹框及合并场景测试完成                          |
| IMPL-003 | TD-004         | 3    | `RichTextViewer` 与 detail 组合层          | 富文本增强且无循环依赖，文件/文本预览链路有测试                   |
| IMPL-004 | TD-001, TD-005 | 4    | `mango-ui/packages/detail/**`              | 独立包、完整骨架、面板渲染、状态、插槽、expose 和样式隔离测试完成 |
| IMPL-005 | TD-006         | 5    | package 配置、README、组件契约、Changeset  | 公开说明与实现一致，导出检查通过                                  |
| IMPL-006 | 全部           | 6    | Mango 定向/全局门禁与保函临时消费者        | 自动化验证通过，人工验收入口可用且保函正式仓无提交                |

## 6. 验收映射与结果

| 要求 ID                                        | 验证方式             | 命令或步骤                                                                                                                                         | 结果 | 证据                                                                                         |
| ---------------------------------------------- | -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | ---- | -------------------------------------------------------------------------------------------- |
| SR-001, SR-002, SR-003, SR-006, SR-007         | M10 组件测试         | `pnpm --filter @mango/detail test`                                                                                                                 | PASS | 3 个测试文件、8 项测试通过                                                                   |
| SR-004, SR-005                                 | M10/M11 组件协作测试 | `pnpm --filter @mango/file test`                                                                                                                   | PASS | 9 个测试文件、47 项测试通过；Happy DOM 对 iframe 禁止加载的日志不影响断言                    |
| SR-006                                         | M10 组件测试         | `pnpm --filter @mango/common test`                                                                                                                 | PASS | 36 个测试文件、364 项测试通过，包含 RichTextViewer 新增场景                                  |
| SR-008                                         | M09 静态验证         | `common/file/detail` 三个包 build、detail typecheck、`package-exports:check`、`component-contracts:check`、consumer typecheck                      | PASS | 三包构建、detail typecheck、公开导出、组件合同及 tarball 独立消费者 `vue-tsc` 和生产构建通过 |
| SR-001 至 SR-008                               | M09 样式治理         | `pnpm admin:styles:check`、`pnpm admin:module-styles:check`                                                                                        | PASS | 19 个 package style export 与 13 个 official module 检查通过                                 |
| SR-001, SR-003, SR-004, SR-005, SR-006, SR-007 | M13 UI 验证          | 在保函任务专用临时 worktree 中执行候选预览页 Playwright：`pnpm -C baohan-ui exec node e2e/scripts/mango-collapse-detail-candidate-browser-e2e.mjs` | PASS | 1440/2560/390 三档通过；宿主布局、菜单收放、骨架组合交互及 console/network 见验收证据目录    |
| SR-001 至 SR-008                               | M16 人工验收         | 用户在保函隔离环境的候选预览页完成人工走查                                                                                                         | PASS | 用户于 2026-09-20 反馈页面未发现问题                                                         |

## 7. 例外与剩余风险

- 当前从尚未合并的 `codex/mango-data-table` 分支堆叠开发；表格 PR 合并后需要将本分支同步到最新 `main` 并重新执行全部门禁。
- 保函系统本次仅作为临时消费者验证，不形成其正式消费关系；临时 worktree 和代码在验收结束后清理。
- `@mango/file` 源码 typecheck 的既有失败为测试未引入 Vitest globals、`MUpload.vue` 的 `Error`/`UploadAjaxError` 不匹配和根入口 `PageResult` 重复导出；本次新增代码必须单独证明无新增类型诊断。
- 全仓 ESLint/Prettier ratchet 仍被任务范围外的 `admin-shell`、`workflow` 等既有诊断阻断；本次新增/修改组件路径的 ESLint、Stylelint、Prettier 定向检查均通过，全仓 Stylelint 通过。
- `catalog:check` 和 `check:affected` 的默认入口在 Windows 分别受 `spawnSync('mvn')`、`spawnSync('corepack')` 无法启动 `.cmd` 影响；catalog 已通过脚本支持的 `--effective-pom` 参数完成等价验证，`check:affected` 的 `quality:versions` 工具链异常保留为未通过项。
- 用户已授权提交本次任务 PR；未授权 PR 合并、npm 发布或部署。

## 8. M13 候选预览页验收证据

旧证据判定为无效：旧脚本只验证了骨架内部内容和交互，没有断言 `.mango-layout-body` 的布局方向、侧栏与内容顶部位置，以及菜单展开前后内容纵坐标稳定性。临时消费者替换整个 `@mango/admin@1.1.10` 后，与仍在运行的 `@mango/admin-shell@1.0.72` 形成双版本样式/运行时代码错配，实际页面已出现侧栏把内容向下推的回归。

拆分为 `@mango/detail` 后，保函临时消费者只替换 `@mango/common`、`@mango/file` 与 `@mango/detail`，保持其既有 `@mango/admin@1.1.8` 和 `@mango/admin-shell@1.0.72` 不变。M13 已在 1440x1000、2560x1440、390x844 三档视口重新执行：桌面端 `.mango-layout-body` 均为横向 flex，侧栏与内容顶部对齐；菜单宽度在 220px 与 64px 之间切换时页签和详情内容纵坐标不变；移动端无横向溢出；console error、page error 和失败请求均为 0。证据位于 `mango-docs/evidence/2026-09-20-mango-collapse-detail/`。

### 8.5 未验证项和风险

| 项目               | 原因                                                           | 影响                                              | 后续处理                                                         | 用户确认                 |
| ------------------ | -------------------------------------------------------------- | ------------------------------------------------- | ---------------------------------------------------------------- | ------------------------ |
| 保函真实业务入口   | 保函本次只作为临时候选包消费者，按已确认范围不形成正式消费关系 | 不证明现有保函业务页已经切换到 Mango 组件         | 保函临时代码在验收结束后清理；后续消费项目按自身入口验收         | 已确认不纳入本次正式交付 |
| npm 发布物         | 本次未获授权发布                                               | 外部消费者暂时无法从 registry 获取这些版本        | 完成 PR、合并和独立发布流程后再回查制品                          | 未授权发布               |
| 保函全量 typecheck | 仓库既有页面存在与本次适配器无关的类型诊断                     | 不能用全量 typecheck 作为保函临时消费者的全绿结论 | 保留既有错误清单；本次适配器无新增诊断，生产构建和边界检查已通过 | 待原业务任务处理         |

### 8.6 业务开发交接输出

| 输出对象         | 交接内容                                          | 材料路径                                                                                                        | 执行入口                                                                                  | 数据/账号边界                                            | 失败/例外处理                                                          | 状态 |
| ---------------- | ------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- | -------------------------------------------------------- | ---------------------------------------------------------------------- | ---- |
| Mango 组件消费者 | 公共组件、类型、slots、events、样式入口和组合示例 | `mango-ui/packages/common/README.md`、`mango-ui/packages/file/README.md`、`mango-ui/packages/detail/README.md`  | 从公开 package 入口导入并引入对应 `style.css`                                             | 消费方提供业务数据、权限和远程能力；组件不保存宿主 token | 缺少输入或非法配置时按公开契约展示空/错误态或明确抛错                  | DONE |
| 保函临时验收     | 候选包适配器、DEV 预览页和可重复 Playwright 脚本  | `D:/Project/baohan-mango-collapse-detail/baohan-ui/e2e/scripts/mango-collapse-detail-candidate-browser-e2e.mjs` | `pnpm -C baohan-ui exec node e2e/scripts/mango-collapse-detail-candidate-browser-e2e.mjs` | 仅使用隔离数据库和测试账号；不连接共享业务库             | 脚本失败时读取 `result.json`、截图和 `trace.zip`，不得提交临时保函代码 | DONE |
