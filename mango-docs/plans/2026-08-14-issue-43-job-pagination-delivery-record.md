# 标准交付记录

> 任务：Job 管理分页链路修复（baohan-open Issue #43）

## 1. 元数据

- 任务 ID：baohan-open Issue #43
- 交付模式：STANDARD
- 需求影响：L2 - `@mango/job` 的四个列表页无法渲染分页器，数据超过一页后用户无法访问后续记录
- 方案风险：L2 - 修复公共 npm 包内组件契约和前后端分页参数适配，并修复 Windows 质量检查与 CLI 后台进程链路；影响五个分页查询入口和本地质量/启动命令，但不改变后端接口
- 最终风险：L2
- 工作区决策：CREATE（`D:\Project\mango-job-pagination`，`codex/fix-job-pagination`）
- 启用能力：M01、M08、M09、M10、M11、M13

## 2. 目标与范围

- 目标：恢复任务定义、执行实例、Worker 节点和告警规则列表的分页展示与翻页能力。
- 成功条件：四个列表页能解析并渲染公共 `Pagination`；切换页码和每页数量后使用后端 `page/size` 契约查询；返回的 `page/size` 能映射回前端分页结果；不再出现 `Failed to resolve component: Pagination`。
- 处理范围：`@mango/job` 四个列表页、Job API 分页参数与响应适配、定向回归测试；以及验证时实际阻塞 Windows `typecheck`、Mango CLI 启动的 runner 兼容问题。
- 不处理范围：后端分页接口、数据库结构、任务调度逻辑、搜索后自动回到第一页等独立交互优化、npm 发布与下游依赖升级。

## 3. 可观察系统要求

| ID      | 参与者或入口               | 输入或前置条件                        | 预期行为                                            | 失败语义                                        | 验收标准                             |
| ------- | -------------------------- | ------------------------------------- | --------------------------------------------------- | ----------------------------------------------- | ------------------------------------ |
| REQ-001 | 管理员进入四个 Job 列表页  | 已登录并拥有对应列表权限              | 页面渲染公共分页器且控制台没有 Pagination 解析警告  | 分页器缺失或出现组件解析警告                    | SFC 编译回归、生产构建和页面检查通过 |
| REQ-002 | 管理员切换页码或每页数量   | 分页器可操作                          | 前端发送 `page/size`，页面使用返回结果刷新列表      | 始终请求第一页或请求参数仍为 `pageNum/pageSize` | API 单测和真实页容量交互通过         |
| REQ-003 | Job API 消费者读取分页结果 | 后端返回 `list/total/page/size/pages` | 对外结果保持 `list/total/pageNum/pageSize` 兼容结构 | 页码元数据回退为请求值或默认值                  | API 单测验证响应映射                 |

## 4. 技术决定

| ID      | 对应要求         | 接口/数据/权限/兼容性决定                                                                                                                                                       | 影响路径                                         | 回滚方式                              |
| ------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ | ------------------------------------- |
| DEC-001 | REQ-001          | 四个页面显式导入 `@mango/common` 的 `Pagination`，统一使用 `page/limit/pagination` 公共组件契约                                                                                 | `mango-ui/packages/job/src/views/**/index.vue`   | 回退四个页面改动                      |
| DEC-002 | REQ-002、REQ-003 | 保留公开前端 `pageNum/pageSize` 类型，在 Job API 边界统一转换为后端 `page/size`，并优先读取返回的 `page/size`                                                                   | `mango-ui/packages/job/src/api/job.ts`           | 回退 API 适配和对应测试               |
| DEC-003 | 全部             | 使用真实 Vue SFC 编译器和 mock HTTP 边界的 Vitest 定向覆盖，不 mock 被测转换逻辑                                                                                                | `mango-ui/packages/job/src/**/__tests__`         | 删除新增测试入口并恢复原 package 配置 |
| DEC-004 | 全部             | Windows 命令 shim 先解析绝对路径并整体引用参数，环境变量名按大小写不敏感读取；后台启动由 detached Node runner 托管 `.cmd`，失败的非强制 `taskkill` 立即升级为整棵进程树强制停止 | `mango-ui/packages/mango-cli/src`                | 回退 CLI runner、进程控制及回归测试   |
| DEC-005 | 全部             | `typecheck` 在 Windows 使用 `vue-tsc.cmd` 和 shell；同步修正 Job 参数编辑器的真实类型诊断                                                                                       | `mango-ui/scripts/quality`、`JobParamEditor.vue` | 回退 runner 和参数编辑器类型修复      |

## 5. 实施清单

