# 标准交付记录

任务：为 `@mango/common` 提供统一数据表格、单元格、状态文本和分页能力。

## 1. 元数据

- 任务 ID：MANGO-DATA-TABLE-20260916
- 交付模式：STANDARD
- 需求影响：L2 - 新增稳定公共组件 API、主题变量和包导出，供 Mango 业务项目统一消费。
- 方案风险：L2 - 涉及 Element Plus 表格交互、分页请求去重、主题覆盖、类型产物和跨项目消费契约。
- 最终风险：L2
- 工作区决策：CREATE - `D:\Project\mango-data-table`，分支 `codex/mango-data-table`，开发基线为 `origin/main@5c1f94d22`，提交前已合入 `origin/main@e277d0623`
- 启用能力：M01、M08、M09、M10、M11、M13

## 2. 目标与范围

- 目标：在 `@mango/common` 建立唯一的统一数据表格实现，提供 `MangoDataTable`、`MangoTableCell`、`MangoStatusText`，并增强现有 `Pagination`。
- 成功条件：组件通过公开 props、emits 和 slots 提供能力；卡片形态可动态切换；状态色跟随 Mango 主题；标准主题表头保持 `#eef1f5` 背景、`#000000` 文字和 `600` 字重；分页大小变化只产生一次业务分页事件；包导出、类型、组件合同、文档和 Changeset 完整。
- 处理范围：`@mango/common` 组件、主题变量、测试、包导出、组件合同、README、能力地图、Changeset 和本交付记录。
- 不处理范围：不修改保函仓；不迁移保函页面；不发布 npm 包；不改业务字段、状态判断、接口、路由、权限或后端；不处理旧页面 Shell。

## 3. 可观察系统要求

| ID    | 参与者或入口                    | 输入或前置条件                                     | 预期行为                                                                                    | 失败语义                                           | 验收标准                                                                     |
| ----- | ------------------------------- | -------------------------------------------------- | ------------------------------------------------------------------------------------------- | -------------------------------------------------- | ---------------------------------------------------------------------------- |
| SR-01 | 业务列表页使用 `MangoDataTable` | 传入行、列和稳定 `rowKey`                          | 渲染表格、序号、选择、操作、空态、loading，并允许具名 slot 覆盖单元格；表头读取主题语义变量 | 缺少数据时显示可配置空态；加载失败时显示明确错误   | 组件单测覆盖主渲染、公开事件和状态分支；真实页面 computed style 符合标准主题 |
| SR-02 | 同一表格在页面或嵌套区域使用    | 切换 `card`                                        | `true` 使用列表卡片表面，`false` 移除卡片背景、边框、阴影和内边距                           | 不允许依赖两套组件实现                             | 单测验证两个形态且实现路径唯一                                               |
| SR-03 | 消费方配置列类型                | text/status/tag/button/input/select/radio/custom   | `MangoTableCell` 统一格式化、空值、状态、编辑和自定义渲染                                   | 无匹配数据时显示明确空值；编辑只在值变化后发出事件 | 单元测试覆盖格式化、嵌套值和编辑事件                                         |
| SR-04 | 消费方展示状态文本              | 传入 `primary/success/warning/danger/info/neutral` | 文案由消费方提供，颜色只从 Mango 主题语义变量读取                                           | 未传 tone 时使用 neutral，不推断业务状态           | 单测、主题源码审计和可覆盖变量验证通过                                       |
| SR-05 | 消费方使用分页                  | 页码、页大小、总数及对齐方式变化                   | 保持现有 `page/limit` 双向绑定；同一轮页大小联动最多发出一次 `pagination`                   | 不重复触发业务加载；禁用态透传                     | 分页单测覆盖对齐、更新事件和微任务去重                                       |
| SR-06 | npm 消费项目                    | 从根入口或稳定子路径导入组件和类型                 | 构建产物、声明文件、exports 与组件合同一致                                                  | 缺失导出或类型时质量门禁失败                       | build、package exports、consumer typecheck、component contract 检查通过      |

## 4. 技术决定

| ID    | 对应要求     | 接口/数据/权限/兼容性决定                                                                                                                                         | 影响路径                                           | 回滚方式                                 |
| ----- | ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- | ---------------------------------------- |
| TD-01 | SR-01、SR-02 | `MangoDataTable` 使用扁平 props、typed emits 和 slots；内部复用 `MangoListPanel`，由单一 `card` 参数切换表面；表头通过主题变量取色并为旧主题包保留标准色 fallback | `packages/common/components/MangoDataTable`、theme | 回退新增组件及导出，不保留第二套公共实现 |
| TD-02 | SR-03、SR-04 | 公共列模型不包含保函业务语义；状态 tone 为 Mango 自有联合类型；颜色通过 `--mango-color-status-*` 解析                                                             | `MangoTableCell`、`MangoStatusText`、theme         | 回退新增组件和主题变量                   |
| TD-03 | SR-05        | 保留 Pagination 的 `page/limit` API，通过微任务合并 Element Plus 同轮 page/limit 更新，并新增 `align`                                                             | `components/Pagination`                            | 回退分页增强提交，恢复原公开 API 行为    |
| TD-04 | SR-06        | 根入口和组件子路径同时导出，更新 `mangoArchitecture.sourceExports`、组件合同、README、能力地图和 Changeset；Changeset 同时升级 Common 与精确依赖 Common 的 Admin  | package metadata 与文档                            | 回退对应导出、合同和文档变更             |

