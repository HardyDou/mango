# 前端页面最新组件基线治理设计

## 1. 背景与目标

2026-07-08 已发布 `MangoListPage`、`MangoSearchPanel`、`MangoListPanel`、`MangoDetailPage`、
`MangoFormPage` 和 `MangoPageSection`，但组件公开合同、平台示例、业务模板、PMO 规则和机器门禁没有形成同一条默认路径。

当前事实：

- Mango 平台源码中有 78 个包含表格的页面，运行时页面没有采用列表页骨架；只有组件演示页采用。
- 31 个包含详情描述的页面没有采用 `MangoDetailPage`，57 个页面仍直接使用 `ElDialog`，只有应用管理使用 `MangoDialog`。
- Business Starter 和 CLI 已使用列表页骨架，但新增/编辑仍直接使用 `ElDialog`。
- 页面骨架组件仍登记在 `legacyComponentExports`，没有形成 C4 业务项目公共组件合同。
- 业务项目 PMO required check 没有前端页面基线检查，规则只能提示，不能阻断新增旧骨架。

目标是让新业务页面和被修改的列表页默认使用当前公共组件，保持存量债务只减不增，并让特殊页面仍能使用与页面语义匹配的 Element Plus 原生能力。

## 2. 范围

本次包含：

- 页面骨架与弹框公共组件的 C4 合同、文档和测试证据。
- PMO 页面规则与 Business Starter PMO baseline。
- Business Starter canonical CRUD 页面与 Mango CLI 投影。
- Mango 主仓和生成业务项目的增量页面基线检查。
- 能力地图、Common/Starter/CLI 使用说明。

本次不包含：

- 一次性重写 Mango 平台全部存量页面。
- 新增 `MangoDrawer` 或改变 Element Plus Drawer 的使用语义。
- npm、CLI、Maven 或其它制品发布；提交、Push、PR 和合并在用户后续明确授权后作为本任务收尾执行。

## 3. 风险与交付模式

- 需求影响：L2。改变业务项目新增和修改页面的默认公共组件合同与 CI 准入。
- 方案风险：L3。修改 PMO 规则、公共组件 C4 分类、starter/CLI 生成源和治理检查自身。
- 最终风险：L3，使用 FULL 治理流程。
- 工作区：M01=CREATE，`governance/frontend-page-baseline`。

## 4. 决策

### 4.1 页面类型与默认组件

| 页面语义 | 默认组件 | 例外边界 |
| --- | --- | --- |
| 管理列表页 | `MangoListPage`、`MangoSearchPanel`、`MangoListPanel`、`Pagination` | 不含列表语义的局部表格不按列表页处理 |
| 独立详情页 | `MangoDetailPage`、`MangoPageSection` | 列表上下文中的短详情可以使用 Drawer |
| 独立表单页 | `MangoFormPage`、`MangoPageSection` | 短表单可以使用 Dialog |
| 标准弹框 | `MangoDialog` | 第三方组件强依赖或特殊原生能力必须保留可复核原因 |
| 抽屉 | Element Plus Drawer | 当前没有统一 `MangoDrawer`，不得用其它组件假冒 |

### 4.2 公共组件合同

上述 8 个组件按 C4 登记：npm 分发、host-agnostic、browser-only，支持 monolith 和 microfrontend。根具名导出与深路径默认导出属于同一组件的两个公开入口，均登记到合同并引用相同 README 和测试证据。

### 4.3 默认生成源

`mango-business-starter` 继续作为业务模块模板唯一源，Mango CLI 只保留完全一致的投影。CRUD 列表页继续使用列表四件套，短表单升级到 `MangoDialog`，短详情继续使用 Drawer。模板必须带页面、区域、动作和字段语义锚点。

### 4.4 增量门禁

新增或修改的业务 `views/**/*.vue` 包含列表表格时，必须同时出现列表四件套；新增或修改独立详情/表单页时必须使用对应页面外壳；新增或修改标准弹框出现原生 `ElDialog` 时失败并要求使用 `MangoDialog`。检查只处理本次变更的页面，不因未改存量页面阻断任务；删除旧标识或迁移页面会自然减少债务。

业务项目 GitHub/Gitea PMO workflow 根据 `mango.config.json.paths.frontend` 运行同一 checker。Mango 主仓前端 workflow 对 `mango-ui` 运行同一 checker，避免规则、模板和平台代码出现两套判定。

## 5. 失败与恢复

- checker 无法读取 base/head、frontend 根目录越界或 Git diff 失败时 fail-closed。
- 模板源和 CLI 投影不一致时现有 SHA-256 门禁继续阻断。
- C4 registry、README、导出或测试证据不一致时 component contract checker 阻断。
- 回退时可整体回退本治理批次；不得只删除 checker 而保留声明为“必须”的规则。

## 6. 验收

- AC-001：8 个页面公共组件不再出现在 legacy registry，根导出和深路径导出均有 C4 合同。
- AC-002：Common README 的 `MangoSearchPanel` 默认值与源码一致，并提供列表、详情、表单、弹框示例。
- AC-003：Starter 与 CLI 生成页面使用列表四件套和 `MangoDialog`，投影检查通过。
- AC-004：PMO 主规则与业务 baseline 同步，详情、表单和弹框规则明确默认组件。
- AC-005：正例列表页通过；缺少列表骨架、详情/表单外壳或新增原生 Dialog 的反例失败；未改存量页面不被阻断。
- AC-006：GitHub/Gitea 业务 workflow 对前端变更产生可审计的页面基线检查结果。