| ID      | 对应决定 | 顺序 | 改动路径                               | 完成条件                                     |
| ------- | -------- | ---: | -------------------------------------- | -------------------------------------------- |
| IMP-001 | DEC-001  |    1 | 四个 Job 列表页                        | 编译产物不再运行时解析 `Pagination`          |
| IMP-002 | DEC-002  |    2 | `mango-ui/packages/job/src/api/job.ts` | 五个分页请求使用 `page/size` 且响应映射正确  |
| IMP-003 | DEC-003  |    3 | Job 包测试、package scripts 和本记录   | 定向测试、静态检查、构建及页面验证有真实结果 |
| IMP-004 | DEC-004  |    4 | Mango CLI Windows 命令与进程控制       | CLI 完整套件覆盖打包、启动、日志、重启和停止 |
| IMP-005 | DEC-005  |    5 | typecheck runner、Job 参数编辑器       | 全量 ratchet 与 Job 定向 typecheck 通过      |

## 6. 验收映射与结果

| 要求 ID                   | 验证方式         | 命令或步骤                                                                                                                                                                              | 结果            | 证据                                                                                                                   |
| ------------------------- | ---------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------- | ---------------------------------------------------------------------------------------------------------------------- |
| REQ-001、REQ-002、REQ-003 | M10 定向测试     | `pnpm -C mango-ui --filter @mango/job test`                                                                                                                                             | PASS            | 2 个测试文件、9 条用例全部通过；覆盖 5 个分页 API、空响应回退、请求失败传播和 4 个 SFC 页面                            |
| REQ-001、REQ-002、REQ-003 | M09 生产构建     | `pnpm -C mango-ui --filter "@mango/job..." build`                                                                                                                                       | PASS            | Job 及 11 个 workspace 依赖构建成功                                                                                    |
| 全部                      | M10 测试质量     | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`                                                                                                                        | PASS            | `Test quality PASS: 7 file(s)`                                                                                         |
| 全部                      | M09 typecheck    | `pnpm -C mango-ui typecheck`；`pnpm -C mango-ui exec vue-tsc --noEmit --incremental false -p packages/job/tsconfig.json`                                                                | PASS            | 全量 ratchet 通过（22 个 workspace、643 条存量诊断，未突破基线）；Job 定向检查 0 诊断                                  |
| 全部                      | M09 lint/format  | `pnpm -C mango-ui lint`；`pnpm -C mango-ui format:check`                                                                                                                                | PASS（ratchet） | ESLint：fatal 0、errors 221、warnings 863；Prettier：425 files，均未突破基线                                           |
| 全部                      | M10/M11 CLI 回归 | `pnpm -C mango-ui --filter @mango/cli test`                                                                                                                                             | PASS            | 80 项中 78 项通过、2 项 Unix-only 在 Windows 跳过；覆盖打包、命令参数、真实 Maven Reactor、后台日志、重启和进程树停止  |
| 全部                      | M10 runner 回归  | `node --test mango-ui/scripts/quality/typecheck-runner.test.mjs mango-ui/packages/mango-cli/tests/platform-command.test.mjs mango-ui/packages/mango-cli/tests/process-control.test.mjs` | PASS            | 17 项中 15 项通过、2 项 Unix-only 在 Windows 跳过                                                                      |
| 全部                      | M09 样式治理     | `pnpm -C mango-ui admin:styles:check`；`pnpm -C mango-ui admin:module-styles:check`                                                                                                     | PASS            | 18 个 package 样式导出和 12 个官方模块治理检查通过                                                                     |
| REQ-001、REQ-002          | M09 页面基线     | `node mango-pmo/tools/check-frontend-page-baseline.mjs --base origin/main --head HEAD --frontend-root mango-ui`                                                                         | PASS            | 检查 5 个变更 view 文件（四个列表页与参数编辑器）；按页面领域语义登记窄范围 list/dialog 例外                           |
| REQ-001、REQ-002          | M13 页面验证     | Mango CLI 启动 workspace `mango_070`，浏览器登录后检查四个 Job 页面并切换任务定义每页数量                                                                                               | PASS            | 四页均显示分页器；任务定义 10→20 条/页成功；实例/告警空态正常；Worker 数据正常；console error 0，Pagination 解析错误 0 |

### 6.1 验证环境与数据边界

- 验证时间：2026-08-14 至 2026-08-15（Asia/Shanghai）。
- 最终验证基线：`origin/main@864271423`；任务分支 `codex/fix-job-pagination`。任务最初基线为 `222ef3d0f`，提交前已 fast-forward 合并最新 `main`。
- 工具版本：Windows、Node.js `22.23.1`、pnpm `11.14.0`、Vitest `4.1.10`、Vue SFC compiler `3.5.13`。
- 运行 workspace：`mango_070`；前端 `http://127.0.0.1:30070`，后端 `http://127.0.0.1:18070`，隔离 MySQL `127.0.0.1:13370`，数据库 `mango_dev_mango_job_pagination_070`。
- 自动化数据：API 单测只在 HTTP 请求边界使用 mock，不替代参数转换和响应映射逻辑；未写入真实数据库。
- CLI 集成数据：完整业务生命周期和 Maven Reactor 测试使用系统临时目录生成独立 fixture；测试结束后停止进程并清理 fixture，不向本地 Maven 仓库安装 workspace 业务制品。
- 账号与租户：使用本地初始化管理员 `admin`、租户“芒果集团”；记录不保存密码、token 或 Cookie。
- 页面证据：四个页面均通过真实菜单进入；任务定义截图保存于本地忽略目录 `.runtime/issue-43-job-pagination.png`，不作为仓库长期资产。
- 数据边界：隔离库只有 1 条任务定义、0 条执行实例、2 条 Worker 和 0 条告警规则；因此 M13 验证了分页器、页容量切换、边界禁用与空态，未伪造第二页数据。`page/size` 和页码响应映射由 9 条定向 API/SFC 回归测试证明。