## 5. 实施清单

| ID    | 对应决定 | 顺序 | 改动路径                                                                                          | 完成条件                                                       |
| ----- | -------- | ---: | ------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| IM-01 | TD-02    |    1 | `packages/common/components/MangoStatusText`、主题文件                                            | tone 类型、主题变量和单测完成                                  |
| IM-02 | TD-02    |    2 | `packages/common/components/MangoDataTable/MangoTableCell.vue`、`types.ts`                        | 通用单元格渲染和编辑事件测试通过                               |
| IM-03 | TD-01    |    3 | `packages/common/components/MangoDataTable/index.vue`                                             | 表格状态、卡片模式、操作、展开和公开事件测试通过               |
| IM-04 | TD-03    |    4 | `packages/common/components/Pagination`                                                           | 兼容 API、对齐和事件去重测试通过                               |
| IM-05 | TD-04    |    5 | `index.ts`、`package.json`、`component-contracts.json`、类型生成脚本、README、能力地图、Changeset | 发布面元数据、子路径产物与源码事实一致                         |
| IM-06 | 全部     |    6 | 定向测试、构建和仓库质量门禁                                                                      | M09/M10/M11 通过；M13 在保函独立消费 worktree 完成真实页面验证 |

## 6. 验收映射与结果

| 要求 ID                           | 验证方式                   | 命令或步骤                                                                                                                                                                                  | 结果    | 证据                                                                                                                                                                                       |
| --------------------------------- | -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| SR-01、SR-02、SR-03、SR-04、SR-05 | M10 组件单元测试           | `pnpm --filter @mango/common test`                                                                                                                                                          | PASS    | 32 test files、354 tests passed                                                                                                                                                            |
| SR-06                             | M09 类型、构建与发布面检查 | `pnpm --filter @mango/common build`、`pnpm component-contracts:check`、`pnpm package-exports:check`、`pnpm package-consumer:typecheck -- --reuse-build`                                     | PASS    | 构建、契约、exports 通过；候选 tarball 在全新消费者中安装后，`vue-tsc` 与生产构建通过                                                                                                      |
| SR-01 至 SR-06                    | M09 受影响范围和样式治理   | `pnpm admin:styles:check`、`pnpm admin:module-styles:check`、`pnpm stylelint`、定向 ESLint/Prettier、`test-quality-check.mjs --base origin/main`、`pnpm check:affected`、`git diff --check` | PARTIAL | admin styles、module styles、Stylelint、定向 ESLint/Prettier、4 个测试文件质量检查、diff check 通过；`check:affected` 被仓库 Windows 工具链门禁阻断                                        |
| SR-06                             | M11 Changeset 闭包检查     | `pnpm release:change-check -- --include-working-tree`                                                                                                                                       | PASS    | 正确识别 `@mango/common` 直接影响及其完整发布闭包                                                                                                                                          |
| SR-06                             | M11 发布版本投影检查       | `pnpm release:impact -- --base=origin/main --head=HEAD`                                                                                                                                     | BLOCKED | Changeset 开发分支未直接提升发布闭包内各包版本，检查按设计报告版本尚未投影；需在正式发布版本投影阶段执行                                                                                   |
| SR-06                             | 文档门禁                   | `check-standard-delivery-record.mjs`、`audit-module-readmes.mjs`、`audit-readme-source-facts.mjs`、`check-business-guides.mjs`                                                              | PASS    | STANDARD 交付记录、模块 README、README 源码事实和业务指南检查均通过                                                                                                                        |
| SR-01、SR-02、SR-04、SR-05        | M13 真实入口验证           | 在独立保函消费 worktree 安装候选包并执行项目 Playwright：`BAOHAN_UI_URL=http://127.0.0.1:30134 node e2e/scripts/guarantee-list-table-card-option-browser-e2e.mjs`                           | PASS    | 表头 computed style 为背景 `rgb(238, 241, 245)`、文字 `rgb(0, 0, 0)`、字重 `600`；console error、page error、失败请求均为 0；证据见保函 worktree `baohan-ui/.runtime/evidence/table-card/` |

## 7. 例外与剩余风险

- 当前阶段不发布 `@mango/common`，保函主仓仍使用已锁定的 `2.0.2`，不会获得本次能力。
- M13 真实业务页面证据已在独立保函任务 worktree 中完成；人工业务验收仍需用户确认，确认前不声明跨项目迁移交付完成。
- 用户已明确授权本地提交；本次授权不包含 Push 或创建 PR。
- 全仓质量命令的既有环境/基线问题已单独记录：`pnpm lint`、`pnpm typecheck` 和 `pnpm format:check` 的新增诊断均位于本任务之外的主分支路径；`pnpm check:affected` 最终在 Windows `quality:versions` 中因进程输出参数异常失败。本次变更文件的定向 ESLint、Stylelint、Prettier、公共包构建与独立消费者验证均通过。
- `pnpm release:impact` 是正式版本投影后的门禁，不应通过在功能分支手工修改全部发布闭包版本来规避；当前以通过 `release:change-check -- --include-working-tree` 证明 Changeset 声明和闭包完整。
