# 标准交付记录

任务：PR #758 文件预览 loading 状态与文件管理页统一骨架迁移。

## 1. 元数据

- 任务 ID：GitHub PR #758
- 交付模式：STANDARD
- 需求影响：L1 - 文件预览加载期间新增明确 loading，失败时展示失败态，不改变文件数据和后端接口合同
- 方案风险：L2 - 预览异步生命周期调整，并将文件管理页搜索、列表、分页和标准弹框迁移到共享 UI 骨架
- 最终风险：L2
- 工作区决策：REUSE（`D:\Project\mango-file-preview-loading`，`codex/file-preview-loading`）
- 启用能力：M01、M02、M08、M09、M10、M11、M13、M15、M16

## 2. 目标与范围

- 目标：让文件预览的元数据、受保护内容和文档预览地址在加载期间统一显示 Element Plus 默认 loading，并使本次改动触达的文件管理页符合 Admin UI 页面基线。
- 成功条件：预览请求开始后可观察到 loading；请求成功后展示对应预览内容；请求失败后展示失败态；关闭或切换预览不会遗留对象 URL 或被旧请求覆盖；文件管理页使用统一搜索、列表、分页和弹框骨架；自动化门禁及真实浏览器验收通过。
- 处理范围：`FilePreviewPanel` 异步状态与单测、文件管理页调用和页面骨架、文件模块与业务指南说明、验收证据和 PR 交付记录。
- 不处理范围：后端 API、数据库结构、文件权限和租户边界、文件存储实现、外部文档预览服务实现。

## 3. 可观察系统要求

| ID      | 参与者或入口         | 输入或前置条件                | 预期行为                                                                     | 失败语义                                     | 验收标准                                               |
| ------- | -------------------- | ----------------------------- | ---------------------------------------------------------------------------- | -------------------------------------------- | ------------------------------------------------------ |
| REQ-001 | 文件管理页图片预览   | 已登录且列表存在可预览图片    | 打开预览后在元数据和内容完成前显示 Element Plus 默认 loading，完成后展示图片 | 任一必要请求失败时结束 loading 并展示失败态  | 单测覆盖 loading/失败，真实浏览器捕获 spinner 与完成态 |
| REQ-002 | 文件预览组件生命周期 | 连续切换文件或关闭弹框        | 仅最后一次请求可以更新 UI，关闭时释放对象 URL                                | 旧请求不得覆盖新预览，不得保留已失效对象 URL | 组件单测、构建和浏览器关闭重开验证通过                 |
| REQ-003 | 文件管理页           | 进入 `#/file/files`           | 搜索、目录树、功能操作、表格和分页保持可用，标准区域使用公共页面骨架         | 页面基线不合规时前端 PR 门禁失败             | 页面基线 checker 通过，桌面布局和核心交互无回归        |
| REQ-004 | 目录与预览弹框       | 打开新建/重命名目录或文件预览 | 使用 `MangoDialog` 的统一标题、内容滚动区、footer 和关闭操作                 | 弹框无法打开、关闭或内容溢出时验收失败       | 代码基线与真实浏览器交互验证通过                       |

## 4. 技术决定

| ID      | 对应要求         | 接口/数据/权限/兼容性决定                                                                                                                      | 影响路径                                                     | 回滚方式                                                       |
| ------- | ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ | -------------------------------------------------------------- |
| DEC-001 | REQ-001、REQ-002 | `FilePreviewPanel` 内聚 metadata、受保护内容和文档地址的 loading；使用请求序号防竞争，并在重载和卸载时清理对象 URL；不增加后端参数或接口       | `mango-ui/packages/file/src/components/FilePreviewPanel.vue` | 恢复为由页面预取 preview 并移除组件 loading 状态               |
| DEC-002 | REQ-003          | 保留目录树作为文件业务特有布局，搜索与列表分别使用 `MangoSearchPanel` 和 `MangoListPanel`，分页切换为公共组件真实 `page/limit/pagination` 合同 | `mango-ui/packages/file/src/views/files/index.vue`           | 回退该页面骨架迁移提交                                         |
| DEC-003 | REQ-003、REQ-004 | 两个原生 `ElDialog` 改用 `MangoDialog`；预览操作放入 `headerExtra`，由公共组件提供标准关闭按钮；保留原权限指令和弹框尺寸                       | `mango-ui/packages/file/src/views/files/index.vue`           | 回退到原生 Dialog 并重新登记可复核例外（如届时确有第三方合同） |
| DEC-004 | REQ-003          | 功能区按钮使用 `plain`；删除确认补齐“确认删除”和 danger 样式，不改变删除接口及二次确认边界                                                     | `mango-ui/packages/file/src/views/files/index.vue`           | 恢复原按钮展示，不影响数据合同                                 |