### 6.2 文档与能力说明质量

| 检查对象      | 命令                                                                                                                                      | 结果                                                    |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------- |
| STANDARD 结构 | `node mango-pmo/tools/check-standard-delivery-record.mjs mango-docs/plans/2026-08-14-issue-43-job-pagination-delivery-record.md`          | PASS                                                    |
| Markdown 格式 | `pnpm -C mango-ui exec prettier --check ../mango-docs/plans/2026-08-14-issue-43-job-pagination-delivery-record.md packages/job/README.md` | PASS                                                    |
| 文档站点      | `npm ci && npm run docs:build`（工作目录 `mango-docs`）                                                                                   | PASS；VitePress 成功渲染，存在非阻断 chunk size warning |
| 模块 README   | `node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs`                                     | PASS；结构、链接、API 路径、package 和页面 key 均为 OK  |
| 业务指南      | `node mango-pmo/tools/check-business-guides.mjs`                                                                                          | PASS                                                    |

- `@mango/job` README 已同步说明公开分页参数、后端传输参数和响应映射边界。
- `@mango/cli` README 已同步 Windows `.cmd` 后台 runner、日志继承、参数传递、进程树停止语义和排障入口；没有新增命令、参数或配置。
- 能力地图不更新：本次恢复既有 Job 分页与 CLI 开发编排能力，没有新增能力入口或组合阅读顺序。业务集成指南不更新：依赖、注册、菜单、权限、租户和启动步骤均未变化。
- PMO 规则及 `mango-pmo/rules/index.json` 不更新：本次没有新增或修改长期规则。
- 本次没有新增 endpoint、类型或配置；属于既有分页能力修复。仍需发布新的 npm 版本并由 `baohan-open` 升级后，业务环境才能获得修复。

## 7. 例外与剩余风险

- 隔离库数据未超过一页，真实“下一页”按钮按边界保持禁用；页面容量切换已通过，页码/容量请求和响应映射由定向单测覆盖。发布后建议在下游含多页数据的环境补一次非阻断验收。
- 公共 `Pagination` 当前会输出 Element Plus `small` 属性将在 3.0 废弃的 warning；四个 Job 页面没有 console error 或组件解析错误，本任务不扩大到公共组件迁移。
- Windows 下首次 Mango CLI 冷启动需要执行完整 Maven Reactor bootstrap/verify，耗时约 4 分钟；已验证前后端最终健康，但仍属于本地启动成本。
- 四个 Job 页面保留现有领域操作台布局，没有在本 PR 迁移 `MangoListPage`/`MangoSearchPanel`/`MangoListPanel`；任务定义、Worker 和告警编辑弹框也保留领域联合表单。源码已按具体页面语义登记 `mango-page-baseline-exception`，后续若统一页面骨架需重新执行完整 UI 验收。
- `npm ci` 报告 VitePress 依赖树存在 2 个 moderate、3 个 high 漏洞；属于当前文档站依赖基线，本任务未升级依赖，文档构建仍通过。
- npm 发布和 `baohan-open` 依赖升级不在本次代码修复授权范围内，源码修复完成后仍需独立发布流程才能进入业务环境。