## 5. 实施清单

| ID      | 对应决定                  | 顺序 | 改动路径                                                               | 完成条件                                            |
| ------- | ------------------------- | ---: | ---------------------------------------------------------------------- | --------------------------------------------------- |
| IMP-001 | DEC-001                   |    1 | `mango-ui/packages/file/src/components/FilePreviewPanel.vue`、组件单测 | loading、失败、竞争和资源清理逻辑完成，模块测试通过 |
| IMP-002 | DEC-002、DEC-003、DEC-004 |    2 | `mango-ui/packages/file/src/views/files/index.vue`                     | 页面基线 checker 通过且业务入口保留                 |
| IMP-003 | 全部                      |    3 | 文件模块 README、组件 README、业务集成指南                             | loading、失败语义和不变边界与源码一致               |
| IMP-004 | 全部                      |    4 | `mango-docs/evidence/2026-08-11-file-preview-loading`、本交付记录      | 自动化与真实浏览器结果可追溯，无虚构通过项          |

## 6. 验收映射与结果

| 要求 ID                            | 验证方式                 | 命令或步骤                                                                                                      | 结果                                  | 证据                                                                                                             |
| ---------------------------------- | ------------------------ | --------------------------------------------------------------------------------------------------------------- | ------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| REQ-001、REQ-002                   | M10 组件单测             | `pnpm --filter @mango/file test`                                                                                | PASS（6 个测试文件，40 项测试）       | Vitest 终端输出；iframe 外链禁载诊断不影响退出码                                                                 |
| REQ-001、REQ-002、REQ-003、REQ-004 | M09 模块构建             | `pnpm --filter @mango/file build`                                                                               | PASS                                  | Vite production build 完成并生成 package types                                                                   |
| REQ-003、REQ-004                   | M09 页面基线             | `node mango-pmo/tools/check-frontend-page-baseline.mjs --base origin/main --head HEAD --frontend-root mango-ui` | PASS（1 个变更 view）                 | checker 终端输出                                                                                                 |
| REQ-001、REQ-003、REQ-004          | M11/M13 真实运行与浏览器 | 独立数据库启动前后端，访问 `#/file/files`，验证搜索、目录、弹框、预览 loading/完成态、关闭重开和 console        | PASS                                  | 后端 health `UP`；搜索/重置、`MangoDialog`、spinner、图片完成态与关闭重开断言通过；见 acceptance evidence 与截图 |
| 全部                               | M09/M16 静态与文档门禁   | lint、stylelint、admin style、Prettier、test quality、README/capability/evidence/risk 检查                      | PASS（风险检查在 PR body 更新后复跑） | 官方 ratchet、样式治理、格式、测试质量、README、capability 和 evidence 检查均通过                                |

## 7. 例外与剩余风险

- PMO Exceptions：None；文件管理页不使用页面基线例外。
- PDF、音视频和外部文档服务的浏览器矩阵不在本次图片 loading 的真实验收范围内，其组件渲染和请求分支由现有单测覆盖。
- 当前 in-app Browser 不提供调整 viewport 的接口；`width <= 960px` 响应式分支已保留并通过构建、Stylelint 和代码检查，但未在本次浏览器会话中切换真实窄屏，后续在可调 viewport 的 E2E 环境补充。
- 浏览器控制台无 error/Vue error；两条 `/file/files` 动态路由 warning 是路由表异步注入前的既有输出。
